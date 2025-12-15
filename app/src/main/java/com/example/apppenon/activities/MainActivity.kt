package com.example.apppenon.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apppenon.PenonSettingsManager
import com.example.apppenon.UIStateManager
import com.example.apppenon.model.Penon
import com.example.apppenon.model.PenonReader
import com.example.apppenon.model.AppData
import com.example.apppenon.adapters.PenonCardAdapter
import com.example.apppenon.R

/**
 * Activité principale de l'application.
 * 
 * Responsabilités:
 * - Gérer l'interface utilisateur (boutons, TextViews, RecyclerView)
 * - Initialiser les managers de scan BLE et de settings
 * - Coordonner les événements utilisateur
 * 
 * Architecture:
 * MainActivity
 * ├─→ UIStateManager (Gestion UI)
 * ├─→ PenonSettingsManager (Gestion settings Penons)
 * ├─→ PenonReader (Scan BLE via BLEScanManager)
 * └─→ PenonCardAdapter (RecyclerView)
 */
class MainActivity : AppCompatActivity() {

    // Éléments UI
    lateinit var tvStatus: TextView
    lateinit var tvReceivedData1: TextView
    lateinit var tvReceivedData2: TextView
    lateinit var tvParsedData1: TextView
    lateinit var tvParsedData2: TextView
    lateinit var btnStartScan: Button
    lateinit var btnStopScan: Button
    lateinit var btnClearData: Button
    lateinit var etFileName: EditText
    lateinit var btnSetP1: Button
    lateinit var btnSetP2: Button
    lateinit var tvEtatPenon1: TextView
    lateinit var tvEtatPenon2: TextView
    lateinit var appModeBtn: Button
    lateinit var rvPenonCards: RecyclerView
    lateinit var layoutDeveloper: android.view.View

    // Adaptateur RecyclerView
    lateinit var penonCardAdapter: PenonCardAdapter

    // Managers délégués
    private lateinit var uiStateManager: UIStateManager
    private lateinit var penonSettingsManager: PenonSettingsManager

    // Lecteur BLE
    val PR = PenonReader(this)

    // Liste des deux Penons configurables
    val deviceList = mutableListOf(
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialiser les vues
        initializeViews()

        // Initialiser les managers
        uiStateManager = UIStateManager(this)
        penonSettingsManager = PenonSettingsManager(
            this,
            tvEtatPenon1,
            tvEtatPenon2,
            deviceList
        )
        penonSettingsManager.initialize()

        // Initialiser le RecyclerView
        penonCardAdapter = PenonCardAdapter()
        rvPenonCards.layoutManager = LinearLayoutManager(this)
        rvPenonCards.adapter = penonCardAdapter

        // Vérifier la disponibilité de Bluetooth
        if (PR.bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non disponible", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        PR.requestBluetoothPermissions()

        // Configurer les listeners des boutons
        setupButtonListeners()

        // Mettre à jour l'UI
        uiStateManager.updateUIState(PR)
        uiStateManager.updateUIForMode()
    }

    /**
     * Initialise toutes les références aux vues.
     */
    private fun initializeViews() {
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
        tvEtatPenon1.text = deviceList[0].penonName
        tvEtatPenon2.text = deviceList[1].penonName
        appModeBtn = findViewById(R.id.appModeBtn)
        rvPenonCards = findViewById(R.id.rvPenonCards)
        layoutDeveloper = findViewById(R.id.layoutDeveloper)
    }

    /**
     * Configure les listeners pour tous les boutons.
     */
    private fun setupButtonListeners() {
        // Mode d'application
        appModeBtn.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        // Settings Penon 1
        btnSetP1.setOnClickListener {
            penonSettingsManager.launchSettingsForPenon1()
        }

        // Settings Penon 2
        btnSetP2.setOnClickListener {
            penonSettingsManager.launchSettingsForPenon2()
        }

        // Démarrer le scan
        btnStartScan.setOnClickListener {
            if (AppData.mode == 0) {
                // Mode Standard : scanner tous les Penons
                PR.TARGET_MAC_ADDRESS1 = ""
                PR.TARGET_MAC_ADDRESS2 = ""
            } else {
                // Mode Développeur : utiliser les adresses configurées
                PR.TARGET_MAC_ADDRESS1 = penonSettingsManager.getPenon1MacAddress()
                PR.TARGET_MAC_ADDRESS2 = penonSettingsManager.getPenon2MacAddress()

                if (PR.TARGET_MAC_ADDRESS1.isEmpty() && PR.TARGET_MAC_ADDRESS2.isEmpty()) {
                    Toast.makeText(this, "Veuillez entrer au moins une adresse MAC", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            PR.startScanning()
        }

        // Arrêter le scan
        btnStopScan.setOnClickListener {
            PR.stopScanning()
        }

        // Effacer les données
        btnClearData.setOnClickListener {
            tvReceivedData1.text = ""
            tvParsedData1.text = "En attente de données..."
            tvReceivedData2.text = ""
            tvParsedData2.text = "En attente de données..."
            PR.frameCount1 = 0
            PR.frameCount2 = 0

            if (AppData.mode == 0) {
                penonCardAdapter.clearAll()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        uiStateManager.updateUIForMode()
    }

    /**
     * Scroll automatique des TextViews du mode développeur.
     */
    fun autoScroll() {
        uiStateManager.autoScroll()
    }

    /**
     * Met à jour l'état des boutons selon le statut du scan.
     */
    fun updateUIState() {
        uiStateManager.updateUIState(PR)
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
