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
        return getAllGroupIds().map { id ->
            PenonGroup(
                groupId = id,
                groupName = prefs.getString("${id}_name", "Groupe") ?: "Groupe"
            )
        }.sortedBy { it.groupName }
    }

    fun loadGroup(groupId: String): PenonGroup? {
        if (!getAllGroupIds().contains(groupId)) return null
        return PenonGroup(
            groupId = groupId,
            groupName = prefs.getString("${groupId}_name", "Groupe") ?: "Groupe"
        )
    }

    fun saveGroup(group: PenonGroup) {
        val ids = getAllGroupIds().toMutableSet()
        ids.add(group.groupId)
        prefs.edit {
            putString(KNOWN_GROUPS_KEY, ids.joinToString(","))
            putString("${group.groupId}_name", group.groupName)
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
        }
    }
}
