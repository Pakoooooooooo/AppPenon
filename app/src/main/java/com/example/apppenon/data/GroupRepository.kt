package com.example.apppenon.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.apppenon.model.PenonGroup
import java.util.UUID

/**
 * Repository pour la gestion des groupes de pénons.
 * Les groupes sont identifiés par un UUID généré à la création.
 */
class GroupRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("group_data", Context.MODE_PRIVATE)

    private val KNOWN_GROUPS_KEY = "known_group_ids"

    fun getAllGroupIds(): Set<String> {
        val str = prefs.getString(KNOWN_GROUPS_KEY, "") ?: ""
        return if (str.isEmpty()) emptySet() else str.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun loadAllGroups(): List<PenonGroup> {
        return getAllGroupIds().map { id -> loadGroupById(id) }.sortedBy { it.groupName }
    }

    fun loadGroup(groupId: String): PenonGroup? {
        if (!getAllGroupIds().contains(groupId)) return null
        return loadGroupById(groupId)
    }

    private fun loadGroupById(id: String): PenonGroup = PenonGroup(
        groupId = id,
        groupName = prefs.getString("${id}_name", "Groupe") ?: "Groupe",
        useSound = prefs.getBoolean("${id}_use_sound", false),
        announceAttached = prefs.getBoolean("${id}_announce_attached", false),
        labelAttached = prefs.getString("${id}_label_attached", "attachée") ?: "attachée",
        labelDetached = prefs.getString("${id}_label_detached", "détachée") ?: "détachée",
        labelDetachedBabord = prefs.getString("${id}_label_detached_babord", "détachée bâbord") ?: "détachée bâbord",
        labelDetachedTribord = prefs.getString("${id}_label_detached_tribord", "détachée tribord") ?: "détachée tribord",
        soundAttachedPath = prefs.getString("${id}_sound_attached", "") ?: "",
        soundDetachedPath = prefs.getString("${id}_sound_detached", "") ?: "",
        soundDetachedBabordPath = prefs.getString("${id}_sound_detached_babord", "") ?: "",
        soundDetachedTribordPath = prefs.getString("${id}_sound_detached_tribord", "") ?: ""
    )

    fun saveGroup(group: PenonGroup) {
        val ids = getAllGroupIds().toMutableSet()
        ids.add(group.groupId)
        prefs.edit {
            putString(KNOWN_GROUPS_KEY, ids.joinToString(","))
            putString("${group.groupId}_name", group.groupName)
            putBoolean("${group.groupId}_use_sound", group.useSound)
            putBoolean("${group.groupId}_announce_attached", group.announceAttached)
            putString("${group.groupId}_label_attached", group.labelAttached)
            putString("${group.groupId}_label_detached", group.labelDetached)
            putString("${group.groupId}_label_detached_babord", group.labelDetachedBabord)
            putString("${group.groupId}_label_detached_tribord", group.labelDetachedTribord)
            putString("${group.groupId}_sound_attached", group.soundAttachedPath)
            putString("${group.groupId}_sound_detached", group.soundDetachedPath)
            putString("${group.groupId}_sound_detached_babord", group.soundDetachedBabordPath)
            putString("${group.groupId}_sound_detached_tribord", group.soundDetachedTribordPath)
        }
    }

    fun createGroup(name: String): PenonGroup {
        val group = PenonGroup(
            groupId = UUID.randomUUID().toString(),
            groupName = name
        )
        saveGroup(group)
        return group
    }

    fun deleteGroup(groupId: String) {
        val ids = getAllGroupIds().toMutableSet()
        ids.remove(groupId)
        prefs.edit {
            putString(KNOWN_GROUPS_KEY, ids.joinToString(","))
            remove("${groupId}_name")
            remove("${groupId}_use_sound")
            remove("${groupId}_announce_attached")
            remove("${groupId}_label_attached")
            remove("${groupId}_label_detached")
            remove("${groupId}_label_detached_babord")
            remove("${groupId}_label_detached_tribord")
            remove("${groupId}_sound_attached")
            remove("${groupId}_sound_detached")
            remove("${groupId}_sound_detached_babord")
            remove("${groupId}_sound_detached_tribord")
        }
    }
}
