package com.example.apppenon.model

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class PenonState {
    private val TAG = "PenonState"
    var frame_cnt: Long = 0
    var frame_type: Int = 0
    var vbat: Double = 0.0
    var avr_mag_z: Double = 0.0
    var sd_mag_z: Double = 0.0
    var avr_acc: Double = 0.0
    var sd_acc: Double = 0.0
    var max_acc: Double = 0.0

    /**
     * Met à jour les propriétés à partir d'une trame brute BLE (Little Endian).
     *
     * Structure après marqueur manufacturer data (0xFF) :
     *   uint32_t frame_cnt     (4 bytes)
     *   uint8_t  frame_type    (1 byte)
     *   uint8_t  _padding      (1 byte, alignement compilateur C)
     *   int16_t  Vbat          (2 bytes, centivolts)
     *   int16_t  avr_mag_z     (2 bytes, Tesla × 10⁻³)
     *   int16_t  sd_mag_z      (2 bytes, Tesla × 10⁻³)
     *   int16_t  avr_acc       (2 bytes, m.s⁻² × 10⁻³)
     *   int16_t  sd_acc        (2 bytes, m.s⁻² × 10⁻³)
     *   int16_t  max_acc       (2 bytes, m.s⁻² × 10⁻³)
     */
    fun updateFromRawData(rawData: ByteArray) {
        Log.d(TAG, "RAW DATA HEX: ${rawData.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "Taille réelle: ${rawData.size} octets")

        if (rawData.size < 40) return

        val buffer = ByteBuffer.wrap(rawData)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // Chercher le marqueur manufacturer data (longueur + 0xFF)
        var startPos = -1
        for (i in 0 until rawData.size - 1) {
            if (rawData[i + 1] == 0xff.toByte()) {
                startPos = i + 2
                break
            }
        }

        if (startPos == -1) {
            Log.e(TAG, "Marqueur manufacturer data (0xFF) non trouvé")
            return
        }

        buffer.position(startPos)

        // uint32_t frame_cnt
        this.frame_cnt = buffer.int.toLong() and 0xFFFFFFFFL

        // uint8_t frame_type
        this.frame_type = buffer.get().toInt() and 0xFF

        // uint8_t padding (alignement struct C)
        buffer.get()

        // int16_t Vbat (centivolts → Volts)
        this.vbat = buffer.short.toDouble() / 100.0

        // int16_t avr_mag_z (Tesla × 10⁻³)
        this.avr_mag_z = buffer.short.toDouble()

        // int16_t sd_mag_z (Tesla × 10⁻³)
        this.sd_mag_z = buffer.short.toDouble()

        // int16_t avr_acc (m.s⁻² × 10⁻³)
        this.avr_acc = buffer.short.toDouble()
        // int16_t sd_acc (m.s⁻² × 10⁻³)
        this.sd_acc = buffer.short.toDouble()
        // int16_t max_acc (m.s⁻² × 10⁻³)
        this.max_acc = buffer.short.toDouble()

        Log.d(TAG, "✅ Frame: $frame_cnt, Type: $frame_type, Vbat: $vbat V, MagZ: $avr_mag_z mT×10⁻³")
    }

    fun getFlowState(): Double {
        return abs(this.avr_mag_z)
    }
}