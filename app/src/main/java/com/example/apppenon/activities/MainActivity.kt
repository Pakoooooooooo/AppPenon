package com.example.apppenon.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apppenon.PenonSettingsManager
import com.example.apppenon.UIStateManager
import com.example.apppenon.model.Penon
import com.example.apppenon.model.PenonReader
import com.example.apppenon.adapters.PenonCardAdapter
import com.example.apppenon.data.PenonSettingsRepository
import com.example.apppenon.R
import com.example.apppenon.simulation.SimulationConfig
import com.example.apppenon.simulation.CSVSimulator
import kotlinx.coroutines.launch

/**
 * Activité principale - VERSION DYNAMIQUE avec support de simulation.
 *
 * ✅ Détecte automatiquement les Penons via BLE
 * ✅ Crée dynamiquement les objets Penon
 * ✅ 🆕 Support du mode simulation depuis CSV
 */
class MainActivity : AppCompatActivity() {

    lateinit var tvStatus: TextView
    lateinit var btnStartScan: Button
    lateinit var btnStopScan: Button
    lateinit var btnClearData: Button
    lateinit var btnGlobalSettings: Button
    lateinit var etFileName: EditText
    lateinit var rvPenonCards: RecyclerView

    lateinit var penonCardAdapter: PenonCardAdapter

    private lateinit var uiStateManager: UIStateManager
    private lateinit var penonSettingsManager: PenonSettingsManager
    private lateinit var repository: PenonSettingsRepository

    val PR = PenonReader(this)
    
    // 🆕 Simulateur CSV
    private lateinit var csvSimulator: CSVSimulator

