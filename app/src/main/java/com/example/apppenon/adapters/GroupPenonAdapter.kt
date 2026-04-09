package com.example.apppenon.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apppenon.R
import com.example.apppenon.model.Penon
import com.example.apppenon.model.Side
import kotlin.math.abs

/**
 * Affiche la liste des pénons d'un groupe dans GroupDetailActivity.
 * Chaque item montre : côté (badge), nom, MAC, état attaché/détaché, batterie.
 */
class GroupPenonAdapter(
    private val penons: List<Penon>,
    private val onPenonClick: (Penon) -> Unit
) : RecyclerView.Adapter<GroupPenonAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSideBadge: TextView = view.findViewById(R.id.tvSideBadge)
        val tvName: TextView = view.findViewById(R.id.tvPenonItemName)
        val tvMac: TextView = view.findViewById(R.id.tvPenonItemMac)
        val tvStatus: TextView = view.findViewById(R.id.tvPenonItemStatus)
        val tvBattery: TextView = view.findViewById(R.id.tvPenonItemBattery)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_penon_in_group, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val penon = penons[position]
        val isAttached = abs(penon.state.avr_avr_mag_z) >= penon.editAttachedThreshold

        // Badge côté
        when (penon.side) {
            Side.BABORD -> {
                holder.tvSideBadge.text = "B"
                holder.tvSideBadge.setBackgroundColor(0xFF1565C0.toInt()) // Bleu bâbord
            }
            Side.TRIBORD -> {
                holder.tvSideBadge.text = "T"
                holder.tvSideBadge.setBackgroundColor(0xFFC62828.toInt()) // Rouge tribord
            }
            Side.NONE -> {
                holder.tvSideBadge.text = "?"
                holder.tvSideBadge.setBackgroundColor(0xFF757575.toInt())
            }
        }

        holder.tvName.text = penon.penonName
        holder.tvMac.text = "MAC: ${penon.macAddress.takeLast(8)}"
        holder.tvBattery.text = "\uD83D\uDD0B ${penon.state.vbat} V"
        holder.tvStatus.apply {
            text = if (isAttached) "🔗 ATTACHÉ" else "❌ DÉTACHÉ"
            setTextColor(if (isAttached) 0xFF4CAF50.toInt() else 0xFFE91E63.toInt())
        }

        holder.itemView.setOnClickListener { onPenonClick(penon) }
    }

    override fun getItemCount(): Int = penons.size
}
