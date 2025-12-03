package com.example.apppenon.activities

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.apppenon.R
import com.example.apppenon.model.AppData


class SettingActivity : AppCompatActivity() {

    lateinit var modeBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        modeBtn = findViewById(R.id.modeBtn)

        modeBtn.setOnClickListener {

        }

    }

}