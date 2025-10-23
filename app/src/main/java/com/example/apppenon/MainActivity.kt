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

    // Adresse MAC cible (format avec :)
    private val TARGET_MAC_ADDRESS = "F3:B9:F6:76:07:8F"
    private var frameCount = 0

    private val PERMISSION_REQUEST_CODE = 100

    // Callback pour BLE
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

            // Vérifier si c'est l'appareil cible
            if (device.address.equals(TARGET_MAC_ADDRESS, ignoreCase = true)) {
                val rssi = result.rssi
                val scanRecord = result.scanRecord

                if (scanRecord != null) {
                    // Récupérer les données manufacturer
                    val manufacturerData = scanRecord.getManufacturerSpecificData(0xFFFF)
                        ?: scanRecord.bytes

                    if (manufacturerData != null && manufacturerData.isNotEmpty()) {
                        frameCount++

                        handler.post {
                            // Afficher les données brutes
                            val hexData = manufacturerData.joinToString(" ") {
                                "%02X".format(it)
                            }

                            val currentText = tvReceivedData.text.toString()
                            val newText = "$currentText\n[Trame $frameCount] RSSI: $rssi dBm\nHEX: $hexData\n"
                            tvReceivedData.text = newText

                            // Décoder les données eTT-SAIL
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
        tvReceivedData.text = "=== ÉCOUTE DE $TARGET_MAC_ADDRESS ===\n"
        tvParsedData.text = "En attente de données..."
        isScanning = true
        updateUIState()

        // Configuration du scan BLE avec Coded PHY si disponible
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
            // Chercher les données manufacturer dans le payload complet
            var manufacturerData = data

            // Si les données commencent par un header advertising, chercher les données manufacturer
            var offset = 0
            while (offset < data.size - 2) {
                val length = data[offset].toInt() and 0xFF
                if (length == 0) break

                val type = data[offset + 1].toInt() and 0xFF

                // Type 0xFF = Manufacturer Specific Data
                if (type == 0xFF && length >= 17) {
                    // Les données commencent après le type
                    manufacturerData = data.copyOfRange(offset + 2, offset + 1 + length)
                    break
                }

                offset += length + 1
            }

            // Structure eTT-SAIL : au moins 17 octets
            // 4 bytes: frame_cnt (uint32)
            // 1 byte: frame_type (uint8)
            // 2 bytes: Vbat (int16)
            // 2 bytes: mean_mag_z (int16)
            // 2 bytes: sd_mag_z (int16)
            // 2 bytes: mean_acc (int16)
            // 2 bytes: sd_acc (int16)
            // 2 bytes: max_acc (int16)

            if (manufacturerData.size < 17) {
                return "Données insuffisantes (${manufacturerData.size} octets)\nBrut: ${
                    manufacturerData.joinToString(" ") { "%02X".format(it) }
                }"
            }

            val buffer = ByteBuffer.wrap(manufacturerData).order(ByteOrder.LITTLE_ENDIAN)

            // Ignorer les 2 premiers octets si c'est le company ID
            var startOffset = 0
            if (manufacturerData.size >= 19) {
                startOffset = 2
            }

            buffer.position(startOffset)

            val frameCnt = buffer.int
            val frameType = buffer.get().toInt() and 0xFF
            val vbat = buffer.short.toInt()
            val meanMagZ = buffer.short.toInt()
            val sdMagZ = buffer.short.toInt()
            val meanAcc = buffer.short.toInt()
            val sdAcc = buffer.short.toInt()
            val maxAcc = buffer.short.toInt()

            // Formatage avec unités
            val vbatV = vbat / 1000.0
            val meanMagZmT = meanMagZ / 1000.0
            val sdMagZmT = sdMagZ / 1000.0
            val meanAccG = meanAcc / 1000.0
            val sdAccG = sdAcc / 1000.0
            val maxAccG = maxAcc / 1000.0

            // Déterminer l'état (ACCROCHE/TRANSITION/DECROCHE)
            val stateFlow = when {
                sdMagZmT <= 50 -> "🟢 ACCROCHÉ"
                sdMagZmT >= 150 -> "🔴 DÉCROCHÉ"
                else -> "🟡 TRANSITION"
            }

            buildString {
                appendLine("╔═══════════════════════════════╗")
                appendLine("║    eTT-SAIL - Trame #$frameCnt")
                appendLine("╠═══════════════════════════════╣")
                appendLine("║ Type: $frameType")
                appendLine("║ Batterie: ${"%.2f".format(vbatV)} V")
                appendLine("╠═══════════════════════════════╣")
                appendLine("║ MAGNÉTOMÈTRE (Z)")
                appendLine("║ • Moyenne: ${"%.1f".format(meanMagZmT)} mT")
                appendLine("║ • Écart-type: ${"%.1f".format(sdMagZmT)} mT")
                appendLine("║ • État: $stateFlow")
                appendLine("╠═══════════════════════════════╣")
                appendLine("║ ACCÉLÉROMÈTRE")
                appendLine("║ • Moyenne: ${"%.3f".format(meanAccG)} g")
                appendLine("║ • Écart-type: ${"%.3f".format(sdAccG)} g")
                appendLine("║ • Maximum: ${"%.3f".format(maxAccG)} g")
                appendLine("╚═══════════════════════════════╝")
            }

        } catch (e: Exception) {
            "Erreur de décodage: ${e.message}\n" +
                    "Taille données: ${data.size} octets\n" +
                    "HEX: ${data.joinToString(" ") { "%02X".format(it) }}"
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