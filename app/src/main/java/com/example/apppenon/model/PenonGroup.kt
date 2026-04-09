package com.example.apppenon.model

import java.io.Serializable
import kotlin.math.abs

data class PenonGroup(
    val groupId: String,
    var groupName: String,

    // Annonces vocales / sons
    var useSound: Boolean = false,
    var announceAttached: Boolean = false,
    var labelAttached: String = "attachée",
    var labelDetached: String = "détachée",
    var labelDetachedBabord: String = "détachée bâbord",
    var labelDetachedTribord: String = "détachée tribord",
    var soundAttachedPath: String = "",
    var soundDetachedPath: String = "",
    var soundDetachedBabordPath: String = "",
    var soundDetachedTribordPath: String = "",

    // Non persisté : état de la dernière annonce pour détecter les changements
    @Transient var lastAnnouncedState: String? = null
) : Serializable {

    fun announcementText(state: String): String = when (state) {
        "attached"          -> "$groupName $labelAttached"
        "detached"          -> "$groupName $labelDetached"
        "detached_babord"   -> "$groupName $labelDetachedBabord"
        "detached_tribord"  -> "$groupName $labelDetachedTribord"
        else -> ""
    }

    fun soundPathForState(state: String): String = when (state) {
        "attached"          -> soundAttachedPath
        "detached"          -> soundDetachedPath
        "detached_babord"   -> soundDetachedBabordPath
        "detached_tribord"  -> soundDetachedTribordPath
        else -> ""
    }

    companion object {
        /**
         * Calcule l'état d'un groupe à partir de ses pénons.
         * Règle majoritaire : K * 2 >= N → côté détaché (favorise détaché pour nombres pairs)
         *
         * États possibles : "attached", "detached", "detached_babord", "detached_tribord"
         */
        fun computeState(penons: List<Penon>): String {
            val babord = penons.filter { it.side == Side.BABORD }
            val tribord = penons.filter { it.side == Side.TRIBORD }

            fun isSideDetached(ps: List<Penon>): Boolean {
                if (ps.isEmpty()) return false
                val detached = ps.count { abs(it.state.avr_avr_mag_z) < it.editAttachedThreshold }
                return detached * 2 >= ps.size
            }

            val hasBabord = babord.isNotEmpty()
            val hasTribord = tribord.isNotEmpty()

            if (!hasBabord && !hasTribord) return "attached"

            val babordDetached = isSideDetached(babord)
            val tribordDetached = isSideDetached(tribord)

            return when {
                hasBabord && hasTribord -> when {
                    babordDetached && tribordDetached -> "detached"
                    babordDetached -> "detached_babord"
                    tribordDetached -> "detached_tribord"
                    else -> "attached"
                }
                hasBabord -> if (babordDetached) "detached_babord" else "attached"
                else -> if (tribordDetached) "detached_tribord" else "attached"
            }
        }

        fun stateLabel(state: String): String = when (state) {
            "attached" -> "Voile attachée"
            "detached" -> "Voile détachée"
            "detached_babord" -> "Voile détachée bâbord"
            "detached_tribord" -> "Voile détachée tribord"
            else -> "Inconnu"
        }

        fun stateColor(state: String): Int = when (state) {
            "attached" -> 0xFF4CAF50.toInt()
            else -> 0xFFE91E63.toInt()
        }

        // announcementText est maintenant une méthode d'instance (utilise les labels personnalisés)
    }
}
