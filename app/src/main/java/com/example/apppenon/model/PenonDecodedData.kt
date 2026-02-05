package com.example.apppenon.model

/**
 * Représente les données décodées reçues d'un Penon.
 */
data class PenonDecodedData(
    val frameCount: Long = 0,
    val frameType: Int = 0,
    val vbat: Double = 0.0,
    val meanMagZ: Int = 0,
    val sdMagZ: Int = 0,
    val meanAcc: Int = 0,
    val sdAcc: Int = 0,
    val maxAcc: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
}
