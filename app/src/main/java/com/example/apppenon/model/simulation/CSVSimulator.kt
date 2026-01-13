package com.example.apppenon.simulation

import android.bluetooth.le.ScanResult
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.apppenon.model.BLEScanManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simulateur de scan BLE à partir d'un fichier CSV.
 * Lit le fichier ligne par ligne et rejoue les trames BLE en respectant les timestamps.
 */
class CSVSimulator(
        private val context: Context,
        private val bleScanManager: BLEScanManager
) {

    private val TAG = "CSVSimulator"
    private val handler = Handler(Looper.getMainLooper())
    private var isSimulating = false
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    // Liste des trames CSV à rejouer
    private data class CSVFrame(
            val timestamp: Long,
            val macAddress: String,
            val frameNumber: Int,
            val rssi: Int,
            val dataSize: Int,
            val rawHexData: String
    )

    private val frames = mutableListOf<CSVFrame>()
    private var currentFrameIndex = 0
    private var simulationStartTime = 0L
    private var firstFrameTimestamp = 0L

    /**
     * Charge le fichier CSV et parse toutes les trames.
     */
    fun loadCSVFile(uri: Uri): Boolean {
        return try {
            frames.clear()
            currentFrameIndex = 0

            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))

            // Ignorer la ligne d'en-tête
            reader.readLine()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { parseLine(it) }
            }

            reader.close()

            if (frames.isNotEmpty()) {
                firstFrameTimestamp = frames[0].timestamp
                Log.d(TAG, "✅ CSV chargé : ${frames.size} trames")
                true
            } else {
                Log.e(TAG, "❌ Aucune trame trouvée dans le CSV")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur chargement CSV", e)
            false
        }
    }

    /**
     * Parse une ligne CSV et crée un objet CSVFrame.
     */
    private fun parseLine(line: String) {
        try {
            // Format: Timestamp,MAC_Address,Frame_Number,RSSI,Data_Size,Raw_Hex_Data
            // Exemple: 2025-11-17 17:43:45.756,EC:69:0B:19:32:8F,1,-66,46,"02 01 06 14 09 42 6C..."

            val parts = line.split(",")
            if (parts.size < 6) return

                    val timestamp = dateFormat.parse(parts[0])?.time ?: return
                    val macAddress = parts[1]
            val frameNumber = parts[2].toIntOrNull() ?: return
                    val rssi = parts[3].toIntOrNull() ?: return
                    val dataSize = parts[4].toIntOrNull() ?: return

                    // Extraire les données hex (entre guillemets)
                    val rawHexData = line.substringAfter("\"").substringBefore("\"")

            frames.add(CSVFrame(
                    timestamp = timestamp,
                    macAddress = macAddress,
                    frameNumber = frameNumber,
                    rssi = rssi,
                    dataSize = dataSize,
                    rawHexData = rawHexData
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing ligne: $line", e)
        }
    }

    /**
     * Démarre la simulation des trames BLE.
     */
    fun startSimulation() {
        if (frames.isEmpty()) {
            Log.e(TAG, "❌ Aucune trame à simuler")
            return
        }

        isSimulating = true
        currentFrameIndex = 0
        simulationStartTime = System.currentTimeMillis()

        Log.d(TAG, "▶️ Démarrage simulation : ${frames.size} trames")

        scheduleNextFrame()
    }

    /**
     * Arrête la simulation.
     */
    fun stopSimulation() {
        isSimulating = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "⏹️ Simulation arrêtée")
    }

    /**
     * Planifie l'envoi de la prochaine trame selon le timestamp.
     */
    private fun scheduleNextFrame() {
        if (!isSimulating || currentFrameIndex >= frames.size) {
            Log.d(TAG, "✅ Simulation terminée")
            isSimulating = false
            return
        }

        val currentFrame = frames[currentFrameIndex]
        val nextFrame = if (currentFrameIndex + 1 < frames.size) {
            frames[currentFrameIndex + 1]
        } else null

        // Envoyer la trame actuelle
        sendFrame(currentFrame)

        // Calculer le délai jusqu'à la prochaine trame
        if (nextFrame != null) {
            val timeDiff = nextFrame.timestamp - currentFrame.timestamp
            val adjustedDelay = (timeDiff / SimulationConfig.playbackSpeed).toLong()

            currentFrameIndex++
            handler.postDelayed({
                    scheduleNextFrame()
            }, adjustedDelay)
        } else {
            // Dernière trame
            Log.d(TAG, "✅ Toutes les trames ont été envoyées")
            isSimulating = false
        }
    }

    /**
     * Envoie une trame simulée au BLEScanManager.
     */
    private fun sendFrame(frame: CSVFrame) {
        try {
            // Convertir les données hex en ByteArray
            val hexBytes = frame.rawHexData.split(" ")
                    .filter { it.isNotBlank() }
                .map { it.toInt(16).toByte() }
                .toByteArray()

            // Créer un ScanResult simulé
            val mockScanResult = MockScanResult(
                    macAddress = frame.macAddress,
                    rssi = frame.rssi,
                    scanRecord = hexBytes
            )

            // Injecter dans le callback du BLEScanManager
            bleScanManager.bleScanCallback.onScanResult(
                    android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
                    mockScanResult.toScanResult()
            )

            Log.d(TAG, "📡 Frame ${frame.frameNumber} envoyée: ${frame.macAddress} (RSSI: ${frame.rssi})")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur envoi frame ${frame.frameNumber}", e)
        }
    }

    /**
     * Vérifie si la simulation est en cours.
     */
    fun isRunning(): Boolean = isSimulating

    /**
     * Retourne le nombre de trames chargées.
     */
    fun getFrameCount(): Int = frames.size

    /**
     * Retourne la progression de la simulation (0.0 à 1.0).
     */
    fun getProgress(): Float {
        return if (frames.isEmpty()) 0f
        else currentFrameIndex.toFloat() / frames.size.toFloat()
    }
}