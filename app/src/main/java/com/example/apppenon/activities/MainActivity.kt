package com.example.apppenon.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apppenon.model.Penon
import com.example.apppenon.model.PenonReader
import com.example.apppenon.model.AppData
import com.example.apppenon.adapters.PenonCardAdapter
import com.example.apppenon.R

class MainActivity : AppCompatActivity() {

    lateinit var tvStatus: TextView
    lateinit var tvReceivedData1: TextView
    lateinit var tvReceivedData2: TextView
    lateinit var tvParsedData1: TextView
    lateinit var tvParsedData2: TextView
    private lateinit var btnStartScan: Button
    private lateinit var btnStopScan: Button
    private lateinit var btnClearData: Button
    lateinit var etFileName: EditText
    private lateinit var btnSetP1: Button
    private lateinit var btnSetP2: Button
    private lateinit var tvEtatPenon1: TextView
    private lateinit var tvEtatPenon2: TextView
    private lateinit var appModeBtn: Button
    private lateinit var rvPenonCards: RecyclerView
    private lateinit var layoutDeveloper: View

    // ✅ NOUVEAU : Adaptateur pour les cartes
    lateinit var penonCardAdapter: PenonCardAdapter

    val PR = PenonReader(this)

    private var devices = mutableListOf(
        Penon(
            penonName = "Penon1 (Babord)",
            macAdress = "AA:BB:CC:DD:EE:01",
            rssi = true,
            rssiLow = -90,
            rssiHigh = -20,
            flowState = true,
            flowStateLow = 500,
            flowStateHigh = 800,
            sDFlowState = true,
            sDFlowStateLow = 100,
            sDFlowStateHigh = 800,
            detachedThresh = 100.0
        ),
        Penon(
            penonName = "Penon2 (Tribord)",
            macAdress = "AA:BB:CC:DD:EE:02",
            rssi = true,
            rssiLow = -90,
            rssiHigh = -20,
            flowState = true,
            flowStateLow = 500,
            flowStateHigh = 800,
            sDFlowState = true,
            sDFlowStateLow = 100,
            sDFlowStateHigh = 800,
            detachedThresh = 100.0
        )
    )

