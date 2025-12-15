package com.example.apppenon.model

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.apppenon.activities.MainActivity
import java.io.File
import java.io.FileWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString


@Suppress("DEPRECATION")
class PenonReader(private val act: MainActivity) {
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    var isScanning = false
    private val handler = Handler(Looper.getMainLooper())
    // Compteurs séparés pour chaque penon
    var frameCount = mutableListOf(0)
    var lastFrameCnt = mutableListOf(-1L)
    private var csvFile1: File? = null
    private var csvWriter1: FileWriter? = null
    private var csvFile2: File? = null
    private var csvWriter2: FileWriter? = null
    private var isRecording = false
    private val PERMISSION_REQUEST_CODE = 100

    private val TAG = "eTT-SAIL-BLE"
    private fun createSingleCSVFile(appFolder: File, baseName: String, penonNumber: Int): File {

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
    @SuppressLint("SetTextI18n")
    fun handlePenonData(result: ScanResult, penonNumber: Int) {
        val rssi = result.rssi
        val scanRecord = result.scanRecord

        if (scanRecord != null) {
            val manufacturerData = scanRecord.getManufacturerSpecificData(0xFFFF)
                ?: scanRecord.bytes

            if (manufacturerData != null && manufacturerData.isNotEmpty()) {
                // Incrémenter le compteur approprié
                frameCount[penonNumber-1]++

                val currentFrameCount = frameCount[penonNumber-1]
                val macAddress = result.device.address


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
                    if (isLadeSEBeacon(manufacturerData)){
                        if (!AppData.checkInList(macAddress)){
                            AppData.devices.add(Penon(macAdress = macAddress, penonName = "Penon_$penonNumber"))
                        }
                        val p = AppData.getPenon(macAddress)
                        val name = p?.penonName ?: "Penon_$penonNumber"
                        val currentText = act.tvReceivedData.text.toString()
                        val newText = "$currentText\n[Trame $currentFrameCount] RSSI: $rssi dBm\nHEX: $hexData\n"
                        act.tvReceivedData.text =  newText

                        val parsedData = parseETTSailData(manufacturerData, penonNumber)
                        "$name : $parsedData"
                    }

                    val recordingStatus = if (isRecording) "📝" else ""

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
            buffer.short.toInt()
            val maxAcc = buffer.short.toInt()

            val vbatV = vbat / 1000.0
            val isCoherent = frameCnt in 0..100000000 &&
                    vbatV in 2.0..4.5

            val lastFrameCnt_ = lastFrameCnt[penonNumber-1]

            val lostFrames = if (lastFrameCnt_ >= 0 && frameCnt > lastFrameCnt_) {
                val lost = frameCnt - lastFrameCnt_ - 1
                if (lost > 0) " ⚠️ $lost trame(s) perdue(s)" else ""
            } else ""

            if (isCoherent && lastFrameCnt_ < frameCnt) {
                lastFrameCnt[penonNumber-1] = frameCnt
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

            var offset = 0

            while (offset < data.size - 2) {
                val length = data[offset].toInt() and 0xFF
                if (length == 0) break

                val type = data[offset + 1].toInt() and 0xFF

                if (type == 0xFF) {
                    manufacturerData = data.copyOfRange(offset + 2, minOf(offset + 1 + length, data.size))
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

        frameCount.forEachIndexed { index, value ->
            frameCount[index] = 0
        }
        lastFrameCnt.forEachIndexed { index, value ->
            lastFrameCnt[index] = -1
        }

        act.tvParsedData.text = "En attente de données..."
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

        val statusMsg = buildString {
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

            // Traitement
            AppData.devices.forEach { device ->
                val device = result.device
                handlePenonData(result, AppData.devices.indexOf<Any>(device) + 1)
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
            val documentsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            )

            val appFolder = File(documentsDir, "eTT_SAIL")
            if (!appFolder.exists()) {
                appFolder.mkdirs()
            }

            var customName = act.etFileName.text.toString().trim()

            // Créer fichier pour chaque penon si configuré
            AppData.devices.forEach { device ->
                if (device.macAdress.isNotEmpty()) {
                    csvFile1 = createSingleCSVFile(appFolder, customName, 1)
                    csvWriter1 = FileWriter(csvFile1, false)
                    csvWriter1?.write("Timestamp,MAC_Address,Frame_Number,RSSI,Data_Size,Raw_Hex_Data\n")
                    csvWriter1?.flush()
                    Log.d(TAG, "Fichier CSV Penon 1 créé: ${csvFile1?.absolutePath}")
                }
            }

            isRecording = true

        } catch (e: Exception) {
            Log.e(TAG, "Erreur création fichiers CSV", e)
            Toast.makeText(act, "Erreur création CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            isRecording = false
        }
    }

    fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (this.size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }

    /**
     * Vérifie si un ByteArray contient une sous-séquence
     */
    fun ByteArray.contains(subArray: ByteArray): Boolean {
        if (subArray.isEmpty()) return true
        if (this.size < subArray.size) return false

        for (i in 0..(this.size - subArray.size)) {
            var found = true
            for (j in subArray.indices) {
                if (this[i + j] != subArray[j]) {
                    found = false
                    break
                }
            }
            if (found) return true
        }
        return false
    }

    fun isLadeSEBeacon(packetData: ByteArray): Boolean {
        // Vérifier la longueur minimale du paquet
        if (packetData.size < 10) {
            return false
        }

        // Vérification 1 : Structure du paquet BLE attendue
        // Signature : 02 01 06 14 09 42
        val expectedPrefix = byteArrayOf(0x02, 0x01, 0x06, 0x14, 0x09, 0x42)
        if (!packetData.startsWith(expectedPrefix)) {
            return false
        }

        // Vérification 2 : Présence de "ladeSE" dans le paquet
        // En ASCII : 6C 61 64 65 53 45
        val ladeSEBytes = "ladeSE".toByteArray(Charsets.US_ASCII)
        return packetData.contains(ladeSEBytes)
    }

    data class LadeSEBeaconInfo(
        val isLadeSE: Boolean,
        val packetLength: Int,
        val rawPacket: String,
        val deviceName: String? = null,
        val manufacturerData: String? = null,
        val counter: Int? = null
    )

    fun ByteArray.toHexString(): String {
        return this.joinToString(" ") { "%02X".format(it) }
    }

    /**
     * Extrait les informations utiles d'un paquet beacon ladeSE.
     *
     * @param packetData Le paquet publicitaire (ByteArray)
     * @return LadeSEBeaconInfo contenant les informations extraites, ou null si ce n'est pas un beacon ladeSE
     */
    fun extractLadeInfo(packetData: ByteArray): LadeSEBeaconInfo? {
        if (!isLadeSEBeacon(packetData)) {
            return null
        }

        val info = LadeSEBeaconInfo(
            isLadeSE = true,
            packetLength = packetData.size,
            rawPacket = packetData.toHexString()
        )

        return try {
            // Extraire le nom complet de l'appareil
            val nameStart = 6
            val ffIndex = packetData.indexOfFirst { it == 0x15.toByte() }
            val nameEnd = if (ffIndex > nameStart) ffIndex else packetData.size

            val deviceName = String(packetData.copyOfRange(nameStart, nameEnd), Charsets.US_ASCII)

            // Extraire les manufacturer data si présentes
            val manufacturerStartIndex = packetData.indexOf(0xFF.toByte())
            val manufacturerData = if (manufacturerStartIndex != -1 && manufacturerStartIndex + 1 < packetData.size) {
                packetData.copyOfRange(manufacturerStartIndex + 1, packetData.size).toHexString()
            } else null

            // Le premier octet après FF peut être un compteur
            val counter = if (manufacturerStartIndex != -1 && manufacturerStartIndex + 1 < packetData.size) {
                packetData[manufacturerStartIndex + 1].toInt() and 0xFF
            } else null

            info.copy(
                deviceName = deviceName,
                manufacturerData = manufacturerData,
                counter = counter
            )
        } catch (e: Exception) {
            info
        }
    }

}