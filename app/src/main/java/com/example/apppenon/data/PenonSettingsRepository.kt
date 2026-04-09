package com.example.apppenon.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.apppenon.model.Penon
import com.example.apppenon.model.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit

/**
 * Repository centralisé pour la gestion des paramètres Penon.
 *
 * ✅ Support d'un nombre dynamique de Penons
 * ✅ Identifiés par leur adresse MAC
 * ✅ Source unique de vérité (SharedPreferences + In-Memory)
 */
class PenonSettingsRepository(context: Context) {

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("penon_data", Context.MODE_PRIVATE)

    private val TAG = "PenonSettingsRepo"

    // Flow global : toujours à jour quelle que soit la date d'abonnement.
    // Mis à jour à chaque load/save/delete — pas de snapshot figé.
    private val _allPenonsFlow = MutableStateFlow<Map<String, Penon?>>(emptyMap())

    // Cache in-memory des dernières valeurs par MAC
    private val penonCache = mutableMapOf<String, Penon?>()

    // Clé pour stocker la liste des MACs connues
    private val KNOWN_MACS_KEY = "known_mac_addresses"

    /**
     * ✅ Récupère toutes les adresses MAC connues
     */
    fun getAllKnownMacAddresses(): Set<String> {
        val macsString = sharedPref.getString(KNOWN_MACS_KEY, "") ?: ""
        return if (macsString.isEmpty()) emptySet()
        else macsString.split(",").toSet()
    }

    /**
     * ✅ Ajoute une MAC à la liste des MACs connues
     */
    private fun addKnownMacAddress(macAddress: String) {
        val currentMacs = getAllKnownMacAddresses().toMutableSet()
        currentMacs.add(macAddress)
        sharedPref.edit { putString(KNOWN_MACS_KEY, currentMacs.joinToString(",")) }
    }

    /**
     * ✅ Observer tous les Penons en temps réel.
     * Le flow inclut automatiquement tout pénon ajouté ou supprimé après l'abonnement.
     */
    fun observeAllPenons(): StateFlow<Map<String, Penon?>> = _allPenonsFlow.asStateFlow()

    /**
     * ✅ Charge un Penon depuis SharedPreferences
     */
    fun loadPenon(penon: Penon) {
        Log.d(TAG, "📄 Chargement Penon: ${penon.macAddress}")

        penon.penonName = sharedPref.getString(
            "${penon.macAddress}_penonName",
            penon.penonName
        ) ?: penon.penonName

        penon.editAttachedThreshold = sharedPref.getInt(
            "${penon.macAddress}_editAttachedThreshold",
            penon.editAttachedThreshold
        )
        penon.timeline = sharedPref.getInt(
            "${penon.macAddress}_timeline",
            penon.timeline
        )
        penon.flowState = sharedPref.getBoolean(
            "${penon.macAddress}_flowState",
            penon.flowState
        )
        penon.sDFlowState = sharedPref.getBoolean(
            "${penon.macAddress}_sDFlowState",
            penon.sDFlowState
        )
        penon.meanAcc = sharedPref.getBoolean(
            "${penon.macAddress}_meanAcc",
            penon.meanAcc
        )
        penon.sDAcc = sharedPref.getBoolean(
            "${penon.macAddress}_sDAcc",
            penon.sDAcc
        )
        penon.maxAcc = sharedPref.getBoolean(
            "${penon.macAddress}_maxAcc",
            penon.maxAcc
        )
        penon.vbat = sharedPref.getBoolean(
            "${penon.macAddress}_vbat",
            penon.vbat
        )
        penon.avrMagZ = sharedPref.getBoolean(
            "${penon.macAddress}_avrMagZ",
            penon.avrMagZ
        )
        penon.avrAvrMagZ = sharedPref.getBoolean(
            "${penon.macAddress}_avrAvrMagZ",
            penon.avrAvrMagZ
        )
        penon.detached = sharedPref.getBoolean(
            "${penon.macAddress}_detached",
            penon.detached
        )
        penon.count = sharedPref.getBoolean(
            "${penon.macAddress}_count",
            penon.count
        )
        penon.ids = sharedPref.getBoolean(
            "${penon.macAddress}_ids",
            penon.ids
        )
        penon.groupId = sharedPref.getString(
            "${penon.macAddress}_groupId",
            penon.groupId
        ) ?: penon.groupId
        penon.side = Side.valueOf(
            sharedPref.getString("${penon.macAddress}_side", Side.NONE.name) ?: Side.NONE.name
        )

        updateFlow(penon.macAddress, penon.copy())

        Log.d(TAG, "✅ Penon chargé: ${penon.penonName} (MAC: ${penon.macAddress})")
    }