    private val penonSettingsLauncher1 = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedPenon = result.data?.getSerializableExtra("updated_penon") as? Penon
            if (updatedPenon != null) {
                devices[0] = updatedPenon
                println("updatedPenon: $updatedPenon")
                tvEtatPenon1.text = "${updatedPenon.penonName} : ✅ Mis à jour"
            }
        } else {
            tvEtatPenon1.text = "${devices[0].penonName} : ❌ Annulé"
        }
    }

    private val penonSettingsLauncher2 = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedPenon = result.data?.getSerializableExtra("updated_penon") as? Penon
            if (updatedPenon != null) {
                devices[1] = updatedPenon
                println("updatedPenon: $updatedPenon")
                tvEtatPenon2.text = "${updatedPenon.penonName} : ✅ Mis à jour"
            }
        } else {
            tvEtatPenon2.text = "${devices[1].penonName} : ❌ Annulé"
        }
    }

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
        btnSetP1 = findViewById(R.id.btnSetP1)
        btnSetP2 = findViewById(R.id.btnSetP2)
        tvEtatPenon1 = findViewById(R.id.tvEtatPenon1)
        tvEtatPenon2 = findViewById(R.id.tvEtatPenon2)
        tvEtatPenon1.text = devices[0].penonName
        tvEtatPenon2.text = devices[1].penonName
        appModeBtn = findViewById(R.id.appModeBtn)
        rvPenonCards = findViewById(R.id.rvPenonCards)
        layoutDeveloper = findViewById(R.id.layoutDeveloper)

        // ✅ NOUVEAU : Initialiser le RecyclerView
        penonCardAdapter = PenonCardAdapter()
        rvPenonCards.layoutManager = LinearLayoutManager(this)
        rvPenonCards.adapter = penonCardAdapter

        appModeBtn.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        btnSetP1.setOnClickListener {
            val intent = Intent(this, PenonsSettingsActivity::class.java)
            intent.putExtra("penon_data", devices[0])
            penonSettingsLauncher1.launch(intent)
            tvEtatPenon1.text = devices[0].penonName + " : ⏳ En attente..."
        }

        btnSetP2.setOnClickListener {
            val intent = Intent(this, PenonsSettingsActivity::class.java)
            intent.putExtra("penon_data", devices[1])
            penonSettingsLauncher2.launch(intent)
            tvEtatPenon2.text = devices[1].penonName + " : ⏳ En attente..."
        }

        if (PR.bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non disponible", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        PR.requestBluetoothPermissions()

        btnStartScan.setOnClickListener {
            // ✅ En mode Standard, on scanne TOUS les Penons
            if (AppData.mode == 0) {
                PR.TARGET_MAC_ADDRESS1 = "" // Vide = tous les Penons
                PR.TARGET_MAC_ADDRESS2 = ""
            } else {
                // Mode développeur : utiliser les adresses configurées
                PR.TARGET_MAC_ADDRESS1 = devices[0].macAdress
                PR.TARGET_MAC_ADDRESS2 = devices[1].macAdress

                if (PR.TARGET_MAC_ADDRESS1.isEmpty() && PR.TARGET_MAC_ADDRESS2.isEmpty()) {
                    Toast.makeText(this, "Veuillez entrer au moins une adresse MAC", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
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

            // ✅ Effacer les cartes en mode Standard
            if (AppData.mode == 0) {
                penonCardAdapter.clearAll()
            }
        }

        updateUIState()
        updateUIForMode()
    }

    override fun onResume() {
        super.onResume()
        updateUIForMode()
    }

    private fun updateUIForMode() {
        when (AppData.mode) {
            0 -> { // Mode Standard
                // Afficher le RecyclerView, cacher le mode développeur
                rvPenonCards.visibility = View.VISIBLE
                layoutDeveloper.visibility = View.GONE

                tvStatus.visibility = View.VISIBLE
                tvReceivedData1.visibility = View.GONE
                tvReceivedData2.visibility = View.GONE
                tvParsedData1.visibility = View.GONE
                tvParsedData2.visibility = View.GONE
                btnSetP1.visibility = View.GONE
                btnSetP2.visibility = View.GONE
                tvEtatPenon1.visibility = View.GONE
                tvEtatPenon2.visibility = View.GONE

                etFileName.visibility = if (AppData.rec) View.VISIBLE else View.GONE

                btnStartScan.visibility = View.VISIBLE
                btnStopScan.visibility = View.VISIBLE
                btnClearData.visibility = View.VISIBLE
                appModeBtn.visibility = View.VISIBLE
            }
            1 -> { // Mode Développeur
                // Cacher le RecyclerView, afficher le mode développeur
                rvPenonCards.visibility = View.GONE
                layoutDeveloper.visibility = View.VISIBLE

                tvStatus.visibility = View.VISIBLE
                tvReceivedData1.visibility = View.VISIBLE
                tvReceivedData2.visibility = View.VISIBLE
                tvParsedData1.visibility = View.VISIBLE
                tvParsedData2.visibility = View.VISIBLE
                btnSetP1.visibility = View.VISIBLE
                btnSetP2.visibility = View.VISIBLE
                tvEtatPenon1.visibility = View.VISIBLE
                tvEtatPenon2.visibility = View.VISIBLE
                btnStartScan.visibility = View.VISIBLE
                btnStopScan.visibility = View.VISIBLE
                btnClearData.visibility = View.VISIBLE
                appModeBtn.visibility = View.VISIBLE

                etFileName.visibility = if (AppData.rec) View.VISIBLE else View.GONE
            }
        }
    }

    fun autoScroll() {
        val scrollView1 = findViewById<ScrollView>(R.id.scrollView1)
        scrollView1?.post {
            scrollView1.fullScroll(View.FOCUS_DOWN)
        }
        val scrollView2 = findViewById<ScrollView>(R.id.scrollView2)
        scrollView2?.post {
            scrollView2.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun updateUIState() {
        btnStartScan.isEnabled = !PR.isScanning
        btnStopScan.isEnabled = PR.isScanning
        etFileName.isEnabled = !PR.isScanning
        btnSetP1.isEnabled = !PR.isScanning
        btnSetP2.isEnabled = !PR.isScanning
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