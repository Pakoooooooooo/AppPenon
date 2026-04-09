package com.example.apppenon.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.apppenon.R
import com.example.apppenon.data.GroupRepository
import com.example.apppenon.data.PenonSettingsRepository
import com.example.apppenon.model.Penon
import com.example.apppenon.model.PenonGroup
import com.example.apppenon.model.Side

/**
 * Création et modification d'un groupe.
 * - Modifier le nom du groupe
 * - Voir les pénons dans le groupe, changer leur côté (Bâbord/Tribord), les retirer
 * - Ajouter des pénons non affectés avec choix du côté
 * - Supprimer le groupe (les pénons deviennent non affectés)
 *
 * Passer "group_id" via l'intent pour modifier un groupe existant.
 * Sans "group_id" → création d'un nouveau groupe.
 */
class GroupSettingsActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var editGroupName: EditText
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnBack: Button
    private lateinit var layoutInGroupPenons: LinearLayout
    private lateinit var layoutAvailablePenons: LinearLayout
    private lateinit var tvAvailableTitle: TextView

    // Annonces
    private lateinit var switchGroupUseSound: SwitchCompat
    private lateinit var switchAnnounceAttached: SwitchCompat
    private lateinit var layoutGroupVoiceLabels: LinearLayout
    private lateinit var layoutGroupCustomSounds: LinearLayout
    private lateinit var editGroupLabelAttached: EditText
    private lateinit var editGroupLabelDetached: EditText
    private lateinit var editGroupLabelDetachedBabord: EditText
    private lateinit var editGroupLabelDetachedTribord: EditText
    private lateinit var tvSoundAttachedStatus: TextView
    private lateinit var tvSoundDetachedStatus: TextView
    private lateinit var tvSoundDetachedBabordStatus: TextView
    private lateinit var tvSoundDetachedTribordStatus: TextView
    private lateinit var btnSelectSoundAttached: Button
    private lateinit var btnSelectSoundDetached: Button
    private lateinit var btnSelectSoundDetachedBabord: Button
    private lateinit var btnSelectSoundDetachedTribord: Button

    private lateinit var soundAttachedLauncher: ActivityResultLauncher<Intent>
    private lateinit var soundDetachedLauncher: ActivityResultLauncher<Intent>
    private lateinit var soundDetachedBabordLauncher: ActivityResultLauncher<Intent>
    private lateinit var soundDetachedTribordLauncher: ActivityResultLauncher<Intent>

    private lateinit var groupRepository: GroupRepository
    private lateinit var penonRepository: PenonSettingsRepository

    private var groupId: String? = null
    private var isCreateMode: Boolean = false
    // Stocke les chemins sons en cours d'édition (mis à jour par les launchers)
    private var soundAttachedPath: String = ""
    private var soundDetachedPath: String = ""
    private var soundDetachedBabordPath: String = ""
    private var soundDetachedTribordPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enregistrer les launchers avant setContentView
        soundAttachedLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) result.data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                soundAttachedPath = uri.toString()
                tvSoundAttachedStatus.text = getFileName(uri)
            }
        }
        soundDetachedLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) result.data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                soundDetachedPath = uri.toString()
                tvSoundDetachedStatus.text = getFileName(uri)
            }
        }
        soundDetachedBabordLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) result.data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                soundDetachedBabordPath = uri.toString()
                tvSoundDetachedBabordStatus.text = getFileName(uri)
            }
        }
        soundDetachedTribordLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) result.data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                soundDetachedTribordPath = uri.toString()
                tvSoundDetachedTribordStatus.text = getFileName(uri)
            }
        }

        setContentView(R.layout.activity_group_settings)

        groupRepository = GroupRepository(this)
        penonRepository = PenonSettingsRepository(this)

        groupId = intent.getStringExtra("group_id")
        isCreateMode = groupId == null

        tvTitle = findViewById(R.id.tvGroupSettingsTitle)
        editGroupName = findViewById(R.id.editGroupName)
        btnSave = findViewById(R.id.btnSaveGroup)
        btnDelete = findViewById(R.id.btnDeleteGroup)
        btnBack = findViewById(R.id.btnGroupSettingsBack)
        layoutInGroupPenons = findViewById(R.id.layoutInGroupPenons)
        layoutAvailablePenons = findViewById(R.id.layoutAvailablePenons)
        tvAvailableTitle = findViewById(R.id.tvAvailableTitle)

        // Annonces
        switchGroupUseSound = findViewById(R.id.switchGroupUseSound)
        switchAnnounceAttached = findViewById(R.id.switchAnnounceAttached)
        layoutGroupVoiceLabels = findViewById(R.id.layoutGroupVoiceLabels)

        layoutGroupCustomSounds = findViewById(R.id.layoutGroupCustomSounds)
        editGroupLabelAttached = findViewById(R.id.editGroupLabelAttached)
        editGroupLabelDetached = findViewById(R.id.editGroupLabelDetached)
        editGroupLabelDetachedBabord = findViewById(R.id.editGroupLabelDetachedBabord)
        editGroupLabelDetachedTribord = findViewById(R.id.editGroupLabelDetachedTribord)
        tvSoundAttachedStatus = findViewById(R.id.tvSoundAttachedStatus)
        tvSoundDetachedStatus = findViewById(R.id.tvSoundDetachedStatus)
        tvSoundDetachedBabordStatus = findViewById(R.id.tvSoundDetachedBabordStatus)
        tvSoundDetachedTribordStatus = findViewById(R.id.tvSoundDetachedTribordStatus)
        btnSelectSoundAttached = findViewById(R.id.btnSelectSoundAttached)
        btnSelectSoundDetached = findViewById(R.id.btnSelectSoundDetached)
        btnSelectSoundDetachedBabord = findViewById(R.id.btnSelectSoundDetachedBabord)
        btnSelectSoundDetachedTribord = findViewById(R.id.btnSelectSoundDetachedTribord)

        if (isCreateMode) {
            tvTitle.text = "Nouveau groupe"
            btnDelete.visibility = View.GONE
            updateSoundUIVisibility(false)
        } else {
            tvTitle.text = "Paramètres du groupe"
            val group = groupRepository.loadGroup(groupId!!)
            editGroupName.setText(group?.groupName ?: "")
            populateAnnouncementUI(group)
        }

        switchGroupUseSound.setOnCheckedChangeListener { _, isChecked ->
            updateSoundUIVisibility(isChecked)
        }
        btnSelectSoundAttached.setOnClickListener { openAudioFilePicker(soundAttachedLauncher) }
        btnSelectSoundDetached.setOnClickListener { openAudioFilePicker(soundDetachedLauncher) }
        btnSelectSoundDetachedBabord.setOnClickListener { openAudioFilePicker(soundDetachedBabordLauncher) }
        btnSelectSoundDetachedTribord.setOnClickListener { openAudioFilePicker(soundDetachedTribordLauncher) }

        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveGroup() }
        btnDelete.setOnClickListener { confirmDeleteGroup() }

        refreshPenonLists()
    }

    private fun populateAnnouncementUI(group: PenonGroup?) {
        if (group == null) return
        soundAttachedPath = group.soundAttachedPath
        soundDetachedPath = group.soundDetachedPath
        soundDetachedBabordPath = group.soundDetachedBabordPath
        soundDetachedTribordPath = group.soundDetachedTribordPath

        switchGroupUseSound.isChecked = group.useSound
        switchAnnounceAttached.isChecked = group.announceAttached
        editGroupLabelAttached.setText(group.labelAttached)
        editGroupLabelDetached.setText(group.labelDetached)
        editGroupLabelDetachedBabord.setText(group.labelDetachedBabord)
        editGroupLabelDetachedTribord.setText(group.labelDetachedTribord)

        if (soundAttachedPath.isNotEmpty()) tvSoundAttachedStatus.text = getFileName(Uri.parse(soundAttachedPath))
        if (soundDetachedPath.isNotEmpty()) tvSoundDetachedStatus.text = getFileName(Uri.parse(soundDetachedPath))
        if (soundDetachedBabordPath.isNotEmpty()) tvSoundDetachedBabordStatus.text = getFileName(Uri.parse(soundDetachedBabordPath))
        if (soundDetachedTribordPath.isNotEmpty()) tvSoundDetachedTribordStatus.text = getFileName(Uri.parse(soundDetachedTribordPath))

        updateSoundUIVisibility(group.useSound)
    }

    private fun updateSoundUIVisibility(useSound: Boolean) {
        layoutGroupVoiceLabels.visibility = if (useSound) View.GONE else View.VISIBLE
        layoutGroupCustomSounds.visibility = if (useSound) View.VISIBLE else View.GONE
    }

    private fun openAudioFilePicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        launcher.launch(intent)
    }

    private fun getFileName(uri: Uri): String {
        var name = "Fichier sélectionné"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) name = cursor.getString(idx)
                }
            }
        } catch (e: Exception) {
            Log.e("GroupSettings", "getFileName: ${e.message}")
        }
        return name
    }

    override fun onResume() {
        super.onResume()
        refreshPenonLists()
    }

    @SuppressLint("SetTextI18n")
    private fun refreshPenonLists() {
        val mainActivity = MainActivity.getInstance() ?: return
        val allPenons = mainActivity.deviceList

        layoutInGroupPenons.removeAllViews()
        layoutAvailablePenons.removeAllViews()

        if (isCreateMode) {
            // En mode création, pas encore de pénons dans le groupe
            val hint = TextView(this)
            hint.text = "Sauvegardez d'abord le groupe pour ajouter des pénons."
            hint.setTextColor(0xFF888888.toInt())
            hint.textSize = 13f
            layoutInGroupPenons.addView(hint)

            // Afficher les pénons disponibles pour info
            tvAvailableTitle.visibility = View.GONE
            layoutAvailablePenons.visibility = View.GONE
            return
        }

        val currentGroupId = groupId ?: return

        // Pénons dans ce groupe
        val inGroupPenons = allPenons.filter { it.groupId == currentGroupId }
        if (inGroupPenons.isEmpty()) {
            val hint = TextView(this)
            hint.text = "Aucun pénon dans ce groupe."
            hint.setTextColor(0xFF888888.toInt())
            hint.textSize = 13f
            layoutInGroupPenons.addView(hint)
        } else {
            inGroupPenons.forEach { penon -> addInGroupPenonRow(penon, allPenons) }
        }

        // Pénons disponibles (non affectés à ce groupe)
        val availablePenons = allPenons.filter { it.groupId.isEmpty() }
        if (availablePenons.isEmpty()) {
            tvAvailableTitle.visibility = View.GONE
            layoutAvailablePenons.visibility = View.GONE
        } else {
            tvAvailableTitle.visibility = View.VISIBLE
            layoutAvailablePenons.visibility = View.VISIBLE
            availablePenons.forEach { penon -> addAvailablePenonRow(penon, allPenons) }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addInGroupPenonRow(penon: Penon, allPenons: MutableList<Penon>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }

        // Nom
        val tvName = TextView(this).apply {
            text = penon.penonName
            textSize = 14f
            setTextColor(0xFF03224C.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Spinner côté
        val spinner = Spinner(this)
        val sides = arrayOf("Bâbord", "Tribord")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sides)
        spinner.setSelection(if (penon.side == Side.TRIBORD) 1 else 0)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val newSide = if (pos == 1) Side.TRIBORD else Side.BABORD
                if (penon.side != newSide) {
                    penon.side = newSide
                    val index = allPenons.indexOfFirst { it.macAddress == penon.macAddress }
                    if (index != -1) allPenons[index].side = newSide
                    penonRepository.savePenon(penon)
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Bouton retirer
        val btnRemove = Button(this).apply {
            text = "✕"
            textSize = 12f
            setBackgroundColor(0xFFC62828.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginStart = 8 }
            setOnClickListener {
                penon.groupId = ""
                penon.side = Side.NONE
                val index = allPenons.indexOfFirst { it.macAddress == penon.macAddress }
                if (index != -1) {
                    allPenons[index].groupId = ""
                    allPenons[index].side = Side.NONE
                }
                penonRepository.savePenon(penon)
                refreshPenonLists()
            }
        }

        row.addView(tvName)
        row.addView(spinner)
        row.addView(btnRemove)
        layoutInGroupPenons.addView(row)
    }

    @SuppressLint("SetTextI18n")
    private fun addAvailablePenonRow(penon: Penon, allPenons: MutableList<Penon>) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }

        val tvName = TextView(this).apply {
            text = penon.penonName
            textSize = 14f
            setTextColor(0xFF03224C.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val spinner = Spinner(this)
        val sides = arrayOf("Bâbord", "Tribord")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sides)

        val btnAdd = Button(this).apply {
            text = "Ajouter"
            textSize = 11f
            setBackgroundColor(0xFF03224C.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginStart = 8 }
            setOnClickListener {
                val side = if (spinner.selectedItemPosition == 1) Side.TRIBORD else Side.BABORD
                penon.groupId = groupId ?: return@setOnClickListener
                penon.side = side
                val index = allPenons.indexOfFirst { it.macAddress == penon.macAddress }
                if (index != -1) {
                    allPenons[index].groupId = penon.groupId
                    allPenons[index].side = side
                }
                penonRepository.savePenon(penon)
                refreshPenonLists()
            }
        }

        row.addView(tvName)
        row.addView(spinner)
        row.addView(btnAdd)
        layoutAvailablePenons.addView(row)
    }

    private fun saveGroup() {
        val name = editGroupName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Veuillez entrer un nom de groupe", Toast.LENGTH_SHORT).show()
            return
        }

        if (isCreateMode) {
            val newGroup = groupRepository.createGroup(name)
            groupId = newGroup.groupId
            isCreateMode = false
            tvTitle.text = "Paramètres du groupe"
            btnDelete.visibility = View.VISIBLE
            Toast.makeText(this, "Groupe \"$name\" créé", Toast.LENGTH_SHORT).show()
            // Maintenant que le groupe existe, rafraîchir l'UI pour montrer l'ajout de pénons
            refreshPenonLists()
        } else {
            val group = PenonGroup(
                groupId = groupId!!,
                groupName = name,
                useSound = switchGroupUseSound.isChecked,
                announceAttached = switchAnnounceAttached.isChecked,
                labelAttached = editGroupLabelAttached.text.toString().takeIf { it.isNotBlank() } ?: "attachée",
                labelDetached = editGroupLabelDetached.text.toString().takeIf { it.isNotBlank() } ?: "détachée",
                labelDetachedBabord = editGroupLabelDetachedBabord.text.toString().takeIf { it.isNotBlank() } ?: "détachée bâbord",
                labelDetachedTribord = editGroupLabelDetachedTribord.text.toString().takeIf { it.isNotBlank() } ?: "détachée tribord",
                soundAttachedPath = soundAttachedPath,
                soundDetachedPath = soundDetachedPath,
                soundDetachedBabordPath = soundDetachedBabordPath,
                soundDetachedTribordPath = soundDetachedTribordPath
            )
            groupRepository.saveGroup(group)
            Toast.makeText(this, "Groupe sauvegardé", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun confirmDeleteGroup() {
        AlertDialog.Builder(this)
            .setTitle("Supprimer le groupe")
            .setMessage("Les pénons du groupe seront désaffectés. Continuer ?")
            .setPositiveButton("Supprimer") { _, _ -> deleteGroup() }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteGroup() {
        val currentGroupId = groupId ?: return
        val mainActivity = MainActivity.getInstance()

        // Désaffecter tous les pénons du groupe
        mainActivity?.deviceList?.forEach { penon ->
            if (penon.groupId == currentGroupId) {
                penon.groupId = ""
                penon.side = Side.NONE
                penonRepository.savePenon(penon)
            }
        }

        groupRepository.deleteGroup(currentGroupId)
        Toast.makeText(this, "Groupe supprimé", Toast.LENGTH_SHORT).show()
        // Finir les deux activités (detail + settings)
        finishAffinity()
    }
}
