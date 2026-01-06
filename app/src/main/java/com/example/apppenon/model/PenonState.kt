package com.example.apppenon.model

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
        if (rawData.size < 15) return

        val buffer = ByteBuffer.wrap(rawData)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        this.frame_cnt = buffer.int.toLong() and 0xFFFFFFFFL
        this.frame_type = buffer.get().toInt() and 0xFF
        this.vbat = buffer.short.toDouble()
        this.avr_mag_z = buffer.short.toDouble()
        this.sd_mag_z = buffer.short.toDouble()
        this.avr_acc = buffer.short.toDouble()
        this.sd_acc = buffer.short.toDouble()
        this.max_acc = buffer.short.toDouble()
    }
}