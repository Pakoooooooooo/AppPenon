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
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    // Managers délégués
    private val csvManager = CSVManager()
    private val dataParser = PenonDataParser()
    val bleScanManager = BLEScanManager(act, bluetoothLeScanner, csvManager, dataParser)

    // Getters pour compatibilité avec le code existant
    var isScanning: Boolean
        get() = bleScanManager.isScanning
        set(value) { bleScanManager.isScanning = value }

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
        bleScanManager.startScanning()
    }

    /**
     * Arrête le scan BLE (délégué à BLEScanManager).
     */
    fun stopScanning() {
        bleScanManager.stopScanning()
    }

    /**
     * Ferme les fichiers CSV (délégué à CSVManager).
     */
    fun closeCSVFiles() {
        csvManager.closeCSVFiles()
    }
}
