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
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvReceivedData: TextView
    private lateinit var tvParsedData: TextView
    private lateinit var btnStartScan: Button
    private lateinit var btnStopScan: Button
    private lateinit var btnClearData: Button
    private lateinit var tvTargetMac: TextView

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    private val TARGET_MAC_ADDRESS = "F3:B9:F6:76:07:8F"
    private var frameCount = 0
    private var lastFrameCnt = -1L // Pour détecter les trames perdues

    private val PERMISSION_REQUEST_CODE = 100
    private val TAG = "eTT-SAIL-BLE"

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

                        handler.post {
                            val hexData = manufacturerData.joinToString(" ") {
                                "%02X".format(it)
                            }

                            // LOG DE DEBUG COMPLET
                            Log.d(TAG, "=== TRAME #$frameCount ===")
                            Log.d(TAG, "Taille: ${manufacturerData.size} octets")
                            Log.d(TAG, "HEX: $hexData")
                            Log.d(TAG, "RSSI: $rssi dBm")

                            val currentText = tvReceivedData.text.toString()
                            val newText = "$currentText\n[Trame $frameCount] RSSI: $rssi dBm\nHEX: $hexData\n"
                            tvReceivedData.text = newText

                            val parsedData = parseETTSailData(manufacturerData)
                            tvParsedData.text = parsedData

                            tvStatus.text = "✓ Réception en cours (${frameCount} trames)"

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
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
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

        tvStatus.text = "Écoute des advertising packets..."

        Toast.makeText(this, "Scan BLE démarré (Coded PHY)", Toast.LENGTH_SHORT).show()
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
        updateUIState()

        tvStatus.text = "Scan arrêté - $frameCount trames reçues"
    }

    private fun parseETTSailData(data: ByteArray): String {
        return try {
            // LOG: Afficher les données brutes complètes
            val fullHex = data.joinToString(" ") { "%02X".format(it) }
            Log.d(TAG, "Données complètes: $fullHex")

            var manufacturerData = data
            var dataStartOffset = 0

            // Chercher les données manufacturer dans le payload
            var offset = 0
            var foundManufacturerData = false

            while (offset < data.size - 2) {
                val length = data[offset].toInt() and 0xFF
                if (length == 0) break

                val type = data[offset + 1].toInt() and 0xFF

                Log.d(TAG, "Offset $offset: length=$length, type=0x${"%02X".format(type)}")

                // Type 0xFF = Manufacturer Specific Data
                if (type == 0xFF) {
                    dataStartOffset = offset + 2
                    manufacturerData = data.copyOfRange(offset + 2, minOf(offset + 1 + length, data.size))
                    foundManufacturerData = true
                    Log.d(TAG, "Manufacturer data trouvée à offset $dataStartOffset, taille=${manufacturerData.size}")
                    break
                }

                offset += length + 1
            }

            // Si pas de structure AD, utiliser les données brutes
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

            // TESTER DIFFÉRENTS OFFSETS ET ENDIANNESS
            val results = mutableListOf<String>()

            // Test 1: Sans offset, Little Endian
            results.add(testDecode(manufacturerData, 0, ByteOrder.LITTLE_ENDIAN, "Sans offset, LE"))

            // Test 2: Avec 2 octets offset (Company ID), Little Endian
            if (manufacturerData.size >= 19) {
                results.add(testDecode(manufacturerData, 2, ByteOrder.LITTLE_ENDIAN, "Offset +2, LE"))
            }

            // Test 3: Sans offset, Big Endian
            results.add(testDecode(manufacturerData, 0, ByteOrder.BIG_ENDIAN, "Sans offset, BE"))

            // Test 4: Avec 2 octets offset, Big Endian
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

            // LOG DÉTAILLÉ
            Log.d(TAG, "[$label] frameCnt=$frameCnt, type=$frameType, vbat=$vbat")

            // Vérifier si c'est cohérent
            val vbatV = vbat / 1000.0
            val isCoherent = frameCnt in 0..100000000 &&
                    vbatV in 2.0..4.5 &&
                    frameType in 0..255

            // Détection de trames perdues
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
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            stopScanning()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}