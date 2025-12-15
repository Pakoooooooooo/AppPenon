package com.example.apppenon

import android.content.Intent
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.apppenon.activities.PenonsSettingsActivity
import com.example.apppenon.model.Penon

/**
 * Gère la configuration des Penons via l'écran de paramètres.
 * Responsabilités:
 * - Créer et gérer les activity result launchers
 * - Lancer l'écran de paramètres pour chaque Penon
 * - Afficher le statut de mise à jour
 */
class PenonSettingsManager(
    private val act: AppCompatActivity,
    private val tvEtatPenon1: TextView,
    private val tvEtatPenon2: TextView,
    private val devices: MutableList<Penon>
) {

    private lateinit var penonSettingsLauncher1: ActivityResultLauncher<Intent>
    private lateinit var penonSettingsLauncher2: ActivityResultLauncher<Intent>

    /**
     * Initialise les activity result launchers.
     * DOIT être appelé dans onCreate avant d'utiliser les launchers.
     */
    fun initialize() {
        penonSettingsLauncher1 = act.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val updatedPenon = result.data?.getSerializableExtra("updated_penon") as? Penon
                if (updatedPenon != null) {
                    devices[0] = updatedPenon
                    println("updatedPenon: $updatedPenon")
                    tvEtatPenon1.text = "${updatedPenon.penonName} : ✅ Mis à jour"
                }
            } else {
                tvEtatPenon1.text = "${devices[0].penonName} : ❌ Annulé"
            }
        }

        penonSettingsLauncher2 = act.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val updatedPenon = result.data?.getSerializableExtra("updated_penon") as? Penon
                if (updatedPenon != null) {
                    devices[1] = updatedPenon
                    println("updatedPenon: $updatedPenon")
                    tvEtatPenon2.text = "${updatedPenon.penonName} : ✅ Mis à jour"
                }
            } else {
                tvEtatPenon2.text = "${devices[1].penonName} : ❌ Annulé"
            }
        }
    }

    /**
     * Lance l'écran de paramètres pour le Penon 1.
     */
    fun launchSettingsForPenon1() {
        val intent = Intent(act, PenonsSettingsActivity::class.java)
        intent.putExtra("penon_data", devices[0])
        penonSettingsLauncher1.launch(intent)
        tvEtatPenon1.text = devices[0].penonName + " : ⏳ En attente..."
    }

    /**
     * Lance l'écran de paramètres pour le Penon 2.
     */
    fun launchSettingsForPenon2() {
        val intent = Intent(act, PenonsSettingsActivity::class.java)
        intent.putExtra("penon_data", devices[1])
        penonSettingsLauncher2.launch(intent)
        tvEtatPenon2.text = devices[1].penonName + " : ⏳ En attente..."
    }

    /**
     * Retourne l'adresse MAC du Penon 1.
     */
    fun getPenon1MacAddress(): String {
        return devices[0].macAdress
    }

    /**
     * Retourne l'adresse MAC du Penon 2.
     */
    fun getPenon2MacAddress(): String {
        return devices[1].macAdress
    }
}
