package com.example.apppenon.model
import java.io.Serializable

data class Penon (
    //Variables modifiables par l'utilisateur
    var penonName: String = "penon",
    var macAddress: String = "00:00:00:00:00:00",
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

    // Labels personnalisés pour les annonces vocales
    var labelAttache: String = "attaché",
    var labelDetache: String = "détaché",

    // Configuration des annonces (vocal ou son)
    var useSound: Boolean = false, // true = son, false = vocal
    var soundAttachePath: String = "", // Chemin vers le fichier son pour "attaché"
    var soundDetachePath: String = "", // Chemin vers le fichier son pour "détaché"

    // État du Penon pour les notifications vocales
    @Transient var lastAttachedState: Boolean? = null,

    var state: PenonState = PenonState()
): Serializable