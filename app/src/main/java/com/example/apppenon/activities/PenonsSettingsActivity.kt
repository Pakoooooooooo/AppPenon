package com.example.apppenon.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.apppenon.R
import com.example.apppenon.data.PenonSettingsRepository
import com.example.apppenon.model.Penon
import com.example.apppenon.model.PenonDecodedData

class PenonsSettingsActivity : AppCompatActivity() {

    lateinit var penon: Penon
    private lateinit var repository: PenonSettingsRepository
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
        @SuppressLint("StaticFieldLeak")
        private var currentActivity: PenonsSettingsActivity? = null

        fun updateDecodedData(data: PenonDecodedData, penomMacAddress: String) {
            currentActivity?.let { activity ->
                if (activity.penon.macAddress == penomMacAddress) {
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

        // ✅ CORRECTION MAJEURE : Récupérer le MAC et charger l'objet proprement
        val macAddress = intent.getStringExtra("penon_mac_address")
        if (macAddress == null) {
            Toast.makeText(this, "Erreur : MAC Address manquante", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Créer un objet Penon temporaire et charger ses vraies valeurs depuis le Repo
        penon = Penon(macAddress = macAddress)
        repository.loadPenon(penon)

        initializeViews()
        populateUI()
        setupListeners()
        showNoDataMessage()
    }

    override fun onResume() {
        super.onResume()
        currentActivity = this
    }

    override fun onPause() {
        super.onPause()
        currentActivity = null
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
        backBtn.setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveSettings() }
        btnDelete.setOnClickListener { finish() }
    }

    fun displayDecodedData(data: PenonDecodedData) {
        lastDecodedData = data
        tableDecodedData.removeAllViews()
        tvNoData.visibility = android.view.View.GONE

        data.toDisplayMap().forEach { (key, value) ->
            val row = TableRow(this)
            val labelView = TextView(this).apply {
                text = key
                textSize = 12f
                setPadding(8, 4, 8, 4)
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.6f)
            }
            val valueView = TextView(this).apply {
                text = value
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(8, 4, 8, 4)
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.4f)
            }
            row.addView(labelView)
            row.addView(valueView)
            tableDecodedData.addView(row)
        }
    }

    fun showNoDataMessage() {
        tableDecodedData.removeAllViews()
        tvNoData.visibility = android.view.View.VISIBLE
    }

    private fun saveSettings() {
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

            // Sauvegarde réelle
            repository.savePenon(penon)

            val resultIntent = Intent().apply {
                putExtra("updated_penon", penon)
            }
            setResult(RESULT_OK, resultIntent)
            Toast.makeText(this, "Sauvegardé", Toast.LENGTH_SHORT).show()
            finish()

        } catch (e: Exception) {
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}