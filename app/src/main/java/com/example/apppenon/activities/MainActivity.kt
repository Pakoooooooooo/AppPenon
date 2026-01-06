package com.example.apppenon.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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

    lateinit var penonCardAdapter: PenonCardAdapter
    private lateinit var uiStateManager: UIStateManager
    private lateinit var penonSettingsManager: PenonSettingsManager
    private lateinit var repository: PenonSettingsRepository

    // ✅ ÉTAPE 1 : Déclarer le launcher au niveau de la classe
    private lateinit var penonSettingsLauncher: ActivityResultLauncher<Intent>

    val PR = PenonReader(this)
    val deviceList = mutableListOf<Penon>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_AppPenon)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ ÉTAPE 2 : Enregistrer le launcher IMMÉDIATEMENT dans onCreate
        // On ne peut pas faire ça dans le callback du clic
        // Dans le onCreate de MainActivity
        penonSettingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // Récupérer le penon mis à jour renvoyé par PenonsSettingsActivity
                val updatedPenon = result.data?.getSerializableExtra("updated_penon") as? Penon


                if (updatedPenon != null) {
                    // 1. Trouver la position de ce penon dans votre liste locale
                    val index = deviceList.indexOfFirst { it.macAddress == updatedPenon.macAddress }

                    if (index != -1) {
                        // 2. Mettre à jour la liste avec les nouvelles données
                        deviceList[index] = updatedPenon

                        // 3. Notifier l'adaptateur qu'un élément spécifique a changé (plus performant que notifyDataSetChanged)
                        Toast.makeText(this, "Données mises à jour", Toast.LENGTH_SHORT).show()
                        penonCardAdapter.notifyItemChanged(index)
                    }
                }
            }
        }

        repository = PenonSettingsRepository(this)
        initializeViews()
        loadKnownPenons()

        uiStateManager = UIStateManager(this)
        penonSettingsManager = PenonSettingsManager(this, deviceList)

        // ✅ ÉTAPE 3 : Configurer l'adaptateur avec le launcher déjà prêt
        penonCardAdapter = PenonCardAdapter(
            onPenonClick = { detectedPenon ->
                val penon = getOrCreatePenon(detectedPenon.macAddress)

                // On utilise simplement le launcher déjà enregistré
                val intent = Intent(this, PenonsSettingsActivity::class.java)
                intent.putExtra("penon_mac_address", penon.macAddress)
                penonSettingsLauncher.launch(intent)
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

    /**
     * ✅ Charge tous les Penons déjà connus depuis SharedPreferences
     */
    private fun loadKnownPenons() {
        val knownMacs = repository.getAllKnownMacAddresses()

        knownMacs.forEach { mac ->
            val penon = Penon(macAddress = mac)
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
        var penon = deviceList.find { it.macAddress == macAddress }

        if (penon == null) {
            // Créer un nouveau Penon avec des valeurs par défaut
            penon = Penon(
                penonName = "Penon ${macAddress.takeLast(5)}",
                macAddress = macAddress,
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
                allPenons.forEach { (mac, penon) ->
                    val index = deviceList.indexOfFirst { it.macAddress == mac }
                    if (index != -1 && penon != null) {
                        // ✅ Correction mutation : On remplace l'objet dans la liste mutable
                        deviceList[index] = penon.copy()
                    }
                }
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
        }
    }

    fun updateColor(){
        if (btnStartScan.isEnabled) {
            btnStartScan.setBackgroundColor(resources.getColor(R.color.grey))
            btnStartScan.setTextColor(resources.getColor(R.color.white))
            btnStartScan.isEnabled = false
        } else {
            btnStartScan.setBackgroundColor(resources.getColor(R.color.sea))
            btnStartScan.setTextColor(resources.getColor(R.color.white))
            btnStartScan.isEnabled = true
        }
        if (btnStopScan.isEnabled) {
            btnStopScan.setBackgroundColor(resources.getColor(R.color.grey))
            btnStopScan.setTextColor(resources.getColor(R.color.white))
            btnStopScan.isEnabled = false
        }
        else {
            btnStopScan.setBackgroundColor(resources.getColor(R.color.sea))
            btnStopScan.setTextColor(resources.getColor(R.color.white))
            btnStopScan.isEnabled = true
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