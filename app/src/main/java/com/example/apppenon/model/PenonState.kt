package com.example.apppenon.model

import android.content.ContentValues.TAG
import android.util.Log
import android.widget.Toast
import java.lang.System.console
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

        // On vérifie qu'on a assez de données pour atteindre la fin de la structure
        if (rawData.size < 40) return

        val buffer = ByteBuffer.wrap(rawData)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // D'après votre HEX, la structure adv_mfg_data commence à l'index 25
        // (Juste après le Company ID 0500)
        buffer.position(25)

        this.frame_cnt = buffer.int.toLong() and 0xFFFFFFFFL // uint32_t [cite: 21]
        this.frame_type = buffer.get().toInt() and 0xFF      // uint8_t [cite: 22]

        // Pas de padding visible dans votre HEX entre frame_type et Vbat
        this.vbat = buffer.short.toDouble()                 // int16_t [cite: 23]

        // LECTURE DE mean_mag_z (avr_mag_z) [cite: 4, 18, 24]
        val meanMagZ = buffer.short.toDouble()
        this.avr_mag_z = meanMagZ
        Log.d(TAG, "avr_mag_z (mean_mag_z): $meanMagZ")

        this.sd_mag_z = buffer.short.toDouble()             // [cite: 25]
        this.avr_acc = buffer.short.toDouble()              // [cite: 26]
        this.sd_acc = buffer.short.toDouble()               // [cite: 27]
        this.max_acc = buffer.short.toDouble()              // [cite: 28]
    }

    fun getFlowState(): Double {
        return this.avr_acc
    }
}