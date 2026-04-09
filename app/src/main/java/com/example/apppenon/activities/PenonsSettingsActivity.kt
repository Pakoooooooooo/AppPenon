package com.example.apppenon.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.apppenon.R
import com.example.apppenon.data.GroupRepository
import com.example.apppenon.data.PenonSettingsRepository
import com.example.apppenon.model.Penon
import com.example.apppenon.model.PenonGroup
import com.example.apppenon.model.Side
import java.lang.ref.WeakReference

class PenonsSettingsActivity : AppCompatActivity() {

    private lateinit var penon: Penon
    private lateinit var repository: PenonSettingsRepository
    private lateinit var groupRepository: GroupRepository
    private var hasUnsavedChanges = false

    // Groupe
    private lateinit var spinnerGroup: Spinner
    private lateinit var spinnerSide: Spinner
    private var availableGroups: List<PenonGroup> = emptyList()

    // UI Components
    private lateinit var backBtn: Button
    private lateinit var tvMacAddress: TextView
    private lateinit var editPenonName: EditText
    private lateinit var editAttachedThreshold: EditText
    private lateinit var switchDetached: SwitchCompat
    private lateinit var editDetached: EditText
    private lateinit var editTimeline: EditText
    private lateinit var switchCount: SwitchCompat
    private lateinit var switchIDs: SwitchCompat
    private lateinit var btnDelete: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private lateinit var switchAvrMagZ: SwitchCompat
    private lateinit var switchAvrAvrMagZ: SwitchCompat
    private lateinit var switchFlowState: SwitchCompat
    private lateinit var switchSDFlowState: SwitchCompat
    private lateinit var switchMeanAcc: SwitchCompat
    private lateinit var switchSDAcc: SwitchCompat
    private lateinit var switchMaxAcc: SwitchCompat
    private lateinit var switchVbat: SwitchCompat
    private lateinit var btnCalibration: Button

    // Créer le launcher pour recevoir le résultat
    private val calibrationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val calibrationResult = result.data?.getIntExtra("calibration_result", -1)
            // Utilisez le résultat ici
            editAttachedThreshold.setText(calibrationResult.toString())

