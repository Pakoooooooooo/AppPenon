package com.example.apppenon.model

object AppData {
    var mode = 0
    val modes = listOf(
        "Standard",
        "Developpeur")

    fun nextMode() {
        mode = (mode + 1) % modes.size
    }

    var rec = false

    var devices = mutableListOf(
        Penon(
            penonName = "Penon1 (Babord)",
            macAdress = "AA:BB:CC:DD:EE:01",
            rssi = true,
            rssiLow = -90,
            rssiHigh = -20,
            flowState = true,
            flowStateLow = 500,
            flowStateHigh = 800,
            sDFlowState = true,
            sDFlowStateLow = 100,
            sDFlowStateHigh = 800,
            detachedThresh = 100.0
        ),
        Penon(
            penonName = "Penon2 (Tribord)",
            macAdress = "AA:BB:CC:DD:EE:02",
            rssi = true,
            rssiLow = -90,
            rssiHigh = -20,
            flowState = true,
            flowStateLow = 500,
            flowStateHigh = 800,
            sDFlowState = true,
            sDFlowStateLow = 100,
            sDFlowStateHigh = 800,
            detachedThresh = 100.0
        )
    )

    fun checkInList (m: String): Boolean {
        for (device in devices) {
            if (device.macAdress == m) {
                return true
            }
        }
        return false
    }

    fun getPenon(mac: String): Penon? {
        for (device in devices) {
            if (device.macAdress == mac) {
                return device
            }
        }
        return null
    }
}