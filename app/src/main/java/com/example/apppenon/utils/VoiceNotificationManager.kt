package com.example.apppenon.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Gestionnaire des notifications vocales et sonores.
 *
 * Responsabilités:
 * - Initialiser TTS pour les annonces vocales
 * - Gérer les sons personnalisés via SoundManager
 * - Annoncer les changements d'état du Penon (attaché/détaché)
 */
class VoiceNotificationManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private val soundManager: SoundManager = SoundManager(context)
    private val TAG = "VoiceNotification"
    private var isInitialized = false
    
    init {
        // Initialiser TTS
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.FRENCH)

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.e(TAG, "❌ Langue FR non supportée")
            } else {
                isInitialized = true
                Log.d(TAG, "✅ TTS prêt en français")
            }
        } else {
            Log.e(TAG, "❌ Erreur init TTS: $status")
        }
    }

    /**
     * Annonce le changement d'état d'un Penon.
     *
     * @param penonName Nom du Penon (ex: "Penon 1")
     * @param isAttached true si attaché, false si détaché
     * @param useSound true pour jouer un son, false pour annonce vocale
     * @param soundAttachePath Chemin du son pour "attaché"
     * @param soundDetachePath Chemin du son pour "détaché"
     * @param labelAttache Label personnalisé pour l'état "attaché"
     * @param labelDetache Label personnalisé pour l'état "détaché"
     */
    fun announceStateChange(
        penonName: String,
        isAttached: Boolean,
        useSound: Boolean = false,
        soundAttachePath: String = "",
        soundDetachePath: String = "",
        labelAttache: String = "attaché",
        labelDetache: String = "détaché"
    ) {
        if (useSound) {
            // Mode son personnalisé
            val soundPath = if (isAttached) soundAttachePath else soundDetachePath
            if (soundPath.isNotEmpty()) {
                Log.d(TAG, "🔊 Lecture son: ${if (isAttached) "attaché" else "détaché"}")
                soundManager.playSound(soundPath)
            } else {
                Log.w(TAG, "⚠️ Aucun son configuré pour cet état")
            }
        } else {
            // Mode annonce vocale (TTS)
            if (!isInitialized || textToSpeech == null) {
                Log.w(TAG, "⚠️ TTS non initialisé, impossible d'annoncer")
                return
            }

            val state = if (isAttached) labelAttache else labelDetache
            val announcement = "$penonName est $state"

            Log.d(TAG, "🔊 Annonce vocale: $announcement")

            // Utiliser speak avec le queue
            textToSpeech?.speak(
                announcement,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "PENON_STATE_CHANGE"
            )
        }
    }
    
    /**
     * Arrête l'annonce en cours.
     */
    fun stopAnnouncement() {
        textToSpeech?.stop()
        soundManager.stopSound()
    }

    /**
     * Libère les ressources de TTS et du SoundManager.
     */
    fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        soundManager.release()
        isInitialized = false
        Log.d(TAG, "🛑 Ressources libérées")
    }
    
    /**
     * Vérifie si TTS est initialisé.
     */
    fun isReady(): Boolean = isInitialized
}
