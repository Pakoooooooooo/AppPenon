package com.example.apppenon.activities

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.apppenon.R
import com.example.apppenon.model.AppData


class SettingActivity : AppCompatActivity() {

    lateinit var modeBtn: Button
    lateinit var backBtn: Button
    lateinit var recSwitch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        modeBtn = findViewById(R.id.modeBtn)
        backBtn = findViewById(R.id.backBtn)
        recSwitch = findViewById(R.id.switch_rec)

        modeBtn.text = AppData.modes[AppData.mode]

        backBtn.setOnClickListener {
            finish()
        }

        modeBtn.setOnClickListener {
            AppData.nextMode()
            modeBtn.text = AppData.modes[AppData.mode]
        }

        recSwitch.isChecked = AppData.rec

        recSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppData.rec = isChecked
            // ✅ Pas besoin d'action ici, la MainActivity se mettra à jour dans onResume()
        }
    }
}