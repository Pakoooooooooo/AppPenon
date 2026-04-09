package com.example.apppenon.model

enum class Side {
    NONE, BABORD, TRIBORD;

    fun label(): String = when (this) {
        NONE -> ""
        BABORD -> "Bâbord"
        TRIBORD -> "Tribord"
    }
}
