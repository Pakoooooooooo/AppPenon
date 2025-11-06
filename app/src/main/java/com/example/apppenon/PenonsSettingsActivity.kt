package com.example.apppenon

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
class PenonsSettingsActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_settings)

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
        switchCount = findViewById(R.id.switch_count)
        switchIDs = findViewById(R.id.switch_ids)
        switchVbat = findViewById(R.id.switch_vbat)
        btnDelete = findViewById(R.id.btn_delete)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
        backBtn = findViewById(R.id.backBtn)

        backBtn.setOnClickListener {
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            finish()
        }

        btnDelete.setOnClickListener {
            finish()
        }

    }

}