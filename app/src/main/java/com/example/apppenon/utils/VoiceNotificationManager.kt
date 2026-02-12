package com.example.apppenon.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.apppenon.model.AppData
import java.util.Locale

/**
 * Gestionnaire des notifications vocales et sonores.
 *
 * Responsabilites:
 * - Initialiser TTS pour les annonces vocales
 * - Gerer les sons personnalises via SoundManager
 * - Annoncer les changements d'etat du Penon (attache/detache)
 * - Bufferiser les annonces selon le mute time global
 */
class VoiceNotificationManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private val soundManager: SoundManager = SoundManager(context)
    private val TAG = "VoiceNotification"
    private var isInitialized = false

    // Buffer pour les annonces en attente (cle = penonName pour eviter les doublons)
    private val pendingAnnouncements = mutableMapOf<String, PendingAnnouncement>()
    private val handler = Handler(Looper.getMainLooper())
    private var flushRunnable: Runnable? = null
    // Timestamp de la derniere annonce (pour savoir si on est en periode mute)
    private var lastAnnouncementTime: Long = 0L

    data class PendingAnnouncement(
        val penonName: String,
        val isAttached: Boolean,
        val useSound: Boolean,
        val soundAttachePath: String,
        val soundDetachePath: String,
        val labelAttache: String,
        val labelDetache: String
    )

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.FRENCH)

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.e(TAG, "Langue FR non supportee")
            } else {
                isInitialized = true
                Log.d(TAG, "TTS pret en francais")
            }
        } else {
            Log.e(TAG, "Erreur init TTS: $status")
        }
    }

    /**
     * Bufferise ou annonce immediatement un changement d'etat selon le mute time global.
     *
     * Logique : apres chaque annonce, le mute timer demarre.
     * Pendant le mute, tous les changements sont bufferises.
     * A la fin du mute, le buffer est annonce d'un coup.
     */
    fun bufferStateChange(
        penonName: String,
        isAttached: Boolean,
        useSound: Boolean = false,
        soundAttachePath: String = "",
        soundDetachePath: String = "",
        labelAttache: String = "attache",
        labelDetache: String = "detache"
    ) {
        val muteTime = AppData.muteTimeSeconds

        if (muteTime <= 0) {
            // Pas de mute : annonce immediate
            announceStateChange(penonName, isAttached, useSound, soundAttachePath, soundDetachePath, labelAttache, labelDetache)
            lastAnnouncementTime = System.currentTimeMillis()
            return
        }

        val now = System.currentTimeMillis()
        val elapsed = now - lastAnnouncementTime
        val muteTimeMs = muteTime * 1000L

        if (elapsed >= muteTimeMs) {
            // Periode mute terminee : annoncer immediatement
            announceStateChange(penonName, isAttached, useSound, soundAttachePath, soundDetachePath, labelAttache, labelDetache)
            lastAnnouncementTime = System.currentTimeMillis()
            return
        }

        // On est en periode mute : bufferiser
        pendingAnnouncements[penonName] = PendingAnnouncement(
            penonName, isAttached, useSound, soundAttachePath, soundDetachePath, labelAttache, labelDetache
        )
        Log.d(TAG, "Mute actif (${(muteTimeMs - elapsed) / 1000}s restantes). Buffered: $penonName (${pendingAnnouncements.size} en attente)")

        // Programmer le flush a la fin de la periode mute si pas deja programme
        if (flushRunnable == null) {
            val remainingMs = muteTimeMs - elapsed
            flushRunnable = Runnable { flushAnnouncements() }
            handler.postDelayed(flushRunnable!!, remainingMs)
            Log.d(TAG, "Flush programme dans ${remainingMs / 1000}s")
        }
    }

    /**
     * Annonce tous les changements bufferises et demarre une nouvelle periode mute.
     */
    private fun flushAnnouncements() {
        flushRunnable = null

        if (pendingAnnouncements.isEmpty()) return

        Log.d(TAG, "Flush: ${pendingAnnouncements.size} annonce(s)")

        val announcements = pendingAnnouncements.values.toList()
        pendingAnnouncements.clear()

        // Separer sons et TTS
        val soundAnnouncements = announcements.filter { it.useSound }
        val ttsAnnouncements = announcements.filter { !it.useSound }

        // Jouer les sons
        if (soundAnnouncements.isNotEmpty()) {
            for (announcement in soundAnnouncements) {
                val soundPath = if (announcement.isAttached) announcement.soundAttachePath else announcement.soundDetachePath
                if (soundPath.isNotEmpty()) {
                    soundManager.playSound(soundPath)
                }
            }
        }

        // Construire une annonce TTS combinee
        if (ttsAnnouncements.isNotEmpty() && isInitialized && textToSpeech != null) {
            val combinedText = ttsAnnouncements.joinToString(". ") { announcement ->
                val state = if (announcement.isAttached) announcement.labelAttache else announcement.labelDetache
                "${announcement.penonName} est $state"
            }

            Log.d(TAG, "Annonce combinee: $combinedText")
            textToSpeech?.speak(
                combinedText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "PENON_STATE_CHANGE_BATCH"
            )
        }

        // Nouvelle periode mute commence maintenant
        lastAnnouncementTime = System.currentTimeMillis()
    }

    /**
     * Annonce le changement d'etat d'un Penon (immediat).
     */
    fun announceStateChange(
        penonName: String,
        isAttached: Boolean,
        useSound: Boolean = false,
        soundAttachePath: String = "",
        soundDetachePath: String = "",
        labelAttache: String = "attache",
        labelDetache: String = "detache"
    ) {
        if (useSound) {
            val soundPath = if (isAttached) soundAttachePath else soundDetachePath
            if (soundPath.isNotEmpty()) {
                Log.d(TAG, "Lecture son: ${if (isAttached) "attache" else "detache"}")
                soundManager.playSound(soundPath)
            } else {
                Log.w(TAG, "Aucun son configure pour cet etat")
            }
        } else {
            if (!isInitialized || textToSpeech == null) {
                Log.w(TAG, "TTS non initialise, impossible d'annoncer")
                return
            }

            val state = if (isAttached) labelAttache else labelDetache
            val announcement = "$penonName est $state"

            Log.d(TAG, "Annonce vocale: $announcement")

            textToSpeech?.speak(
                announcement,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "PENON_STATE_CHANGE"
            )
        }
    }

    /**
     * Arrete l'annonce en cours.
     */
    fun stopAnnouncement() {
        textToSpeech?.stop()
        soundManager.stopSound()
    }

    /**
     * Libere les ressources de TTS et du SoundManager.
     */
    fun release() {
        flushRunnable?.let { handler.removeCallbacks(it) }
        flushRunnable = null
        pendingAnnouncements.clear()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        soundManager.release()
        isInitialized = false
        Log.d(TAG, "Ressources liberees")
    }

    /**
     * Verifie si TTS est initialise.
     */
    fun isReady(): Boolean = isInitialized
}
