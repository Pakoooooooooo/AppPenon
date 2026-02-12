package com.example.apppenon.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
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
import com.example.apppenon.utils.VoiceNotificationManager
import java.lang.ref.WeakReference
import androidx.core.net.toUri
import com.example.apppenon.model.PenonReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PenonsCalibrationActivity : AppCompatActivity() {

    private lateinit var penon: Penon
    private lateinit var repository: PenonSettingsRepository
    private lateinit var voiceNotificationManager: VoiceNotificationManager
    private var hasUnsavedChanges = false
    private lateinit var tvMacAddress: TextView
    private lateinit var txtRequest: TextView
    private lateinit var txtTitle: TextView
    private lateinit var btnStart: Button

    // UI Components
    private lateinit var backBtn: Button
    private var avrAttached: Double = 0.0
    private var avrDetached: Double = 0.0
    val mainActivity = MainActivity.getInstance()

    companion object {
        private var currentActivity: WeakReference<PenonsCalibrationActivity>? = null

    }

    private fun finishCalibration(resultValue: Int) {
        val resultIntent = Intent()
        resultIntent.putExtra("calibration_result", resultValue)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_penon_calibration)

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
            Log.d("PenonsSettings", "Après chargement - avrMagZ: ${penon.avrMagZ}")
        } catch (e: Exception) {
            Log.e("PenonsSettings", "Error loading penon: ${e.message}")
            Toast.makeText(this, "Erreur de chargement des données", Toast.LENGTH_SHORT).show()
        }

        initializeViews()
        populateUI()
        setupBackPressedHandler()
        setupListeners()
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
        Log.d("PenonsCalib", "⏸️ onPause: Activity pas en focus")
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceNotificationManager.release()
        Log.d("PenonsCalib", "🛑 onDestroy: Ressources libérées")
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChanges) {
                    //showUnsavedChangesDialog()
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
        txtRequest = findViewById(R.id.txt_request)
        txtTitle = findViewById(R.id.txt_title)
        btnStart = findViewById(R.id.btn_start)
    }

    @SuppressLint("SetTextI18n")
    private fun populateUI() {
        txtTitle.text = "Calibration de ${penon.penonName}"
        tvMacAddress.text = "MAC: ${penon.macAddress}"
        if (avrAttached == 0.0){
            txtRequest.text = "Mettez ${penon.penonName} en position attachée puis cliquez sur commencer."
        } else {
            txtRequest.text = "Mettez ${penon.penonName} en position détachée puis cliquez sur commencer."
        }
    }

    private fun setupListeners() {
        backBtn.setOnClickListener {
            if (hasUnsavedChanges) {
                //showUnsavedChangesDialog()
            } else {
                finish()
            }
        }
        btnStart.setOnClickListener {
            mainActivity?.let {
                val PR = PenonReader(it)

                // Désactiver le bouton
                btnStart.text = "Please wait 10s..."
                btnStart.isEnabled = false
                btnStart.setBackgroundColor(resources.getColor(R.color.grey))

                // Démarrer le scan
                PR.startScanning()

                // Attendre 10 secondes puis arrêter
                CoroutineScope(Dispatchers.Main).launch {
                    delay(10000) // Attend 10 secondes

                    // Arrêter le scan
                    PR.stopScanning()
                    if (avrAttached == 0.0){
                        avrAttached = 20.0}
                    else{
                        avrDetached = 40.0}

                    // Réactiver le bouton
                    if (avrDetached==0.0){
                        btnStart.text = "Commencer"
                        btnStart.isEnabled = true
                        btnStart.setBackgroundColor(resources.getColor(R.color.sea))
                        txtRequest.text = "Mettez ${penon.penonName} en position détachée puis cliquez sur commencer."
                    } else {
                        btnStart.text = "Terminé"
                    }

                    // Vérifier et fermer si les valeurs sont définies
                    if (avrAttached != 0.0 && avrDetached != 0.0) {
                        finishCalibration(((avrAttached+avrDetached)/2).toInt())
                    }
                }
            }
        }
    }
}