package com.example.apppenon.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.apppenon.model.AppData
import com.example.apppenon.model.PenonGroup
import java.util.Locale

/**
 * Gestionnaire des notifications vocales et sonores pour les groupes.
 *
 * Responsabilites:
 * - Initialiser TTS pour les annonces vocales
 * - Gerer les sons personnalises via SoundManager
 * - Annoncer les changements d'etat d'un groupe (attache/detache/babord/tribord)
 * - Bufferiser les annonces selon le mute time global
 */
class VoiceNotificationManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private val soundManager: SoundManager = SoundManager(context)
    private val TAG = "VoiceNotification"
    private var isInitialized = false

    private val handler = Handler(Looper.getMainLooper())
    private var flushRunnable: Runnable? = null
    private var lastAnnouncementTime: Long = 0L

    data class PendingGroupAnnouncement(
        val groupId: String,
        val group: PenonGroup,
        val state: String
    )

    private val pendingGroupAnnouncements = mutableMapOf<String, PendingGroupAnnouncement>()

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.FRENCH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
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
     * Bufferise ou annonce immédiatement un changement d'état d'un groupe.
     */
    fun bufferGroupStateChange(group: PenonGroup, state: String) {
        val muteTime = AppData.muteTimeSeconds

        if (muteTime <= 0) {
            announceGroupState(group, state)
            lastAnnouncementTime = System.currentTimeMillis()
            return
        }

        val now = System.currentTimeMillis()
        val elapsed = now - lastAnnouncementTime
        val muteTimeMs = muteTime * 1000L

        if (elapsed >= muteTimeMs) {
            announceGroupState(group, state)
            lastAnnouncementTime = System.currentTimeMillis()
            return
        }

        pendingGroupAnnouncements[group.groupId] = PendingGroupAnnouncement(group.groupId, group, state)

        if (flushRunnable == null) {
            val remainingMs = muteTimeMs - elapsed
            flushRunnable = Runnable { flushAnnouncements() }
            handler.postDelayed(flushRunnable!!, remainingMs)
        }
    }

    /**
     * Annonce vocale ou sonore immédiate de l'état d'un groupe.
     */
    fun announceGroupState(group: PenonGroup, state: String) {
        if (group.useSound) {
            val soundPath = group.soundPathForState(state)
            if (soundPath.isNotEmpty()) {
                Log.d(TAG, "Lecture son groupe: $state")
                soundManager.playSound(soundPath)
            } else {
                Log.w(TAG, "Aucun son configuré pour l'état $state du groupe ${group.groupName}")
            }
        } else {
            if (!isInitialized || textToSpeech == null) return
            val text = group.announcementText(state)
            if (text.isEmpty()) return
            Log.d(TAG, "Annonce groupe: $text")
            textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, "GROUP_${group.groupId}")
        }
    }

    private fun flushAnnouncements() {
        flushRunnable = null
        if (pendingGroupAnnouncements.isEmpty()) return

        Log.d(TAG, "Flush: ${pendingGroupAnnouncements.size} annonce(s) groupe")

        val announcements = pendingGroupAnnouncements.values.toList()
        pendingGroupAnnouncements.clear()

        for (ga in announcements) {
            announceGroupState(ga.group, ga.state)
        }

        lastAnnouncementTime = System.currentTimeMillis()
    }

    fun stopAnnouncement() {
        textToSpeech?.stop()
        soundManager.stopSound()
    }

    fun release() {
        flushRunnable?.let { handler.removeCallbacks(it) }
        flushRunnable = null
        pendingGroupAnnouncements.clear()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        soundManager.release()
        isInitialized = false
        Log.d(TAG, "Ressources liberees")
    }

    fun isReady(): Boolean = isInitialized
}
