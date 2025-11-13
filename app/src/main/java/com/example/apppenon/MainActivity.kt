package com.example.apppenon

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random
import java.util.*

class MainActivity : AppCompatActivity() {

    /* ===== PARAMÈTRES CONFIGURABLES ===== */
    private val windowSize = 10               // nombre de valeurs pour la moyenne glissante (10 sec)
    private val attachThreshold = 100         // seuil pour attaché
    private val simInterval = 1000L           // intervalle de simulation en ms (1Hz)
    private val fakeDevices = listOf(
        "Babord" to "AA:BB:CC:DD:EE:01",
        "Tribord" to "AA:BB:CC:DD:EE:02"
    )

    /* ===== UI ===== */
    private lateinit var tvStatus: TextView
    private lateinit var tvReceivedData: TextView
    private lateinit var tvParsedData: TextView
    private lateinit var tvEtatBabord: TextView
    private lateinit var tvEtatTribord: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnClear: Button

    /* ===== ÉTAT ===== */
    private var isRunning = false
    private val uiHandler = Handler(Looper.getMainLooper())
    private var frameBabord = 0
    private var frameTribord = 0
    private val babordValues = mutableListOf<Int>()
    private val tribordValues = mutableListOf<Int>()

    /* ===== VARIABLES VOCALE ===== */
    private var lastEtatBabord: String? = null
    private var lastEtatTribord: String? = null
    private lateinit var tts: TextToSpeech

    /* ===== SIMULATION ===== */
    private val simulationHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind UI
        tvStatus = findViewById(R.id.tvStatus)
        tvReceivedData = findViewById(R.id.tvReceivedData)
        tvParsedData = findViewById(R.id.tvParsedData)
        tvEtatBabord = findViewById(R.id.tvEtatBabord)
        tvEtatTribord = findViewById(R.id.tvEtatTribord)
        btnStart = findViewById(R.id.btnStartScan)
        btnStop = findViewById(R.id.btnStopScan)
        btnClear = findViewById(R.id.btnClearData)

        // Initialiser TTS
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                tvStatus.text = "Erreur TTS"
            } else {
                tts.language = Locale.FRANCE
            }
        }

        btnStart.setOnClickListener { startSimulation() }
        btnStop.setOnClickListener { stopSimulation() }
        btnClear.setOnClickListener {
            tvReceivedData.text = ""
            tvParsedData.text = ""
            tvEtatBabord.text = "Babord : ⏳ En attente..."
            tvEtatTribord.text = "Tribord : ⏳ En attente..."
            frameBabord = 0
            frameTribord = 0
            babordValues.clear()
            tribordValues.clear()
            lastEtatBabord = null
            lastEtatTribord = null
        }

        updateUI()
    }

    /* ===== CODE COMMUN ===== */
    private fun updateUI() {
        btnStart.isEnabled = !isRunning
        btnStop.isEnabled = isRunning
    }

    private fun autoScroll() {
        val scroll = findViewById<ScrollView>(R.id.scrollView)
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    /* ===== SIMULATION ===== */
    private fun startSimulation() {
        frameBabord = 0
        frameTribord = 0
        babordValues.clear()
        tribordValues.clear()
        lastEtatBabord = null
        lastEtatTribord = null
        isRunning = true
        updateUI()
        tvStatus.text = "Simulation 1Hz…"
        tvReceivedData.text = ""
        tvParsedData.text = ""

        simulationHandler.post(object : Runnable {
            override fun run() {
                fakeDevices.forEach { (name, _) ->

                    val value = Random.nextInt(0, 201)
                    val turbulent = value < attachThreshold
                    val hexFrame = generateHexFrame(value, turbulent)

                    // Comptage séparé et mise à jour moyenne glissante
                    val frameNumber = if (name == "Babord") {
                        frameBabord++
                        babordValues.add(value)
                        if (babordValues.size > windowSize) babordValues.removeAt(0)
                        frameBabord
                    } else {
                        frameTribord++
                        tribordValues.add(value)
                        if (tribordValues.size > windowSize) tribordValues.removeAt(0)
                        frameTribord
                    }

                    val avgBabord = if (babordValues.isNotEmpty()) babordValues.average() else 0.0
                    val avgTribord = if (tribordValues.isNotEmpty()) tribordValues.average() else 0.0

                    val etatBabord = if (avgBabord >= attachThreshold) "Attaché" else "Turbulent"
                    val etatTribord = if (avgTribord >= attachThreshold) "Attaché" else "Turbulent"

                    // Annonces vocales si changement
                    if (etatBabord != lastEtatBabord) {
                        speak("Babord : $etatBabord")
                        lastEtatBabord = etatBabord
                    }
                    if (etatTribord != lastEtatTribord) {
                        speak("Tribord : $etatTribord")
                        lastEtatTribord = etatTribord
                    }

                    val brut = "[$name] $hexFrame"
                    val decode = "Trame ${name.first()}#$frameNumber | $name | Valeur: $value | ${if (turbulent) "Turbulent" else "Attaché"}"

                    uiHandler.post {
                        tvReceivedData.append("$brut\n")
                        tvParsedData.append("$decode\n")
                        tvEtatBabord.text = "Babord : $etatBabord"
                        tvEtatTribord.text = "Tribord : $etatTribord"
                        tvStatus.text = "B:$frameBabord  T:$frameTribord"
                        autoScroll()
                    }
                }

                simulationHandler.postDelayed(this, simInterval)
            }
        })
    }

    private fun stopSimulation() {
        isRunning = false
        simulationHandler.removeCallbacksAndMessages(null)
        tts.stop()
        tvStatus.text = "Arrêté (B:$frameBabord  T:$frameTribord)"
        updateUI()
    }

    private fun generateHexFrame(value: Int, turbulent: Boolean): String {
        val b1 = 0xA1
        val b2 = value and 0xFF
        val b3 = if (turbulent) 0x01 else 0x00
        val b4 = Random.nextInt(255)
        val b5 = Random.nextInt(255)
        val b6 = Random.nextInt(255)
        val b7 = Random.nextInt(255)
        val b8 = (b1 + b2 + b3) and 0xFF
        return "%02X %02X %02X %02X %02X %02X %02X %02X"
            .format(b1, b2, b3, b4, b5, b6, b7, b8)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSimulation()
        tts.shutdown()
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }
}
