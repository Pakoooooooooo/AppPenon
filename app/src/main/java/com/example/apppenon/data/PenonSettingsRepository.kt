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

        penon.flowStateThreshold = sharedPref.getInt(
            "${penon.macAddress}_flowStateThreshold",
            penon.flowStateThreshold
        )
        penon.rssiLow = sharedPref.getInt(
            "${penon.macAddress}_rssiLow",
            penon.rssiLow
        )
        penon.rssiHigh = sharedPref.getInt(
            "${penon.macAddress}_rssiHigh",
            penon.rssiHigh
        )
        penon.sDFlowStateLow = sharedPref.getInt(
            "${penon.macAddress}_sDFlowStateLow",
            penon.sDFlowStateLow
        )
        penon.sDFlowStateHigh = sharedPref.getInt(
            "${penon.macAddress}_sDFlowStateHigh",
            penon.sDFlowStateHigh
        )
        penon.meanAccLow = sharedPref.getInt(
            "${penon.macAddress}_meanAccLow",
            penon.meanAccLow
        )
        penon.meanAccHigh = sharedPref.getInt(
            "${penon.macAddress}_meanAccHigh",
            penon.meanAccHigh
        )
        penon.sDAccLow = sharedPref.getInt(
            "${penon.macAddress}_sDAccLow",
            penon.sDAccLow
        )
        penon.sDAccHigh = sharedPref.getInt(
            "${penon.macAddress}_sDAccHigh",
            penon.sDAccHigh
        )
        penon.maxAccLow = sharedPref.getInt(
            "${penon.macAddress}_maxAccLow",
            penon.maxAccLow
        )
        penon.maxAccHigh = sharedPref.getInt(
            "${penon.macAddress}_maxAccHigh",
            penon.maxAccHigh
        )
        penon.vbatLow = sharedPref.getFloat(
            "${penon.macAddress}_vbatLow",
            penon.vbatLow.toFloat()
        ).toDouble()
        penon.vbatHigh = sharedPref.getFloat(
            "${penon.macAddress}_vbatHigh",
            penon.vbatHigh.toFloat()
        ).toDouble()
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
        penon.detachedThresh = sharedPref.getFloat(
            "${penon.macAddress}_detachedThresh",
            penon.detachedThresh.toFloat()
        ).toDouble()
        penon.count = sharedPref.getBoolean(
            "${penon.macAddress}_count",
            penon.count
        )
        penon.ids = sharedPref.getBoolean(
            "${penon.macAddress}_ids",
            penon.ids
        )

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
            putInt("${penon.macAddress}_flowStateThreshold", penon.flowStateThreshold)
            putInt("${penon.macAddress}_rssiLow", penon.rssiLow)
            putInt("${penon.macAddress}_rssiHigh", penon.rssiHigh)
            putInt("${penon.macAddress}_sDFlowStateLow", penon.sDFlowStateLow)
            putInt("${penon.macAddress}_sDFlowStateHigh", penon.sDFlowStateHigh)
            putInt("${penon.macAddress}_meanAccLow", penon.meanAccLow)
            putInt("${penon.macAddress}_meanAccHigh", penon.meanAccHigh)
            putInt("${penon.macAddress}_sDAccLow", penon.sDAccLow)
            putInt("${penon.macAddress}_sDAccHigh", penon.sDAccHigh)
            putInt("${penon.macAddress}_maxAccLow", penon.maxAccLow)
            putInt("${penon.macAddress}_maxAccHigh", penon.maxAccHigh)
            putFloat("${penon.macAddress}_vbatLow", penon.vbatLow.toFloat())
            putFloat("${penon.macAddress}_vbatHigh", penon.vbatHigh.toFloat())
            putInt("${penon.macAddress}_timeline", penon.timeline)
            putBoolean("${penon.macAddress}_rssi", penon.rssi)
            putBoolean("${penon.macAddress}_flowState", penon.flowState)
            putBoolean("${penon.macAddress}_sDFlowState", penon.sDFlowState)
            putBoolean("${penon.macAddress}_meanAcc", penon.meanAcc)
            putBoolean("${penon.macAddress}_sDAcc", penon.sDAcc)
            putBoolean("${penon.macAddress}_maxAcc", penon.maxAcc)
            putBoolean("${penon.macAddress}_vbat", penon.vbat)
            putBoolean("${penon.macAddress}_detached", penon.detached)
            putFloat("${penon.macAddress}_detachedThresh", penon.detachedThresh.toFloat())
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