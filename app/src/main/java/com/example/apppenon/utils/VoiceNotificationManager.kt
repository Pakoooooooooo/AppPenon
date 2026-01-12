package com.example.apppenon.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Gestionnaire des notifications vocales (Text-to-Speech).
 * 
 * Responsabilités:
 * - Initialiser TTS
 * - Annoncer les changements d'état du Penon (attaché/détaché)
 * - Permettre la personnalisation des annonces par des sons ultérieurement
 */
class VoiceNotificationManager(private val context: Context) : TextToSpeech.OnInitListener {
    
    private var textToSpeech: TextToSpeech? = null
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
     */
    fun announceStateChange(penonName: String, isAttached: Boolean) {
        if (!isInitialized || textToSpeech == null) {
            Log.w(TAG, "⚠️ TTS non initialisé, impossible d'annoncer")
            return
        }
        
        val state = if (isAttached) "attaché" else "détaché"
        val announcement = "$penonName est $state"
        
        Log.d(TAG, "🔊 Annonce: $announcement")
        
        // Utiliser speak avec le queue
        textToSpeech?.speak(
            announcement,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "PENON_STATE_CHANGE"
        )
    }
    
    /**
     * Arrête l'annonce en cours.
     */
    fun stopAnnouncement() {
        textToSpeech?.stop()
    }
    
    /**
     * Libère les ressources de TTS.
     */
    fun release() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        isInitialized = false
        Log.d(TAG, "🛑 Ressources TTS libérées")
    }
    
    /**
     * Vérifie si TTS est initialisé.
     */
    fun isReady(): Boolean = isInitialized
}
