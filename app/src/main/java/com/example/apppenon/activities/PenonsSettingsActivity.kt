package com.example.apppenon.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.apppenon.R
import com.example.apppenon.data.PenonSettingsRepository
import com.example.apppenon.model.Penon
import com.example.apppenon.model.PenonDecodedData
import com.example.apppenon.utils.VoiceNotificationManager
import java.lang.ref.WeakReference

class PenonsSettingsActivity : AppCompatActivity() {

    private lateinit var penon: Penon
    private lateinit var repository: PenonSettingsRepository
    private lateinit var voiceNotificationManager: VoiceNotificationManager
    private var lastDecodedData: PenonDecodedData? = null
    private var hasUnsavedChanges = false

    // UI Components
    private lateinit var backBtn: Button
    private lateinit var tvMacAddress: TextView
    private lateinit var tableDecodedData: TableLayout
    private lateinit var tvNoData: TextView
    private lateinit var editPenonName: EditText
    private lateinit var editAttachedThreshold: EditText
    private lateinit var switchDetached: SwitchCompat
    private lateinit var editDetached: EditText
    private lateinit var editTimeline: EditText
    private lateinit var editLabelAttache: EditText
    private lateinit var editLabelDetache: EditText
    private lateinit var switchUseSound: SwitchCompat
    private lateinit var layoutVoiceLabels: LinearLayout
    private lateinit var layoutCustomSounds: LinearLayout
    private lateinit var tvSoundAttacheStatus: TextView
    private lateinit var tvSoundDetacheStatus: TextView
    private lateinit var btnSelectSoundAttache: Button
    private lateinit var btnSelectSoundDetache: Button
    private lateinit var switchCount: SwitchCompat
    private lateinit var switchIDs: SwitchCompat
    private lateinit var btnDelete: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    // Launchers pour sélectionner les fichiers audio
    private lateinit var soundAttacheLauncher: ActivityResultLauncher<Intent>
    private lateinit var soundDetacheLauncher: ActivityResultLauncher<Intent>
    private lateinit var switchMagZ: SwitchCompat
    private lateinit var switchAvrMagZ: SwitchCompat
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

