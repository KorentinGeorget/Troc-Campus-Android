# 📦 Troc'Campus — Application Android Native

Auteur : **Korentin Georget** — Master 2 DFS (Università di Corsica)  
* **Télécharger l'APK :** [GitHub Release v1.0.0 — `app-debug.apk`](https://github.com/KorentinGeorget/Troc-Campus-Android/releases/tag/v1.0.0)  
* **Dépôt PWA :** [https://github.com/KorentinGeorget/Troc-Campus](https://github.com/KorentinGeorget/Troc-Campus)  
* **PWA en ligne :** [https://troc-campus.korentingeorget.com](https://troc-campus.korentingeorget.com)  

---

## Présentation du projet

Ce projet est le portage natif en **Kotlin** et **Jetpack Compose** de ma PWA Troc'Campus, une application de petites annonces étudiantes (dons, ventes et trocs) pour les campus de l'Université de Corse (Corte, Bastia, Ajaccio).

L'application a été conçue pour fonctionner complètement hors ligne, avec synchronisation en arrière-plan et intégration des fonctionnalités matérielles du téléphone.

---

## Ce qui a été développé

### 1. Base de données locale Room et Migrations
* Les annonces sont stockées en local avec **Room** (SQLite).
* J'ai mis en place les migrations de la version 1 à la version 4 pour faire évoluer le schéma de base de données sans perdre les données existantes :
  * **v1 :** Structure initiale des annonces.
  * **v2 :** Ajout de la fonctionnalité pour épingler des annonces (`pinned`).
  * **v3 :** Ajout de la gestion de l'état de synchronisation (`synced`).
  * **v4 :** Ajout de la géolocalisation (`lat`, `lng`, `accuracy`) et du chemin de la photo (`photoPath`).
* Les photos prises avec l'appareil photo sont enregistrées sous forme de fichiers dans le dossier interne de l'application (`filesDir/photos/`) pour éviter d'alourdir la base SQLite.

### 2. Synchronisation hors ligne avec WorkManager
* L'application est utilisable même sans connexion internet ou en mode avion : les annonces créées sont enregistrées localement avec un statut en attente.
* Dès que le réseau est disponible, un **SyncWorker** géré par **WorkManager** s'exécute en arrière-plan pour envoyer les annonces en attente vers le serveur d'écho et met à jour leur statut en base une fois envoyées.
* Une notification est affichée pour confirmer que la synchronisation s'est bien terminée.

### 3. Gestion du cycle de vie
* J'ai utilisé `SavedStateHandle` dans le ViewModel et `rememberSaveable` pour l'interface : si l'utilisateur tourne son écran ou si l'application est coupée en arrière-plan par le système, le formulaire en cours de rédaction, les photos et les textes saisis ne sont pas perdus.

### 4. Fonctionnalités natives exclusives

* **Scanner IA avec CameraX et ML Kit :**
  * En cliquant sur le bouton de scan dans le formulaire, l'appareil photo s'ouvre avec CameraX.
  * Un modèle ML Kit analyse le flux vidéo en direct : il reconnaît le texte (OCR) pour extraire le titre des manuels ou des cours et permet de pré-remplir l'annonce en un clic.
  * Il identifie également les objets visibles (ex: livre, ordinateur, etc.).
  * Tout fonctionne à 100% en local sur le téléphone, sans appel réseau ni clé API.

* **Boussole Campus avec capteurs magnétiques :**
  * Sur la page de détail d'une annonce, une boussole animée utilise l'accéléromètre et le magnétomètre du téléphone (`SensorManager`).
  * Elle calcule en temps réel le cap et la distance vers le campus de remise de l'objet (Campus Mariani, Campus Grimaldi, etc.) et pointe directement dans sa direction.

### 5. Intégrations système Android
* **App Links vérifiés :** L'adresse `https://troc-campus.korentingeorget.com` est associée à l'application grâce au fichier `assetlinks.json`. Si on clique sur un lien d'annonce depuis une messagerie ou une note, l'application native s'ouvre directement sur la bonne fiche.
* **Notifications :** Canaux de notifications configurés (synchronisation et alertes de campus) avec service Firebase Cloud Messaging (FCM) et bouton de test différé de 5 secondes pour tester la réception.
* **Partage :** Partage d'annonces avec photo vers d'autres applications via un `FileProvider`, et possibilité de recevoir du texte partagé depuis une autre app pour créer une annonce.
* **Permissions :** Gestion des permissions (caméra, localisation et notifications) avec explication pour l'utilisateur et redirection vers les paramètres système en cas de refus définitif.

---

## Compilation et exécution

```bash
# Compiler l'APK de debug
./gradlew assembleDebug

# Installer sur le téléphone
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

L'APK précompilé est également téléchargeable directement sur la page des [Releases GitHub](https://github.com/KorentinGeorget/Troc-Campus-Android/releases/tag/v1.0.0).
