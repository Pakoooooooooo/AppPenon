package com.example.apppenon.activities

import android.annotation.SuppressLint
import android.util.Log
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
import com.example.apppenon.UIStateManager
import com.example.apppenon.model.AppData
import com.example.apppenon.model.Penon
import com.example.apppenon.model.PenonGroup
import com.example.apppenon.model.PenonReader
import com.example.apppenon.adapters.MainListAdapter
import com.example.apppenon.data.GroupRepository
import com.example.apppenon.data.PenonSettingsRepository
import com.example.apppenon.R
import com.example.apppenon.model.simulation.SimulationConfig
import com.example.apppenon.model.simulation.CSVSimulator
import com.example.apppenon.utils.VoiceNotificationManager
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    lateinit var tvStatus: TextView
    lateinit var btnStartScan: Button
    lateinit var btnStopScan: Button
    lateinit var btnClearData: Button
    lateinit var btnGlobalSettings: Button
    lateinit var btnAddGroup: Button
    lateinit var etFileName: EditText
    lateinit var rvMain: RecyclerView

    lateinit var mainListAdapter: MainListAdapter
    private lateinit var uiStateManager: UIStateManager
    private lateinit var repository: PenonSettingsRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var voiceNotificationManager: VoiceNotificationManager
    private var wasScanning: Boolean = false

    val PR = PenonReader(this)
    private lateinit var csvSimulator: CSVSimulator

    val deviceList = mutableListOf<Penon>()
    private val groups = mutableListOf<PenonGroup>()

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: MainActivity? = null
        fun getInstance(): MainActivity? = instance
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_AppPenon)
        super.onCreate(savedInstanceState)
        instance = this
        setContentView(R.layout.activity_main)

        repository = PenonSettingsRepository(this)
        groupRepository = GroupRepository(this)
        voiceNotificationManager = VoiceNotificationManager(this)

        val globalPrefs = getSharedPreferences("global_settings", MODE_PRIVATE)
        AppData.muteTimeSeconds = globalPrefs.getInt("mute_time_seconds", 0)

        csvSimulator = CSVSimulator(this, PR.bleScanManager)

        initializeViews()
        loadKnownPenons()
        loadGroups()

        uiStateManager = UIStateManager(this)

        mainListAdapter = MainListAdapter(
            allPenons = deviceList,
            groups = groups,
            voiceNotificationManager = voiceNotificationManager,
            onUnconfigPenonClick = { penon ->
                val intent = Intent(this, PenonsSettingsActivity::class.java)
                intent.putExtra("penon_mac_address", penon.macAddress)
                startActivity(intent)
            },
            onGroupClick = { group ->
                val intent = Intent(this, GroupDetailActivity::class.java)
                intent.putExtra("group_id", group.groupId)
                startActivity(intent)
            }
        )

        rvMain.layoutManager = LinearLayoutManager(this)
        rvMain.adapter = mainListAdapter

        if (PR.bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non disponible", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        PR.requestBluetoothPermissions()
        setupButtonListeners()
        uiStateManager.updateUIState(PR)
        observeSettingsChanges()
        updateColor("stop")
    }

    override fun onPause() {
        super.onPause()
        wasScanning = PR.isScanning
        PR.stopScanning()
    }

    override fun onResume() {
        super.onResume()
        deviceList.forEach { penon -> repository.loadPenon(penon) }
        loadGroups()
        mainListAdapter.refresh()

        if (!SimulationConfig.isSimulationMode) ensureSimulationStopped()
        if (wasScanning) PR.startScanning()
    }

    private fun loadKnownPenons() {
        val knownMacs = repository.getAllKnownMacAddresses()
        knownMacs.forEach { mac ->
            val penon = Penon(macAddress = mac)
            repository.loadPenon(penon)
            deviceList.add(penon)
        }
        if (deviceList.isNotEmpty()) {
            Toast.makeText(this, "${deviceList.size} Penon(s) chargé(s)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGroups() {
        groups.clear()
        groups.addAll(groupRepository.loadAllGroups())
    }

    fun getOrCreatePenon(macAddress: String): Penon {
        var penon = deviceList.find { it.macAddress == macAddress }
        if (penon == null) {
            penon = Penon(
                penonName = "Penon ${macAddress.takeLast(5)}",
                macAddress = macAddress,
                flowState = true,
                editAttachedThreshold = 3500,
                sDFlowState = true,
            )
            repository.loadPenon(penon)
            deviceList.add(penon)
            mainListAdapter.refresh()
            Toast.makeText(this, "Nouveau Penon détecté : ${penon.penonName}", Toast.LENGTH_SHORT).show()
        }
        return penon
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeSettingsChanges() {
        lifecycleScope.launch {
            repository.observeAllPenons().collect { allPenons ->
                allPenons.forEach { (mac, penon) ->
                    val index = deviceList.indexOfFirst { it.macAddress == mac }
                    if (index != -1 && penon != null) {
                        deviceList[index] = penon.copy()
                    }
                }
                mainListAdapter.refresh()
            }
        }
    }

    private fun initializeViews() {
        tvStatus = findViewById(R.id.tvStatus)
        btnStartScan = findViewById(R.id.btnStartScan)
        btnStopScan = findViewById(R.id.btnStopScan)
        btnClearData = findViewById(R.id.btnClearData)
        btnGlobalSettings = findViewById(R.id.btnGlobalSettings)
        btnAddGroup = findViewById(R.id.btnAddGroup)
        etFileName = findViewById(R.id.etFileName)
        rvMain = findViewById(R.id.rvPenonCards)
    }

    @SuppressLint("SetTextI18n")
    private fun setupButtonListeners() {
        btnAddGroup.setOnClickListener {
            startActivity(Intent(this, GroupSettingsActivity::class.java))
        }

        btnStartScan.setOnClickListener {
            if (SimulationConfig.isReadyToSimulate()) {
                startSimulation()
            } else if (SimulationConfig.isSimulationMode) {
                Toast.makeText(this, "Veuillez sélectionner un fichier CSV dans les paramètres", Toast.LENGTH_LONG).show()
            } else {
                startRealBLEScan()
            }
            updateColor("start")
        }

        btnStopScan.setOnClickListener {
            if (SimulationConfig.isSimulationMode) {
                if (csvSimulator.isRunning()) {
                    csvSimulator.pauseSimulation()
                    tvStatus.text = "⏸️ Simulation en pause"
                    btnStopScan.text = "▶️ Reprendre"
                } else if (csvSimulator.isPaused()) {
                    csvSimulator.resumeSimulation()
                    tvStatus.text = "🎬 Simulation en cours (${csvSimulator.getFrameCount()} trames)"
                    btnStopScan.text = "⏸️ Pause"
                }
            } else {
                stopRealBLEScan()
                btnStopScan.text = "⏹️ Arrêter"
            }
            updateColor("stop")
        }

        btnClearData.setOnClickListener {
            deviceList.clear()
            mainListAdapter.refresh()
            if (SimulationConfig.isSimulationMode) {
                csvSimulator.reset()
                tvStatus.text = "En attente..."
            }
            updateColor("clear")
        }

        btnGlobalSettings.setOnClickListener {
            startActivity(Intent(this, SettingActivity::class.java))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startSimulation() {
        val uri = SimulationConfig.csvFileUri ?: return
        val success = csvSimulator.loadCSVFile(uri)
        if (success) {
            csvSimulator.startSimulation()
            tvStatus.text = "🎬 Simulation en cours (${csvSimulator.getFrameCount()} trames)"
            btnStopScan.text = "⏸️ Pause"
            Toast.makeText(this, "Simulation démarrée : ${SimulationConfig.csvFileName}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Erreur : impossible de charger le fichier CSV", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun ensureSimulationStopped() {
        if (csvSimulator.isRunning() || csvSimulator.isPaused()) {
            csvSimulator.stopSimulation()
            tvStatus.text = "En attente..."
            btnStopScan.text = "⏹️ Arrêter"
        }
    }

    private fun startRealBLEScan() { PR.startScanning() }
    private fun stopRealBLEScan() { PR.stopScanning() }

    fun updateColor(btn: String) {
        if (btn == "start") {
            btnStartScan.setBackgroundColor(resources.getColor(R.color.grey))
            btnStartScan.setTextColor(resources.getColor(R.color.white))
            btnStartScan.isEnabled = false
            btnStopScan.setBackgroundColor(resources.getColor(R.color.sea))
            btnStopScan.setTextColor(resources.getColor(R.color.white))
            btnStopScan.isEnabled = true
            btnClearData.setBackgroundColor(resources.getColor(R.color.grey))
            btnClearData.setTextColor(resources.getColor(R.color.white))
            btnClearData.isEnabled = false
        } else if (btn == "stop") {
            btnStopScan.setBackgroundColor(resources.getColor(R.color.grey))
            btnStopScan.setTextColor(resources.getColor(R.color.white))
            btnStopScan.isEnabled = false
            btnStartScan.setBackgroundColor(resources.getColor(R.color.sea))
            btnStartScan.setTextColor(resources.getColor(R.color.white))
            btnStartScan.isEnabled = true
            btnClearData.setBackgroundColor(resources.getColor(R.color.sea))
            btnClearData.setTextColor(resources.getColor(R.color.white))
            btnClearData.isEnabled = true
        }
        if (btn == "clear") {
            btnStopScan.setBackgroundColor(resources.getColor(R.color.grey))
            btnStopScan.setTextColor(resources.getColor(R.color.white))
            btnStopScan.isEnabled = false
            btnStartScan.setBackgroundColor(resources.getColor(R.color.sea))
            btnStartScan.setTextColor(resources.getColor(R.color.white))
            btnStartScan.isEnabled = true
            btnClearData.setBackgroundColor(resources.getColor(R.color.grey))
            btnClearData.setTextColor(resources.getColor(R.color.white))
            btnClearData.isEnabled = false
        }
    }

    fun getPenonByMac(macAddress: String): Penon? =
        deviceList.find { it.macAddress == macAddress }

    override fun onDestroy() {
        super.onDestroy()
        try {
            csvSimulator.stopSimulation()
            PR.stopScanning()
            PR.closeCSVFiles()
            voiceNotificationManager.release()
            instance = null
        } catch (e: Exception) {
            Log.e("MainActivity", "Erreur lors du nettoyage: ${e.message}", e)
        }
    }
}
