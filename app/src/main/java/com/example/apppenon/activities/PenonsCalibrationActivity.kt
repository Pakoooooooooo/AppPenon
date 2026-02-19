package com.example.apppenon.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apppenon.R
import com.example.apppenon.adapters.PenonCardAdapter
import com.example.apppenon.data.PenonSettingsRepository
import com.example.apppenon.model.Penon
import com.example.apppenon.utils.VoiceNotificationManager
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PenonsCalibrationActivity : AppCompatActivity() {

    private lateinit var targetMacAddress: String
    private lateinit var repository: PenonSettingsRepository
    private lateinit var voiceNotificationManager: VoiceNotificationManager
    private var hasUnsavedChanges = false

    private lateinit var tvMacAddress: TextView
    private lateinit var txtRequest: TextView
    private lateinit var txtTitle: TextView
    private lateinit var btnStart: Button
    private lateinit var backBtn: Button
    private lateinit var rvPenonCards: RecyclerView
    private lateinit var penonCardAdapter: PenonCardAdapter

    private var avrAttached: Double = 0.0
    private var avrDetached: Double = 0.0

    private var mainActivity: MainActivity? = null

    companion object {
        private const val TAG = "PenonsCalibration"
        private var currentActivity: WeakReference<PenonsCalibrationActivity>? = null
    }

    private fun finishCalibration(resultValue: Int) {
        val resultIntent = Intent().apply {
            putExtra("calibration_result", resultValue)
        }
        setResult(RESULT_OK, resultIntent)
        Log.d(TAG, "🏁 Calibration terminée avec seuil: $resultValue")
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_penon_calibration)

        repository = PenonSettingsRepository(this)
        voiceNotificationManager = VoiceNotificationManager(this)

        targetMacAddress = intent.getStringExtra("penon_mac_address") ?: run {
            Toast.makeText(this, "Erreur : MAC Address manquante", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mainActivity = MainActivity.getInstance()
        if (mainActivity == null) {
            Log.e(TAG, "MainActivity instance not available")
            Toast.makeText(this, "Erreur d'initialisation", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "✅ Calibration pour MAC: $targetMacAddress")

        initializeViews()
        populateUI()
        setupBackPressedHandler()
        setupListeners()

        // ✅ L'adaptateur pointe directement sur la liste vivante de MainActivity
        //    Aucun deviceList local : tout passe par mainActivity.deviceList
        penonCardAdapter = PenonCardAdapter(
            onPenonClick = { detectedPenon ->
                mainActivity?.getOrCreatePenon(detectedPenon.macAddress)
            },
            penonSettings = mainActivity!!.deviceList.filter { it.macAddress == targetMacAddress }.toMutableList(),
            voiceNotificationManager = voiceNotificationManager
        )

        rvPenonCards.layoutManager = LinearLayoutManager(this)
        rvPenonCards.adapter = penonCardAdapter

        observeSettingsChanges()
    }

    override fun onResume() {
        super.onResume()
        currentActivity = WeakReference(this)
        penonCardAdapter.notifyDataSetChanged()
        Log.d(TAG, "✅ onResume: Activity en focus pour MAC=$targetMacAddress")
    }

    override fun onPause() {
        super.onPause()
        if (currentActivity?.get() == this) currentActivity = null
        Log.d(TAG, "⏸️ onPause: Activity pas en focus")
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ On ne touche pas au PenonReader : il appartient à MainActivity
        voiceNotificationManager.release()
        Log.d(TAG, "🛑 onDestroy: Ressources libérées")
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observeSettingsChanges() {
        lifecycleScope.launch {
            repository.observeAllPenons().collect { allPenons ->
                val list = mainActivity?.deviceList ?: return@collect
                allPenons.forEach { (mac, penon) ->
                    val index = list.indexOfFirst { it.macAddress == mac }
                    if (index != -1 && penon != null) {
                        list[index] = penon.copy()
                    }
                }
                penonCardAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChanges) {
                    // showUnsavedChangesDialog()
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
        rvPenonCards = findViewById(R.id.rvPenonCards)
    }

    @SuppressLint("SetTextI18n")
    private fun populateUI() {
        val penonName = mainActivity?.getPenonByMac(targetMacAddress)?.penonName ?: "Penon"
        txtTitle.text = "Calibration de $penonName"
        tvMacAddress.text = "MAC: $targetMacAddress"
        txtRequest.text = "Mettez $penonName en position attachée puis cliquez sur commencer."
    }

    private fun setupListeners() {
        backBtn.setOnClickListener {
            if (hasUnsavedChanges) {
                // showUnsavedChangesDialog()
            } else {
                finish()
            }
        }

        btnStart.setOnClickListener {
            startCalibrationScan()
        }
    }

    private fun startCalibrationScan() {
        // ✅ On réutilise le PenonReader de MainActivity, pas de nouveau scan concurrent
        val reader = mainActivity?.PR ?: run {
            Toast.makeText(this, "PenonReader non disponible", Toast.LENGTH_SHORT).show()
            return
        }

        btnStart.text = "Collecting data..."
        btnStart.isEnabled = false
        btnStart.setBackgroundColor(resources.getColor(R.color.grey, null))

        val startTime = System.currentTimeMillis()
        reader.startScanning()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                var newFramesReceived = 0
                var lastLoggedFrame = 0
                var initialFrameCount: Int = -1

                while (newFramesReceived < 10) {
                    delay(100)

                    val penon = mainActivity?.getPenonByMac(targetMacAddress)
                    if (penon == null) {
                        Log.e(TAG, "❌ Penon non trouvé pour MAC: $targetMacAddress")
                        break
                    }

                    if (initialFrameCount == -1) {
                        initialFrameCount = penon.state.frame_cnt
                        Log.d(TAG, "📍 Frame count initial: $initialFrameCount")
                    }

                    newFramesReceived = penon.state.frame_cnt - initialFrameCount

                    if (newFramesReceived > lastLoggedFrame) {
                        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                        Log.d(TAG, "📡 Frame ${newFramesReceived}/10 reçue (${String.format("%.1f", elapsedTime)}s)")
                        if (penon.state.magZWindow.isNotEmpty()) {
                            Log.d(TAG, "   - Valeur: ${String.format("%.2f", penon.state.magZWindow.last())}")
                        }
                        lastLoggedFrame = newFramesReceived
                    }

                    if ((System.currentTimeMillis() - startTime) / 1000 >= 30) {
                        Log.w(TAG, "⚠️ Timeout: Seulement $newFramesReceived frames reçues en 30s")
                        break
                    }
                }

                // ✅ On ne stoppe pas le scan : il appartient à MainActivity
                val finalPenon = mainActivity?.getPenonByMac(targetMacAddress)

                if (finalPenon == null) {
                    Toast.makeText(this@PenonsCalibrationActivity, "Erreur: Penon non trouvé", Toast.LENGTH_SHORT).show()
                    resetButton()
                    return@launch
                }

                val totalNewFrames = if (initialFrameCount != -1) finalPenon.state.frame_cnt - initialFrameCount else 0
                val totalTime = (System.currentTimeMillis() - startTime) / 1000.0
                Log.d(TAG, "🛑 Collecte terminée après ${String.format("%.1f", totalTime)}s — $totalNewFrames frames")

                if (totalNewFrames < 5) {
                    Toast.makeText(
                        this@PenonsCalibrationActivity,
                        "Trop peu de données ($totalNewFrames frames). Vérifiez que le Penon est allumé et à portée.",
                        Toast.LENGTH_LONG
                    ).show()
                    resetButton()
                    return@launch
                }

                val averageValue = calculateAverageMagZ(finalPenon) ?: run {
                    Toast.makeText(this@PenonsCalibrationActivity, "Erreur de calcul. Veuillez réessayer.", Toast.LENGTH_LONG).show()
                    resetButton()
                    return@launch
                }

                if (avrAttached == 0.0) {
                    avrAttached = averageValue
                    Log.d(TAG, "✅ Valeur attachée: $avrAttached")

                    btnStart.text = "Commencer"
                    btnStart.isEnabled = true
                    btnStart.setBackgroundColor(resources.getColor(R.color.sea, null))
                    txtRequest.text = "Mettez ${finalPenon.penonName} en position détachée puis cliquez sur commencer."

                    Toast.makeText(
                        this@PenonsCalibrationActivity,
                        "Position attachée calibrée: ${String.format("%.2f", avrAttached)}",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {
                    avrDetached = averageValue
                    Log.d(TAG, "✅ Valeur détachée: $avrDetached")

                    val threshold = ((avrAttached + avrDetached) / 2).toInt()
                    Log.d(TAG, "📊 Seuil: $threshold (attaché=${String.format("%.2f", avrAttached)}, détaché=${String.format("%.2f", avrDetached)})")

                    btnStart.text = "Calibration terminée ✓"
                    btnStart.isEnabled = false
                    btnStart.setBackgroundColor(resources.getColor(R.color.grey, null))

                    Toast.makeText(this@PenonsCalibrationActivity, "Calibration réussie! Seuil: $threshold", Toast.LENGTH_SHORT).show()

                    delay(1500)
                    finishCalibration(threshold)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur: ${e.message}", e)
                Toast.makeText(this@PenonsCalibrationActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                resetButton()
            }
        }
    }

    private fun calculateAverageMagZ(penon: Penon): Double? {
        val values = penon.state.magZWindow
        if (values.isEmpty()) {
            Log.w(TAG, "⚠️ magZWindow est vide!")
            return null
        }
        val average = values.average()
        Log.d(TAG, "📊 ${values.size} échantillons — moy=${String.format("%.2f", average)}, min=${String.format("%.2f", values.min())}, max=${String.format("%.2f", values.max())}")
        return average
    }

    private fun resetButton() {
        btnStart.text = "Commencer"
        btnStart.isEnabled = true
        btnStart.setBackgroundColor(resources.getColor(R.color.sea, null))
    }
}