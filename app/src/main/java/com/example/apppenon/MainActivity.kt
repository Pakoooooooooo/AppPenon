package com.example.apppenon

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.InputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvReceivedData: TextView
    private lateinit var btnStartScan: Button
    private lateinit var btnStopScan: Button
    private lateinit var btnClearData: Button

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var isScanning = false
    private var isConnected = false
    private val handler = Handler(Looper.getMainLooper())

    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val PERMISSION_REQUEST_CODE = 100

    // Receiver pour détecter les appareils Bluetooth classiques
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                Manifest.permission.BLUETOOTH_CONNECT
                            else
                                Manifest.permission.BLUETOOTH
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }

                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                    device?.let {
                        if (!discoveredDevices.contains(it)) {
                            discoveredDevices.add(it)
                            val deviceName = it.name ?: "Appareil inconnu"
                            val deviceAddress = it.address

                            handler.post {
                                val currentText = tvReceivedData.text.toString()
                                tvReceivedData.text = currentText +
                                        "\n[DÉCOUVERT] $deviceName ($deviceAddress)"
                                autoScroll()
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    handler.post {
                        tvStatus.text = "Scan terminé - ${discoveredDevices.size} appareils trouvés"
                        updateUIState()
                    }
                }
            }
        }
    }

    // Callback pour BLE (Bluetooth Low Energy)
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
            if (!discoveredDevices.contains(device)) {
                discoveredDevices.add(device)

                val deviceName = device.name ?: "Appareil BLE inconnu"
                val deviceAddress = device.address
                val rssi = result.rssi

                // Extraire les données de broadcast si disponibles
                val scanRecord = result.scanRecord
                val broadcastData = scanRecord?.bytes

                handler.post {
                    val currentText = tvReceivedData.text.toString()
                    var newText = currentText +
                            "\n[BLE DÉCOUVERT] $deviceName ($deviceAddress) RSSI: $rssi dBm"

                    // Afficher les données de broadcast
                    if (broadcastData != null && broadcastData.isNotEmpty()) {
                        val hexData = broadcastData.joinToString(" ") {
                            "%02X".format(it)
                        }
                        newText += "\n  Données: $hexData"
                    }

                    tvReceivedData.text = newText
                    autoScroll()
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            handler.post {
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
        btnStartScan = findViewById(R.id.btnStartScan)
        btnStopScan = findViewById(R.id.btnStopScan)
        btnClearData = findViewById(R.id.btnClearData)

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non disponible", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        requestBluetoothPermissions()

        // Enregistrer le receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(bluetoothReceiver, filter)

        btnStartScan.setOnClickListener {
            startScanning()
        }

        btnStopScan.setOnClickListener {
            stopScanning()
        }

        btnClearData.setOnClickListener {
            tvReceivedData.text = ""
            discoveredDevices.clear()
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

        discoveredDevices.clear()
        tvReceivedData.text = "=== SCAN EN COURS ===\n"
        isScanning = true
        updateUIState()

        // Scanner Bluetooth classique
        bluetoothAdapter?.startDiscovery()

        // Scanner BLE
        bluetoothLeScanner?.startScan(bleScanCallback)

        tvStatus.text = "Scan en cours..."

        Toast.makeText(this, "Scan démarré (Bluetooth + BLE)", Toast.LENGTH_SHORT).show()
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

        bluetoothAdapter?.cancelDiscovery()
        bluetoothLeScanner?.stopScan(bleScanCallback)

        isScanning = false
        updateUIState()

        tvStatus.text = "Scan arrêté - ${discoveredDevices.size} appareils trouvés"

        // Afficher la liste des appareils découverts
        if (discoveredDevices.isNotEmpty()) {
            showDiscoveredDevices()
        }
    }

    private fun showDiscoveredDevices() {
        if (ActivityCompat.checkSelfPermission(
                this,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    Manifest.permission.BLUETOOTH_CONNECT
                else
                    Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val deviceList = discoveredDevices.map {
            "${it.name ?: "Inconnu"}\n${it.address}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Appareils découverts (${discoveredDevices.size})")
            .setItems(deviceList) { _, which ->
                connectToDevice(discoveredDevices[which])
            }
            .setNegativeButton("Fermer", null)
            .show()
    }

    private fun connectToDevice(device: BluetoothDevice) {
        tvStatus.text = "Tentative de connexion..."

        Thread {
            try {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            Manifest.permission.BLUETOOTH_CONNECT
                        else
                            Manifest.permission.BLUETOOTH
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@Thread
                }

                bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID)
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()
                inputStream = bluetoothSocket?.inputStream
                isConnected = true

                handler.post {
                    tvStatus.text = "Connecté à ${device.name ?: device.address}"
                    tvReceivedData.text = tvReceivedData.text.toString() +
                            "\n\n=== CONNEXION ÉTABLIE ===\n"
                    updateUIState()
                    Toast.makeText(this, "Connecté avec succès", Toast.LENGTH_SHORT).show()
                }

                listenForData()

            } catch (e: Exception) {
                e.printStackTrace()
                handler.post {
                    tvStatus.text = "Échec de connexion"
                    tvReceivedData.text = tvReceivedData.text.toString() +
                            "\n[ERREUR] Impossible de se connecter: ${e.message}\n"
                    Toast.makeText(this, "Connexion impossible", Toast.LENGTH_LONG).show()
                    autoScroll()
                }
            }
        }.start()
    }

    private fun listenForData() {
        val buffer = ByteArray(1024)
        var bytes: Int

        while (isConnected) {
            try {
                bytes = inputStream?.read(buffer) ?: -1

                if (bytes > 0) {
                    val receivedData = String(buffer, 0, bytes)

                    handler.post {
                        val currentText = tvReceivedData.text.toString()
                        tvReceivedData.text = currentText + receivedData
                        autoScroll()
                    }
                }

            } catch (e: Exception) {
                isConnected = false
                handler.post {
                    tvStatus.text = "Connexion perdue"
                    Toast.makeText(this, "Connexion interrompue", Toast.LENGTH_SHORT).show()
                    updateUIState()
                }
                break
            }
        }
    }

    private fun autoScroll() {
        val scrollView = findViewById<android.widget.ScrollView>(R.id.scrollView)
        scrollView?.post {
            scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun updateUIState() {
        btnStartScan.isEnabled = !isScanning && !isConnected
        btnStopScan.isEnabled = isScanning
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothReceiver)
            stopScanning()
            inputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}