package com.example.apppenon

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    lateinit var tvStatus: TextView
    lateinit var tvReceivedData1: TextView
    lateinit var tvReceivedData2: TextView
    lateinit var tvParsedData1: TextView
    lateinit var tvParsedData2: TextView
    private lateinit var btnStartScan: Button
    private lateinit var btnStopScan: Button
    private lateinit var btnClearData: Button
    lateinit var etFileName: android.widget.EditText
    private lateinit var inputMacP1: android.widget.EditText
    private lateinit var inputMacP2: android.widget.EditText
    private val PR = PenonReader(this)

    // Variables pour l'enregistrement CSV - un fichier par penon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvReceivedData1 = findViewById(R.id.tvReceivedData1)
        tvReceivedData2 = findViewById(R.id.tvReceivedData2)
        tvParsedData1 = findViewById(R.id.tvParsedData1)
        tvParsedData2 = findViewById(R.id.tvParsedData2)
        btnStartScan = findViewById(R.id.btnStartScan)
        btnStopScan = findViewById(R.id.btnStopScan)
        btnClearData = findViewById(R.id.btnClearData)
        etFileName = findViewById(R.id.etFileName)
        inputMacP1 = findViewById(R.id.inputMacP1)
        inputMacP2 = findViewById(R.id.inputMacP2)

        if (PR.bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non disponible", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        PR.requestBluetoothPermissions()

        btnStartScan.setOnClickListener {
            PR.TARGET_MAC_ADDRESS1 = inputMacP1.text.toString().trim()
            PR.TARGET_MAC_ADDRESS2 = inputMacP2.text.toString().trim()

            if (PR.TARGET_MAC_ADDRESS1.isEmpty() && PR.TARGET_MAC_ADDRESS2.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer au moins une adresse MAC", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PR.startScanning()
        }

        btnStopScan.setOnClickListener {
            PR.stopScanning()
        }

        btnClearData.setOnClickListener {
            tvReceivedData1.text = ""
            tvParsedData1.text = "En attente de données..."
            tvReceivedData2.text = ""
            tvParsedData2.text = "En attente de données..."
            PR.frameCount1 = 0
            PR.frameCount2 = 0
            PR.lastFrameCnt1 = -1
            PR.lastFrameCnt2 = -1
        }

        updateUIState()
    }

    fun autoScroll() {
        val scrollView1 = findViewById<android.widget.ScrollView>(R.id.scrollView1)
        scrollView1?.post {
            scrollView1.fullScroll(android.view.View.FOCUS_DOWN)
        }
        val scrollView2 = findViewById<android.widget.ScrollView>(R.id.scrollView2)
        scrollView2?.post {
            scrollView2.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    fun updateUIState() {
        btnStartScan.isEnabled = !PR.isScanning
        btnStopScan.isEnabled = PR.isScanning
        etFileName.isEnabled = !PR.isScanning
        inputMacP1.isEnabled = !PR.isScanning
        inputMacP2.isEnabled = !PR.isScanning
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            PR.stopScanning()
            PR.closeCSVFiles()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}