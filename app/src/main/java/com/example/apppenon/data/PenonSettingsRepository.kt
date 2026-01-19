package com.example.apppenon.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.apppenon.model.Penon
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

/**
 * Repository centralisé pour la gestion des paramètres Penon.
 *
 * ✅ Support d'un nombre dynamique de Penons
 * ✅ Identifiés par leur adresse MAC
 * ✅ Source unique de vérité (SharedPreferences + In-Memory)
 */
class PenonSettingsRepository(private val context: Context) {

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("penon_data", Context.MODE_PRIVATE)

    private val TAG = "PenonSettingsRepo"

    // Map de StateFlow pour chaque Penon identifié par sa MAC
    private val penonFlowMap = mutableMapOf<String, MutableStateFlow<Penon?>>()

    // Clé pour stocker la liste des MACs connues
    private val KNOWN_MACS_KEY = "known_mac_addresses"

    /**
     * ✅ Récupère toutes les adresses MAC connues
     */
    fun getAllKnownMacAddresses(): Set<String> {
        val macsString = sharedPref.getString(KNOWN_MACS_KEY, "") ?: ""
        return if (macsString.isEmpty()) {
            emptySet()
        } else {
            macsString.split(",").toSet()
        }
    }

    /**
     * ✅ Ajoute une MAC à la liste des MACs connues
     */
    private fun addKnownMacAddress(macAddress: String) {
        val currentMacs = getAllKnownMacAddresses().toMutableSet()
        currentMacs.add(macAddress)
        sharedPref.edit().putString(KNOWN_MACS_KEY, currentMacs.joinToString(",")).apply()
    }

    /**
     * ✅ Obtient le StateFlow pour un Penon spécifique (par MAC)
     */
    fun getPenonFlow(macAddress: String): StateFlow<Penon?> {
        if (!penonFlowMap.containsKey(macAddress)) {
            penonFlowMap[macAddress] = MutableStateFlow(null)
        }
        return penonFlowMap[macAddress]!!.asStateFlow()
    }

    /**
     * ✅ Observer tous les Penons en temps réel
     */
    fun observeAllPenons(): StateFlow<Map<String, Penon?>> {
        val allFlows = penonFlowMap.values.toList()
        val combinedFlow = MutableStateFlow<Map<String, Penon?>>(emptyMap())

        if (allFlows.isEmpty()) {
            return combinedFlow.asStateFlow()
        }

        // Combiner tous les flows
        GlobalScope.launch {
            combine(allFlows) { penons ->
                penonFlowMap.keys.zip(penons).toMap()
            }.collect { combinedFlow.value = it }
        }

        return combinedFlow.asStateFlow()
    }

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
        penon.rssi = sharedPref.getBoolean(
            "${penon.macAddress}_rssi",
            penon.rssi
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
        penon.labelAttache = sharedPref.getString(
            "${penon.macAddress}_labelAttache",
            penon.labelAttache
        ) ?: penon.labelAttache
        penon.labelDetache = sharedPref.getString(
            "${penon.macAddress}_labelDetache",
            penon.labelDetache
        ) ?: penon.labelDetache
        penon.useSound = sharedPref.getBoolean(
            "${penon.macAddress}_useSound",
            penon.useSound
        )
        penon.soundAttachePath = sharedPref.getString(
            "${penon.macAddress}_soundAttachePath",
            penon.soundAttachePath
        ) ?: penon.soundAttachePath
        penon.soundDetachePath = sharedPref.getString(
            "${penon.macAddress}_soundDetachePath",
            penon.soundDetachePath
        ) ?: penon.soundDetachePath

        // Mettre à jour le StateFlow correspondant
        updateStateFlow(penon)

        Log.d(TAG, "✅ Penon chargé: ${penon.penonName} (MAC: ${penon.macAddress})")
    }

    /**
     * ✅ Sauvegarde un Penon dans SharedPreferences ET notifie les observateurs
     */
    fun savePenon(penon: Penon) {
        Log.d(TAG, "💾 Sauvegarde Penon: ${penon.penonName}")

        // Ajouter la MAC à la liste des MACs connues
        addKnownMacAddress(penon.macAddress)

        sharedPref.edit().apply {
            putString("${penon.macAddress}_penonName", penon.penonName)
            putInt("${penon.macAddress}_timeline", penon.timeline)
            putBoolean("${penon.macAddress}_rssi", penon.rssi)
            putBoolean("${penon.macAddress}_flowState", penon.flowState)
            putBoolean("${penon.macAddress}_sDFlowState", penon.sDFlowState)
            putBoolean("${penon.macAddress}_meanAcc", penon.meanAcc)
            putBoolean("${penon.macAddress}_sDAcc", penon.sDAcc)
            putBoolean("${penon.macAddress}_maxAcc", penon.maxAcc)
            putBoolean("${penon.macAddress}_vbat", penon.vbat)
            putBoolean("${penon.macAddress}_detached", penon.detached)
            putString("${penon.macAddress}_labelAttache", penon.labelAttache)
            putString("${penon.macAddress}_labelDetache", penon.labelDetache)
            putBoolean("${penon.macAddress}_useSound", penon.useSound)
            putString("${penon.macAddress}_soundAttachePath", penon.soundAttachePath)
            putString("${penon.macAddress}_soundDetachePath", penon.soundDetachePath)
            putBoolean("${penon.macAddress}_count", penon.count)
            putBoolean("${penon.macAddress}_ids", penon.ids)
            apply()
        }

        // Mettre à jour le StateFlow pour notifier les observateurs
        updateStateFlow(penon)

        Log.d(TAG, "✅ Penon sauvegardé et notifié")
    }

    /**
     * ✅ Met à jour le StateFlow du Penon correspondant
     */
    private fun updateStateFlow(penon: Penon) {
        val macAddress = penon.macAddress

        // Créer le StateFlow si nécessaire
        if (!penonFlowMap.containsKey(macAddress)) {
            penonFlowMap[macAddress] = MutableStateFlow(null)
        }

        // Mettre à jour la valeur
        penonFlowMap[macAddress]?.value = penon.copy()
        Log.d(TAG, "🔄 StateFlow Penon (MAC: $macAddress) mis à jour")
    }

    /**
     * ✅ Récupère les settings courants d'un Penon (snapshot)
     */
    fun getPenonSettings(macAddress: String): Penon? {
        if (macAddress.isEmpty()) return null

        val penon = Penon(macAddress = macAddress)
        loadPenon(penon)
        return penon
    }
}