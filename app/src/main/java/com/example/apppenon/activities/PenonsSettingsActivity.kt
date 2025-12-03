package com.example.apppenon.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.apppenon.R
import com.example.apppenon.model.Penon

class PenonsSettingsActivity : AppCompatActivity() {

    lateinit var penon: Penon
    private lateinit var backBtn: Button
    private lateinit var editPenonName: EditText
    private lateinit var editRSSIlow: EditText
    private lateinit var editRSSIhigh: EditText
    private lateinit var editFlowStateLow: EditText
    private lateinit var editFlowStateHigh: EditText
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


    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_penon_settings)

        penon = intent.getSerializableExtra("penon_data") as Penon

        editPenonName = findViewById(R.id.edit_penon_name)
        editRSSIlow = findViewById(R.id.edit_rssi_low)
        editRSSIhigh = findViewById(R.id.edit_rssi_high)
        editFlowStateLow = findViewById(R.id.edit_flow_state_low)
        editFlowStateHigh = findViewById(R.id.edit_flow_state_high)
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
        switchVbat = findViewById(R.id.switch_vbat)
        btnDelete = findViewById(R.id.btn_delete)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
        backBtn = findViewById(R.id.backBtn)

        editPenonName.setText(penon.penonName)
        editRSSIlow.setText(penon.rssiLow.toString())
        editRSSIhigh.setText(penon.rssiHigh.toString())
        editFlowStateLow.setText(penon.flowStateLow.toString())
        editFlowStateHigh.setText(penon.flowStateHigh.toString())
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

        var waitSwitchRSSI = penon.rssi
        var waitSwitchFlowState = penon.flowState
        var waitSwitchSDFlowState = penon.sDFlowState
        var waitSwitchMeanAcc = penon.meanAcc
        var waitSwitchSDAcc = penon.sDAcc
        var waitSwitchMaxAcc = penon.maxAcc
        var waitSwitchVbat = penon.vbat
        var waitSwitchDetached = penon.detached
        var waitSwitchCount = penon.count
        var waitSwitchIDs = penon.ids
        var waitSwitchTimeline = penon.timeline

        switchRSSI.setOnCheckedChangeListener { _, isChecked ->
            waitSwitchRSSI = isChecked
        }
        switchFlowState.setOnCheckedChangeListener { _, isChecked ->
            waitSwitchFlowState = isChecked
        }
        switchSDFlowState.setOnCheckedChangeListener { _, isChecked ->
            waitSwitchSDFlowState = isChecked
        }
        switchMeanAcc.setOnCheckedChangeListener { _, isChecked ->
            waitSwitchMeanAcc = isChecked
        }
        switchSDAcc.setOnCheckedChangeListener { _, isChecked ->
            waitSwitchSDAcc = isChecked
        }
        switchMaxAcc.setOnCheckedChangeListener { _, isChecked ->
            waitSwitchMaxAcc = isChecked
        }
        switchVbat.setOnCheckedChangeListener { _, isChecked ->
            waitSwitchVbat = isChecked
        }
        switchDetached.setOnCheckedChangeListener { _, isChecked ->
            waitSwitchDetached = isChecked
        }
        switchCount.setOnCheckedChangeListener { _, isChecked ->
            waitSwitchCount = isChecked
        }

        backBtn.setOnClickListener {
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            penon.penonName = editPenonName.text.toString()
            penon.rssiLow = editRSSIlow.text.toString().toInt()
            penon.rssiHigh = editRSSIhigh.text.toString().toInt()
            penon.flowStateLow = editFlowStateLow.text.toString().toInt()
            penon.flowStateHigh = editFlowStateHigh.text.toString().toInt()
            penon.sDFlowStateLow = editSDFlowStateLow.text.toString().toInt()
            penon.sDFlowStateHigh = editSDFlowStateHigh.text.toString().toInt()
            penon.meanAccLow = editMeanAccLow.text.toString().toInt()
            penon.meanAccHigh = editMeanAccHigh.text.toString().toInt()
            penon.sDAccLow = editSDAccLow.text.toString().toInt()
            penon.sDAccHigh = editSDAccHigh.text.toString().toInt()
            penon.maxAccLow = editMaxAccLow.text.toString().toInt()
            penon.maxAccHigh = editMaxAccHigh.text.toString().toInt()
            penon.vbatLow = editVbatLow.text.toString().toDouble()
            penon.vbatHigh = editVbatHigh.text.toString().toDouble()
            penon.timeline = editTimeline.text.toString().toInt()
            penon.rssi = waitSwitchRSSI
            penon.flowState = waitSwitchFlowState
            penon.sDFlowState = waitSwitchSDFlowState
            penon.meanAcc = waitSwitchMeanAcc
            penon.sDAcc = waitSwitchSDAcc
            penon.maxAcc = waitSwitchMaxAcc
            penon.vbat = waitSwitchVbat
            penon.detached = waitSwitchDetached
            penon.detachedThresh = editDetached.text.toString().toDouble()
            penon.count = waitSwitchCount
            penon.ids = waitSwitchIDs
            penon.timeline = waitSwitchTimeline

            val resultIntent = Intent()
            resultIntent.putExtra("penon_data", penon)
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        btnDelete.setOnClickListener {
            finish()
        }

    }

}