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
import kotlinx.coroutines.launch

/**
 * Activité principale - VERSION DYNAMIQUE
 *
 * ✅ Détecte automatiquement les Penons via BLE
 * ✅ Crée dynamiquement les objets Penon
 * ✅ Plus de Penon1/Penon2 prédéfinis
 */
class MainActivity : AppCompatActivity() {

    lateinit var tvStatus: TextView
    lateinit var btnStartScan: Button
    lateinit var btnStopScan: Button
    lateinit var btnClearData: Button
    lateinit var etFileName: EditText
    lateinit var rvPenonCards: RecyclerView

    // Adaptateur RecyclerView
    lateinit var penonCardAdapter: PenonCardAdapter

    // Managers délégués
    private lateinit var uiStateManager: UIStateManager
    private lateinit var penonSettingsManager: PenonSettingsManager

    // Repository centralisé
    private lateinit var repository: PenonSettingsRepository

    // Lecteur BLE
    val PR = PenonReader(this)

    // ✅ Liste DYNAMIQUE - plus de valeurs par défaut !
    val deviceList = mutableListOf<Penon>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_AppPenon)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialiser le Repository
        repository = PenonSettingsRepository(this)

        // Initialiser les vues
        initializeViews()

        // ✅ Charger les Penons déjà connus depuis le Repository
        loadKnownPenons()

        // Initialiser les managers
        uiStateManager = UIStateManager(this)
        penonSettingsManager = PenonSettingsManager(
            this,
            deviceList
        )

        // Initialiser le RecyclerView
        penonCardAdapter = PenonCardAdapter(
            onPenonClick = { detectedPenon ->
                // Trouver ou créer le Penon correspondant
                val penon = getOrCreatePenon(detectedPenon.macAddress)

                // Ouvrir l'activité des paramètres
                val intent = Intent(this, PenonsSettingsActivity::class.java)
                intent.putExtra("penon_data", penon)
                startActivity(intent)
            },
            penonSettings = deviceList
        )
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

        // Observer les changements de settings en temps réel
        observeSettingsChanges()
    }

    /**
     * ✅ Charge tous les Penons déjà connus depuis SharedPreferences
     */
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

    /**
     * ✅ Récupère un Penon existant ou en crée un nouveau
     */
    fun getOrCreatePenon(macAddress: String): Penon {
        // Chercher dans la liste existante
        var penon = deviceList.find { it.macAdress == macAddress }

        if (penon == null) {
            // Créer un nouveau Penon avec des valeurs par défaut
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

            // Charger depuis le Repository (au cas où il existe déjà)
            repository.loadPenon(penon)

            // Ajouter à la liste
            deviceList.add(penon)

            Toast.makeText(
                this,
                "Nouveau Penon détecté : ${penon.penonName}",
                Toast.LENGTH_SHORT
            ).show()
        }

        return penon
    }

    /**
     * ✅ Observer les changements de settings en temps réel
     */
    private fun observeSettingsChanges() {
        lifecycleScope.launch {
            repository.observeAllPenons().collect { allPenons ->
                // Mettre à jour deviceList avec les nouvelles valeurs
                allPenons.forEach { (mac, penon) ->
                    val index = deviceList.indexOfFirst { it.macAdress == mac }
                    if (index != -1 && penon != null) {
                        deviceList[index] = penon.copy()
                    }
                }

                // Mettre à jour l'adaptateur
                penonCardAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Recharger tous les Penons connus
        deviceList.forEach { penon ->
            repository.loadPenon(penon)
        }

        // Mettre à jour l'adaptateur
        penonCardAdapter.notifyDataSetChanged()
    }

    private fun initializeViews() {
        tvStatus = findViewById(R.id.tvStatus)
        btnStartScan = findViewById(R.id.btnStartScan)
        btnStopScan = findViewById(R.id.btnStopScan)
        btnClearData = findViewById(R.id.btnClearData)
        etFileName = findViewById(R.id.etFileName)
        rvPenonCards = findViewById(R.id.rvPenonCards)
    }

    private fun setupButtonListeners() {
        // Démarrer le scan
        btnStartScan.setOnClickListener {
            PR.TARGET_MAC_ADDRESS1 = ""
            PR.TARGET_MAC_ADDRESS2 = ""
            PR.startScanning()
            updateColor()
        }

        // Arrêter le scan
        btnStopScan.setOnClickListener {
            PR.stopScanning()
            updateColor()
        }

        // Effacer les données
        btnClearData.setOnClickListener {
            penonCardAdapter.clearAll()
            updateColor()
        }
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
            PR.stopScanning()
            PR.closeCSVFiles()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}