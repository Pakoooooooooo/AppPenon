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

}