package com.example.apppenon.model

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import com.example.apppenon.activities.MainActivity

/**
 * Classe principale de gestion BLE pour la lecture des données Penon.
 * 
 * Cette classe agit comme un coordinateur principal qui délègue les responsabilités à des managers spécialisés:
 * - CSVManager: Gestion des fichiers CSV
 * - PenonDataParser: Parsing et validation des données
 * - BLEScanManager: Scan BLE et détection des Penons
 * 
 * Architecture:
 * ┌─────────────┐
 * │PenonReader  │ (Coordinateur)
 * └──────┬──────┘
 *        ├─→ CSVManager (Fichiers)
 *        ├─→ PenonDataParser (Décodage)
 *        └─→ BLEScanManager (Scan)
 */
class PenonReader(act: MainActivity) {
    private val act = act
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    lateinit var TARGET_MAC_ADDRESS1: String
    lateinit var TARGET_MAC_ADDRESS2: String

    // Managers délégués
    private val csvManager = CSVManager(act)
    private val dataParser = PenonDataParser()
    val bleScanManager = BLEScanManager(act, bluetoothLeScanner, csvManager, dataParser)

    // Getters pour compatibilité avec le code existant
    var isScanning: Boolean
        get() = bleScanManager.isScanning
        set(value) { bleScanManager.isScanning = value }

    var frameCount1: Int
        get() = bleScanManager.frameCount1
        set(value) { bleScanManager.frameCount1 = value }

    var frameCount2: Int
        get() = bleScanManager.frameCount2
        set(value) { bleScanManager.frameCount2 = value }

    var lastFrameCnt1: Long
        get() = -1L
        set(value) { /* deprecated */ }

    var lastFrameCnt2: Long
        get() = -1L
        set(value) { /* deprecated */ }

    /**
     * Demande les permissions Bluetooth (délégué à BLEScanManager).
     */
    fun requestBluetoothPermissions() {
        bleScanManager.requestBluetoothPermissions()
    }

    /**
     * Démarre le scan BLE (délégué à BLEScanManager).
     */
    fun startScanning() {
        bleScanManager.startScanning(TARGET_MAC_ADDRESS1, TARGET_MAC_ADDRESS2)
    }

    /**
     * Arrête le scan BLE (délégué à BLEScanManager).
     */
    fun stopScanning() {
        bleScanManager.stopScanning(TARGET_MAC_ADDRESS1, TARGET_MAC_ADDRESS2)
    }

    /**
     * Ferme les fichiers CSV (délégué à CSVManager).
     */
    fun closeCSVFiles() {
        csvManager.closeCSVFiles()
    }
}
