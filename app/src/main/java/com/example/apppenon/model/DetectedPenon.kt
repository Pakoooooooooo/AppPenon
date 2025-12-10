package com.example.apppenon.model

data class DetectedPenon(
    val macAddress: String,
    var name: String = "Penon",
    var rssi: Int = 0,
    var frameCount: Int = 0,
    var battery: Float = 0f,
    var flowState: Int = 0,
    var lastUpdate: Long = System.currentTimeMillis(),
    var rawHexData: ByteArray = byteArrayOf()  // ✅ Ajout des données brutes du paquet
) {
    // Override equals et hashCode pour ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DetectedPenon) return false

        if (macAddress != other.macAddress) return false
        if (name != other.name) return false
        if (rssi != other.rssi) return false
        if (frameCount != other.frameCount) return false
        if (battery != other.battery) return false
        if (flowState != other.flowState) return false
        if (lastUpdate != other.lastUpdate) return false
        if (!rawHexData.contentEquals(other.rawHexData)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = macAddress.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + rssi
        result = 31 * result + frameCount
        result = 31 * result + battery.hashCode()
        result = 31 * result + flowState
        result = 31 * result + lastUpdate.hashCode()
        result = 31 * result + rawHexData.contentHashCode()
        return result
    }
}