            // Exemple : afficher le résultat
            Toast.makeText(this, "Calibration: $calibrationResult", Toast.LENGTH_SHORT).show()
        } else if (result.resultCode == RESULT_CANCELED) {
            // L'utilisateur a annulé
            Toast.makeText(this, "Calibration annulée", Toast.LENGTH_SHORT).show()
        }
    }
    companion object {
        private var currentActivity: WeakReference<PenonsSettingsActivity>? = null
        @SuppressLint("StaticFieldLeak")
        private var instance: PenonsSettingsActivity? = null
        fun getInstance(): PenonsSettingsActivity? = instance
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        setContentView(R.layout.activity_penon_settings)

        repository = PenonSettingsRepository(this)
        groupRepository = GroupRepository(this)

        val macAddress = intent.getStringExtra("penon_mac_address")
        if (macAddress == null) {
            Toast.makeText(this, "Erreur : MAC Address manquante", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Utiliser le nom déjà en mémoire (ex: "Penon EE:01") comme valeur par défaut
        val inMemoryName = MainActivity.getInstance()?.getPenonByMac(macAddress)?.penonName
            ?: "Penon ${macAddress.takeLast(5)}"
        penon = Penon(macAddress = macAddress, penonName = inMemoryName)
        try {
            repository.loadPenon(penon)
            Log.d("PenonsSettings", "Après chargement - avrMagZ: ${penon.avrMagZ}")
        } catch (e: Exception) {
            Log.e("PenonsSettings", "Error loading penon: ${e.message}")
            Toast.makeText(this, "Erreur de chargement des données", Toast.LENGTH_SHORT).show()
        }

        initializeViews()
        populateUI()
        setupBackPressedHandler()
        setupListeners()
        setupChangeListeners()
    }

    override fun onResume() {
        super.onResume()
        currentActivity = WeakReference(this)
        Log.d("PenonsSettings", "✅ onResume: Activity en focus pour MAC=${penon.macAddress}")
    }

    override fun onPause() {
        super.onPause()
        if (currentActivity?.get() == this) {
            currentActivity = null
        }
        Log.d("PenonsSettings", "⏸️ onPause: Activity pas en focus")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d("PenonsSettings", "🛑 onDestroy: Ressources libérées")
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChanges) {
                    showUnsavedChangesDialog()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun initializeViews() {
        backBtn = findViewById(R.id.backBtn)
        tvMacAddress = findViewById(R.id.tvMacAddress)
        editPenonName = findViewById(R.id.edit_penon_name)
        editAttachedThreshold = findViewById(R.id.edit_attached_threshold)
        editTimeline = findViewById(R.id.edit_timeline)

        switchAvrMagZ = findViewById(R.id.switch_avr_mag_z)
        switchAvrAvrMagZ = findViewById(R.id.switch_avr_avr_mag_z)
        switchFlowState = findViewById(R.id.switch_flow_state)
        switchSDFlowState = findViewById(R.id.switch_sd_flow_state)
        switchMeanAcc = findViewById(R.id.switch_mean_acc)
        switchMaxAcc = findViewById(R.id.switch_max_acc)
        switchSDAcc = findViewById(R.id.switch_sd_acc)
        switchVbat = findViewById(R.id.switch_vbat)
        spinnerGroup = findViewById(R.id.spinner_group)
        spinnerSide = findViewById(R.id.spinner_side)
        switchDetached = findViewById(R.id.switch_detached)
        editDetached = findViewById(R.id.edit_detached)
        editTimeline = findViewById(R.id.edit_timeline)
        switchCount = findViewById(R.id.switch_count)
        switchIDs = findViewById(R.id.switch_ids)
        btnDelete = findViewById(R.id.btn_delete)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
        btnCalibration = findViewById(R.id.btn_calibration)
    }

    @SuppressLint("SetTextI18n")
    private fun populateUI() {
        tvMacAddress.text = "MAC: ${penon.macAddress}"
        editPenonName.setText(penon.penonName)
        editTimeline.setText(penon.timeline.toString())

        switchAvrMagZ.isChecked = penon.avrMagZ
        switchAvrAvrMagZ.isChecked = penon.avrAvrMagZ
        switchFlowState.isChecked = penon.flowState
        switchSDFlowState.isChecked = penon.sDFlowState
        switchMeanAcc.isChecked = penon.meanAcc
        switchMaxAcc.isChecked = penon.maxAcc
        switchSDAcc.isChecked = penon.sDAcc
        switchVbat.isChecked = penon.vbat
        switchDetached.isChecked = penon.detached
        switchCount.isChecked = penon.count
        populateGroupSpinners()
        switchIDs.isChecked = penon.ids
        editAttachedThreshold.setText(penon.editAttachedThreshold.toString())
    }

    private fun blackTextAdapter(items: List<String>): ArrayAdapter<String> {
        val adapter = object : ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, items
        ) {
            override fun getView(pos: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                return (super.getView(pos, convertView, parent) as TextView).also {
                    it.setTextColor(0xFF000000.toInt())
                }
            }
            override fun getDropDownView(pos: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                return (super.getDropDownView(pos, convertView, parent) as TextView).also {
                    it.setTextColor(0xFF000000.toInt())
                    it.setBackgroundColor(0xFFFFFFFF.toInt())
                }
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

    private fun populateGroupSpinners() {
        availableGroups = groupRepository.loadAllGroups()

        // Spinner groupe : "Aucun groupe" + liste des groupes
        val groupNames = mutableListOf("— Aucun groupe —")
        groupNames.addAll(availableGroups.map { it.groupName })
        spinnerGroup.adapter = blackTextAdapter(groupNames)

        // Sélectionner le groupe actuel du pénon
        val currentGroupIdx = availableGroups.indexOfFirst { it.groupId == penon.groupId }
        spinnerGroup.setSelection(if (currentGroupIdx >= 0) currentGroupIdx + 1 else 0)

        // Spinner côté
        spinnerSide.adapter = blackTextAdapter(listOf("Bâbord", "Tribord"))
        spinnerSide.setSelection(if (penon.side == Side.TRIBORD) 1 else 0)

        spinnerGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                hasUnsavedChanges = true
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spinnerSide.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                hasUnsavedChanges = true
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        backBtn.setOnClickListener {
            if (hasUnsavedChanges) {
                showUnsavedChangesDialog()
            } else {
                finish()
            }
        }

        btnCancel.setOnClickListener {
            if (hasUnsavedChanges) {
                showUnsavedChangesDialog()
            } else {
                finish()
            }
        }

        btnSave.setOnClickListener { saveSettings() }
        btnDelete.setOnClickListener { showDeleteConfirmationDialog() }

        btnCalibration.setOnClickListener {
            val intent = Intent(this, PenonsCalibrationActivity::class.java)
            intent.putExtra("penon_mac_address", penon.macAddress)
            calibrationLauncher.launch(intent)
        }
    }

    private fun setupChangeListeners() {
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                hasUnsavedChanges = true
            }
        }

        val switchListener = { _: CompoundButton, _: Boolean ->
            hasUnsavedChanges = true
        }

        listOf(
            editPenonName,
            editAttachedThreshold,
            editTimeline
        ).forEach { it.addTextChangedListener(textWatcher) }

        listOf(
            switchAvrMagZ,
            switchAvrAvrMagZ,
            switchFlowState,
            switchSDFlowState,
            switchMeanAcc,
            switchSDAcc,
            switchMaxAcc,
            switchVbat,
            switchDetached,
            switchCount,
            switchIDs
        ).forEach { it.setOnCheckedChangeListener(switchListener) }
    }


    private fun saveSettings() {
        try {
            // ✅ CORRECTION: Mise à jour correcte de l'objet Penon
            penon.apply {
                penonName = editPenonName.text.toString()
                timeline = editTimeline.text.toString().toIntOrNull() ?: timeline

                avrMagZ = switchAvrMagZ.isChecked
                avrAvrMagZ = switchAvrAvrMagZ.isChecked
                flowState = switchFlowState.isChecked
                sDFlowState = switchSDFlowState.isChecked
                meanAcc = switchMeanAcc.isChecked
                maxAcc = switchMaxAcc.isChecked
                sDAcc = switchSDAcc.isChecked
                vbat = switchVbat.isChecked
                detached = switchDetached.isChecked
                count = switchCount.isChecked

                // Affectation groupe
                val groupPos = spinnerGroup.selectedItemPosition
                if (groupPos == 0) {
                    groupId = ""
                    side = Side.NONE
                } else {
                    groupId = availableGroups[groupPos - 1].groupId
                    side = if (spinnerSide.selectedItemPosition == 1) Side.TRIBORD else Side.BABORD
                }
                ids = switchIDs.isChecked

                // ✅ CORRECTION: Convertir EditText en Int
                editAttachedThreshold = this@PenonsSettingsActivity.editAttachedThreshold.text.toString().toIntOrNull()
                    ?: this.editAttachedThreshold
            }

            Log.d("PenonsSettings", "Penon object updated, attempting to save...")
            Log.d("PenonsSettings", "Avant sauvegarde - avrMagZ: ${penon.avrMagZ}")
            repository.savePenon(penon)

            Log.d("PenonsSettings", "Repository save completed successfully")

            val resultIntent = Intent().apply {
                putExtra("updated_penon_mac", penon.macAddress)
                putExtra("should_refresh", true)
            }
            setResult(RESULT_OK, resultIntent)

            hasUnsavedChanges = false
            Toast.makeText(this, "Paramètres sauvegardés avec succès", Toast.LENGTH_SHORT).show()
            finish()

        } catch (e: Exception) {
            Log.e("PenonsSettings", "Error saving settings: ${e.message}", e)
            Toast.makeText(this, "Erreur de sauvegarde: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le Penon")
            .setMessage("Êtes-vous sûr de vouloir supprimer ce Penon ? Cette action est irréversible.")
            .setPositiveButton("Supprimer") { _, _ ->
                deletePenon()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deletePenon() {
        try {
            repository.deletePenon(penon.macAddress)
            MainActivity.getInstance()?.removePenon(penon.macAddress)

            val resultIntent = Intent().apply {
                putExtra("deleted_penon_mac", penon.macAddress)
            }
            setResult(RESULT_OK, resultIntent)

            Toast.makeText(this, "Penon supprimé", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Log.e("PenonsSettings", "Error deleting penon: ${e.message}")
            Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_LONG).show()
        }
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Modifications non sauvegardées")
            .setMessage("Vous avez des modifications non sauvegardées. Que souhaitez-vous faire ?")
            .setPositiveButton("Sauvegarder") { _, _ ->
                saveSettings()
            }
            .setNegativeButton("Abandonner") { _, _ ->
                hasUnsavedChanges = false
                finish()
            }
            .setNeutralButton("Annuler", null)
            .show()
    }
}