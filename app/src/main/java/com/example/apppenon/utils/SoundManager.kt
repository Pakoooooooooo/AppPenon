package com.example.apppenon.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

/**
 * Gestionnaire des sons personnalisés.
 *
 * Responsabilités:
 * - Jouer des fichiers audio depuis l'URI
 * - Gérer le MediaPlayer
 * - Libérer les ressources
 */
class SoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val TAG = "SoundManager"

    /**
     * Joue un son depuis un chemin URI.
     *
     * @param soundPath Chemin URI du fichier son
     */
    fun playSound(soundPath: String) {
        if (soundPath.isEmpty()) {
            Log.w(TAG, "⚠️ Chemin du son vide")
            return
        }

        try {
            // Libérer le MediaPlayer précédent s'il existe
            release()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(soundPath))
                prepare()
                start()

                // Libérer automatiquement après la lecture
                setOnCompletionListener {
                    release()
                }
            }

            Log.d(TAG, "🔊 Lecture du son: $soundPath")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lecture son: ${e.message}", e)
        }
    }

    /**
     * Arrête la lecture en cours.
     */
    fun stopSound() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Erreur arrêt son: ${e.message}")
        }
    }

    /**
     * Libère les ressources du MediaPlayer.
     */
    fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Erreur libération ressources: ${e.message}")
        }
    }
}
