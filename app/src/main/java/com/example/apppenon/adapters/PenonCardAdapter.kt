package com.example.apppenon.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apppenon.R
import com.example.apppenon.model.DetectedPenon
import com.example.apppenon.model.BLEScanManager

class PenonCardAdapter (
    private val onPenonClick: ((DetectedPenon) -> Unit)? = null,
    private val penonSettings: MutableList<com.example.apppenon.model.Penon> = mutableListOf()
) : RecyclerView.Adapter<PenonCardAdapter.PenonViewHolder>() {

    private val penonList = mutableListOf<DetectedPenon>()

    class PenonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPenonName: TextView = view.findViewById(R.id.tvPenonName)
        val tvRssi: TextView = view.findViewById(R.id.tvRssi)
        val tvMacAddress: TextView = view.findViewById(R.id.tvMacAddress)
        val tvBattery: TextView = view.findViewById(R.id.tvBattery)
        val tvFlowState: TextView = view.findViewById(R.id.tvFlowState)
        val tvSDFlowState: TextView = view.findViewById(R.id.tvSDFlowState)
        val tvLastUpdate: TextView = view.findViewById(R.id.tvLastUpdate)
        val tvAttachedStatus: TextView = view.findViewById(R.id.tvAttachedStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PenonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_penon_card, parent, false)
        return PenonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PenonViewHolder, position: Int) {
        val penon = penonList[position]

        // 1. Récupérer les réglages mis à jour
        // IMPORTANT : penonSettings DOIT être la liste mise à jour depuis MainActivity
        val settings = penonSettings.find { it.macAddress == penon.macAddress }

        val nameToDisplay = settings?.penonName ?: penon.name
        val threshold = settings?.flowStateThreshold ?: 500

        // 2. Mise à jour des textes
        holder.tvPenonName.text = nameToDisplay
        holder.tvMacAddress.text = "MAC: ${penon.macAddress}"

        // ... (votre code RSSI et Batterie est correct)

        // 3. Logique d'attachement (Calculée avec le nouveau seuil)
        val isAttached = penon.flowState >= threshold

        holder.tvAttachedStatus.apply {
            text = if (isAttached) "🔗 ATTACHÉ" else "❌ DÉTACHÉ"
            setTextColor(if (isAttached) 0xFF4CAF50.toInt() else 0xFFE91E63.toInt())
        }

        // 4. Click listener
        holder.itemView.setOnClickListener {
            // On passe l'objet de données détectées
            onPenonClick?.invoke(penon)
        }
    }

    override fun getItemCount() = penonList.size

    fun updatePenon(penon: DetectedPenon, bleScanManager: BLEScanManager) {
        val index = penonList.indexOfFirst { it.macAddress == penon.macAddress }
        if (index != -1) {
            penonList[index] = penon
            notifyItemChanged(index)
        } else if (penon.rawHexData.isNotEmpty() && bleScanManager.isLadeSEBeacon(penon.rawHexData)) {
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