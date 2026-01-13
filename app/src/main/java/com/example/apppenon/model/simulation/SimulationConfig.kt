package com.example.apppenon.simulation

import android.net.Uri

/**
 * Configuration globale pour le mode simulation.
 * Permet d'activer/désactiver la simulation et de stocker le fichier CSV sélectionné.
 */
object SimulationConfig {

/**
 * Active ou désactive le mode simulation.
 * Quand true, les données viennent du CSV au lieu du scan BLE.
 */
var isSimulationMode: Boolean = false

/**
 * URI du fichier CSV sélectionné pour la simulation.
 * Null si aucun fichier n'est sélectionné.
 */
var csvFileUri: Uri? = null

/**
 * Nom du fichier CSV pour l'affichage.
 */
var csvFileName: String = "Aucun fichier"

/**
 * Vitesse de lecture du fichier CSV.
 * 1.0 = temps réel
 * 2.0 = 2x plus rapide
 * 0.5 = 2x plus lent
 */
var playbackSpeed: Float = 1.0f

/**
 * Réinitialise la configuration de simulation.
 */
fun reset() {
    isSimulationMode = false
    csvFileUri = null
    csvFileName = "Aucun fichier"
    playbackSpeed = 1.0f
}

/**
 * Vérifie si la simulation est prête à démarrer.
 */
fun isReadyToSimulate(): Boolean {
    return isSimulationMode && csvFileUri != null
}
}