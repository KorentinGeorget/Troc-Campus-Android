# 📦 Troc'Campus — Application Android Native

Projet de TP noté pour le module **Développement d'applications mobiles** (Master 2 Informatique, Università di Corsica).  
Auteur : **Korentin Georget**  
Portage complet de ma Progressive Web App : [https://troc-campus.korentingeorget.com](https://troc-campus.korentingeorget.com) en application native Android (Kotlin + Jetpack Compose).

---

## 🎯 Ce que j'ai fait sur ce projet

L'objectif de ce projet était de reprendre ma PWA du premier TP (Troc'Campus, une plateforme d'échange, de don et de vente d'objets entre étudiants sur les campus de Corte, Bastia et Ajaccio) et de la transformer en une véritable application native Android moderne, en appliquant tous les concepts vus en cours et en TD (Jours 4 et 5).

J'ai choisi de construire une application $100\%$ Kotlin avec l'UI déclarative **Jetpack Compose** (BOM 2026.09) et la nouvelle API **Navigation 3**, en respectant scrupuleusement l'architecture en couches recommandée par Google et vue en cours.

---

## 💡 Remarque sur les données (PWA vs Application Native)

Si vous comparez la PWA en ligne et l'application Android :
* Au premier lancement, les deux affichent exactement les **mêmes 3 annonces de départ** (Livre Tanenbaum en don, Calculatrice TI-84 en vente, Bureau compact en troc).
* En revanche, les nouvelles annonces créées sur la PWA ou sur l'application native ne se synchronisent pas entre elles sur une base centrale. C'est tout à fait normal et conforme aux consignes du cours :
  * La **PWA** stocke ses données dans l'**IndexedDB** du navigateur web et synchronise via Background Sync vers le serveur d'écho.
  * L'**application Android** stocke ses données localement dans **SQLite via Room v4** et synchronise via l'Outbox **WorkManager** vers le serveur d'écho (`httpbin.org/post` ou `/api/sync`).
  * Il n'y a pas de base de données backend partagée (pas de Firestore temps réel) : chaque client est autonome en architecture *offline-first*.

---

## 🏗️ Architecture en 4 couches

J'ai structuré le code selon les 4 couches demandées (slide 11 du Jour 5) :

```text
UI (Jetpack Compose, Navigation 3, Material 3)
       ↕ (StateFlow, événements)
ViewModel (TrocViewModel + SavedStateHandle)
       ↕ (suspend fun, Flow)
Repository (AnnoncesRepository — source unique de vérité)
       ↕
┌───────────────────────┴───────────────────────┐
Local                                           Remote
- Room v4 (TrocDatabase + Migrations 1→2→3→4)   - Retrofit 3 + OkHttp 5
- DataStore Preferences (SettingsStore)         - WorkManager Outbox (SyncWorker)
- Fichiers photos (filesDir/photos/)
```

### 1. Persistance locale avec Room v4 & Migrations séquentielles
* **La base de données :** J'ai défini l'entité [`AnnonceEntity`](app/src/main/java/com/korentingeorget/troccampus/data/local/AnnonceEntity.kt) et le DAO [`AnnonceDao`](app/src/main/java/com/korentingeorget/troccampus/data/local/AnnonceDao.kt).
* **Les migrations v1 à v4 :** Pour reproduire le cycle de vie réel d'une application en production, j'ai configuré `exportSchema = true` dans le build Gradle et écrit les migrations séquentielles dans [`TrocDatabase.kt`](app/src/main/java/com/korentingeorget/troccampus/data/local/TrocDatabase.kt) :
  * **v1 :** Schéma de base (id, titre, type, prix, campus, contact, description, date).
  * **v1 $\rightarrow$ v2 :** Ajout de la colonne `pinned` (annonces épinglées).
  * **v2 $\rightarrow$ v3 :** Ajout de la colonne `synced` (Outbox pattern).
  * **v3 $\rightarrow$ v4 :** Ajout des colonnes `lat`, `lng`, `accuracy` (position GPS) et `photoPath` (chemin de photo).
