package com.example.apppenon

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvReceivedData: TextView
    private lateinit var tvParsedData: TextView
    private lateinit var btnStartScan: Button
    private lateinit var btnStopScan: Button
    private lateinit var btnClearData: Button
    private lateinit var tvTargetMac: TextView
    private lateinit var etFileName: android.widget.EditText

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    private val TARGET_MAC_ADDRESS = "DF:92:AC:16:EE:16"
    private var frameCount = 0
    private var lastFrameCnt = -1L

    private val PERMISSION_REQUEST_CODE = 100
    private val TAG = "eTT-SAIL-BLE"

    // Variables pour l'enregistrement CSV
    private var csvFile: File? = null
    private var csvWriter: FileWriter? = null
    private var isRecording = false

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        Manifest.permission.BLUETOOTH_CONNECT
                    else
                        Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val device = result.device

            if (device.address.equals(TARGET_MAC_ADDRESS, ignoreCase = true)) {
                val rssi = result.rssi
                val scanRecord = result.scanRecord

                if (scanRecord != null) {
                    val manufacturerData = scanRecord.getManufacturerSpecificData(0xFFFF)
                        ?: scanRecord.bytes

                    if (manufacturerData != null && manufacturerData.isNotEmpty()) {
                        frameCount++

                        // Enregistrer dans le CSV
                        if (isRecording) {
                            saveToCSV(manufacturerData, rssi)
                        }

                        handler.post {
                            val hexData = manufacturerData.joinToString(" ") {
                                "%02X".format(it)
                            }

                            Log.d(TAG, "=== TRAME #$frameCount ===")
                            Log.d(TAG, "Taille: ${manufacturerData.size} octets")
                            Log.d(TAG, "HEX: $hexData")
                            Log.d(TAG, "RSSI: $rssi dBm")

                            val currentText = tvReceivedData.text.toString()
                            val newText = "$currentText\n[Trame $frameCount] RSSI: $rssi dBm\nHEX: $hexData\n"
                            tvReceivedData.text = newText

                            val parsedData = parseETTSailData(manufacturerData)
                            tvParsedData.text = parsedData

                            val recordingStatus = if (isRecording) "📝" else ""
                            tvStatus.text = "$recordingStatus✓ Réception en cours (${frameCount} trames)"

                            autoScroll()
                        }
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            handler.post {
                tvStatus.text = "Échec du scan BLE: $errorCode"
                Toast.makeText(
                    this@MainActivity,
                    "Échec du scan BLE: $errorCode",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvReceivedData = findViewById(R.id.tvReceivedData)
        tvParsedData = findViewById(R.id.tvParsedData)
        btnStartScan = findViewById(R.id.btnStartScan)
        btnStopScan = findViewById(R.id.btnStopScan)
        btnClearData = findViewById(R.id.btnClearData)
        tvTargetMac = findViewById(R.id.tvTargetMac)
        etFileName = findViewById(R.id.etFileName)

        tvTargetMac.text = "Appareil eTT-SAIL: $TARGET_MAC_ADDRESS"

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non disponible", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        requestBluetoothPermissions()

        btnStartScan.setOnClickListener {
            startScanning()
        }

        btnStopScan.setOnClickListener {
            stopScanning()
        }

        btnClearData.setOnClickListener {
            tvReceivedData.text = ""
            tvParsedData.text = "En attente de données..."
            frameCount = 0
            lastFrameCnt = -1
        }

        updateUIState()
    }

    private fun requestBluetoothPermissions() {
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
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startScanning() {
        if (ActivityCompat.checkSelfPermission(
                this,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    Manifest.permission.BLUETOOTH_SCAN
                else
                    Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permissions manquantes", Toast.LENGTH_SHORT).show()
            return
        }

        frameCount = 0
        lastFrameCnt = -1
        tvReceivedData.text = "=== ÉCOUTE DE $TARGET_MAC_ADDRESS ===\n"
        tvParsedData.text = "En attente de données..."
        isScanning = true

        // Créer un nouveau fichier CSV
        createCSVFile()

        updateUIState()

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

        tvStatus.text = "📝 Écoute et enregistrement..."

        Toast.makeText(this, "Scan BLE démarré - Enregistrement CSV actif", Toast.LENGTH_SHORT).show()
    }

    private fun stopScanning() {
        if (ActivityCompat.checkSelfPermission(
                this,
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

        // Fermer le fichier CSV
        closeCSVFile()

        updateUIState()

        val csvPath = csvFile?.absolutePath ?: "inconnu"
        tvStatus.text = "Scan arrêté - $frameCount trames reçues\nFichier: $csvPath"

        Toast.makeText(this, "Données enregistrées dans:\n$csvPath", Toast.LENGTH_LONG).show()
    }

    private fun createCSVFile() {
        try {
            // Utiliser le dossier Documents public
            val documentsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOCUMENTS
            )

            // Créer un sous-dossier pour l'application
            val appFolder = File(documentsDir, "eTT_SAIL")
            if (!appFolder.exists()) {
                appFolder.mkdirs()
            }

            // Récupérer le nom personnalisé ou utiliser le format par défaut
            var customName = etFileName.text.toString().trim()

            val fileName: String
            if (customName.isNotEmpty()) {
                // Nettoyer le nom (retirer caractères invalides)
                customName = customName.replace(Regex("[^a-zA-Z0-9_-]"), "_")

                // Ajouter .csv si pas déjà présent
                fileName = if (customName.endsWith(".csv", ignoreCase = true)) {
                    customName
                } else {
                    "$customName.csv"
                }

                // Si le fichier existe déjà, ajouter un suffixe numérique
                var finalFile = File(appFolder, fileName)
                var counter = 1
                val baseNameWithoutExt = fileName.removeSuffix(".csv")

                while (finalFile.exists()) {
                    val numberedFileName = "${baseNameWithoutExt}_$counter.csv"
                    finalFile = File(appFolder, numberedFileName)
                    counter++
                }

                csvFile = finalFile
            } else {
                // Format par défaut : rec_0001.csv
                var fileNumber = 1
                var tempFile: File

                do {
                    val defaultFileName = "rec_${"%04d".format(fileNumber)}.csv"
                    tempFile = File(appFolder, defaultFileName)
                    fileNumber++
                } while (tempFile.exists())

                csvFile = tempFile
            }

            csvWriter = FileWriter(csvFile, false)

            // Écrire l'en-tête du CSV
            csvWriter?.write("Timestamp,Frame_Number,RSSI,Data_Size,Raw_Hex_Data\n")
            csvWriter?.flush()

            isRecording = true

            Log.d(TAG, "Fichier CSV créé: ${csvFile?.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur création fichier CSV", e)
            Toast.makeText(this, "Erreur création CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            isRecording = false
        }
    }

    private fun saveToCSV(data: ByteArray, rssi: Int) {
        try {
            if (!isRecording || csvWriter == null) return

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val hexData = data.joinToString(" ") { "%02X".format(it) }

            // Format: Timestamp, Frame Number, RSSI, Data Size, Raw Hex Data
            val line = "$timestamp,$frameCount,$rssi,${data.size},\"$hexData\"\n"

            csvWriter?.write(line)
            csvWriter?.flush() // Forcer l'écriture immédiate

        } catch (e: Exception) {
            Log.e(TAG, "Erreur écriture CSV", e)
            // Ne pas arrêter l'enregistrement pour une erreur ponctuelle
        }
    }

    private fun closeCSVFile() {
        try {
            csvWriter?.flush()
            csvWriter?.close()
            csvWriter = null
            isRecording = false

            Log.d(TAG, "Fichier CSV fermé: ${csvFile?.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur fermeture fichier CSV", e)
        }
    }

    private fun parseETTSailData(data: ByteArray): String {
        return try {
            val fullHex = data.joinToString(" ") { "%02X".format(it) }
            Log.d(TAG, "Données complètes: $fullHex")

            var manufacturerData = data
            var dataStartOffset = 0

            var offset = 0
            var foundManufacturerData = false

            while (offset < data.size - 2) {
                val length = data[offset].toInt() and 0xFF
                if (length == 0) break

                val type = data[offset + 1].toInt() and 0xFF

                Log.d(TAG, "Offset $offset: length=$length, type=0x${"%02X".format(type)}")

                if (type == 0xFF) {
                    dataStartOffset = offset + 2
                    manufacturerData = data.copyOfRange(offset + 2, minOf(offset + 1 + length, data.size))
                    foundManufacturerData = true
                    Log.d(TAG, "Manufacturer data trouvée à offset $dataStartOffset, taille=${manufacturerData.size}")
                    break
                }

                offset += length + 1
            }

            if (!foundManufacturerData) {
                Log.d(TAG, "Pas de structure AD détectée, utilisation données brutes")
            }

            val dataHex = manufacturerData.joinToString(" ") { "%02X".format(it) }
            Log.d(TAG, "Données à décoder: $dataHex")

            if (manufacturerData.size < 17) {
                return "⚠️ Données insuffisantes (${manufacturerData.size} octets)\n" +
                        "Minimum requis: 17 octets\n\n" +
                        "HEX: $dataHex"
            }

            val results = mutableListOf<String>()

            results.add(testDecode(manufacturerData, 0, ByteOrder.LITTLE_ENDIAN, "Sans offset, LE"))

            if (manufacturerData.size >= 19) {
                results.add(testDecode(manufacturerData, 2, ByteOrder.LITTLE_ENDIAN, "Offset +2, LE"))
            }

            results.add(testDecode(manufacturerData, 0, ByteOrder.BIG_ENDIAN, "Sans offset, BE"))

            if (manufacturerData.size >= 19) {
                results.add(testDecode(manufacturerData, 2, ByteOrder.BIG_ENDIAN, "Offset +2, BE"))
            }

            buildString {
                appendLine("═══════════════════════════════")
                appendLine("TESTS DE DÉCODAGE")
                appendLine("═══════════════════════════════")
                results.forEach { appendLine(it) }
                appendLine("═══════════════════════════════")
                appendLine("\nDonnées brutes:")
                appendLine(dataHex)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur de décodage", e)
            "❌ Erreur: ${e.message}\n" +
                    "Taille: ${data.size} octets\n" +
                    "HEX: ${data.joinToString(" ") { "%02X".format(it) }}"
        }
    }

    private fun testDecode(data: ByteArray, offset: Int, order: ByteOrder, label: String): String {
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

            Log.d(TAG, "[$label] frameCnt=$frameCnt, type=$frameType, vbat=$vbat")

            val vbatV = vbat / 1000.0
            val isCoherent = frameCnt in 0..100000000 &&
                    vbatV in 2.0..4.5 &&
                    frameType in 0..255

            val lostFrames = if (lastFrameCnt >= 0 && frameCnt > lastFrameCnt) {
                val lost = frameCnt - lastFrameCnt - 1
                if (lost > 0) " ⚠️ $lost trame(s) perdue(s)" else ""
            } else ""

            if (isCoherent && lastFrameCnt < frameCnt) {
                lastFrameCnt = frameCnt
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

    private fun autoScroll() {
        val scrollView = findViewById<android.widget.ScrollView>(R.id.scrollView)
        scrollView?.post {
            scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun updateUIState() {
        btnStartScan.isEnabled = !isScanning
        btnStopScan.isEnabled = isScanning
        etFileName.isEnabled = !isScanning // Désactiver pendant l'enregistrement
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            stopScanning()
            closeCSVFile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}