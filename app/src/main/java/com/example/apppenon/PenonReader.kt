package com.example.apppenon

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString


class PenonReader(act: MainActivity) {
    private val act = act
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    var isScanning = false
    private val handler = Handler(Looper.getMainLooper())
    lateinit var TARGET_MAC_ADDRESS1: String
    lateinit var TARGET_MAC_ADDRESS2: String

    // Compteurs séparés pour chaque penon
    var frameCount1 = 0
    var frameCount2 = 0
    var lastFrameCnt1 = -1L
    var lastFrameCnt2 = -1L
    private var csvFile1: File? = null
    private var csvWriter1: FileWriter? = null
    private var csvFile2: File? = null
    private var csvWriter2: FileWriter? = null
    private var isRecording = false
    private val PERMISSION_REQUEST_CODE = 100

    private val TAG = "eTT-SAIL-BLE"
    private fun createSingleCSVFile(appFolder: File, baseName: String, penonNumber: Int): File {
        val fileName: String

        if (baseName.isNotEmpty()) {
            var cleanName = baseName.replace(Regex("[^a-zA-Z0-9_-]"), "_")

            // Retirer .csv si présent (insensible à la casse)
            if (cleanName.lowercase().endsWith(".csv")) {
                cleanName = cleanName.substring(0, cleanName.length - 4)
            }

            // Ajouter le numéro du penon
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
            // Format par défaut : rec_0001_P1.csv
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
    private fun saveToCSV(data: ByteArray, rssi: Int, penonNumber: Int, frameNumber: Int, macAddress: String) {
        try {
            if (!isRecording) return

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val hexData = data.joinToString(" ") { "%02X".format(it) }

            // Format: Timestamp, MAC, Frame Number, RSSI, Data Size, Raw Hex Data
            val line = "$timestamp,$macAddress,$frameNumber,$rssi,${data.size},\"$hexData\"\n"

            // Écrire dans le fichier approprié
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
    fun handlePenonData(result: ScanResult, penonNumber: Int) {
        val rssi = result.rssi
        val scanRecord = result.scanRecord

        if (scanRecord != null) {
            val manufacturerData = scanRecord.getManufacturerSpecificData(0xFFFF)
                ?: scanRecord.bytes

            if (manufacturerData != null && manufacturerData.isNotEmpty()) {
                // Incrémenter le compteur approprié
                if (penonNumber == 1) {
                    frameCount1++
                } else {
                    frameCount2++
                }

                val currentFrameCount = if (penonNumber == 1) frameCount1 else frameCount2
                val macAddress = if (penonNumber == 1) TARGET_MAC_ADDRESS1 else TARGET_MAC_ADDRESS2

                // Enregistrer dans le CSV approprié
                if (isRecording) {
                    saveToCSV(manufacturerData, rssi, penonNumber, currentFrameCount, macAddress)
                }

                handler.post {
                    val hexData = manufacturerData.joinToString(" ") {
                        "%02X".format(it)
                    }

                    Log.d(TAG, "=== PENON $penonNumber - TRAME #$currentFrameCount ===")
                    Log.d(TAG, "MAC: $macAddress")
                    Log.d(TAG, "Taille: ${manufacturerData.size} octets")
                    Log.d(TAG, "HEX: $hexData")
                    Log.d(TAG, "RSSI: $rssi dBm")

                    // Mettre à jour l'affichage approprié
                    if (penonNumber == 1) {
                        val currentText = act.tvReceivedData1.text.toString()
                        val newText = "$currentText\n[Trame $currentFrameCount] RSSI: $rssi dBm\nHEX: $hexData\n"
                        act.tvReceivedData1.text = newText

                        val parsedData = parseETTSailData(manufacturerData, 1)
                        act.tvParsedData1.text = parsedData
                    } else {
                        val currentText = act.tvReceivedData2.text.toString()
                        val newText = "$currentText\n[Trame $currentFrameCount] RSSI: $rssi dBm\nHEX: $hexData\n"
                        act.tvReceivedData2.text = newText

                        val parsedData = parseETTSailData(manufacturerData, 2)
                        act.tvParsedData2.text = parsedData
                    }

                    val recordingStatus = if (isRecording) "📝" else ""
                    val totalFrames = frameCount1 + frameCount2
                    act.tvStatus.text = "$recordingStatus✓ Réception: P1=$frameCount1, P2=$frameCount2 (Total: $totalFrames)"

                    act.autoScroll()
                }
            }
        }
    }
    fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        val permissionsToRequest = permissions.filter {
            ActivityCompat.checkSelfPermission(act, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                act,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    private fun testDecode(data: ByteArray, offset: Int, order: ByteOrder, label: String, penonNumber: Int): String {
        return try {
            if (data.size < offset + 17) {
                return "[$label] Taille insuffisante"
            }

            val buffer = ByteBuffer.wrap(data).order(order)
            buffer.position(offset)

            val frameCnt = buffer.int.toLong() and 0xFFFFFFFFL
            val frameType = buffer.get().toInt() and 0xFF
            val vbat = buffer.short.toInt()
            val meanMagZ = buffer.short.toInt()
            val sdMagZ = buffer.short.toInt()
            val meanAcc = buffer.short.toInt()
            val sdAcc = buffer.short.toInt()
            val maxAcc = buffer.short.toInt()

            val vbatV = vbat / 1000.0
            val isCoherent = frameCnt in 0..100000000 &&
                    vbatV in 2.0..4.5 &&
                    frameType in 0..255

            val lastFrameCnt = if (penonNumber == 1) lastFrameCnt1 else lastFrameCnt2

            val lostFrames = if (lastFrameCnt >= 0 && frameCnt > lastFrameCnt) {
                val lost = frameCnt - lastFrameCnt - 1
                if (lost > 0) " ⚠️ $lost trame(s) perdue(s)" else ""
            } else ""

            if (isCoherent && lastFrameCnt < frameCnt) {
                if (penonNumber == 1) {
                    lastFrameCnt1 = frameCnt
                } else {
                    lastFrameCnt2 = frameCnt
                }
            }

            val coherentMark = if (isCoherent) "✅" else "❌"

            buildString {
                appendLine("\n[$label] $coherentMark")
                appendLine("  Frame: $frameCnt$lostFrames")
                appendLine("  Type: $frameType")
                appendLine("  Vbat: ${"%.3f".format(vbatV)} V")
                appendLine("  MagZ: mean=${meanMagZ/1000.0}mT, sd=${sdMagZ/1000.0}mT")
                appendLine("  Acc: mean=${meanAcc/1000.0}g, max=${maxAcc/1000.0}g")
            }

        } catch (e: Exception) {
            "[$label] Erreur: ${e.message}"
        }
    }
    private fun parseETTSailData(data: ByteArray, penonNumber: Int): String {
        return try {
            val fullHex = data.joinToString(" ") { "%02X".format(it) }
            Log.d(TAG, "Penon $penonNumber - Données complètes: $fullHex")

            var manufacturerData = data
            var dataStartOffset = 0

            var offset = 0
            var foundManufacturerData = false

            while (offset < data.size - 2) {
                val length = data[offset].toInt() and 0xFF
                if (length == 0) break

                val type = data[offset + 1].toInt() and 0xFF

                if (type == 0xFF) {
                    dataStartOffset = offset + 2
                    manufacturerData = data.copyOfRange(offset + 2, minOf(offset + 1 + length, data.size))
                    foundManufacturerData = true
                    break
                }

                offset += length + 1
            }

            val dataHex = manufacturerData.joinToString(" ") { "%02X".format(it) }

            if (manufacturerData.size < 17) {
                return "⚠️ Données insuffisantes (${manufacturerData.size} octets)\n" +
                        "Minimum requis: 17 octets\n\n" +
                        "HEX: $dataHex"
            }

            val results = mutableListOf<String>()

            results.add(testDecode(manufacturerData, 0, ByteOrder.LITTLE_ENDIAN, "Sans offset, LE", penonNumber))

            if (manufacturerData.size >= 19) {
                results.add(testDecode(manufacturerData, 2, ByteOrder.LITTLE_ENDIAN, "Offset +2, LE", penonNumber))
            }

            results.add(testDecode(manufacturerData, 0, ByteOrder.BIG_ENDIAN, "Sans offset, BE", penonNumber))

            if (manufacturerData.size >= 19) {
                results.add(testDecode(manufacturerData, 2, ByteOrder.BIG_ENDIAN, "Offset +2, BE", penonNumber))
            }

            buildString {
                appendLine("═══════════════════════════════")
                appendLine("PENON $penonNumber - TESTS DE DÉCODAGE")
                appendLine("═══════════════════════════════")
                results.forEach { appendLine(it) }
                appendLine("═══════════════════════════════")
                appendLine("\nDonnées brutes:")
                appendLine(dataHex)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur de décodage Penon $penonNumber", e)
            "❌ Erreur: ${e.message}\n" +
                    "Taille: ${data.size} octets\n" +
                    "HEX: ${data.joinToString(" ") { "%02X".format(it) }}"
        }
    }
    fun startScanning() {
        if (ActivityCompat.checkSelfPermission(
                act,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    Manifest.permission.BLUETOOTH_SCAN
                else
                    Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(act, "Permissions manquantes", Toast.LENGTH_SHORT).show()
            return
        }

        frameCount1 = 0
        frameCount2 = 0
        lastFrameCnt1 = -1
        lastFrameCnt2 = -1

        act.tvReceivedData1.text = if (TARGET_MAC_ADDRESS1.isNotEmpty()) {
            "=== ÉCOUTE PENON 1: $TARGET_MAC_ADDRESS1 ===\n"
        } else {
            "=== PENON 1: Non configuré ===\n"
        }

        act.tvReceivedData2.text = if (TARGET_MAC_ADDRESS2.isNotEmpty()) {
            "=== ÉCOUTE PENON 2: $TARGET_MAC_ADDRESS2 ===\n"
        } else {
            "=== PENON 2: Non configuré ===\n"
        }

        act.tvParsedData1.text = "En attente de données..."
        act.tvParsedData2.text = "En attente de données..."
        isScanning = true

        // Créer les fichiers CSV
        createCSVFiles()

        act.updateUIState()

        val scanSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setLegacy(false)
                .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                .build()
        } else {
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
        }

        bluetoothLeScanner?.startScan(null, scanSettings, bleScanCallback)

        val listeningMsg = buildString {
            append("📝 Écoute: ")
            if (TARGET_MAC_ADDRESS1.isNotEmpty()) append("P1 ")
            if (TARGET_MAC_ADDRESS2.isNotEmpty()) append("P2 ")
        }
        act.tvStatus.text = listeningMsg

        Toast.makeText(act, "Scan BLE démarré - Enregistrement CSV actif", Toast.LENGTH_SHORT).show()
    }
    fun stopScanning() {
        if (ActivityCompat.checkSelfPermission(
                act,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    Manifest.permission.BLUETOOTH_SCAN
                else
                    Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        bluetoothLeScanner?.stopScan(bleScanCallback)

        isScanning = false

        // Fermer les fichiers CSV
        closeCSVFiles()

        act.updateUIState()

        val totalFrames = frameCount1 + frameCount2
        val statusMsg = buildString {
            appendLine("Scan arrêté - P1: $frameCount1, P2: $frameCount2 (Total: $totalFrames)")
            if (csvFile1 != null) {
                appendLine("Fichier P1: ${csvFile1?.name}")
            }
            if (csvFile2 != null) {
                appendLine("Fichier P2: ${csvFile2?.name}")
            }
        }
        act.tvStatus.text = statusMsg

        val toastMsg = buildString {
            appendLine("Données enregistrées:")
            if (csvFile1 != null) {
                appendLine("P1: ${csvFile1?.absolutePath}")
            }
            if (csvFile2 != null) {
                appendLine("P2: ${csvFile2?.absolutePath}")
            }
        }
        Toast.makeText(act, toastMsg, Toast.LENGTH_LONG).show()
    }
    fun closeCSVFiles() {
        try {
            // Fermer fichier Penon 1
            if (csvWriter1 != null) {
                csvWriter1?.flush()
                csvWriter1?.close()
                csvWriter1 = null
                Log.d(TAG, "Fichier CSV Penon 1 fermé: ${csvFile1?.absolutePath}")
            }

            // Fermer fichier Penon 2
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
    val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (ActivityCompat.checkSelfPermission(
                    act,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        Manifest.permission.BLUETOOTH_CONNECT
                    else
                        Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val device = result.device
            val deviceAddress = device.address

            // Traitement pour Penon 1
            if (deviceAddress.equals(TARGET_MAC_ADDRESS1, ignoreCase = true)) {
                handlePenonData(result, 1)
            }

            // Traitement pour Penon 2 (pas de else, pour permettre le double affichage)
            if (deviceAddress.equals(TARGET_MAC_ADDRESS2, ignoreCase = true)) {
                handlePenonData(result, 2)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            handler.post {
                act.tvStatus.text = "Échec du scan BLE: $errorCode"
                Toast.makeText(
                    act,
                    "Échec du scan BLE: $errorCode",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    fun createCSVFiles() {
        try {
            val documentsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOCUMENTS
            )

            val appFolder = File(documentsDir, "eTT_SAIL")
            if (!appFolder.exists()) {
                appFolder.mkdirs()
            }

            var customName = act.etFileName.text.toString().trim()

            // Créer fichier pour Penon 1 si configuré
            if (TARGET_MAC_ADDRESS1.isNotEmpty()) {
                csvFile1 = createSingleCSVFile(appFolder, customName, 1)
                csvWriter1 = FileWriter(csvFile1, false)
                csvWriter1?.write("Timestamp,MAC_Address,Frame_Number,RSSI,Data_Size,Raw_Hex_Data\n")
                csvWriter1?.flush()
                Log.d(TAG, "Fichier CSV Penon 1 créé: ${csvFile1?.absolutePath}")
            }

            // Créer fichier pour Penon 2 si configuré
            if (TARGET_MAC_ADDRESS2.isNotEmpty()) {
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
}