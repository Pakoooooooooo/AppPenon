package com.example.apppenon

import com.example.apppenon.activities.MainActivity
import com.example.apppenon.model.PenonReader

/**
 * Gère l'état de l'interface utilisateur de MainActivity.
 * Responsabilités:
 * - Gérer l'état des boutons (actifs/inactifs)
 */
class UIStateManager(private val act: MainActivity) {

    /**
     * Mets à jour l'état des boutons selon le statut du scan BLE.
     * Active/désactive les boutons appropriés lors du scan.
     */
    fun updateUIState(PR: PenonReader) {
        act.btnStartScan.isEnabled = !PR.isScanning
        act.btnStopScan.isEnabled = PR.isScanning
        act.etFileName.isEnabled = !PR.isScanning
    }
}
