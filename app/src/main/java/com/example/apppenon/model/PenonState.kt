package com.example.apppenon.model

import android.content.ContentValues.TAG
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PenonState {
    var frame_cnt: Long = 0
    var frame_type: Int = 0
    var vbat: Double = 0.0
    var avr_mag_z: Double = 0.0
    var sd_mag_z: Double = 0.0
    var avr_acc: Double = 0.0
    var sd_acc: Double = 0.0
    var max_acc: Double = 0.0

    /**
     * Met à jour les propriétés à partir d'une trame brute de 15 octets (Little Endian)
     */
    fun updateFromRawData(rawData: ByteArray) {
        Log.d(TAG, "RAW DATA HEX: ${rawData.joinToString("") { "%02x".format(it) }}")
        Log.d(TAG, "Taille réelle: ${rawData.size} octets")

        if (rawData.size < 40) return

        val buffer = ByteBuffer.wrap(rawData)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // Cherchons le marqueur 0x15ff (Manufacturer Specific Data)
        var startPos = -1
        for (i in 0 until rawData.size - 1) {
            if (rawData[i] == 0x15.toByte() && rawData[i + 1] == 0xff.toByte()) {
                startPos = i + 2 // ⚠️ Juste après 15ff, PAS +4 !
                break
            }
        }

        if (startPos == -1) {
            Log.e(TAG, "Marqueur manufacturer data (15ff) non trouvé")
            return
        }

        Log.d(TAG, "STARTPOS: $startPos")

        val fcBytes = rawData.slice(startPos until minOf(startPos + 4, rawData.size))
        Log.d(TAG, "Frame count bytes (position $startPos): ${fcBytes.joinToString(" ") { "%02x".format(it) }}")

        buffer.position(startPos)

        this.frame_cnt = buffer.int.toLong() and 0xFFFFFFFFL // uint32_t - 4 octets
        Log.d(TAG, "Frame count décodé: $frame_cnt")

        this.frame_type = buffer.get().toInt() and 0xFF      // uint8_t - 1 octet
        this.vbat = buffer.short.toDouble()                  // int16_t - 2 octets
        this.avr_mag_z = buffer.short.toDouble()             // int16_t - 2 octets
        this.sd_mag_z = buffer.short.toDouble()              // int16_t - 2 octets
        this.avr_acc = buffer.short.toDouble()               // int16_t - 2 octets
        this.sd_acc = buffer.short.toDouble()                // int16_t - 2 octets
        this.max_acc = buffer.short.toDouble()               // int16_t - 2 octets

        Log.d(TAG, "✅ Frame: $frame_cnt, Type: $frame_type, Vbat: $vbat, MagZ: $avr_mag_z")
    }
    fun getFlowState(): Double {
        return this.avr_mag_z
    }
}