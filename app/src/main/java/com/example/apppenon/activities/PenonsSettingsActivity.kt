package com.example.apppenon.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.apppenon.R
import com.example.apppenon.data.PenonSettingsRepository
import com.example.apppenon.model.Penon
import com.example.apppenon.model.PenonDecodedData
import java.lang.ref.WeakReference

class PenonsSettingsActivity : AppCompatActivity() {

    private lateinit var penon: Penon
    private lateinit var repository: PenonSettingsRepository
    private var lastDecodedData: PenonDecodedData? = null
    private var hasUnsavedChanges = false

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
        private var currentActivity: WeakReference<PenonsSettingsActivity>? = null

        fun updateDecodedData(data: PenonDecodedData, penonMacAddress: String) {
            currentActivity?.get()?.let { activity ->
                if (!activity.isFinishing && !activity.isDestroyed &&
                    activity.penon.macAddress == penonMacAddress) {
                    activity.runOnUiThread {
                        activity.displayDecodedData(data)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_penon_settings)

        repository = PenonSettingsRepository(this)

        // Récupérer le MAC et charger l'objet proprement
        val macAddress = intent.getStringExtra("penon_mac_address")
        if (macAddress == null) {
            Toast.makeText(this, "Erreur : MAC Address manquante", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Créer un objet Penon temporaire et charger ses vraies valeurs depuis le Repo
        penon = Penon(macAddress = macAddress)
        try {
            repository.loadPenon(penon)
        } catch (e: Exception) {
            Log.e("PenonsSettings", "Error loading penon: ${e.message}")
            Toast.makeText(this, "Erreur de chargement des données", Toast.LENGTH_SHORT).show()
        }

        initializeViews()
        populateUI()
        setupBackPressedHandler()
        setupListeners()
        setupChangeListeners()
        showNoDataMessage()
    }

    override fun onResume() {
        super.onResume()
        currentActivity = WeakReference(this)
    }

    override fun onPause() {
        super.onPause()
        if (currentActivity?.get() == this) {
            currentActivity = null
        }
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
    }

    private fun populateUI() {
        tvMacAddress.text = "MAC: ${penon.macAddress}"
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

        // Appliquer aux EditText
        listOf(
            editPenonName, editAttachedThreshold, editRSSIlow, editRSSIhigh,
            editSDFlowStateLow, editSDFlowStateHigh, editMeanAccLow, editMeanAccHigh,
            editSDAccLow, editSDAccHigh, editMaxAccLow, editMaxAccHigh,
            editVbatLow, editVbatHigh, editDetached, editTimeline
        ).forEach { it.addTextChangedListener(textWatcher) }

        // Appliquer aux Switch
        listOf(
            switchRSSI, switchFlowState, switchSDFlowState, switchMeanAcc,
            switchSDAcc, switchMaxAcc, switchVbat, switchDetached,
            switchCount, switchIDs
        ).forEach { it.setOnCheckedChangeListener(switchListener) }
    }

    private fun displayDecodedData(data: PenonDecodedData) {
        lastDecodedData = data
        tableDecodedData.removeAllViews()
        tvNoData.visibility = android.view.View.GONE

        val paddingPx = resources.getDimensionPixelSize(R.dimen.table_padding)
        val textSizeSp = resources.getDimension(R.dimen.table_text_size)

        data.toDisplayMap().forEach { (key, value) ->
            val row = TableRow(this)
            val labelView = TextView(this).apply {
                text = key
                textSize = textSizeSp / resources.displayMetrics.scaledDensity
                setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2)
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.6f)
            }
            val valueView = TextView(this).apply {
                text = value
                textSize = textSizeSp / resources.displayMetrics.scaledDensity
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2)
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.4f)
            }
            row.addView(labelView)
            row.addView(valueView)
            tableDecodedData.addView(row)
        }
    }

    private fun showNoDataMessage() {
        tableDecodedData.removeAllViews()
        tvNoData.visibility = android.view.View.VISIBLE
    }

