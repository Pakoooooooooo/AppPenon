package com.example.apppenon.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apppenon.R
import com.example.apppenon.model.Penon
import com.example.apppenon.model.PenonGroup
import com.example.apppenon.utils.VoiceNotificationManager
import kotlin.math.abs

/**
 * Adapter principal de la MainActivity.
 * Gère 3 types de vues :
 *  - TYPE_UNCONFIG_HEADER : titre "Nouveaux pénons à configurer"
 *  - TYPE_UNCONFIG_PENON  : carte d'un pénon non affecté à un groupe
 *  - TYPE_GROUP           : carte d'un groupe avec son état
 */
class MainListAdapter(
    private val allPenons: MutableList<Penon>,
    private val groups: MutableList<PenonGroup>,
    private val voiceNotificationManager: VoiceNotificationManager?,
    private val onUnconfigPenonClick: (Penon) -> Unit,
    private val onGroupClick: (PenonGroup) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_UNCONFIG_HEADER = 0
        const val TYPE_UNCONFIG_PENON = 1
        const val TYPE_GROUP = 2
    }

    // ---- Construction de la liste d'items ----

    private fun unconfiguredPenons(): List<Penon> =
        allPenons.filter { it.groupId.isEmpty() }

    /**
     * Construit la liste plate d'items à afficher :
     * [header?, pénons non configurés..., groupes...]
     */
    private fun buildItems(): List<Any> {
        val items = mutableListOf<Any>()
        val unconfig = unconfiguredPenons()
        if (unconfig.isNotEmpty()) {
            items.add("UNCONFIG_HEADER")
            items.addAll(unconfig)
        }
        items.addAll(groups)
        return items
    }

    private var items: List<Any> = buildItems()

    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        items = buildItems()
        notifyDataSetChanged()
    }

    // ---- ViewHolders ----

    class UnconfigHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class UnconfigPenonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPenonName)
        val tvMac: TextView = view.findViewById(R.id.tvMacAddress)
        val tvStatus: TextView = view.findViewById(R.id.tvAttachedStatus)
        val tvBattery: TextView = view.findViewById(R.id.tvBattery)
        val tvData: TextView = view.findViewById(R.id.tvData)
    }

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvGroupName: TextView = view.findViewById(R.id.tvGroupName)
        val tvGroupState: TextView = view.findViewById(R.id.tvGroupState)
        val tvPenonCount: TextView = view.findViewById(R.id.tvPenonCount)
    }

    // ---- Adapter methods ----

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is String -> TYPE_UNCONFIG_HEADER
        is Penon -> TYPE_UNCONFIG_PENON
        is PenonGroup -> TYPE_GROUP
        else -> TYPE_GROUP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_UNCONFIG_HEADER -> UnconfigHeaderViewHolder(
                inflater.inflate(R.layout.item_unconfig_header, parent, false)
            )
            TYPE_UNCONFIG_PENON -> UnconfigPenonViewHolder(
                inflater.inflate(R.layout.item_penon_card, parent, false)
            )
            else -> GroupViewHolder(
                inflater.inflate(R.layout.item_group_card, parent, false)
            )
        }
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Penon -> bindUnconfigPenon(holder as UnconfigPenonViewHolder, item)
            is PenonGroup -> bindGroup(holder as GroupViewHolder, item)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindUnconfigPenon(holder: UnconfigPenonViewHolder, penon: Penon) {
        holder.tvName.text = penon.penonName
        holder.tvMac.text = "MAC: ${penon.macAddress}"
        holder.tvBattery.text = "\uD83D\uDD0B ${penon.state.vbat} V"
        holder.tvData.text = "⚙️ Cliquer pour configurer et affecter à un groupe"

        val isAttached = abs(penon.state.avr_avr_mag_z) >= penon.editAttachedThreshold
        holder.tvStatus.apply {
            text = if (isAttached) "🔗 ATTACHÉ" else "❌ DÉTACHÉ"
            setTextColor(if (isAttached) 0xFF4CAF50.toInt() else 0xFFE91E63.toInt())
        }

        holder.itemView.setOnClickListener { onUnconfigPenonClick(penon) }
    }

    @SuppressLint("SetTextI18n")
    private fun bindGroup(holder: GroupViewHolder, group: PenonGroup) {
        val penonsInGroup = allPenons.filter { it.groupId == group.groupId }
        val state = PenonGroup.computeState(penonsInGroup)

        // Détection de changement d'état pour annonce vocale
        if (group.lastAnnouncedState != null && group.lastAnnouncedState != state) {
            voiceNotificationManager?.bufferGroupStateChange(group.groupId, group.groupName, state)
        }
        group.lastAnnouncedState = state

        holder.tvGroupName.text = group.groupName
        holder.tvGroupState.apply {
            text = PenonGroup.stateLabel(state)
            setTextColor(PenonGroup.stateColor(state))
        }
        holder.tvPenonCount.text = "${penonsInGroup.size} pénon(s)"

        holder.itemView.setOnClickListener { onGroupClick(group) }
    }

    // ---- Mise à jour des données BLE ----

    /**
     * Met à jour les données brutes d'un pénon (appelé depuis BLEScanManager).
     */
    fun updatePenonData(macAddress: String, rawData: ByteArray) {
        val penon = allPenons.find { it.macAddress == macAddress } ?: return
        penon.state.updateFromRawData(rawData)
        refresh()
    }
}