* Tous les schémas JSON intermédiaires (1.json, 2.json, 3.json, 4.json) sont versionnés dans `app/schemas/`.
* **Évitement du crash CursorWindow (2 Mo) :** Au lieu de stocker les photos sous forme de `BLOB` dans SQLite (ce qui fait crasher l'app dès que les photos dépassent la taille du tampon SQLite de 2 Mo), j'ai créé [`PhotoStore.kt`](app/src/main/java/com/korentingeorget/troccampus/data/local/PhotoStore.kt) qui stocke les vrais fichiers JPEG dans le dossier privé `context.filesDir/photos/`. Room ne stocke que le chemin textuel.

### 2. Gestion du cycle de vie (`rememberSaveable` & `SavedStateHandle`)
* **Survie à la rotation :** Les variables d'affichage de la modal (`showCreateSheet`) utilisent `rememberSaveable` pour que le formulaire reste ouvert quand l'utilisateur tourne son écran.
* **Survie à la mort du process (`am kill`) :** Toutes les valeurs du formulaire (`draftTitle`, `draftPrice`, `draftPhotoPath`, etc.) sont stockées dans le **`SavedStateHandle`** du `TrocViewModel`. Si le système Android manque de mémoire et tue le process de l'application en arrière-plan, lors de la réouverture, le texte en cours de saisie et la photo sont instantanément restaurés depuis le Bundle système.

### 3. Synchronisation arrière-plan & Outbox (`WorkManager`)
* Dès qu'une annonce est créée (même hors ligne ou en mode avion), elle est enregistrée dans Room avec `synced = 0`.
* Le repository déclenche ensuite [`SyncWorker`](app/src/main/java/com/korentingeorget/troccampus/work/SyncWorker.kt) via `workManager.enqueueUniqueWork("troc_sync", ExistingWorkPolicy.KEEP, ...)`.
* Le worker possède la contrainte matérielle `NetworkType.CONNECTED` et une politique de retry exponentiel (backoff).
* Dès que le réseau revient, Android exécute le worker en tâche de fond même si l'application est fermée, envoie les annonces en attente vers le serveur d'écho (avec repli sur `httpbin.org/post`), passe leur flag à `synced = 1` dans Room, et déclenche une notification locale indiquant le nombre d'annonces synchronisées. Aucun risque de dédoublement.

---

## 🚀 Fonctionnalités Natives Exclusives (Impossibles en PWA)

Pour la part libre du projet (slide 107 du Jour 5), j'ai développé **deux fonctionnalités exclusives** qui tirent parti du matériel et que le Web ne peut pas faire :

### 1. Scanner IA On-Device (CameraX + Google ML Kit)
* Fichier : [`CameraMlScannerDialog.kt`](app/src/main/java/com/korentingeorget/troccampus/ui/scanner/CameraMlScannerDialog.kt)
* J'ai configuré `CameraX` avec un cas d'usage `ImageAnalysis` relié au moteur Google ML Kit local :
  * **Reconnaissance de texte (OCR) :** Quand l'étudiant cadre la couverture d'un manuel universitaire (ex: Tanenbaum, calculatrice TI-84), le texte est détecté en direct à 30 images/s et proposé sous forme de suggestion émeraude pour pré-remplir le titre en 1 clic.
  * **Classification d'objets :** Détection d'objets avec seuil de confiance élevé ($\ge 78\%$) et filtrage des étiquettes abstraites.
  * **$100\%$ hors-ligne :** Zéro clé API, aucun appel cloud, respect total de la vie privée.

### 2. Boussole Campus Magnétique 3D (SensorManager)
* Fichier : [`CampusCompassCard.kt`](app/src/main/java/com/korentingeorget/troccampus/ui/sensors/CampusCompassCard.kt)
* Sur la fiche détaillée de chaque annonce, une boussole matérielle utilise la fusion de capteurs Android :
  * Accéléromètre (`TYPE_ACCELEROMETER`) + Magnétomètre (`TYPE_MAGNETIC_FIELD`) échantillonnés à $60\text{ Hz}$ (`SENSOR_DELAY_UI`).
  * Calcul de la matrice de rotation via `SensorManager.getRotationMatrix` et de l'orientation azimuth.
  * Calcul du relèvement géodésique et de la distance en mètres entre la position de l'étudiant et le campus universitaire de remise (Campus Mariani, Campus Grimaldi, IUT, etc.).
  * L'aiguille tourne en temps réel en suivant la rotation physique du téléphone.

---

## 🔗 Intégrations Système Android

* **App Link vérifié (`https://troc-campus.korentingeorget.com`) :**
  * Déclaration de l'intent-filter avec `android:autoVerify="true"` dans l'[`AndroidManifest.xml`](app/src/main/AndroidManifest.xml).
  * Fichier `/.well-known/assetlinks.json` hébergé sur mon VPS Coolify avec l'empreinte SHA-256 de ma clé debug, validé par l'API Google Digital Asset Links (`{"linked": true}`).
  * L'ouverture d'un lien `https://troc-campus.korentingeorget.com/?ad=2` depuis WhatsApp, SMS ou Notes ouvre directement l'application native sur l'annonce ciblée.
  * J'ai également ajouté un Smart App Banner sur ma PWA pour permettre la bascule en 1 clic depuis Chrome.
* **Notifications & FCM :**
  * Création des canaux de notification Android au démarrage : `sync` (importance basse) et `campus_alerts` (importance haute).
  * Service [`TrocMessagingService.kt`](app/src/main/java/com/korentingeorget/troccampus/notifications/TrocMessagingService.kt) pour la réception de push distants Firebase.
  * Bouton de test différé ($5\text{ s}$) via [`SimulatedPushWorker.kt`](app/src/main/java/com/korentingeorget/troccampus/work/SimulatedPushWorker.kt) pour faire la démonstration en classe sans dépendre de la connexion Wi-Fi de l'université.
* **Partage système (Intents & FileProvider) :**
  * Possibilité de partager n'importe quelle annonce (texte + photo JPEG) vers WhatsApp ou d'autres applications via le `FileProvider` (`ACTION_SEND`).
  * Réception de partages externes : si on partage du texte vers Troc'Campus, l'application s'ouvre avec le texte pré-rempli dans le titre de l'annonce.
* **Machine à états des permissions (`PermissionFlow`) :**
  * Gestion propre des permissions (Caméra, Localisation fine/approximative, Notifications POST_NOTIFICATIONS).
  * Si l'utilisateur refuse une fois, une explication pédagogique s'affiche.
  * En cas de refus définitif (`shouldShowRequestPermissionRationale == false`), l'application propose un bouton direct vers les Paramètres système de l'OS sans bloquer l'accès aux autres fonctions.

---

## 🛠️ Comment compiler et lancer l'application

### Prérequis
* Android Studio (Quail 4 / 2026.1 ou plus récent) ou SDK Android en ligne de commande.
* JDK 21 (inclus avec Android Studio).
* Un smartphone Android (Android 8.0+ / API 26+) avec le débogage USB activé, ou un émulateur Pixel.

### Commandes utiles

```bash
# 1. Compiler et exécuter les tests unitaires
./gradlew testDebugUnitTest

# 2. Générer l'APK de debug
./gradlew assembleDebug

# 3. Installer l'APK sur l'appareil connecté
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Tester l'App Link via ADB
adb shell am start -a android.intent.action.VIEW -d "https://troc-campus.korentingeorget.com/?ad=2"

# 5. Tester la survie à la mort du process
adb shell am kill com.korentingeorget.troccampus
```

L'APK généré se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.
