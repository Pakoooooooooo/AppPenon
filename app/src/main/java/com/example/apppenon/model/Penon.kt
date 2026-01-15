package com.example.apppenon.model
import java.io.Serializable

data class Penon (
    var penonName: String = "penon",
    var macAddress: String = "00:00:00:00:00:00",
    var rssi: Boolean = false,
    var rssiLow: Int = -75,
    var rssiHigh: Int = -50,
    var flowState: Boolean = false,
    var flowStateThreshold: Int = 500,
    var sDFlowState: Boolean = false,
    var sDFlowStateLow: Int = 200,
    var sDFlowStateHigh: Int = 800,
    var meanAcc: Boolean = false,
    var meanAccLow: Int = 900,
    var meanAccHigh: Int = 1200,
    var sDAcc: Boolean = false,
    var sDAccLow: Int = 6000,
    var sDAccHigh: Int = 8000,
    var maxAcc: Boolean = false,
    var maxAccLow: Int = 1000,
    var maxAccHigh: Int = 2000,
    var vbat: Boolean = false,
    var vbatLow: Double = 2.4,
    var vbatHigh: Double = 2.8,
    var detached: Boolean = false,
    var detachedThresh: Double = 10.0,
    var count: Boolean = false,
    var ids: Boolean = false,
    var timeline: Int = 0,

    // État du Penon pour les notifications vocales
    @Transient var lastAttachedState: Boolean? = null,

    var state: PenonState = PenonState()
): Serializable