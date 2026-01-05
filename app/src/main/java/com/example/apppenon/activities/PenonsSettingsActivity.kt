package com.example.apppenon.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.apppenon.R
import com.example.apppenon.data.PenonSettingsRepository
import com.example.apppenon.model.Penon
import com.example.apppenon.model.PenonDecodedData

/**
 * Activité de modification des paramètres d'un Penon.
 * 
 * Responsabilités:
 * - Affichage des settings courants
 * - Capture des modifications utilisateur
 * - Sauvegarde via le Repository (pas de SharedPreferences ici!)
 * - Notification du changement via Intent Result
 * 
 * Architecture:
 * UI (Form) → Repository.savePenon() → SharedPrefs + StateFlow → Observers (BLE, etc.)
 */
class PenonsSettingsActivity : AppCompatActivity() {

    lateinit var penon: Penon
    private lateinit var repository: PenonSettingsRepository
    
    // Variable pour stocker les dernières données décodées
    private var lastDecodedData: PenonDecodedData? = null
    
    // UI Components
    private lateinit var backBtn: Button
    private lateinit var tvMacAddress: TextView
    private lateinit var tableDecodedData: TableLayout
    private lateinit var tvNoData: TextView
    private lateinit var editPenonName: EditText
    private lateinit var editAttachedThreshold: EditText
    private lateinit var editRSSIlow: EditText
    private lateinit var editRSSIhigh: EditText
    private lateinit var editSDFlowStateLow: EditText
    private lateinit var editSDFlowStateHigh: EditText
    private lateinit var editMeanAccLow: EditText
    private lateinit var editMeanAccHigh: EditText
    private lateinit var editSDAccLow: EditText
    private lateinit var editSDAccHigh: EditText
    private lateinit var editMaxAccLow: EditText
    private lateinit var editMaxAccHigh: EditText
    private lateinit var editVbatLow: EditText
    private lateinit var editVbatHigh: EditText
    private lateinit var switchDetached: SwitchCompat
    private lateinit var editDetached: EditText
    private lateinit var editTimeline: EditText
    private lateinit var switchCount: SwitchCompat
    private lateinit var switchIDs: SwitchCompat
    private lateinit var btnDelete: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var switchRSSI: SwitchCompat
    private lateinit var switchFlowState: SwitchCompat
    private lateinit var switchSDFlowState: SwitchCompat
    private lateinit var switchMeanAcc: SwitchCompat
    private lateinit var switchSDAcc: SwitchCompat
    private lateinit var switchMaxAcc: SwitchCompat
    private lateinit var switchVbat: SwitchCompat

    companion object {
        private var currentActivity: PenonsSettingsActivity? = null

        fun updateDecodedData(data: PenonDecodedData, penomMacAddress: String) {
            Log.d("PenonsSettings", "📊 updateDecodedData appelé: MAC=$penomMacAddress")
            currentActivity?.let { activity ->
                Log.d("PenonsSettings", "Activity existe: ${activity.penon.macAdress}")
                if (activity.penon.macAdress == penomMacAddress) {
                    Log.d("PenonsSettings", "✅ MAC match! Affichage des données")
                    activity.displayDecodedData(data)
                } else {
                    Log.d("PenonsSettings", "⚠️ MAC ne correspond pas: ${activity.penon.macAdress} != $penomMacAddress")
                }
            } ?: run {
                Log.d("PenonsSettings", "⚠️ currentActivity est null")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        currentActivity = this
        Log.d("PenonsSettings", "✅ onResume: Activity en focus pour MAC=${penon.macAdress}")
    }

    override fun onPause() {
        super.onPause()
        currentActivity = null
        Log.d("PenonsSettings", "⏸️ onPause: Activity pas en focus")
    }

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_penon_settings)

        // Récupérer le Penon depuis l'Intent
        penon = intent.getSerializableExtra("penon_data") as Penon
        
        // Initialiser le Repository
        repository = PenonSettingsRepository(this)

        // Charger les settings depuis SharedPreferences
        // (pour s'assurer qu'on a les dernières valeurs)
        repository.loadPenon(penon)

        // Initialiser les références UI
        initializeViews()

        // Remplir les champs avec les valeurs courantes
        populateUI()

        // Configurer les listeners
        setupListeners()
        
        // Afficher le message "pas de données" initialement
        showNoDataMessage()
    }