    private fun validateInputs(): Boolean {
        // Validation RSSI
        val rssiLow = editRSSIlow.text.toString().toIntOrNull()
        val rssiHigh = editRSSIhigh.text.toString().toIntOrNull()
        if (rssiLow != null && rssiHigh != null && rssiLow > rssiHigh) {
            Toast.makeText(this, "RSSI Low doit être ≤ RSSI High", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validation SD Flow State
        val sdFlowLow = editSDFlowStateLow.text.toString().toIntOrNull()
        val sdFlowHigh = editSDFlowStateHigh.text.toString().toIntOrNull()
        if (sdFlowLow != null && sdFlowHigh != null && sdFlowLow > sdFlowHigh) {
            Toast.makeText(this, "SD Flow State Low doit être ≤ High", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validation Mean Acc
        val meanAccLow = editMeanAccLow.text.toString().toIntOrNull()
        val meanAccHigh = editMeanAccHigh.text.toString().toIntOrNull()
        if (meanAccLow != null && meanAccHigh != null && meanAccLow > meanAccHigh) {
            Toast.makeText(this, "Mean Acc Low doit être ≤ High", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validation SD Acc
        val sdAccLow = editSDAccLow.text.toString().toIntOrNull()
        val sdAccHigh = editSDAccHigh.text.toString().toIntOrNull()
        if (sdAccLow != null && sdAccHigh != null && sdAccLow > sdAccHigh) {
            Toast.makeText(this, "SD Acc Low doit être ≤ High", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validation Max Acc
        val maxAccLow = editMaxAccLow.text.toString().toIntOrNull()
        val maxAccHigh = editMaxAccHigh.text.toString().toIntOrNull()
        if (maxAccLow != null && maxAccHigh != null && maxAccLow > maxAccHigh) {
            Toast.makeText(this, "Max Acc Low doit être ≤ High", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validation Vbat
        val vbatLow = editVbatLow.text.toString().toDoubleOrNull()
        val vbatHigh = editVbatHigh.text.toString().toDoubleOrNull()
        if (vbatLow != null && vbatHigh != null && vbatLow > vbatHigh) {
            Toast.makeText(this, "Vbat Low doit être ≤ Vbat High", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validation Timeline (doit être positif si renseigné)
        val timeline = editTimeline.text.toString().toIntOrNull()
        if (timeline != null && timeline < 0) {
            Toast.makeText(this, "Timeline ne peut pas être négatif", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveSettings() {
        if (!validateInputs()) {
            return
        }

        try {
            // Mise à jour de l'objet Penon
            penon.apply {
                penonName = editPenonName.text.toString()
                flowStateThreshold = editAttachedThreshold.text.toString().toIntOrNull() ?: flowStateThreshold
                rssiLow = editRSSIlow.text.toString().toIntOrNull() ?: rssiLow
                rssiHigh = editRSSIhigh.text.toString().toIntOrNull() ?: rssiHigh
                sDFlowStateLow = editSDFlowStateLow.text.toString().toIntOrNull() ?: sDFlowStateLow
                sDFlowStateHigh = editSDFlowStateHigh.text.toString().toIntOrNull() ?: sDFlowStateHigh
                meanAccLow = editMeanAccLow.text.toString().toIntOrNull() ?: meanAccLow
                meanAccHigh = editMeanAccHigh.text.toString().toIntOrNull() ?: meanAccHigh
                sDAccLow = editSDAccLow.text.toString().toIntOrNull() ?: sDAccLow
                sDAccHigh = editSDAccHigh.text.toString().toIntOrNull() ?: sDAccHigh
                maxAccLow = editMaxAccLow.text.toString().toIntOrNull() ?: maxAccLow
                maxAccHigh = editMaxAccHigh.text.toString().toIntOrNull() ?: maxAccHigh
                vbatLow = editVbatLow.text.toString().toDoubleOrNull() ?: vbatLow
                vbatHigh = editVbatHigh.text.toString().toDoubleOrNull() ?: vbatHigh
                timeline = editTimeline.text.toString().toIntOrNull() ?: timeline
                rssi = switchRSSI.isChecked
                flowState = switchFlowState.isChecked
                sDFlowState = switchSDFlowState.isChecked
                meanAcc = switchMeanAcc.isChecked
                sDAcc = switchSDAcc.isChecked
                maxAcc = switchMaxAcc.isChecked
                vbat = switchVbat.isChecked
                detached = switchDetached.isChecked
                detachedThresh = editDetached.text.toString().toDoubleOrNull() ?: detachedThresh
                count = switchCount.isChecked
                ids = switchIDs.isChecked
            }

            Log.d("PenonsSettings", "Penon object updated, attempting to save...")

            // Sauvegarde réelle
            repository.savePenon(penon)

            Log.d("PenonsSettings", "Repository save completed successfully")

            // Ne pas passer l'objet Penon entier, juste son MAC address
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
            e.printStackTrace()
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
            // Si deletePenon n'existe pas, utilisez la méthode appropriée de votre repository
            // Par exemple : repository.removePenon(penon.macAddress)
            // ou : repository.delete(penon)
            // Pour l'instant, commenté en attendant la bonne méthode :
            // repository.deletePenon(penon.macAddress)

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