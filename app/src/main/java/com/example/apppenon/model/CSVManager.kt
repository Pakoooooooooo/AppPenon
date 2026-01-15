package com.example.apppenon.model

import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.example.apppenon.activities.MainActivity
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
class CSVManager(private val act: MainActivity) {
    
    private val TAG = "eTT-SAIL-BLE"
    private var csvFile1: File? = null
    private var csvWriter1: FileWriter? = null
    private var csvFile2: File? = null
    private var csvWriter2: FileWriter? = null
    private var isRecording = false

    /**
     * Crée un fichier CSV unique avec suffixe P1 ou P2.
     * Incrémente le numéro si le fichier existe déjà.
     */
    private fun createSingleCSVFile(appFolder: File, baseName: String, penonNumber: Int): File {
        val fileName: String

        if (baseName.isNotEmpty()) {
            var cleanName = baseName.replace(Regex("[^a-zA-Z0-9_-]"), "_")

            if (cleanName.lowercase().endsWith(".csv")) {
                cleanName = cleanName.substring(0, cleanName.length - 4)
            }

            cleanName = "${cleanName}_P${penonNumber}.csv"

            var finalFile = File(appFolder, cleanName)
            var counter = 1
            val baseNameWithoutExt = cleanName.removeSuffix(".csv")

            while (finalFile.exists()) {
                val numberedFileName = "${baseNameWithoutExt}_$counter.csv"
                finalFile = File(appFolder, numberedFileName)
                counter++
            }

            return finalFile
        } else {
            var fileNumber = 1
            var tempFile: File

            do {
                val defaultFileName = "rec_${"%04d".format(fileNumber)}_P${penonNumber}.csv"
                tempFile = File(appFolder, defaultFileName)
                fileNumber++
            } while (tempFile.exists())

            return tempFile
        }
    }

    /**
     * Écrit une ligne de données dans le fichier CSV du Penon spécifié.
     */
    fun saveToCSV(data: ByteArray, rssi: Int, penonNumber: Int, frameNumber: Int, macAddress: String) {
        try {
            if (!isRecording || !AppData.rec) return

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val hexData = data.joinToString(" ") { "%02X".format(it) }

            val line = "$timestamp,$macAddress,$frameNumber,$rssi,${data.size},\"$hexData\"\n"

            if (penonNumber == 1 && csvWriter1 != null) {
                csvWriter1?.write(line)
                csvWriter1?.flush()
            } else if (penonNumber == 2 && csvWriter2 != null) {
                csvWriter2?.write(line)
                csvWriter2?.flush()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur écriture CSV Penon $penonNumber", e)
        }
    }

    /**
     * Crée les fichiers CSV dans le dossier Documents/eTT_SAIL.
     * Initialise les FileWriter avec les en-têtes appropriés.
     */
    fun createCSVFiles(targetMacAddress1: String, targetMacAddress2: String) {
        try {
            if (!AppData.rec) {
                Log.d(TAG, "Création CSV annulée - Rec: ${AppData.rec}")
                return
            }

            val documentsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            )

            val appFolder = File(documentsDir, "eTT_SAIL")
            if (!appFolder.exists()) {
                appFolder.mkdirs()
            }

            var customName = act.etFileName.text.toString().trim()

            if (targetMacAddress1.isNotEmpty()) {
                csvFile1 = createSingleCSVFile(appFolder, customName, 1)
                csvWriter1 = FileWriter(csvFile1, false)
                csvWriter1?.write("Timestamp,MAC_Address,Frame_Number,RSSI,Data_Size,Raw_Hex_Data\n")
                csvWriter1?.flush()
                Log.d(TAG, "Fichier CSV Penon 1 créé: ${csvFile1?.absolutePath}")
            }

            if (targetMacAddress2.isNotEmpty()) {
                csvFile2 = createSingleCSVFile(appFolder, customName, 2)
                csvWriter2 = FileWriter(csvFile2, false)
                csvWriter2?.write("Timestamp,MAC_Address,Frame_Number,RSSI,Data_Size,Raw_Hex_Data\n")
                csvWriter2?.flush()
                Log.d(TAG, "Fichier CSV Penon 2 créé: ${csvFile2?.absolutePath}")
            }

            isRecording = true

        } catch (e: Exception) {
            Log.e(TAG, "Erreur création fichiers CSV", e)
            Toast.makeText(act, "Erreur création CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            isRecording = false
        }
    }

    /**
     * Ferme les FileWriter et sauvegarde les fichiers.
     */
    fun closeCSVFiles() {
        try {
            if (csvWriter1 != null) {
                csvWriter1?.flush()
                csvWriter1?.close()
                csvWriter1 = null
                Log.d(TAG, "Fichier CSV Penon 1 fermé: ${csvFile1?.absolutePath}")
            }

            if (csvWriter2 != null) {
                csvWriter2?.flush()
                csvWriter2?.close()
                csvWriter2 = null
                Log.d(TAG, "Fichier CSV Penon 2 fermé: ${csvFile2?.absolutePath}")
            }

            isRecording = false

        } catch (e: Exception) {
            Log.e(TAG, "Erreur fermeture fichiers CSV", e)
        }
    }

    /**
     * Retourne les informations des fichiers créés.
     */
    fun getCreatedFiles(): Pair<File?, File?> {
        return Pair(csvFile1, csvFile2)
    }

    fun isRecordingActive(): Boolean = isRecording
}
