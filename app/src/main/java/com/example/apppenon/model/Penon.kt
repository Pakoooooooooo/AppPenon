package com.example.apppenon.model
import java.io.Serializable

data class Penon (
    //Variables modifiables par l'utilisateur
    var penonName: String = "penon",
    var macAddress: String = "00:00:00:00:00:00",

    // Appartenance à un groupe
    var groupId: String = "",        // vide = pénon non affecté à un groupe
    var side: Side = Side.NONE,      // côté dans le groupe (bâbord / tribord)
    var flowState: Boolean = false,
    var sDFlowState: Boolean = false,
    var meanAcc: Boolean = false,
    var sDAcc: Boolean = false,
    var maxAcc: Boolean = false,
    var vbat: Boolean = false,
    var detached: Boolean = false,
    var editAttachedThreshold: Int = 3500,
    var count: Boolean = true,
    var ids: Boolean = false,
    var timeline: Int = 0,
    var avrMagZ: Boolean = true,
    var avrAvrMagZ: Boolean = true,

    var state: PenonState = PenonState()
): Serializable