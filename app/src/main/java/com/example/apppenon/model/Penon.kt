package com.example.apppenon.model
import java.io.Serializable

data class Penon (
    var penonName: String = "penon",
    var macAddress: String = "00:00:00:00:00:00",
    var rssi: Boolean = false,
    var flowState: Boolean = false,
    var sDFlowState: Boolean = false,
    var meanAcc: Boolean = false,
    var sDAcc: Boolean = false,
    var maxAcc: Boolean = false,
    var vbat: Boolean = false,
    var detached: Boolean = false,
    var editAttachedThreshold: Int = 500,
    var count: Boolean = true,
    var ids: Boolean = false,
    var timeline: Int = 0,
    var avrMagZ: Boolean = true,
    var avrAvrMagZ: Boolean = true,

    // État du Penon pour les notifications vocales
    @Transient var lastAttachedState: Boolean? = null,

    var state: PenonState = PenonState()
): Serializable