    val deviceList = mutableListOf<Penon>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_AppPenon)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = PenonSettingsRepository(this)
        
        // 🆕 Initialiser le simulateur
        csvSimulator = CSVSimulator(this, PR.bleScanManager)

        initializeViews()
        loadKnownPenons()

        uiStateManager = UIStateManager(this)
        penonSettingsManager = PenonSettingsManager(this, deviceList)

        penonCardAdapter = PenonCardAdapter(
            onPenonClick = { detectedPenon ->
                val penon = getOrCreatePenon(detectedPenon.macAddress)
                val intent = Intent(this, PenonsSettingsActivity::class.java)
                intent.putExtra("penon_data", penon)
                startActivity(intent)
            },
            penonSettings = deviceList
        )
        rvPenonCards.layoutManager = LinearLayoutManager(this)
        rvPenonCards.adapter = penonCardAdapter

        if (PR.bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non disponible", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        PR.requestBluetoothPermissions()

        setupButtonListeners()
        uiStateManager.updateUIState(PR)
        observeSettingsChanges()
    }

    private fun loadKnownPenons() {
        val knownMacs = repository.getAllKnownMacAddresses()

        knownMacs.forEach { mac ->
            val penon = Penon(macAdress = mac)
            repository.loadPenon(penon)
            deviceList.add(penon)
        }

        if (deviceList.isNotEmpty()) {
            Toast.makeText(
                this,
                "${deviceList.size} Penon(s) chargé(s)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun getOrCreatePenon(macAddress: String): Penon {
        var penon = deviceList.find { it.macAdress == macAddress }

        if (penon == null) {
            penon = Penon(
                penonName = "Penon ${macAddress.takeLast(5)}",
                macAdress = macAddress,
                rssi = true,
                rssiLow = -90,
                rssiHigh = -20,
                flowState = true,
                flowStateThreshold = 500,
                sDFlowState = true,
                sDFlowStateLow = 100,
                sDFlowStateHigh = 800,
                detachedThresh = 100.0
            )

            repository.loadPenon(penon)
            deviceList.add(penon)

            Toast.makeText(
                this,
                "Nouveau Penon détecté : ${penon.penonName}",
                Toast.LENGTH_SHORT
            ).show()
        }

        return penon
    }

    private fun observeSettingsChanges() {
        lifecycleScope.launch {
            repository.observeAllPenons().collect { allPenons ->
                allPenons.forEach { (mac, penon) ->
                    val index = deviceList.indexOfFirst { it.macAdress == mac }
                    if (index != -1 && penon != null) {
                        deviceList[index] = penon.copy()
                    }
                }
                penonCardAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        deviceList.forEach { penon ->
            repository.loadPenon(penon)
        }
        penonCardAdapter.notifyDataSetChanged()

        // Si on revient des paramètres et que le mode simulation a été désactivé
        if (!SimulationConfig.isSimulationMode) {
            ensureSimulationStopped()
        }
    }

    private fun initializeViews() {
        tvStatus = findViewById(R.id.tvStatus)
        btnStartScan = findViewById(R.id.btnStartScan)
        btnStopScan = findViewById(R.id.btnStopScan)
        btnClearData = findViewById(R.id.btnClearData)
        btnGlobalSettings = findViewById(R.id.btnGlobalSettings)
        etFileName = findViewById(R.id.etFileName)
        rvPenonCards = findViewById(R.id.rvPenonCards)
    }

    private fun setupButtonListeners() {
        // 🆕 Démarrer le scan (BLE ou Simulation)
        btnStartScan.setOnClickListener {
            if (SimulationConfig.isReadyToSimulate()) {
                startSimulation()
            } else if (SimulationConfig.isSimulationMode) {
                Toast.makeText(
                    this,
                    "Veuillez sélectionner un fichier CSV dans les paramètres",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                startRealBLEScan()
            }
            updateColor()
        }

        // 🆕 Arrêter le scan (BLE ou Simulation)
        btnStopScan.setOnClickListener {
            if (SimulationConfig.isSimulationMode) {
                if (csvSimulator.isRunning()) {
                    // Mettre en pause
                    csvSimulator.pauseSimulation()
                    tvStatus.text = "⏸️ Simulation en pause"
                    btnStopScan.text = "▶️ Reprendre"
                    Toast.makeText(this, "Simulation en pause", Toast.LENGTH_SHORT).show()
                } else if (csvSimulator.isPaused()) {
                    // Reprendre
                    csvSimulator.resumeSimulation()
                    tvStatus.text = "🎬 Simulation en cours (${csvSimulator.getFrameCount()} trames)"
                    btnStopScan.text = "⏸️ Pause"
                    Toast.makeText(this, "Simulation reprise", Toast.LENGTH_SHORT).show()
                }
            } else {
                stopRealBLEScan()
                btnStopScan.text = "⏹️ Arrêter"
            }
            updateColor()
        }

        btnClearData.setOnClickListener {
            penonCardAdapter.clearAll()

            // Si en mode simulation, arrêter et réinitialiser
            if (SimulationConfig.isSimulationMode) {
                csvSimulator.reset()
                tvStatus.text = "En attente..."
            }

            updateColor()
        }

        // 🆕 Bouton Paramètres globaux (Simulation)
        btnGlobalSettings.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * 🆕 Démarre la simulation depuis le fichier CSV.
     */
    private fun startSimulation() {
        val uri = SimulationConfig.csvFileUri ?: return

        // Charger le fichier CSV
        val success = csvSimulator.loadCSVFile(uri)

        if (success) {
            csvSimulator.startSimulation()
            tvStatus.text = "🎬 Simulation en cours (${csvSimulator.getFrameCount()} trames)"
            btnStopScan.text = "⏸️ Pause"
            Toast.makeText(
                this,
                "Simulation démarrée : ${SimulationConfig.csvFileName}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Erreur : impossible de charger le fichier CSV",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 🆕 Arrête complètement la simulation.
     */
    private fun stopSimulation() {
        csvSimulator.stopSimulation()
        tvStatus.text = "⏹️ Simulation arrêtée"
        btnStopScan.text = "⏹️ Arrêter"
        Toast.makeText(this, "Simulation arrêtée", Toast.LENGTH_SHORT).show()
    }

    /**
     * Arrête la simulation si elle est en cours (appelé lors du changement de mode).
     */
    private fun ensureSimulationStopped() {
        if (csvSimulator.isRunning() || csvSimulator.isPaused()) {
            csvSimulator.stopSimulation()
            tvStatus.text = "En attente..."
            btnStopScan.text = "⏹️ Arrêter"
        }
    }

    /**
     * Démarre le scan BLE réel.
     */
    private fun startRealBLEScan() {
        PR.TARGET_MAC_ADDRESS1 = ""
        PR.TARGET_MAC_ADDRESS2 = ""
        PR.startScanning()
    }

    /**
     * Arrête le scan BLE réel.
     */
    private fun stopRealBLEScan() {
        PR.stopScanning()
    }

    fun updateColor(){
        if (btnStartScan.isEnabled) {
            btnStartScan.setBackgroundColor(resources.getColor(R.color.sea))
        } else {
            btnStartScan.setBackgroundColor(resources.getColor(R.color.grey))
        }
        if (btnStopScan.isEnabled) {
            btnStopScan.setBackgroundColor(resources.getColor(R.color.sea))
        }
        else {
            btnStopScan.setBackgroundColor(resources.getColor(R.color.grey))
        }
        if (btnClearData.isEnabled) {
            btnClearData.setBackgroundColor(resources.getColor(R.color.sea))
        }
        else {
            btnClearData.setBackgroundColor(resources.getColor(R.color.grey))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            csvSimulator.stopSimulation()
            PR.stopScanning()
            PR.closeCSVFiles()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}