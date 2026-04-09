package com.example.apppenon.model.simulation

import android.bluetooth.le.ScanSettings
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
    private var isPaused = false
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
            Log.d(TAG, "📂 Tentative de chargement du fichier : $uri")

            frames.clear()
            currentFrameIndex = 0

            val inputStream = context.contentResolver.openInputStream(uri)

            if (inputStream == null) {
                Log.e(TAG, "❌ Impossible d'ouvrir l'InputStream - Permission refusée ?")
                return false
            }

            val reader = BufferedReader(InputStreamReader(inputStream))

            // Ignorer la ligne d'en-tête
            val header = reader.readLine()
            Log.d(TAG, "📋 En-tête CSV : $header")

            var line: String?
            var lineCount = 0
            while (reader.readLine().also { line = it } != null) {
                lineCount++
                line?.let {
                    parseLine(it)
                    if (lineCount <= 3) {
                        Log.d(TAG, "📝 Ligne $lineCount : $it")
                    }
                }
            }

            reader.close()

            if (frames.isNotEmpty()) {
                firstFrameTimestamp = frames[0].timestamp
                Log.d(TAG, "✅ CSV chargé : ${frames.size} trames sur $lineCount lignes")
                true
            } else {
                Log.e(TAG, "❌ Aucune trame trouvée dans le CSV ($lineCount lignes lues)")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur chargement CSV : ${e.message}", e)
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
            if (parts.size < 6) {
                Log.w(TAG, "⚠️ Ligne ignorée (moins de 6 colonnes) : $line")
                return
            }

            val timestamp = dateFormat.parse(parts[0])?.time
            if (timestamp == null) {
                Log.w(TAG, "⚠️ Timestamp invalide : ${parts[0]}")
                return
            }

            val macAddress = parts[1]
            val frameNumber = parts[2].toIntOrNull()
            val rssi = parts[3].toIntOrNull()
            val dataSize = parts[4].toIntOrNull()

            if (frameNumber == null || rssi == null || dataSize == null) {
                Log.w(TAG, "⚠️ Données numériques invalides : frame=$frameNumber, rssi=$rssi, size=$dataSize")
                return
            }

            // Extraire les données hex : entre guillemets si présents, sinon colonne 5 directement
            // (les fichiers record_..._decoded_final.csv n'ont pas de guillemets)
            val rawHexData = if (line.contains("\"")) {
                line.substringAfter("\"").substringBefore("\"")
            } else {
                parts[5].trim()
            }

            if (rawHexData.isBlank()) {
                Log.w(TAG, "⚠️ Données hex vides pour frame $frameNumber")
                return
            }

            frames.add(CSVFrame(
                timestamp = timestamp,
                macAddress = macAddress,
                frameNumber = frameNumber,
                rssi = rssi,
                dataSize = dataSize,
                rawHexData = rawHexData
            ))

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parsing ligne : ${e.message}", e)
            Log.e(TAG, "   Ligne : $line")
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
        isPaused = false
        currentFrameIndex = 0
        simulationStartTime = System.currentTimeMillis()

        Log.d(TAG, "▶️ Démarrage simulation : ${frames.size} trames")

        scheduleNextFrame()
    }

    /**
     * Met en pause la simulation.
     */
    fun pauseSimulation() {
        if (isSimulating && !isPaused) {
            isPaused = true
            handler.removeCallbacksAndMessages(null)
            Log.d(TAG, "⏸️ Simulation en pause (frame ${currentFrameIndex}/${frames.size})")
        }
    }

    /**
     * Reprend la simulation après une pause.
     */
    fun resumeSimulation() {
        if (isSimulating && isPaused) {
            isPaused = false
            Log.d(TAG, "▶️ Reprise simulation (frame ${currentFrameIndex}/${frames.size})")
            scheduleNextFrame()
        }
    }

    /**
     * Arrête complètement la simulation et réinitialise.
     */
    fun stopSimulation() {
        isSimulating = false
        isPaused = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "⏹️ Simulation arrêtée")
    }

    /**
     * Planifie l'envoi de la prochaine trame selon le timestamp.
     */
    private fun scheduleNextFrame() {
        if (!isSimulating || isPaused || currentFrameIndex >= frames.size) {
            if (currentFrameIndex >= frames.size) {
                Log.d(TAG, "✅ Simulation terminée")
                isSimulating = false
            }
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
                    ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
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
    fun isRunning(): Boolean = isSimulating && !isPaused

    /**
     * Vérifie si la simulation est en pause.
     */
    fun isPaused(): Boolean = isPaused

    /**
     * Retourne le nombre de trames chargées.
     */
    fun getFrameCount(): Int = frames.size

    /**
     * Réinitialise la simulation au début.
     */
    fun reset() {
        stopSimulation()
        currentFrameIndex = 0
        Log.d(TAG, "🔄 Simulation réinitialisée")
    }
}