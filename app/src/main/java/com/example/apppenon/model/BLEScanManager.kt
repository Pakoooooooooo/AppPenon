package com.example.apppenon.model

import android.Manifest
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
import com.example.apppenon.activities.MainActivity

/**
 * Gère le scan BLE, la détection des Penons et les permissions.
 * Responsabilités:
 * - Demander les permissions Bluetooth
 * - Démarrer/arrêter le scan BLE
 * - Traiter les résultats du scan (modes Standard et Développeur)
 * - Valider les paquets BLE reçus
 */
class BLEScanManager(
    private val act: MainActivity,
    private val bluetoothLeScanner: BluetoothLeScanner?,
    private val csvManager: CSVManager,
    private val dataParser: PenonDataParser
) {
    
    private val TAG = "eTT-SAIL-BLE"
    private val handler = Handler(Looper.getMainLooper())
    private val PERMISSION_REQUEST_CODE = 100
    
    // Adresses MAC cibles (stockées localement)
    private var targetMacAddress1 = ""
    private var targetMacAddress2 = ""
    
    // Map pour suivre les compteurs de chaque Penon en mode Standard
    private val penonFrameCounts = mutableMapOf<String, Int>()

    var isScanning = false
    var frameCount1 = 0
    var frameCount2 = 0

    /**
     * Valide si le paquet est un beacon LadeSE valide.
     * Signature attendue: 02 01 06 14 09 42
     */
    fun isLadeSEBeacon(packetData: ByteArray): Boolean {
        if (packetData.size < 45) {
            Log.d(TAG, "isLadeSEBeacon: paquet trop court (${packetData.size} octets)")
            return false
        }

        val expectedPrefix = byteArrayOf(0x02, 0x01, 0x06, 0x14, 0x09, 0x42)
        val isValid = packetData.startsWith(expectedPrefix)
        
        if (isValid) {
            Log.d(TAG, "isLadeSEBeacon: ✅ Signature valide détectée!")
        } else {
            val actualPrefix = packetData.take(minOf(6, packetData.size))
                .joinToString(" ") { "%02X".format(it) }
            Log.d(TAG, "isLadeSEBeacon: ❌ Signature invalide. Reçu: $actualPrefix, attendu: 02 01 06 14 09 42")
        }
        
        return isValid
    }

    /**
     * Demande les permissions Bluetooth nécessaires à l'utilisateur.
     */
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

    /**
     * Traite les données reçues d'un Penon en mode Développeur.
     */
    fun handlePenonData(result: ScanResult, penonNumber: Int, targetMacAddress: String) {
        val rssi = result.rssi
        val scanRecord = result.scanRecord

        if (scanRecord != null) {
            val manufacturerData = scanRecord.getManufacturerSpecificData(0xFFFF)
                ?: scanRecord.bytes

            if (manufacturerData != null && manufacturerData.isNotEmpty()) {
                if (penonNumber == 1) {
                    frameCount1++
                } else {
                    frameCount2++
                }

                val currentFrameCount = if (penonNumber == 1) frameCount1 else frameCount2

                // Enregistrer si rec activé (peu importe le mode)
                if (AppData.rec && csvManager.isRecordingActive()) {
                    csvManager.saveToCSV(manufacturerData, rssi, penonNumber, currentFrameCount, targetMacAddress)
                }

                handler.post {
                    val hexData = manufacturerData.joinToString(" ") {
                        "%02X".format(it)
                    }

                    Log.d(TAG, "=== PENON $penonNumber - TRAME #$currentFrameCount ===")
                    Log.d(TAG, "MAC: $targetMacAddress")
                    Log.d(TAG, "Taille: ${manufacturerData.size} octets")
                    Log.d(TAG, "HEX: $hexData")
                    Log.d(TAG, "RSSI: $rssi dBm")

                    // Parser les données (sans affichage UI)
                    val parsedData = dataParser.parseETTSailData(manufacturerData, penonNumber)
                    Log.d(TAG, "Parsed: $parsedData")
                    
                    // Décoder les données et envoyer à PenonsSettingsActivity
                    val decodedData = dataParser.decodePenonData(manufacturerData)
                    if (decodedData != null) {
                        // Importer pour utiliser le callback statique
                        try {
                            val settingsActivityClass = Class.forName("com.example.apppenon.activities.PenonsSettingsActivity")
                            val updateMethod = settingsActivityClass.getDeclaredMethod(
                                "updateDecodedData", 
                                PenonDecodedData::class.java, 
                                String::class.java
                            )
                            updateMethod.invoke(null, decodedData, targetMacAddress)
                        } catch (e: Exception) {
                            Log.d(TAG, "Could not update PenonsSettingsActivity: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    /**
     * Démarrage du scan BLE avec les paramètres appropriés.
     */
    fun startScanning(targetMacAddress1: String, targetMacAddress2: String) {
        // Stocker les adresses localement
        this.targetMacAddress1 = targetMacAddress1
        this.targetMacAddress2 = targetMacAddress2
        
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
        penonFrameCounts.clear()
        dataParser.resetFrameCounters()

        isScanning = true

        // Créer les fichiers CSV si rec activé (peu importe le mode)
        if (AppData.rec) {
            csvManager.createCSVFiles(targetMacAddress1, targetMacAddress2)
        }

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

        val listeningMsg = when (AppData.mode) {
            0 -> if (AppData.rec) "📝 Scan et enregistrement..." else "✓ Recherche de Penons..."
            1 -> buildString {
                if (AppData.rec) append("📝 ")
                append("Écoute: ")
                if (targetMacAddress1.isNotEmpty()) append("P1 ")
                if (targetMacAddress2.isNotEmpty()) append("P2 ")
            }
            else -> "Scan démarré"
        }
        act.tvStatus.text = listeningMsg

        val toastMsg = if (AppData.rec) {
            "Scan BLE démarré - Enregistrement CSV actif"
        } else {
            "Scan BLE démarré"
        }
        Toast.makeText(act, toastMsg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Arrête le scan BLE et ferme les fichiers CSV.
     */
    fun stopScanning(targetMacAddress1: String, targetMacAddress2: String) {
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
        csvManager.closeCSVFiles()

        val (csvFile1, csvFile2) = csvManager.getCreatedFiles()

        val statusMsg = when (AppData.mode) {
            0 -> "✓ Scan arrêté - ${act.penonCardAdapter.itemCount} Penon(s) détecté(s)"
            1 -> buildString {
                appendLine("Scan arrêté - P1: $frameCount1, P2: $frameCount2 (Total: ${frameCount1 + frameCount2})")
                if (csvFile1 != null) {
                    appendLine("Fichier P1: ${csvFile1.name}")
                }
                if (csvFile2 != null) {
                    appendLine("Fichier P2: ${csvFile2.name}")
                }
            }
            else -> "Scan arrêté"
        }
        act.tvStatus.text = statusMsg

        if (AppData.mode == 1 && (csvFile1 != null || csvFile2 != null)) {
            val toastMsg = buildString {
                appendLine("Données enregistrées:")
                if (csvFile1 != null) {
                    appendLine("P1: ${csvFile1.absolutePath}")
                }
                if (csvFile2 != null) {
                    appendLine("P2: ${csvFile2.absolutePath}")
                }
            }
            Toast.makeText(act, toastMsg, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(act, "Scan arrêté", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Callback BLE qui traite les résultats du scan.
     */
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

            if (AppData.mode == 0) {
                val scanRecord = result.scanRecord
                val manufacturerData = scanRecord?.getManufacturerSpecificData(0xFFFF)
                    ?: scanRecord?.bytes

                if (manufacturerData != null && manufacturerData.isNotEmpty()) {
                    val hexString = manufacturerData.take(minOf(20, manufacturerData.size))
                        .joinToString(" ") { "%02X".format(it) }
                    Log.d(TAG, "Scan reçu de $deviceAddress - Données: $hexString...")

                    if (isLadeSEBeacon(manufacturerData)) {
                        Log.d(TAG, "✅ Penon détecté: $deviceAddress")

                        val currentCount = penonFrameCounts.getOrDefault(deviceAddress, 0) + 1
                        penonFrameCounts[deviceAddress] = currentCount

                        val penonData = dataParser.extractPenonData(manufacturerData)

                        // ✅ Décoder les données complètes
                        val decodedData = dataParser.decodePenonData(manufacturerData)

                        val detectedPenon = DetectedPenon(
                            macAddress = deviceAddress,
                            name = "Penon ${deviceAddress.takeLast(5)}",
                            rssi = result.rssi,
                            frameCount = currentCount,
                            battery = penonData?.first ?: 0f,
                            flowState = penonData?.second ?: 0,
                            lastUpdate = System.currentTimeMillis(),
                            rawHexData = manufacturerData
                        )

                        handler.post {
                            // ✅ Utiliser la méthode existante d'update
                            act.penonCardAdapter.updatePenon(detectedPenon, this@BLEScanManager)

                            val totalPenons = act.penonCardAdapter.itemCount
                            val recordingStatus = if (csvManager.isRecordingActive() && AppData.rec) "🔴 " else ""
                            act.tvStatus.text = "${recordingStatus}📡 $totalPenons Penon(s) détecté(s)"
                        }
                    }
                } else {
                    Log.d(TAG, "Aucune donnée manufacturer reçue de $deviceAddress")
                }
            } else if (AppData.mode == 1) {
                // Mode Développeur: listen to specific MAC addresses
                if (deviceAddress == targetMacAddress1) {
                    handlePenonData(result, 1, targetMacAddress1)
                } else if (deviceAddress == targetMacAddress2) {
                    handlePenonData(result, 2, targetMacAddress2)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            handler.post {
                act.tvStatus.text = "Échec du scan BLE: $errorCode"
                Toast.makeText(act, "Échec du scan BLE: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Extension pour vérifier si un ByteArray commence par un prefix donné.
     */
    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (this.size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }
}
