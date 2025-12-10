package com.example.apppenon.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apppenon.R
import com.example.apppenon.model.DetectedPenon
import com.example.apppenon.model.PenonReader
import java.text.SimpleDateFormat
import java.util.*

class PenonCardAdapter () : RecyclerView.Adapter<PenonCardAdapter.PenonViewHolder>() {

    private val penonList = mutableListOf<DetectedPenon>()

    class PenonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPenonName: TextView = view.findViewById(R.id.tvPenonName)
        val tvRssi: TextView = view.findViewById(R.id.tvRssi)
        val tvMacAddress: TextView = view.findViewById(R.id.tvMacAddress)
        val tvFrameCount: TextView = view.findViewById(R.id.tvFrameCount)
        val tvBattery: TextView = view.findViewById(R.id.tvBattery)
        val tvFlowState: TextView = view.findViewById(R.id.tvFlowState)
        val tvLastUpdate: TextView = view.findViewById(R.id.tvLastUpdate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PenonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_penon_card, parent, false)
        return PenonViewHolder(view)
    }

    override fun onBindViewHolder(holder: PenonViewHolder, position: Int) {
        val penon = penonList[position]

        holder.tvPenonName.text = penon.name
        holder.tvMacAddress.text = "MAC: ${penon.macAddress}"

        // Afficher RSSI avec signal strength
        val signalIcon = when {
            penon.rssi > -50 -> "📶"
            penon.rssi > -70 -> "📶"
            penon.rssi > -85 -> "📡"
            else -> "📉"
        }
        holder.tvRssi.text = "$signalIcon ${penon.rssi} dBm"

        holder.tvFrameCount.text = "📊 Trames: ${penon.frameCount}"

        // Afficher batterie si disponible
        holder.tvBattery.text = if (penon.battery > 0) {
            val batteryIcon = when {
                penon.battery > 4.0 -> "🔋"
                penon.battery > 3.5 -> "🔋"
                penon.battery > 3.0 -> "🪫"
                else -> "⚠️"
            }
            "$batteryIcon Batterie: ${"%.2f".format(penon.battery)}V"
        } else {
            "🔋 Batterie: --"
        }

        // Afficher flow state si disponible
        holder.tvFlowState.text = if (penon.flowState > 0) {
            "🌊 Débit: ${penon.flowState}"
        } else {
            "🌊 Débit: --"
        }

        // Temps depuis dernière mise à jour
        val timeDiff = System.currentTimeMillis() - penon.lastUpdate
        val seconds = timeDiff / 1000
        holder.tvLastUpdate.text = when {
            seconds < 5 -> "🕒 À l'instant"
            seconds < 60 -> "🕒 Il y a ${seconds}s"
            else -> "🕒 Il y a ${seconds / 60}min"
        }
    }

    override fun getItemCount() = penonList.size

    fun updatePenon(penon: DetectedPenon, PR: PenonReader) {
        val index = penonList.indexOfFirst { it.macAddress == penon.macAddress }
        if (index != -1) {
            penonList[index] = penon
            notifyItemChanged(index)
        } else if (penon.rawHexData.isNotEmpty() && PR.isLadeSEBeacon(penon.rawHexData)) {
            // ✅ Utiliser rawHexData au lieu de macAddress
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