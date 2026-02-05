package com.example.apppenon.model

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gère la création et l'écriture des fichiers CSV pour l'enregistrement des données Penon.
 * Responsabilités:
 * - Créer les fichiers CSV avec des noms uniques
 * - Écrire les données brutes dans les fichiers
 * - Gérer les FileWriter pour les deux Penons
 */
class CSVManager() {
    
    private val TAG = "eTT-SAIL-BLE"
    private var csvFile: File? = null
    private var csvWriter: FileWriter? = null
    private var isRecording = false

    /**
     * Écrit une ligne de données dans le fichier CSV du Penon spécifié.
     */
    fun saveToCSV(data: ByteArray, rssi: Int, frameNumber: Int, macAddress: String) {
        try {
            if (!isRecording || !AppData.rec) return

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val hexData = data.joinToString(" ") { "%02X".format(it) }

            val line = "$timestamp,$macAddress,$frameNumber,$rssi,${data.size},\"$hexData\"\n"

            if (csvWriter != null) {
                csvWriter?.write(line)
                csvWriter?.flush()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur écriture CSV Penon", e)
        }
    }

    /**
     * Ferme les FileWriter et sauvegarde les fichiers.
     */
    fun closeCSVFiles() {
        try {
            if (csvWriter != null) {
                csvWriter?.flush()
                csvWriter?.close()
                csvWriter = null
                Log.d(TAG, "Fichier CSV Penon 1 fermé: ${csvFile?.absolutePath}")
            }

            isRecording = false

        } catch (e: Exception) {
            Log.e(TAG, "Erreur fermeture fichiers CSV", e)
        }
    }

    /**
     * Retourne les informations des fichiers créés.
     */
    fun getCreatedFiles(): File? {
        return csvFile
    }

    fun isRecordingActive(): Boolean = isRecording
}
