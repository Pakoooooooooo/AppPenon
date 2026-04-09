# Manuel d'utilisation — AppPenon

---

## Vue d'ensemble

AppPenon surveille des capteurs BLE de pénons de voile (BladeSENSE eTT SAIL).  
Chaque pénon mesure en continu son champ magnétique Z pour détecter s'il est **attaché** (dans la bonne position) ou **détaché** (retombé ou absent).  
Les pénons sont regroupés par voile (bâbord / tribord) et l'app annonce vocalement l'état de la voile.

---

## 1. Premier démarrage

### 1.1 Permissions requises
Au premier lancement, accepter toutes les permissions demandées :
- **Bluetooth** (Scan + Connexion)
- **Localisation** (requis par Android pour le scan BLE)
- **Stockage** (optionnel — pour l'enregistrement CSV)

### 1.2 Écran principal
L'écran affiche deux zones :
| Zone | Contenu |
|------|---------|
| **Pénons non configurés** | Pénons détectés mais pas encore affectés à un groupe |
| **Groupes** | Chaque voile avec son état global (Attachée / Détachée bâbord / Détachée tribord) |

---

## 2. Démarrer / Arrêter le scan

| Bouton | Action |
|--------|--------|
| **▶ Démarrer** | Lance le scan BLE — détecte les pénons à portée |
| **⏹ Arrêter** | Arrête le scan (et ferme le fichier CSV si enregistrement actif) |
| **🗑 Effacer** | Vide la liste des pénons en mémoire (ne supprime pas leurs réglages) |

> **Note :** Le scan se met en pause automatiquement quand l'app passe en arrière-plan et reprend à l'avant-plan.

---

## 3. Configurer un pénon

1. Lancer le scan et attendre que les pénons apparaissent dans **"Pénons non configurés"**
2. Appuyer sur un pénon → s'ouvre **Paramètres Pénon**

### Paramètres disponibles

| Champ | Description |
|-------|-------------|
| **Nom** | Nom libre (ex : "Pénon Grand-Voile Bâbord 1") |
| **Groupe** | Voile à laquelle appartient ce pénon (sélecteur déroulant) |
| **Côté** | Bâbord ou Tribord au sein du groupe |
| **Seuil attaché** | Valeur de calibration — en dessous = détaché |
| **Timeline** | Fenêtre de temps pour la moyenne glissante (0 = 10 trames par défaut) |

### Données affichées (interrupteurs)
Activer les colonnes à afficher dans la carte pénon :
- **AvrMagZ** / **AvrAvrMagZ** — champ magnétique moyen / moyenne glissante
- **FlowState** / **SDFlowState** — état flux / écart-type
- **MeanAcc / SDAcc / MaxAcc** — accélération
- **Vbat** — tension batterie

### Boutons
| Bouton | Action |
|--------|--------|
| **Calibrer** | Lance la calibration interactive (voir §4) |
| **Sauvegarder** | Enregistre tous les paramètres |
| **Supprimer** | Efface définitivement ce pénon et tous ses réglages |
| **Annuler** | Retour sans sauvegarder |

> Si des modifications non sauvegardées existent, une boîte de dialogue de confirmation apparaît.

---

## 4. Calibrer un pénon

La calibration mesure le seuil entre "attaché" et "détaché" pour un pénon spécifique.

### Procédure
1. Depuis **Paramètres Pénon** → **Calibrer**
2. Mettre le pénon en **position attachée** → appuyer sur **Démarrer**
   - L'app collecte 10 trames et calcule la moyenne magnétique
3. Passer le pénon en **position détachée** et attendre
   - L'app collecte 10 nouvelles trames
4. Le seuil calculé = `(moyenne_attaché + moyenne_détaché) / 2` est automatiquement rempli
5. Revenir dans Paramètres Pénon → **Sauvegarder**

> **Timeout :** Si moins de 10 trames sont reçues en 30 secondes, la calibration s'annule.

---

## 5. Créer et gérer un groupe (voile)

### Créer un groupe
1. Écran principal → **+ Ajouter un groupe**
2. Saisir le nom (ex : "Grand-Voile", "Génois")
3. Configurer les annonces vocales (voir §5.1)
4. **Sauvegarder**

### Modifier un groupe
- Depuis la liste des groupes → appuyer sur le nom du groupe → **⚙️ (bouton éditer)**

### Supprimer un groupe
- Dans les paramètres du groupe → **Supprimer**
- Tous les pénons membres redeviennent "non configurés"

### 5.1 Annonces vocales du groupe

| Paramètre | Description |
|-----------|-------------|
| **Utiliser le son** | Active/désactive les annonces pour ce groupe |
| **Libellé attachée** | Texte lu quand la voile est attachée (défaut : "attachée") |
| **Libellé détachée** | Texte lu quand la voile est détachée (défaut : "détachée") |
| **Libellé détachée bâbord** | Texte spécifique bâbord (défaut : "détachée bâbord") |
| **Libellé détachée tribord** | Texte spécifique tribord (défaut : "détachée tribord") |
| **Sons personnalisés** | Remplace la synthèse vocale par vos fichiers audio (MP3/WAV) |

> **Logique d'annonce :** Une annonce est déclenchée uniquement quand l'état **change**. Un délai de silence (configurable) évite les répétitions trop fréquentes.

### 5.2 Logique d'état du groupe

L'état d'un groupe est calculé à partir de ses pénons :

| État | Condition |
|------|-----------|
| **Attachée** | Majorité des pénons attachés des deux côtés |
| **Détachée bâbord** | Majorité des pénons bâbord détachés |
| **Détachée tribord** | Majorité des pénons tribord détachés |
| **Détachée** | Majorité des pénons détachés (sans distinction de côté) |

> "Majorité" = au moins la moitié des pénons du côté concerné.

---

## 6. Voir le détail d'un groupe

Appuyer sur un groupe dans la liste principale → **Détail du groupe**

- État global du groupe (couleur et libellé)
- Liste de tous les pénons du groupe avec :
  - Côté (B = Bâbord en bleu, T = Tribord en vert, ? = non défini)
  - Nom et MAC
  - État individuel (Attaché / Détaché)
  - Niveau de batterie
- Appuyer sur un pénon → ouvre ses paramètres

> La page se rafraîchit automatiquement à chaque retour depuis les paramètres d'un pénon.

---

## 7. Paramètres globaux

Bouton **⚙️** depuis l'écran principal.

| Paramètre | Description |
|-----------|-------------|
| **Mode** | Standard (normal) / Développeur (logs détaillés) |
| **Enregistrement CSV** | Enregistre toutes les trames BLE reçues dans un fichier |
| **Temps de silence** | Secondes entre deux annonces vocales (0 = immédiat) |
| **Mode simulation** | Active la simulation depuis un fichier CSV |
| **Choisir fichier CSV** | Sélectionne le fichier de simulation |

---

## 8. Mode simulation (tests sans matériel)

Le mode simulation permet de tester l'app avec un fichier CSV pré-enregistré.

### Activer la simulation
1. **Paramètres globaux** → activer **Mode simulation**
2. **Choisir fichier CSV** → sélectionner votre fichier
3. Retour à l'écran principal → **▶ Démarrer**
4. L'app rejoue les trames du fichier à la vitesse d'origine

### Formats CSV acceptés

Deux formats sont supportés :

**Format enregistrement brut** (généré par l'app) :
```
Timestamp,MAC_Address,Frame_Number,RSSI,Data_Size,Raw_Hex_Data
2025-11-17 17:43:45.756,EC:69:0B:19:32:8E,1,-66,46,"02 01 06 ..."
```

**Format décodé** (généré par les scripts d'analyse) :
```
Timestamp,MAC_Address,Frame_Number,RSSI,Data_Size,Raw_Hex_Data,frame_count,...
2025-11-17 17:43:45.756,EC:69:0B:19:32:8E,1,-66,46,02 01 06 ...,6296,...
```

> La différence : le deuxième format a des colonnes décodées supplémentaires et les données hex sans guillemets. Les deux fonctionnent.

### Fichier de test multi-pénons

Le fichier `test_multi_penon.csv` simule **6 pénons** sur une voile complète :

| Pénon | MAC | Côté | Comportement |
|-------|-----|------|--------------|
| Pénon B1 | AA:BB:CC:DD:EE:01 | Bâbord | Détaché de 30s à 60s |
| Pénon B2 | AA:BB:CC:DD:EE:02 | Bâbord | Détaché de 30s à 60s |
| Pénon B3 | AA:BB:CC:DD:EE:03 | Bâbord | Toujours attaché |
| Pénon T1 | AA:BB:CC:DD:EE:04 | Tribord | Toujours attaché |
| Pénon T2 | AA:BB:CC:DD:EE:05 | Tribord | Détaché de 90s à 120s |
| Pénon T3 | AA:BB:CC:DD:EE:06 | Tribord | Détaché de 90s à 120s |

**Scénario (180 secondes) :**
```
  0s –  30s : Tous ATTACHÉS
 30s –  60s : B1 + B2 DÉTACHÉS → groupe passe en "Détachée bâbord"
 60s –  90s : Tous ATTACHÉS
 90s – 120s : T2 + T3 DÉTACHÉS → groupe passe en "Détachée tribord"
120s – 180s : Tous ATTACHÉS
```

### Procédure de test complète
1. Créer un groupe (ex : "Grand-Voile")
2. Affecter les 6 MACs simulées au groupe avec côté bâbord/tribord
3. Paramètres globaux → Mode simulation → Choisir `test_multi_penon.csv`
4. Démarrer le scan → observer les changements d'état toutes les 30 secondes

### Régénérer le fichier de test
```
py generate_test_csv.py
```
Le fichier `test_multi_penon.csv` est recréé à la racine du projet.

---

## 9. Enregistrement CSV

Quand l'enregistrement est activé (Paramètres → Enregistrement CSV) et le scan démarré :
- Chaque trame BLE reçue est écrite dans un fichier CSV
- Le fichier est créé au démarrage du scan
- Il est fermé proprement à l'arrêt du scan

> Le fichier peut ensuite être utilisé pour la simulation ou l'analyse.

---

## 10. Indicateurs visuels

| Couleur | Signification |
|---------|---------------|
| 🟢 Vert | Attaché(e) |
| 🔴/Rose | Détaché(e) |
| 🔵 Bleu (badge B) | Pénon bâbord |
| 🟢 Vert (badge T) | Pénon tribord |
| ⚪ Gris (badge ?) | Pénon sans côté défini |

---

## 11. Dépannage

| Problème | Solution |
|----------|----------|
| Les pénons n'apparaissent pas | Vérifier que le scan est démarré et que le BLE est activé |
| Tous les pénons sont "détachés" | Recalibrer — le seuil par défaut (3500) est peut-être inadapté |
| Pas d'annonce vocale | Vérifier que le groupe a "Utiliser le son" activé et que le temps de silence n'est pas trop long |
| La simulation ne charge pas | Vérifier que le fichier CSV a bien les 6 colonnes minimum et le bon format de timestamp (`yyyy-MM-dd HH:mm:ss.SSS`) |
| Un pénon supprimé réapparaît | C'est normal si le vrai capteur BLE est à portée — il sera recréé comme nouveau pénon non configuré |
