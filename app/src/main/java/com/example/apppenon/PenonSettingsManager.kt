package com.example.apppenon

import android.content.Context
import com.example.apppenon.model.Penon

/**
 * Gère la sauvegarde et le chargement des paramètres Penon via SharedPreferences.
 *
 * ✅ ZÉRO UI
 * ✅ ZÉRO Intent  
 * ✅ ZÉRO Activity
 * 
 * Responsabilités:
 * - Sauvegarde les settings des Penons
 * - Charge les settings depuis SharedPreferences
 */
class PenonSettingsManager(
    private val context: Context,
    private val penons: List<Penon>
) {

    private val sharedPref =
        context.getSharedPreferences("penon_data", Context.MODE_PRIVATE)

    fun savePenonSettings(penon: Penon) {
        with(sharedPref.edit()) {
            putInt("${penon.macAdress}_flowStateThreshold", penon.flowStateThreshold)
            putBoolean("${penon.macAdress}_flowState", penon.flowState)
            apply()
        }
    }

    fun loadPenonSettings(penon: Penon) {
        penon.flowStateThreshold =
            sharedPref.getInt(
                "${penon.macAdress}_flowStateThreshold",
                penon.flowStateThreshold
            )
        penon.flowState =
            sharedPref.getBoolean(
                "${penon.macAdress}_flowState",
                penon.flowState
            )
    }
}
