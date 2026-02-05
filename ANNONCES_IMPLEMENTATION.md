# Implémentation des Annonces Vocales et Sons Personnalisés

## ✅ Ce qui a été fait

### 1. Modèle de données (Penon.kt)
- ✅ Ajout de `useSound: Boolean` - choix entre vocal et son
- ✅ Ajout de `soundAttachePath: String` - chemin du son "attaché"
- ✅ Ajout de `soundDetachePath: String` - chemin du son "détaché"
- ✅ Labels personnalisés (`labelAttache`, `labelDetache`)

### 2. Gestionnaire de sons (SoundManager.kt)
- ✅ Créé `SoundManager` pour jouer les fichiers audio via MediaPlayer
- ✅ Gestion de l'URI des fichiers
- ✅ Libération automatique des ressources

### 3. VoiceNotificationManager.kt
- ✅ Support des deux modes: vocal (TTS) et son
- ✅ Méthode `announceStateChange()` mise à jour avec tous les paramètres
- ✅ Libération des ressources (TTS + SoundManager)

### 4. Adaptateur (PenonCardAdapter.kt)
- ✅ Passage des nouveaux paramètres lors de l'annonce

### 5. Interface utilisateur (activity_penon_settings.xml)
- ✅ Switch pour choisir entre vocal et son
- ✅ Section labels vocaux (visible par défaut)
- ✅ Section sons personnalisés (cachée par défaut)
- ✅ Boutons pour sélectionner les fichiers audio

## ⏳ Ce qu'il reste à faire dans PenonsSettingsActivity.kt

### 1. Déclarer les nouveaux composants UI

```kotlin
private lateinit var switchUseSound: SwitchCompat
private lateinit var layoutVoiceLabels: LinearLayout
private lateinit var layoutCustomSounds: LinearLayout
private lateinit var tvSoundAttacheStatus: TextView
private lateinit var tvSoundDetacheStatus: TextView
private lateinit var btnSelectSoundAttache: Button
private lateinit var btnSelectSoundDetache: Button

// Launchers pour sélectionner les fichiers
private lateinit var soundAttacheLauncher: ActivityResultLauncher<Intent>
private lateinit var soundDetacheLauncher: ActivityResultLauncher<Intent>
```

### 2. Initialiser les launchers dans onCreate()

```kotlin
// Avant setContentView()
soundAttacheLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == RESULT_OK) {
        result.data?.data?.let { uri ->
            // Prendre la permission persistante
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            penon.soundAttachePath = uri.toString()
            tvSoundAttacheStatus.text = getFileName(uri)
            hasUnsavedChanges = true
        }
    }
}

soundDetacheLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == RESULT_OK) {
        result.data?.data?.let { uri ->
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            penon.soundDetachePath = uri.toString()
            tvSoundDetacheStatus.text = getFileName(uri)
            hasUnsavedChanges = true
        }
    }
}
```

### 3. Initialiser les vues dans initializeViews()

```kotlin
switchUseSound = findViewById(R.id.switch_use_sound)
layoutVoiceLabels = findViewById(R.id.layout_voice_labels)
layoutCustomSounds = findViewById(R.id.layout_custom_sounds)
tvSoundAttacheStatus = findViewById(R.id.tv_sound_attache_status)
tvSoundDetacheStatus = findViewById(R.id.tv_sound_detache_status)
btnSelectSoundAttache = findViewById(R.id.btn_select_sound_attache)
btnSelectSoundDetache = findViewById(R.id.btn_select_sound_detache)
```

### 4. Peupler les données dans populateUI()

```kotlin
switchUseSound.isChecked = penon.useSound

// Afficher les noms de fichiers si des sons sont configurés
if (penon.soundAttachePath.isNotEmpty()) {
    tvSoundAttacheStatus.text = getFileName(Uri.parse(penon.soundAttachePath))
}
if (penon.soundDetachePath.isNotEmpty()) {
    tvSoundDetacheStatus.text = getFileName(Uri.parse(penon.soundDetachePath))
}

// Afficher/masquer les sections appropriées
updateSoundUIVisibility(penon.useSound)
```

### 5. Ajouter les listeners dans setupListeners()

```kotlin
// Basculer entre vocal et son
switchUseSound.setOnCheckedChangeListener { _, isChecked ->
    updateSoundUIVisibility(isChecked)
    hasUnsavedChanges = true
}

// Boutons de sélection de fichiers
btnSelectSoundAttache.setOnClickListener {
    openAudioFilePicker(soundAttacheLauncher)
}

btnSelectSoundDetache.setOnClickListener {
    openAudioFilePicker(soundDetacheLauncher)
}

// Ajouter le switch dans la liste des listeners
listOf(
    // ... autres switches ...
    switchUseSound
).forEach { it.setOnCheckedChangeListener(switchListener) }
```

### 6. Ajouter les méthodes utilitaires

```kotlin
/**
 * Affiche/masque les sections selon le mode choisi
 */
private fun updateSoundUIVisibility(useSound: Boolean) {
    if (useSound) {
        layoutVoiceLabels.visibility = View.GONE
        layoutCustomSounds.visibility = View.VISIBLE
    } else {
        layoutVoiceLabels.visibility = View.VISIBLE
        layoutCustomSounds.visibility = View.GONE
    }
}

/**
 * Ouvre le sélecteur de fichiers audio
 */
private fun openAudioFilePicker(launcher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "audio/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    }
    launcher.launch(intent)
}

/**
 * Récupère le nom du fichier depuis l'URI
 */
private fun getFileName(uri: Uri): String {
    var fileName = "Fichier sélectionné"
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }
    return fileName
}
```

### 7. Sauvegarder les données dans saveSettings()

```kotlin
// Dans le bloc penon.apply { ... }
useSound = switchUseSound.isChecked
soundAttachePath = this.soundAttachePath // Déjà mis à jour par les launchers
soundDetachePath = this.soundDetachePath
```

### 8. Ajouter dans PenonSettingsRepository.kt

#### Dans loadPenon():
```kotlin
penon.useSound = sharedPref.getBoolean(
    "${penon.macAddress}_useSound",
    penon.useSound
)
penon.soundAttachePath = sharedPref.getString(
    "${penon.macAddress}_soundAttachePath",
    penon.soundAttachePath
) ?: penon.soundAttachePath
penon.soundDetachePath = sharedPref.getString(
    "${penon.macAddress}_soundDetachePath",
    penon.soundDetachePath
) ?: penon.soundDetachePath
```

#### Dans savePenon():
```kotlin
putBoolean("${penon.macAddress}_useSound", penon.useSound)
putString("${penon.macAddress}_soundAttachePath", penon.soundAttachePath)
putString("${penon.macAddress}_soundDetachePath", penon.soundDetachePath)
```

## 🎯 Utilisation

1. **Mode Vocal (par défaut)** :
   - Configurez les labels "attaché" et "détaché"
   - L'application lira "Penon X est [votre label]"

2. **Mode Sons** :
   - Activez le switch "Utiliser des sons personnalisés"
   - Cliquez sur "Choisir" pour chaque état
   - Sélectionnez vos fichiers audio (Mario, Sonic, etc.)
   - L'application jouera le son correspondant à chaque changement d'état

## 📝 Notes importantes

- Les fichiers audio doivent être accessibles (permissions accordées)
- Formats supportés : MP3, WAV, OGG, etc.
- Les URIs sont persistantes même après redémarrage
- Les sons courts (< 2 secondes) sont recommandés
