package com.example.apppenon

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apppenon.activities.MainActivity
import com.example.apppenon.model.AppData
import com.example.apppenon.model.PenonReader

/**
 * Gère l'état de l'interface utilisateur de MainActivity.
 * Responsabilités:
 * - Mettre à jour la visibilité des composants selon le mode (Standard/Développeur)
 * - Gérer l'état des boutons (actifs/inactifs)
 * - Gérer le scroll automatique des TextViews
 */
class UIStateManager(private val act: MainActivity) {

    /**
     * Mets à jour la visibilité des composants selon le mode d'application.
     * Mode 0: Standard (RecyclerView des Penons)
     * Mode 1: Développeur (Logs détaillés de deux Penons spécifiques)
     */
    fun updateUIForMode() {
        when (AppData.mode) {
            0 -> updateUIForStandardMode()
            1 -> updateUIForDeveloperMode()
        }
    }

    /**
     * Affiche l'interface du mode Standard.
     * Montre le RecyclerView avec les Penons détectés.
     */
    private fun updateUIForStandardMode() {
        act.rvPenonCards.visibility = View.VISIBLE
        act.layoutDeveloper.visibility = View.GONE

        act.tvStatus.visibility = View.VISIBLE
        act.tvReceivedData1.visibility = View.GONE
        act.tvReceivedData2.visibility = View.GONE
        act.tvParsedData1.visibility = View.GONE
        act.tvParsedData2.visibility = View.GONE
        act.btnSetP1.visibility = View.GONE
        act.btnSetP2.visibility = View.GONE
        act.tvEtatPenon1.visibility = View.GONE
        act.tvEtatPenon2.visibility = View.GONE

        act.etFileName.visibility = if (AppData.rec) View.VISIBLE else View.GONE

        act.btnStartScan.visibility = View.VISIBLE
        act.btnStopScan.visibility = View.VISIBLE
        act.btnClearData.visibility = View.VISIBLE
        act.appModeBtn.visibility = View.VISIBLE
    }

    /**
     * Affiche l'interface du mode Développeur.
     * Affiche les logs détaillés pour deux Penons spécifiques.
     */
    private fun updateUIForDeveloperMode() {
        act.rvPenonCards.visibility = View.GONE
        act.layoutDeveloper.visibility = View.VISIBLE

        act.tvStatus.visibility = View.VISIBLE
        act.tvReceivedData1.visibility = View.VISIBLE
        act.tvReceivedData2.visibility = View.VISIBLE
        act.tvParsedData1.visibility = View.VISIBLE
        act.tvParsedData2.visibility = View.VISIBLE
        act.btnSetP1.visibility = View.VISIBLE
        act.btnSetP2.visibility = View.VISIBLE
        act.tvEtatPenon1.visibility = View.VISIBLE
        act.tvEtatPenon2.visibility = View.VISIBLE
        act.btnStartScan.visibility = View.VISIBLE
        act.btnStopScan.visibility = View.VISIBLE
        act.btnClearData.visibility = View.VISIBLE
        act.appModeBtn.visibility = View.VISIBLE

        act.etFileName.visibility = if (AppData.rec) View.VISIBLE else View.GONE
    }

    /**
     * Mets à jour l'état des boutons selon le statut du scan BLE.
     * Active/désactive les boutons appropriés lors du scan.
     */
    fun updateUIState(PR: PenonReader) {
        act.btnStartScan.isEnabled = !PR.isScanning
        act.btnStopScan.isEnabled = PR.isScanning
        act.etFileName.isEnabled = !PR.isScanning
        act.btnSetP1.isEnabled = !PR.isScanning
        act.btnSetP2.isEnabled = !PR.isScanning
    }

    /**
     * Scroll automatique des TextViews pour les logs du mode Développeur.
     */
    fun autoScroll() {
        val scrollView1 = act.findViewById<ScrollView>(com.example.apppenon.R.id.scrollView1)
        scrollView1?.post {
            scrollView1.fullScroll(View.FOCUS_DOWN)
        }
        val scrollView2 = act.findViewById<ScrollView>(com.example.apppenon.R.id.scrollView2)
        scrollView2?.post {
            scrollView2.fullScroll(View.FOCUS_DOWN)
        }
    }
}
