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

        // Nom du Penon
        holder.tvPenonName.text = penon.name
        
        // MAC Address
        holder.tvMacAddress.text = "MAC: ${penon.macAddress}"

        // RSSI avec icône de signal (en haut à droite)
        val signalIcon = when {
            penon.rssi > -50 -> "📶"
            penon.rssi > -70 -> "📶"
            penon.rssi > -85 -> "📡"
            else -> "📉"
        }
        holder.tvRssi.text = "$signalIcon ${penon.rssi} dBm"

        // Batterie (en haut à droite)
        holder.tvBattery.text = if (penon.battery > 0) {
            val batteryIcon = when {
                penon.battery > 4.0 -> "🔋"
                penon.battery > 3.5 -> "🔋"
                penon.battery > 3.0 -> "🪫"
                else -> "⚠️"
            }
            "$batteryIcon ${"%.2f".format(penon.battery)}V"
        } else {
            "🔋 --"
        }

        // Calculer l'état Attaché/Détaché basé sur le seuil de flow state
        val flowStateThreshold = penonSettings.find { it.macAdress == penon.macAddress }?.flowStateThreshold ?: 500
        val isAttached = penon.flowState >= flowStateThreshold
        
        // STATUT ATTACHÉ/DÉTACHÉ - DONNÉE PRINCIPALE
        holder.tvAttachedStatus.text = if (isAttached) {
            "🔗 ATTACHÉ"
        } else {
            "❌ DÉTACHÉ"
        }
        
        holder.tvAttachedStatus.setTextColor(
            if (isAttached) 0xFF4CAF50.toInt() else 0xFFE91E63.toInt()
        )

        // Masquer Flow State et SD Flow State (pas nécessaires à afficher)
        holder.tvFlowState.visibility = View.GONE
        holder.tvSDFlowState.visibility = View.GONE
        
        // Masquer le temps de mise à jour
        holder.tvLastUpdate.visibility = View.GONE

        // Click listener sur toute la carte
        holder.itemView.setOnClickListener {
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