    /**
     * Initialise toutes les références aux vues.
     */
    private fun initializeViews() {
        backBtn = findViewById(R.id.backBtn)
        tvMacAddress = findViewById(R.id.tvMacAddress)
        tableDecodedData = findViewById(R.id.tableDecodedData)
        tvNoData = findViewById(R.id.tvNoData)
        editPenonName = findViewById(R.id.edit_penon_name)
        editAttachedThreshold = findViewById(R.id.edit_attached_threshold)
        editRSSIlow = findViewById(R.id.edit_rssi_low)
        editRSSIhigh = findViewById(R.id.edit_rssi_high)
        editSDFlowStateLow = findViewById(R.id.edit_sd_flow_state_low)
        editSDFlowStateHigh = findViewById(R.id.edit_sd_flow_state_high)
        editMeanAccLow = findViewById(R.id.edit_mean_acc_low)
        editMeanAccHigh = findViewById(R.id.edit_mean_acc_high)
        editSDAccLow = findViewById(R.id.edit_sd_acc_low)
        editSDAccHigh = findViewById(R.id.edit_sd_acc_high)
        editMaxAccLow = findViewById(R.id.edit_max_acc_low)
        editMaxAccHigh = findViewById(R.id.edit_max_acc_high)
        editVbatLow = findViewById(R.id.edit_vbat_low)
        editVbatHigh = findViewById(R.id.edit_vbat_high)
        editTimeline = findViewById(R.id.edit_timeline)
        switchRSSI = findViewById(R.id.switch_rssi)
        switchFlowState = findViewById(R.id.switch_flow_state)
        switchSDFlowState = findViewById(R.id.switch_sd_flow_state)
        switchMeanAcc = findViewById(R.id.switch_mean_acc)
        switchSDAcc = findViewById(R.id.switch_sd_acc)
        switchMaxAcc = findViewById(R.id.switch_max_acc)
        switchVbat = findViewById(R.id.switch_vbat)
        switchDetached = findViewById(R.id.switch_detached)
        editDetached = findViewById(R.id.edit_detached)
        switchCount = findViewById(R.id.switch_count)
        switchIDs = findViewById(R.id.switch_ids)
        btnDelete = findViewById(R.id.btn_delete)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
        backBtn = findViewById(R.id.backBtn)
    }

    /**
     * Remplit tous les champs avec les valeurs courantes du Penon.
     */
    private fun populateUI() {
        // Afficher le MAC du Penon
        tvMacAddress.text = "MAC: ${penon.macAdress}"
        
        editPenonName.setText(penon.penonName)
        editAttachedThreshold.setText(penon.flowStateThreshold.toString())
        editRSSIlow.setText(penon.rssiLow.toString())
        editRSSIhigh.setText(penon.rssiHigh.toString())
        editSDFlowStateLow.setText(penon.sDFlowStateLow.toString())
        editSDFlowStateHigh.setText(penon.sDFlowStateHigh.toString())
        editMeanAccLow.setText(penon.meanAccLow.toString())
        editMeanAccHigh.setText(penon.meanAccHigh.toString())
        editSDAccLow.setText(penon.sDAccLow.toString())
        editSDAccHigh.setText(penon.sDAccHigh.toString())
        editMaxAccLow.setText(penon.maxAccLow.toString())
        editMaxAccHigh.setText(penon.maxAccHigh.toString())
        editVbatLow.setText(penon.vbatLow.toString())
        editVbatHigh.setText(penon.vbatHigh.toString())
        editTimeline.setText(penon.timeline.toString())
        switchRSSI.isChecked = penon.rssi
        switchFlowState.isChecked = penon.flowState
        switchSDFlowState.isChecked = penon.sDFlowState
        switchMeanAcc.isChecked = penon.meanAcc
        switchSDAcc.isChecked = penon.sDAcc
        switchMaxAcc.isChecked = penon.maxAcc
        switchVbat.isChecked = penon.vbat
        switchDetached.isChecked = penon.detached
        editDetached.setText(penon.detachedThresh.toString())
        switchCount.isChecked = penon.count
        switchIDs.isChecked = penon.ids
    }

    /**
     * Configure tous les listeners de boutons.
     */
    private fun setupListeners() {
        // Bouton Retour
        backBtn.setOnClickListener {
            finish()
        }

        // Bouton Annuler
        btnCancel.setOnClickListener {
            finish()
        }

        // Bouton Sauvegarder
        btnSave.setOnClickListener {
            saveSettings()
        }

        // Bouton Supprimer
        btnDelete.setOnClickListener {
            // TODO: Implémenter la suppression si nécessaire
            finish()
        }
    }

