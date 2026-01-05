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
        Log.d(TAG, "📄 Chargement Penon: ${penon.macAdress}")

        penon.penonName = sharedPref.getString(
            "${penon.macAdress}_penonName",
            penon.penonName
        ) ?: penon.penonName

        penon.flowStateThreshold = sharedPref.getInt(
            "${penon.macAdress}_flowStateThreshold",
            penon.flowStateThreshold
        )
        penon.rssiLow = sharedPref.getInt(
            "${penon.macAdress}_rssiLow",
            penon.rssiLow
        )
        penon.rssiHigh = sharedPref.getInt(
            "${penon.macAdress}_rssiHigh",
            penon.rssiHigh
        )
        penon.sDFlowStateLow = sharedPref.getInt(
            "${penon.macAdress}_sDFlowStateLow",
            penon.sDFlowStateLow
        )
        penon.sDFlowStateHigh = sharedPref.getInt(
            "${penon.macAdress}_sDFlowStateHigh",
            penon.sDFlowStateHigh
        )
        penon.meanAccLow = sharedPref.getInt(
            "${penon.macAdress}_meanAccLow",
            penon.meanAccLow
        )
        penon.meanAccHigh = sharedPref.getInt(
            "${penon.macAdress}_meanAccHigh",
            penon.meanAccHigh
        )
        penon.sDAccLow = sharedPref.getInt(
            "${penon.macAdress}_sDAccLow",
            penon.sDAccLow
        )
        penon.sDAccHigh = sharedPref.getInt(
            "${penon.macAdress}_sDAccHigh",
            penon.sDAccHigh
        )
        penon.maxAccLow = sharedPref.getInt(
            "${penon.macAdress}_maxAccLow",
            penon.maxAccLow
        )
        penon.maxAccHigh = sharedPref.getInt(
            "${penon.macAdress}_maxAccHigh",
            penon.maxAccHigh
        )
        penon.vbatLow = sharedPref.getFloat(
            "${penon.macAdress}_vbatLow",
            penon.vbatLow.toFloat()
        ).toDouble()
        penon.vbatHigh = sharedPref.getFloat(
            "${penon.macAdress}_vbatHigh",
            penon.vbatHigh.toFloat()
        ).toDouble()
        penon.timeline = sharedPref.getInt(
            "${penon.macAdress}_timeline",
            penon.timeline
        )
        penon.rssi = sharedPref.getBoolean(
            "${penon.macAdress}_rssi",
            penon.rssi
        )
        penon.flowState = sharedPref.getBoolean(
            "${penon.macAdress}_flowState",
            penon.flowState
        )
        penon.sDFlowState = sharedPref.getBoolean(
            "${penon.macAdress}_sDFlowState",
            penon.sDFlowState
        )
        penon.meanAcc = sharedPref.getBoolean(
            "${penon.macAdress}_meanAcc",
            penon.meanAcc
        )
        penon.sDAcc = sharedPref.getBoolean(
            "${penon.macAdress}_sDAcc",
            penon.sDAcc
        )
        penon.maxAcc = sharedPref.getBoolean(
            "${penon.macAdress}_maxAcc",
            penon.maxAcc
        )
        penon.vbat = sharedPref.getBoolean(
            "${penon.macAdress}_vbat",
            penon.vbat
        )
        penon.detached = sharedPref.getBoolean(
            "${penon.macAdress}_detached",
            penon.detached
        )
        penon.detachedThresh = sharedPref.getFloat(
            "${penon.macAdress}_detachedThresh",
            penon.detachedThresh.toFloat()
        ).toDouble()
        penon.count = sharedPref.getBoolean(
            "${penon.macAdress}_count",
            penon.count
        )
        penon.ids = sharedPref.getBoolean(
            "${penon.macAdress}_ids",
            penon.ids
        )

        // Mettre à jour le StateFlow correspondant
        updateStateFlow(penon)

        Log.d(TAG, "✅ Penon chargé: ${penon.penonName} (MAC: ${penon.macAdress})")
    }

    /**
     * ✅ Sauvegarde un Penon dans SharedPreferences ET notifie les observateurs
     */
    fun savePenon(penon: Penon) {
        Log.d(TAG, "💾 Sauvegarde Penon: ${penon.penonName}")

        // Ajouter la MAC à la liste des MACs connues
        addKnownMacAddress(penon.macAdress)

        sharedPref.edit().apply {
            putString("${penon.macAdress}_penonName", penon.penonName)
            putInt("${penon.macAdress}_flowStateThreshold", penon.flowStateThreshold)
            putInt("${penon.macAdress}_rssiLow", penon.rssiLow)
            putInt("${penon.macAdress}_rssiHigh", penon.rssiHigh)
            putInt("${penon.macAdress}_sDFlowStateLow", penon.sDFlowStateLow)
            putInt("${penon.macAdress}_sDFlowStateHigh", penon.sDFlowStateHigh)
            putInt("${penon.macAdress}_meanAccLow", penon.meanAccLow)
            putInt("${penon.macAdress}_meanAccHigh", penon.meanAccHigh)
            putInt("${penon.macAdress}_sDAccLow", penon.sDAccLow)
            putInt("${penon.macAdress}_sDAccHigh", penon.sDAccHigh)
            putInt("${penon.macAdress}_maxAccLow", penon.maxAccLow)
            putInt("${penon.macAdress}_maxAccHigh", penon.maxAccHigh)
            putFloat("${penon.macAdress}_vbatLow", penon.vbatLow.toFloat())
            putFloat("${penon.macAdress}_vbatHigh", penon.vbatHigh.toFloat())
            putInt("${penon.macAdress}_timeline", penon.timeline)
            putBoolean("${penon.macAdress}_rssi", penon.rssi)
            putBoolean("${penon.macAdress}_flowState", penon.flowState)
            putBoolean("${penon.macAdress}_sDFlowState", penon.sDFlowState)
            putBoolean("${penon.macAdress}_meanAcc", penon.meanAcc)
            putBoolean("${penon.macAdress}_sDAcc", penon.sDAcc)
            putBoolean("${penon.macAdress}_maxAcc", penon.maxAcc)
            putBoolean("${penon.macAdress}_vbat", penon.vbat)
            putBoolean("${penon.macAdress}_detached", penon.detached)
            putFloat("${penon.macAdress}_detachedThresh", penon.detachedThresh.toFloat())
            putBoolean("${penon.macAdress}_count", penon.count)
            putBoolean("${penon.macAdress}_ids", penon.ids)
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
        val macAddress = penon.macAdress

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

        val penon = Penon(macAdress = macAddress)
        loadPenon(penon)
        return penon
    }
}