    /**
     * ✅ Sauvegarde un Penon dans SharedPreferences ET notifie les observateurs
     */
    fun savePenon(penon: Penon) {
        Log.d(TAG, "💾 Sauvegarde Penon: ${penon.penonName}")

        addKnownMacAddress(penon.macAddress)

        sharedPref.edit().apply {
            putString("${penon.macAddress}_penonName", penon.penonName)
            putInt("${penon.macAddress}_editAttachedThreshold", penon.editAttachedThreshold)
            putInt("${penon.macAddress}_timeline", penon.timeline)
            putBoolean("${penon.macAddress}_flowState", penon.flowState)
            putBoolean("${penon.macAddress}_sDFlowState", penon.sDFlowState)
            putBoolean("${penon.macAddress}_meanAcc", penon.meanAcc)
            putBoolean("${penon.macAddress}_sDAcc", penon.sDAcc)
            putBoolean("${penon.macAddress}_maxAcc", penon.maxAcc)
            putBoolean("${penon.macAddress}_vbat", penon.vbat)
            putBoolean("${penon.macAddress}_avrMagZ", penon.avrMagZ)
            putBoolean("${penon.macAddress}_avrAvrMagZ", penon.avrAvrMagZ)
            putBoolean("${penon.macAddress}_detached", penon.detached)
            putBoolean("${penon.macAddress}_count", penon.count)
            putBoolean("${penon.macAddress}_ids", penon.ids)
            putString("${penon.macAddress}_groupId", penon.groupId)
            putString("${penon.macAddress}_side", penon.side.name)
            apply()
        }

        updateFlow(penon.macAddress, penon.copy())

        Log.d(TAG, "✅ Penon sauvegardé et notifié")
    }

    /**
     * ✅ Supprime toutes les données d'un Penon (SharedPreferences + Flow)
     */
    fun deletePenon(macAddress: String) {
        Log.d(TAG, "🗑️ Suppression Penon: $macAddress")

        val keys = listOf(
            "penonName", "editAttachedThreshold", "timeline",
            "flowState", "sDFlowState", "meanAcc", "sDAcc", "maxAcc",
            "vbat", "avrMagZ", "avrAvrMagZ", "detached", "count", "ids",
            "groupId", "side"
        )
        sharedPref.edit().apply {
            keys.forEach { key -> remove("${macAddress}_$key") }
            apply()
        }

        val currentMacs = getAllKnownMacAddresses().toMutableSet()
        currentMacs.remove(macAddress)
        sharedPref.edit { putString(KNOWN_MACS_KEY, currentMacs.joinToString(",")) }

        // Retirer du cache et notifier le flow
        penonCache.remove(macAddress)
        _allPenonsFlow.value = penonCache.toMap()

        Log.d(TAG, "✅ Penon supprimé: $macAddress")
    }

    /**
     * Met à jour le cache et émet une nouvelle valeur dans le flow global.
     */
    private fun updateFlow(macAddress: String, penon: Penon?) {
        penonCache[macAddress] = penon
        _allPenonsFlow.value = penonCache.toMap()
        Log.d(TAG, "🔄 Flow mis à jour (${penonCache.size} pénon(s))")
    }
}