    /**
     * Affiche les données décodées du Penon dans un tableau.
     */
    fun displayDecodedData(data: PenonDecodedData) {
        lastDecodedData = data
        
        tableDecodedData.removeAllViews()
        tvNoData.visibility = android.view.View.GONE
        
        val displayMap = data.toDisplayMap()
        
        for ((key, value) in displayMap) {
            val row = TableRow(this)
            row.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            
            // Colonne 1: Label (clé)
            val labelView = TextView(this)
            labelView.text = key
            labelView.textSize = 12f
            labelView.setTextColor(android.graphics.Color.parseColor("#666666"))
            labelView.setPadding(8, 4, 8, 4)
            labelView.layoutParams = TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                0.6f
            )
            row.addView(labelView)
            
            // Colonne 2: Valeur
            val valueView = TextView(this)
            valueView.text = value
            valueView.textSize = 12f
            valueView.setTextColor(android.graphics.Color.parseColor("#00897B"))
            valueView.setTypeface(null, android.graphics.Typeface.BOLD)
            valueView.setPadding(8, 4, 8, 4)
            valueView.layoutParams = TableRow.LayoutParams(
                0,
                TableRow.LayoutParams.WRAP_CONTENT,
                0.4f
            )
            row.addView(valueView)
            
            tableDecodedData.addView(row)
        }
    }
    
    /**
     * Affiche le message "pas de données"
     */
    fun showNoDataMessage() {
        tableDecodedData.removeAllViews()
        tvNoData.visibility = android.view.View.VISIBLE
    }

    /**
     * Sauvegarde les modifications dans le Repository.
     * Le Repository se charge d'écrire dans SharedPreferences et de notifier les observateurs.
     */
    private fun saveSettings() {
        try {
            // Mettre à jour l'objet Penon avec les valeurs saisies
            penon.penonName = editPenonName.text.toString()
            penon.flowStateThreshold = editAttachedThreshold.text.toString().toIntOrNull() 
                ?: penon.flowStateThreshold
            penon.rssiLow = editRSSIlow.text.toString().toIntOrNull() 
                ?: penon.rssiLow
            penon.rssiHigh = editRSSIhigh.text.toString().toIntOrNull() 
                ?: penon.rssiHigh
            penon.sDFlowStateLow = editSDFlowStateLow.text.toString().toIntOrNull() 
                ?: penon.sDFlowStateLow
            penon.sDFlowStateHigh = editSDFlowStateHigh.text.toString().toIntOrNull() 
                ?: penon.sDFlowStateHigh
            penon.meanAccLow = editMeanAccLow.text.toString().toIntOrNull() 
                ?: penon.meanAccLow
            penon.meanAccHigh = editMeanAccHigh.text.toString().toIntOrNull() 
                ?: penon.meanAccHigh
            penon.sDAccLow = editSDAccLow.text.toString().toIntOrNull() 
                ?: penon.sDAccLow
            penon.sDAccHigh = editSDAccHigh.text.toString().toIntOrNull() 
                ?: penon.sDAccHigh
            penon.maxAccLow = editMaxAccLow.text.toString().toIntOrNull() 
                ?: penon.maxAccLow
            penon.maxAccHigh = editMaxAccHigh.text.toString().toIntOrNull() 
                ?: penon.maxAccHigh
            penon.vbatLow = editVbatLow.text.toString().toDoubleOrNull() 
                ?: penon.vbatLow
            penon.vbatHigh = editVbatHigh.text.toString().toDoubleOrNull() 
                ?: penon.vbatHigh
            penon.timeline = editTimeline.text.toString().toIntOrNull() 
                ?: penon.timeline
            penon.rssi = switchRSSI.isChecked
            penon.flowState = switchFlowState.isChecked
            penon.sDFlowState = switchSDFlowState.isChecked
            penon.meanAcc = switchMeanAcc.isChecked
            penon.sDAcc = switchSDAcc.isChecked
            penon.maxAcc = switchMaxAcc.isChecked
            penon.vbat = switchVbat.isChecked
            penon.detached = switchDetached.isChecked
            penon.detachedThresh = editDetached.text.toString().toDoubleOrNull() 
                ?: penon.detachedThresh
            penon.count = switchCount.isChecked
            penon.ids = switchIDs.isChecked

            // ✅ IMPORTANT : Sauvegarder via le Repository
            // (Qui se charge du SharedPreferences ET de notifier les observateurs)
            repository.savePenon(penon)

            // Retour à MainActivity avec le Penon mis à jour
            val resultIntent = Intent()
            resultIntent.putExtra("updated_penon", penon)
            setResult(RESULT_OK, resultIntent)

            Toast.makeText(this, "Paramètres sauvegardés ✅", Toast.LENGTH_SHORT).show()
            finish()

        } catch (e: Exception) {
            Toast.makeText(this, "Erreur de sauvegarde: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
