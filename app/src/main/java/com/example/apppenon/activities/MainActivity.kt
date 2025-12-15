package com.example.apppenon.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apppenon.model.PenonReader
import com.example.apppenon.R

class MainActivity : AppCompatActivity() {

    lateinit var tvStatus: TextView
    lateinit var tvReceivedData: TextView
    lateinit var tvParsedData: TextView
    private lateinit var btnStartScan: Button
    private lateinit var btnStopScan: Button
    private lateinit var btnClearData: Button
    lateinit var etFileName: EditText
    private lateinit var appModeBtn: Button

    private val PR = PenonReader(this)


    // Variables pour l'enregistrement CSV - un fichier par penon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvReceivedData = findViewById(R.id.tvReceivedData)
        tvParsedData = findViewById(R.id.tvParsedData)
        btnStartScan = findViewById(R.id.btnStartScan)
        btnStopScan = findViewById(R.id.btnStopScan)
        btnClearData = findViewById(R.id.btnClearData)
        etFileName = findViewById(R.id.etFileName)
        appModeBtn = findViewById(R.id.appModeBtn)

        appModeBtn.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        if (PR.bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non disponible", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        PR.requestBluetoothPermissions()

        btnStartScan.setOnClickListener {
            PR.startScanning()
        }

        btnStopScan.setOnClickListener {
            PR.stopScanning()
        }

        btnClearData.setOnClickListener {
            tvReceivedData.text = ""
            tvParsedData.text = "En attente de données..."
            PR.frameCount.forEachIndexed { index, value ->
                PR.frameCount[index] = 0
            }
            PR.lastFrameCnt.forEachIndexed { index, value ->
                PR.lastFrameCnt[index] = -1
            }
        }

        updateUIState()
    }

    fun autoScroll() {
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        scrollView?.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun updateUIState() {
        btnStartScan.isEnabled = !PR.isScanning
        btnStopScan.isEnabled = PR.isScanning
        etFileName.isEnabled = !PR.isScanning
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