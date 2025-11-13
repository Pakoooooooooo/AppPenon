package com.example.apppenon

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.example.apppenon.Penon

class MainActivity : AppCompatActivity() {

    /* ===== PARAMÈTRES CONFIGURABLES ===== */
    private val windowSize = 10               // nombre de valeurs pour la moyenne glissante (10 sec)
    private val simInterval = 1000L           // intervalle de simulation en ms (1Hz)
    private var fakeDevices = mutableListOf(
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

    /* ===== CODE COMMUN (UI + affichage) ===== */
    private lateinit var tvStatus: TextView
    private lateinit var tvReceivedData: TextView
    private lateinit var tvEtatPenon1: TextView
    private lateinit var tvEtatPenon2: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnClear: Button
    private lateinit var Penon1SettingsBtn: Button
    private lateinit var Penon2SettingsBtn: Button

    private var isRunning = false
    private val uiHandler = Handler(Looper.getMainLooper())

    // Comptage séparé
    private var frameBabord = 0
    private var frameTribord = 0

    // Moyenne glissante (10 valeurs = 10 sec)
    private val babordValues = mutableListOf<Int>()
    private val tribordValues = mutableListOf<Int>()

    private val penonSettingsLauncher1 = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedPenon = result.data?.getSerializableExtra("updated_penon") as? Penon
            if (updatedPenon != null) {
                fakeDevices[0] = updatedPenon
                println("updatedPenon: $updatedPenon")
                tvEtatPenon1.text = "${updatedPenon.penonName} : ✅ Mis à jour"
            }
        } else {
            tvEtatPenon1.text = "${fakeDevices[0].penonName} : ❌ Annulé"
        }
    }

    private val penonSettingsLauncher2 = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedPenon = result.data?.getSerializableExtra("updated_penon") as? Penon
            if (updatedPenon != null) {
                fakeDevices[1] = updatedPenon
                println("updatedPenon: $updatedPenon")
                tvEtatPenon2.text = "${updatedPenon.penonName} : ✅ Mis à jour"
            }
        } else {
            tvEtatPenon2.text = "${fakeDevices[1].penonName} : ❌ Annulé"
        }
    }


    /* ===== CODE SIMULATION ===== */
    private val simulationHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind UI
        tvStatus = findViewById(R.id.tvStatus)
        tvReceivedData = findViewById(R.id.tvReceivedData)
        tvEtatPenon1 = findViewById(R.id.tvEtatPenon1)
        tvEtatPenon2 = findViewById(R.id.tvEtatPenon2)
        btnStart = findViewById(R.id.btnStartScan)
        btnStop = findViewById(R.id.btnStopScan)
        btnClear = findViewById(R.id.btnClearData)

        Penon1SettingsBtn = findViewById(R.id.btnParamPenon1)
        Penon2SettingsBtn = findViewById(R.id.btnParamPenon2)
        tvEtatPenon1.text = fakeDevices[0].penonName+" : ⏳ En attente..."
        tvEtatPenon2.text = fakeDevices[1].penonName+" : ⏳ En attente..."

        Penon1SettingsBtn.setOnClickListener {
            val intent = Intent(this, PenonsSettingsActivity::class.java)
            intent.putExtra("penon_data", fakeDevices[0])
            penonSettingsLauncher1.launch(intent)
            tvEtatPenon1.text = fakeDevices[0].penonName + " : ⏳ En attente..."
        }

        Penon2SettingsBtn.setOnClickListener {
            val intent = Intent(this, PenonsSettingsActivity::class.java)
            intent.putExtra("penon_data", fakeDevices[1])
            penonSettingsLauncher2.launch(intent)
            tvEtatPenon2.text = fakeDevices[2].penonName + " : ⏳ En attente..."
        }

        btnStart.setOnClickListener { startSimulation() }
        btnStop.setOnClickListener { stopSimulation() }
        btnClear.setOnClickListener {
            tvReceivedData.text = ""
            tvEtatPenon1.text = fakeDevices[0].penonName+" : ⏳ En attente..."
            tvEtatPenon2.text = fakeDevices[1].penonName+" : ⏳ En attente..."
            frameBabord = 0
            frameTribord = 0
            babordValues.clear()
            tribordValues.clear()
        }

        updateUI()
    }


    /* ===== CODE COMMUN ===== */
    private fun updateUI() {
        btnStart.isEnabled = !isRunning
        btnStop.isEnabled = isRunning
    }

    private fun autoScroll() {
        val scroll = findViewById<ScrollView>(R.id.scrollView)
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }


    /* ===== CODE SIMULATION ===== */
    private fun startSimulation() {
        frameBabord = 0
        frameTribord = 0
        babordValues.clear()
        tribordValues.clear()
        isRunning = true
        updateUI()
        tvStatus.text = "Simulation 1Hz…"
        tvReceivedData.text = ""  // Supprime "En attente..."

        simulationHandler.post(object : Runnable {
            override fun run() {
                fakeDevices.forEach { (name, _) ->

                    // Génération d'une valeur aléatoire 0-200
                    val value = Random.nextInt(0, 201)
                    val turbulent = value < fakeDevices[0].detachedThresh
                    val hexFrame = generateHexFrame(value, turbulent)

                    // Comptage séparé
                    val frameNumber = if (name == "Babord") {
                        frameBabord++
                        babordValues.add(value)
                        if (babordValues.size > windowSize) babordValues.removeAt(0)
                        frameBabord
                    } else {
                        frameTribord++
                        tribordValues.add(value)
                        if (tribordValues.size > windowSize) tribordValues.removeAt(0)
                        frameTribord
                    }

                    // Calcul moyenne glissante
                    val avgBabord = if (babordValues.isNotEmpty()) babordValues.average() else 0.0
                    val avgTribord = if (tribordValues.isNotEmpty()) tribordValues.average() else 0.0

                    val etatBabord = if (avgBabord >= fakeDevices[0].detachedThresh) "Attaché 🟢" else "Turbulent 🔴"
                    val etatTribord = if (avgTribord >= fakeDevices[0].detachedThresh) "Attaché 🟢" else "Turbulent 🔴"

                    val brut = "[$name] $hexFrame"

                    uiHandler.post {
                        tvReceivedData.append("$brut\n")
                        tvEtatPenon1.text = "Babord : $etatBabord"
                        tvEtatPenon2.text = "Tribord : $etatTribord"
                        tvStatus.text = "B:$frameBabord  T:$frameTribord"
                        autoScroll()
                    }
                }

                simulationHandler.postDelayed(this, simInterval)
            }
        })
    }

    private fun stopSimulation() {
        isRunning = false
        simulationHandler.removeCallbacksAndMessages(null)
        tvStatus.text = "Arrêté (B:$frameBabord  T:$frameTribord)"
        updateUI()
    }

    private fun generateHexFrame(value: Int, turbulent: Boolean): String {
        val b1 = 0xA1
        val b2 = value and 0xFF
        val b3 = if (turbulent) 0x01 else 0x00
        val b4 = Random.nextInt(255)
        val b5 = Random.nextInt(255)
        val b6 = Random.nextInt(255)
        val b7 = Random.nextInt(255)
        val b8 = (b1 + b2 + b3) and 0xFF
        return "%02X %02X %02X %02X %02X %02X %02X %02X"
            .format(b1, b2, b3, b4, b5, b6, b7, b8)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSimulation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val updatedPenon = data?.getSerializableExtra("penon_data") as? Penon
            if (updatedPenon != null) {
                val mutableList = fakeDevices.toMutableList()
                mutableList[0] = updatedPenon
                fakeDevices = mutableList
            }
        }
    }
}
