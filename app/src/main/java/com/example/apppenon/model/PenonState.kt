package com.example.apppenon.model

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class PenonState {
    // Variables contenant les données reçues par bluetooth
    private val TAG = "PenonState"
    var frame_cnt: Long = 0
    var frame_type: Int = 0
    var vbat: Double = 0.0
    var sd_mag_z: Double = 0.0
    var avr_acc: Double = 0.0
    var sd_acc: Double = 0.0
    var max_acc: Double = 0.0
    var time: Int = 10
    var avr_mag_z: MutableList<Double> = mutableListOf()
    var avr_avr_mag_z: Double = 0.0


    /**
     * Met à jour les propriétés à partir d'une trame brute de 15 octets (Little Endian)
     */
    fun updateFromRawData(rawData: ByteArray) {
        Log.d(TAG, "RAW DATA HEX: ${rawData.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "Taille réelle: ${rawData.size} octets")

        if (rawData.size < 40) return

        val buffer = ByteBuffer.wrap(rawData)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        var startPos = -1
        for (i in 0 until rawData.size - 1) {
            if (rawData[i + 1] == 0xff.toByte()) {
                startPos = i + 2
                break
            }
        }

        if (startPos == -1) {
            Log.e(TAG, "Marqueur manufacturer data (15ff) non trouvé")
            return
        }

        buffer.position(startPos)

        // uint32_t
        this.frame_cnt = buffer.int.toLong() and 0xFFFFFFFFL

        // uint8_t
        this.frame_type = buffer.get().toInt() and 0xFF

        // padding (alignement struct C)
        buffer.get()

        // int16_t - Voltage en cV, convertir en V
        this.vbat = buffer.short.toDouble() / 100.0

        // int16_t - Déjà en mT×10⁻³ selon la doc
        this.avr_mag_z.add(buffer.short.toDouble())

        // int16_t
        this.sd_mag_z = buffer.short.toDouble()

        // int16_t - Accélération en mg (milli-g)
        this.avr_acc = buffer.short.toDouble()
        this.sd_acc = buffer.short.toDouble()
        this.max_acc = buffer.short.toDouble()

        this.avr_avr_mag_z = avr_mag_z.filter { abs(it - avr_mag_z.last()) < this.time }.average()

        Log.d(TAG, "✅ Frame: $frame_cnt, Type: $frame_type, Vbat: $vbat V, MagZ: $avr_mag_z mT×10⁻³")
    }
}