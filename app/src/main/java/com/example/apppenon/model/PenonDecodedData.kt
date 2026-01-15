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
    fun toDisplayMap(): Map<String, String> {
        return mapOf(
            "Frame" to frameCount.toString(),
            "Type" to frameType.toString(),
            "Vbat (V)" to "%.3f".format(vbat),
            "MagZ Mean" to meanMagZ.toString(),
            "MagZ SD" to sdMagZ.toString(),
            "Acc Mean" to meanAcc.toString(),
            "Acc SD" to sdAcc.toString(),
            "Acc Max" to maxAcc.toString()
        )
    }
}
