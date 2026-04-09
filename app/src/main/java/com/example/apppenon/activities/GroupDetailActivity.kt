package com.example.apppenon.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apppenon.R
import com.example.apppenon.adapters.GroupPenonAdapter
import com.example.apppenon.data.GroupRepository
import com.example.apppenon.data.PenonSettingsRepository
import com.example.apppenon.model.PenonGroup

/**
 * Affiche le détail d'un groupe : état global + liste des pénons avec leur état individuel.
 * Cliquer sur un pénon ouvre ses paramètres.
 * Le bouton ⚙️ ouvre GroupSettingsActivity pour modifier le groupe.
 */
class GroupDetailActivity : AppCompatActivity() {

    private lateinit var tvGroupName: TextView
    private lateinit var tvGroupState: TextView
    private lateinit var rvGroupPenons: RecyclerView
    private lateinit var btnBack: Button
    private lateinit var btnEdit: Button

    private lateinit var groupRepository: GroupRepository
    private lateinit var penonRepository: PenonSettingsRepository
    private var groupId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_detail)

        groupId = intent.getStringExtra("group_id") ?: run { finish(); return }
        groupRepository = GroupRepository(this)
        penonRepository = PenonSettingsRepository(this)

        tvGroupName = findViewById(R.id.tvGroupDetailName)
        tvGroupState = findViewById(R.id.tvGroupDetailState)
        rvGroupPenons = findViewById(R.id.rvGroupPenons)
        btnBack = findViewById(R.id.btnBack)
        btnEdit = findViewById(R.id.btnEditGroup)

        rvGroupPenons.layoutManager = LinearLayoutManager(this)

        btnBack.setOnClickListener { finish() }
        btnEdit.setOnClickListener {
            val intent = Intent(this, GroupSettingsActivity::class.java)
            intent.putExtra("group_id", groupId)
            startActivity(intent)
        }

        refreshUI()
    }

    override fun onResume() {
        super.onResume()
        // Recharger les paramètres à jour depuis SharedPreferences avant d'afficher
        MainActivity.getInstance()?.deviceList?.forEach { penon ->
            penonRepository.loadPenon(penon)
        }
        refreshUI()
    }

    @SuppressLint("SetTextI18n")
    private fun refreshUI() {
        val mainActivity = MainActivity.getInstance() ?: return
        val group = groupRepository.loadGroup(groupId) ?: run { finish(); return }

        val penonsInGroup = mainActivity.deviceList.filter { it.groupId == groupId }
        val state = PenonGroup.computeState(penonsInGroup)

        tvGroupName.text = group.groupName
        tvGroupState.text = PenonGroup.stateLabel(state)
        tvGroupState.setTextColor(PenonGroup.stateColor(state))

        val adapter = GroupPenonAdapter(penonsInGroup) { penon ->
            val intent = Intent(this, PenonsSettingsActivity::class.java)
            intent.putExtra("penon_mac_address", penon.macAddress)
            startActivity(intent)
        }
        rvGroupPenons.adapter = adapter
    }
}
