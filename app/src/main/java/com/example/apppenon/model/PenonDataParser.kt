package com.example.apppenon.model

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Gère le parsing et le décodage des données brutes Penon BLE.
 */
class PenonDataParser {

    private val TAG = "eTT-SAIL-BLE"

    /**
     * Décode les données du Penon et retourne un objet PenonDecodedData.
     */
    fun decodePenonData(data: ByteArray): PenonDecodedData? {
        return try {
            if (data.size < 18) return null

            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(0)

            val frameCount = buffer.int.toLong() and 0xFFFFFFFFL
            val frameType = buffer.get().toInt() and 0xFF
            buffer.get() // padding (alignement struct C)
            val vbat = buffer.short.toInt()
            val meanMagZ = buffer.short.toInt()
            val sdMagZ = buffer.short.toInt()
            val meanAcc = buffer.short.toInt()
            val sdAcc = buffer.short.toInt()
            val maxAcc = buffer.short.toInt()

            PenonDecodedData(
                frameCount = frameCount,
                frameType = frameType,
                vbat = vbat / 100.0,
                meanMagZ = meanMagZ,
                sdMagZ = sdMagZ,
                meanAcc = meanAcc,
                sdAcc = sdAcc,
                maxAcc = maxAcc
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur décodage", e)
            null
        }
    }

    fun resetFrameCounters() {
        // conservé pour compatibilité avec BLEScanManager.startScanning()
    }
}