        // Initialiser les launchers pour sélectionner les fichiers audio
        soundAttacheLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        // Prendre la permission persistante
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        penon.soundAttachePath = uri.toString()
                        tvSoundAttacheStatus.text = getFileName(uri)
                        hasUnsavedChanges = true
                    } catch (e: Exception) {
                        Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        soundDetacheLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        penon.soundDetachePath = uri.toString()
                        tvSoundDetacheStatus.text = getFileName(uri)
                        hasUnsavedChanges = true
                    } catch (e: Exception) {
                        Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        setContentView(R.layout.activity_penon_settings)

        repository = PenonSettingsRepository(this)

        // Initialiser le gestionnaire de notifications vocales
        voiceNotificationManager = VoiceNotificationManager(this)

        val macAddress = intent.getStringExtra("penon_mac_address")
        if (macAddress == null) {
            Toast.makeText(this, "Erreur : MAC Address manquante", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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
        voiceNotificationManager.release()
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
        tableDecodedData = findViewById(R.id.tableDecodedData)
        tvNoData = findViewById(R.id.tvNoData)
        editPenonName = findViewById(R.id.edit_penon_name)
        editAttachedThreshold = findViewById(R.id.edit_attached_threshold)
        editTimeline = findViewById(R.id.edit_timeline)

        // ✅ CORRECTION: Associer les bons IDs
        switchAvrMagZ = findViewById(R.id.switch_avr_avr_mag_z)  // "Avr Mag Z"
        switchMagZ = findViewById(R.id.switch_avr_mag_z)          // "Mag Z"
        switchFlowState = findViewById(R.id.switch_flow_state)
        switchSDFlowState = findViewById(R.id.switch_sd_flow_state)
        switchMeanAcc = findViewById(R.id.switch_mean_acc)
        switchMaxAcc = findViewById(R.id.switch_max_acc)
        switchSDAcc = findViewById(R.id.switch_sd_acc)
        switchVbat = findViewById(R.id.switch_vbat)
        switchDetached = findViewById(R.id.switch_detached)
        editDetached = findViewById(R.id.edit_detached)
        editLabelAttache = findViewById(R.id.edit_label_attache)
        editLabelDetache = findViewById(R.id.edit_label_detache)
        switchUseSound = findViewById(R.id.switch_use_sound)
        layoutVoiceLabels = findViewById(R.id.layout_voice_labels)
        layoutCustomSounds = findViewById(R.id.layout_custom_sounds)
        tvSoundAttacheStatus = findViewById(R.id.tv_sound_attache_status)
        tvSoundDetacheStatus = findViewById(R.id.tv_sound_detache_status)
        btnSelectSoundAttache = findViewById(R.id.btn_select_sound_attache)
        btnSelectSoundDetache = findViewById(R.id.btn_select_sound_detache)
        switchCount = findViewById(R.id.switch_count)
        switchIDs = findViewById(R.id.switch_ids)
        btnDelete = findViewById(R.id.btn_delete)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
    }

    private fun populateUI() {
        tvMacAddress.text = "MAC: ${penon.macAddress}"
        editPenonName.setText(penon.penonName)
        editTimeline.setText(penon.timeline.toString())

        // ✅ CORRECTION: Utiliser les bonnes propriétés
        switchAvrMagZ.isChecked = penon.avrAvrMagZ  // avr_avr_mag_z
        switchMagZ.isChecked = penon.avrMagZ         // avr_mag_z
        switchFlowState.isChecked = penon.flowState
        switchSDFlowState.isChecked = penon.sDFlowState
        switchMeanAcc.isChecked = penon.meanAcc
        switchMaxAcc.isChecked = penon.maxAcc
        switchSDAcc.isChecked = penon.sDAcc
        switchVbat.isChecked = penon.vbat
        switchDetached.isChecked = penon.detached
        editLabelAttache.setText(penon.labelAttache)
        editLabelDetache.setText(penon.labelDetache)

        // Configuration sons/vocal
        switchUseSound.isChecked = penon.useSound
        if (penon.soundAttachePath.isNotEmpty()) {
            tvSoundAttacheStatus.text = getFileName(Uri.parse(penon.soundAttachePath))
        }
        if (penon.soundDetachePath.isNotEmpty()) {
            tvSoundDetacheStatus.text = getFileName(Uri.parse(penon.soundDetachePath))
        }
        updateSoundUIVisibility(penon.useSound)

        switchCount.isChecked = penon.count
        switchIDs.isChecked = penon.ids
        editAttachedThreshold.hint = penon.editAttachedThreshold.toString()
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

        // Listeners pour les sons personnalisés
        switchUseSound.setOnCheckedChangeListener { _, isChecked ->
            updateSoundUIVisibility(isChecked)
            hasUnsavedChanges = true
        }

        btnSelectSoundAttache.setOnClickListener {
            openAudioFilePicker(soundAttacheLauncher)
        }

        btnSelectSoundDetache.setOnClickListener {
            openAudioFilePicker(soundDetacheLauncher)
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

        // ✅ CORRECTION: Seulement les EditText qui existent
        listOf(
            editPenonName,
            editAttachedThreshold,
            editTimeline,
            editLabelAttache,
            editLabelDetache
        ).forEach { it.addTextChangedListener(textWatcher) }

        // ✅ CORRECTION: Tous les switches qui existent
        listOf(
            switchAvrMagZ,
            switchMagZ,
            switchRSSI,
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

    private fun displayDecodedData(data: PenonDecodedData) {
        lastDecodedData = data
        tableDecodedData.removeAllViews()
        tvNoData.visibility = android.view.View.GONE

        checkAndAnnounceStateChange(data)

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

    private fun checkAndAnnounceStateChange(data: PenonDecodedData) {
        val isCurrentlyAttached = determineAttachedState(data)
        val previousState = penon.lastAttachedState

        if (previousState == null) {
            penon.lastAttachedState = isCurrentlyAttached
            Log.d("VoiceNotification", "🆕 État initial = $isCurrentlyAttached (pas d'annonce)")
            return
        }

        if (previousState != isCurrentlyAttached) {
            Log.d("VoiceNotification", "🔁 Changement d'état: $previousState → $isCurrentlyAttached")
            voiceNotificationManager.announceStateChange(
                penonName = penon.penonName,
                isAttached = isCurrentlyAttached,
                useSound = penon.useSound,
                soundAttachePath = penon.soundAttachePath,
                soundDetachePath = penon.soundDetachePath,
                labelAttache = penon.labelAttache,
                labelDetache = penon.labelDetache
            )

            penon.lastAttachedState = isCurrentlyAttached
        }
    }

    private fun determineAttachedState(data: PenonDecodedData): Boolean {
        return data.meanAcc > 0 && data.sdAcc > 0
    }

    private fun showNoDataMessage() {
        tableDecodedData.removeAllViews()
        tvNoData.visibility = android.view.View.VISIBLE
    }

    private fun saveSettings() {
        try {
            // ✅ CORRECTION: Mise à jour correcte de l'objet Penon
            penon.apply {
                penonName = editPenonName.text.toString()
                timeline = editTimeline.text.toString().toIntOrNull() ?: timeline

                // ✅ CORRECTION: Mapper correctement les switches
                avrAvrMagZ = switchAvrMagZ.isChecked  // Avr Mag Z
                avrMagZ = switchMagZ.isChecked         // Mag Z
                flowState = switchFlowState.isChecked
                sDFlowState = switchSDFlowState.isChecked
                meanAcc = switchMeanAcc.isChecked
                maxAcc = switchMaxAcc.isChecked
                sDAcc = switchSDAcc.isChecked
                vbat = switchVbat.isChecked
                detached = switchDetached.isChecked
                labelAttache = editLabelAttache.text.toString().takeIf { it.isNotBlank() } ?: "attaché"
                labelDetache = editLabelDetache.text.toString().takeIf { it.isNotBlank() } ?: "détaché"
                useSound = switchUseSound.isChecked
                // soundAttachePath et soundDetachePath sont déjà mis à jour par les launchers
                count = switchCount.isChecked
                ids = switchIDs.isChecked

                // ✅ CORRECTION: Convertir EditText en Int
                editAttachedThreshold = this@PenonsSettingsActivity.editAttachedThreshold.text.toString().toIntOrNull()
                    ?: this.editAttachedThreshold
            }

            Log.d("PenonsSettings", "Penon object updated, attempting to save...")

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
            // TODO: Implémenter repository.deletePenon(penon.macAddress)

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

    /**
     * Affiche/masque les sections selon le mode choisi (vocal ou son)
     */
    private fun updateSoundUIVisibility(useSound: Boolean) {
        if (useSound) {
            layoutVoiceLabels.visibility = View.GONE
            layoutCustomSounds.visibility = View.VISIBLE
        } else {
            layoutVoiceLabels.visibility = View.VISIBLE
            layoutCustomSounds.visibility = View.GONE
        }
    }

    /**
     * Ouvre le sélecteur de fichiers audio
     */
    private fun openAudioFilePicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        launcher.launch(intent)
    }

    /**
     * Récupère le nom du fichier depuis l'URI
     */
    private fun getFileName(uri: Uri): String {
        var fileName = "Fichier sélectionné"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PenonsSettings", "Error getting file name: ${e.message}")
        }
        return fileName
    }
}