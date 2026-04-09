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

class PenonCardAdapter(
    private val onPenonClick: ((Penon) -> Unit)? = null,
    // ✅ Une seule liste — elle sert à la fois de source d'affichage ET de settings
    private val penonSettings: MutableList<Penon> = mutableListOf(),
    private val voiceNotificationManager: VoiceNotificationManager? = null
) : RecyclerView.Adapter<PenonCardAdapter.PenonViewHolder>() {

    // ✅ Suppression de penonList : on affiche directement penonSettings
    class PenonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPenonName: TextView = view.findViewById(R.id.tvPenonName)
        val tvMacAddress: TextView = view.findViewById(R.id.tvMacAddress)
        val tvAttachedStatus: TextView = view.findViewById(R.id.tvAttachedStatus)
        val tvBattery: TextView = view.findViewById(R.id.tvBattery)
        val tvData: TextView = view.findViewById(R.id.tvData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PenonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_penon_card, parent, false)
        return PenonViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PenonViewHolder, position: Int) {
        val penon = penonSettings[position]
        val threshold = penon.editAttachedThreshold

        holder.tvPenonName.text = penon.penonName
        holder.tvMacAddress.text = "MAC: ${penon.macAddress}"

        var print = ""
        if (penon.count == true) print += "Frame: ${penon.state.frame_cnt}\n"
        if (penon.vbat == true) print += "Vbat: ${penon.state.vbat} V\n"
        if (penon.avrMagZ == true) print += "MagZ: ${penon.state.avr_mag_z.toInt()} mT×10⁻³\n"
        if (penon.avrAvrMagZ == true) print += "AvrMagZ: ${penon.state.avr_avr_mag_z.toInt()} mT×10⁻³\n"
        if (penon.meanAcc == true) print += "AvrAcc: ${penon.state.avr_acc.toInt()} m.s⁻²×10⁻³\n"
        if (penon.maxAcc == true) print += "MaxAcc: ${penon.state.max_acc.toInt()} m.s⁻²×10⁻³\n"
        holder.tvData.text = print

        holder.tvBattery.text = "\uD83D\uDD0B ${penon.state.vbat} V"

        val isAttached = abs(penon.state.avr_avr_mag_z) >= threshold

        holder.tvAttachedStatus.apply {
            text = if (isAttached) "🔗 ATTACHÉ" else "❌ DÉTACHÉ"
            setTextColor(if (isAttached) 0xFF4CAF50.toInt() else 0xFFE91E63.toInt())
        }

        holder.itemView.setOnClickListener {
            onPenonClick?.invoke(penon)
        }
    }

    override fun getItemCount() = penonSettings.size

    fun updatePenon(macAddress: String, rawHexData: ByteArray, bleScanManager: BLEScanManager) {
        val index = penonSettings.indexOfFirst { it.macAddress == macAddress }
        if (index != -1) {
            // ✅ Le Penon existe déjà (chargé depuis MainActivity) : on met à jour son state
            penonSettings[index].state.updateFromRawData(rawHexData)
            notifyItemChanged(index)
        } else if (rawHexData.isNotEmpty() && bleScanManager.isLadeSEBeacon(rawHexData)) {
            // ✅ Nouveau Penon découvert via BLE : on l'ajoute
            val penon = Penon(macAddress = macAddress)
            penon.state.updateFromRawData(rawHexData)
            penonSettings.add(penon)
            notifyItemInserted(penonSettings.size - 1)
        }
    }

    fun clearAll() {
        val size = penonSettings.size
        penonSettings.clear()
        notifyItemRangeRemoved(0, size)
    }
}