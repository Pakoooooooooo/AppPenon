package com.example.apppenon.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apppenon.R
import com.example.apppenon.model.BLEScanManager
import com.example.apppenon.model.Penon
import com.example.apppenon.utils.VoiceNotificationManager
import kotlin.math.abs

class PenonCardAdapter (
    private val onPenonClick: ((Penon) -> Unit)? = null,
    private val penonSettings: MutableList<Penon> = mutableListOf(),
    private val voiceNotificationManager: VoiceNotificationManager? = null
) : RecyclerView.Adapter<PenonCardAdapter.PenonViewHolder>() {

    private val penonList = mutableListOf<Penon>()

    class PenonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPenonName: TextView = view.findViewById(R.id.tvPenonName)
        val tvMacAddress: TextView = view.findViewById(R.id.tvMacAddress)
        val tvAttachedStatus: TextView = view.findViewById(R.id.tvAttachedStatus)
        val tvData: TextView = view.findViewById(R.id.tvData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PenonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_penon_card, parent, false)
        return PenonViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PenonViewHolder, position: Int) {
        val penon = penonList[position]

        // 1. Récupérer les réglages mis à jour
        // IMPORTANT : penonSettings DOIT être la liste mise à jour depuis MainActivity
        val settings = penonSettings.find { it.macAddress == penon.macAddress }

        val nameToDisplay = settings?.penonName ?: penon.penonName
        val threshold = settings?.editAttachedThreshold ?: 3500

        // 2. Mise à jour des textes en fonction des paramètres de chaque penon
        holder.tvPenonName.text = nameToDisplay
        holder.tvMacAddress.text = "MAC: ${penon.macAddress}"
        var print = ""
        if (settings?.count == true) {
            print += "Frame: ${penon.state.frame_cnt}\n"
        }
        if (settings?.vbat == true) {
            print += "Vbat: ${penon.state.vbat} V\n"
        }
        if (settings?.avrMagZ == true) {
            print += "MagZ: ${penon.state.avr_mag_z.toInt()} mT×10⁻³\n"
        }
        if (settings?.meanAcc == true) {
            print += "AvrAcc: ${penon.state.avr_acc.toInt()} m.s⁻²×10⁻³\n"
        }
        if (settings?.maxAcc == true) {
            print += "MaxAcc: ${penon.state.max_acc.toInt()} m.s⁻²×10⁻³\n"
        }
        holder.tvData.text = print

        // ... (votre code RSSI et Batterie est correct)

        // 3. Logique d'attachement (Calculée avec le nouveau seuil)
        val mathDone = penon.state.frame_cnt > 10
        val isAttached = abs(penon.state.getFlowState()) >= threshold

        // 🔊 Détecter les changements d'état et annoncer (vocal ou son)
        if (settings != null) {
            val previousState = settings.lastAttachedState
            if (previousState != null && previousState != isAttached) {
                // L'état a changé, annoncer (vocal ou son selon la config)
                voiceNotificationManager?.announceStateChange(
                    penonName = settings.penonName,
                    isAttached = isAttached,
                    useSound = settings.useSound,
                    soundAttachePath = settings.soundAttachePath,
                    soundDetachePath = settings.soundDetachePath,
                    labelAttache = settings.labelAttache,
                    labelDetache = settings.labelDetache
                )
            }
            // Mettre à jour l'état précédent
            settings.lastAttachedState = isAttached
        }

        holder.tvAttachedStatus.apply {
            if (mathDone) {
                text = if (isAttached) "🔗 ATTACHÉ" else "❌ DÉTACHÉ"
                setTextColor(if (isAttached) 0xFF4CAF50.toInt() else 0xFFE91E63.toInt())
            } else {
                text = "⏳ WAITING FOR DATA"
                setTextColor(0xFF00BCD4.toInt())
            }
        }

        // 4. Click listener
        holder.itemView.setOnClickListener {
            // On passe l'objet de données détectées
            onPenonClick?.invoke(penon)
        }
    }

    override fun getItemCount() = penonList.size

    fun updatePenon(macAddress: String, rawHexData: ByteArray, bleScanManager: BLEScanManager) {
        val index = penonList.indexOfFirst { it.macAddress == macAddress }
        if (index != -1) {
            penonList[index].state.updateFromRawData(rawHexData)
            notifyItemChanged(index)
        } else if (rawHexData.isNotEmpty() && bleScanManager.isLadeSEBeacon(rawHexData)) {
            val penon = Penon(macAddress = macAddress)
            penon.state.updateFromRawData(rawHexData)
            penonList.add(penon)
            notifyItemInserted(penonList.size - 1)
        }
    }

    fun clearAll() {
        val size = penonList.size
        penonList.clear()
        notifyItemRangeRemoved(0, size)
    }
}