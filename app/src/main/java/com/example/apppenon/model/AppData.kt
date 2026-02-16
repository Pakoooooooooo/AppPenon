package com.example.apppenon.model

object AppData {
    var mode = 0
    var rec = false
    var muteTimeSeconds = 0 // 0 = annonce immediate, >0 = delai entre annonces
    val modes = listOf(
        "Standard",
        "Developpeur"
    )
    fun nextMode() {
        mode = (mode + 1) % modes.size
    }
}