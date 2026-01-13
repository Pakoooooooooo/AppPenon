package com.example.apppenon.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.apppenon.R
import com.example.apppenon.model.AppData
import com.example.apppenon.simulation.SimulationConfig

/**
 * Activité des paramètres de l'application.
 * Gère les modes d'affichage, l'enregistrement CSV et la simulation.
 */
class SettingActivity : AppCompatActivity() {

    lateinit var modeBtn: Button
    lateinit var backBtn: Button
    lateinit var recSwitch: SwitchCompat
    
    // 🆕 Nouveaux éléments pour la simulation
    lateinit var simulationSwitch: SwitchCompat
    lateinit var btnSelectCSV: Button
    lateinit var tvSelectedFile: TextView

    // Launcher pour sélectionner un fichier CSV
    private val selectFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Sauvegarder l'URI du fichier sélectionné
                SimulationConfig.csvFileUri = uri
                
                // Extraire le nom du fichier pour l'affichage
                val fileName = getFileName(uri)
                SimulationConfig.csvFileName = fileName
                tvSelectedFile.text = "📄 $fileName"
                
                Toast.makeText(
                    this, 
                    "Fichier sélectionné : $fileName", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_AppPenon)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialiser les vues existantes
        modeBtn = findViewById(R.id.modeBtn)
        backBtn = findViewById(R.id.backBtn)
        recSwitch = findViewById(R.id.switch_rec)
        
        // 🆕 Initialiser les nouvelles vues de simulation
        simulationSwitch = findViewById(R.id.switch_simulation)
        btnSelectCSV = findViewById(R.id.btn_select_csv)
        tvSelectedFile = findViewById(R.id.tv_selected_file)

        // Charger l'état actuel
        modeBtn.text = AppData.modes[AppData.mode]
        recSwitch.isChecked = AppData.rec
        simulationSwitch.isChecked = SimulationConfig.isSimulationMode
        tvSelectedFile.text = if (SimulationConfig.csvFileUri != null) {
            "📄 ${SimulationConfig.csvFileName}"
        } else {
            "Aucun fichier sélectionné"
        }

        // Bouton retour
        backBtn.setOnClickListener {
            finish()
        }

        // Bouton mode (Standard/Développeur)
        modeBtn.setOnClickListener {
            AppData.nextMode()
            modeBtn.text = AppData.modes[AppData.mode]
        }

        // Switch enregistrement
        recSwitch.setOnCheckedChangeListener { _, isChecked ->
            AppData.rec = isChecked
        }

        // 🆕 Switch simulation
        simulationSwitch.setOnCheckedChangeListener { _, isChecked ->
            SimulationConfig.isSimulationMode = isChecked
            
            // Activer/désactiver le bouton de sélection de fichier
            btnSelectCSV.isEnabled = isChecked
            
            if (isChecked) {
                Toast.makeText(
                    this,
                    "Mode simulation activé - Sélectionnez un fichier CSV",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Mode BLE réel activé",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // 🆕 Bouton sélection fichier CSV
        btnSelectCSV.setOnClickListener {
            openFilePicker()
        }
        
        // État initial du bouton
        btnSelectCSV.isEnabled = SimulationConfig.isSimulationMode
    }

    /**
     * Ouvre le sélecteur de fichiers pour choisir un CSV.
     */
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // Accepter tous les fichiers
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values"))
        }
        
        selectFileLauncher.launch(intent)
    }

    /**
     * Extrait le nom du fichier depuis l'URI.
     */
    private fun getFileName(uri: android.net.Uri): String {
        var fileName = "fichier.csv"
        
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        
        return fileName
    }
}