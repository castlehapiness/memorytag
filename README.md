# MemoryTag — Application NFC Souvenirs
## Architecture & Guide d'implémentation

---

## Structure du projet

```
MemoryTag/
├── app/src/main/
│   ├── AndroidManifest.xml              ← Permissions NFC + déclaration activités
│   ├── java/com/memorytag/app/
│   │   ├── data/
│   │   │   ├── model/
│   │   │   │   └── Memory.kt            ← Modèles de données + MemoryUiState
│   │   │   └── repository/
│   │   │       └── MemoryRepository.kt  ← Source de données (mock / API)
│   │   ├── nfc/
│   │   │   └── NfcHelper.kt             ← Gestion NFC (foreground dispatch, extraction ID)
│   │   └── ui/
│   │       ├── MainActivity.kt          ← Écran scan NFC
│   │       ├── MemoryDetailActivity.kt  ← Écran immersif souvenir
│   │       ├── adapter/
│   │       │   └── PhotoGalleryAdapter.kt  ← RecyclerView galerie
│   │       ├── view/
│   │       │   └── GlobeView.kt         ← Vue custom globe Canvas
│   │       └── viewmodel/
│   │           └── MemoryViewModel.kt   ← MVVM ViewModel
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml        ← Layout scan NFC
│       │   ├── activity_memory_detail.xml  ← Layout souvenir immersif
│       │   └── item_photo_thumbnail.xml ← Item galerie
│       ├── anim/
│       │   ├── pulse_scale.xml          ← Animation pulsation NFC
│       │   └── pulse_scale_delayed.xml
│       ├── drawable/                    ← Voir DRAWABLES_REFERENCE.txt
│       └── values/
│           └── themes.xml
└── app/build.gradle                     ← Dépendances
```

---

## Flux de l'application

```
[Tag NFC scanné]
      ↓
NfcHelper.extractMemoryId(intent)
      ↓
MemoryViewModel.loadMemory(id)
      ↓
MemoryRepository.fetchMemory(id)   ← API REST / mock
      ↓
MemoryUiState.Success(memory)
      ↓
MemoryDetailActivity affiche :
  ├── Grande photo principale
  ├── Galerie horizontale (RecyclerView)
  ├── Globe custom (GlobeView)
  └── Titre / Lieu / Description
```

---

## Mise en place Android Studio

### 1. Créer le projet
- New Project → Empty Views Activity
- Package : `com.memorytag.app`
- Language : Kotlin, Min SDK : 26

### 2. Copier les fichiers
Placer chaque fichier dans son répertoire correspondant (voir structure ci-dessus).

### 3. Ajouter les polices SF Pro
Dans `res/font/` ajouter :
- `sf_pro_display_semibold.ttf`
- `sf_pro_text_regular.ttf`
- `sf_pro_text_medium.ttf`
- `sf_pro_text_semibold.ttf`

> **Alternative** : remplacer par `@font/roboto` ou `sans-serif` dans les layouts
> si SF Pro n'est pas disponible.

### 4. Créer les drawables manquants
Voir `DRAWABLES_REFERENCE.txt` pour les 5 drawables XML à créer.

Pour les icônes vectorielles (ic_nfc, ic_location_pin, ic_arrow_back) :
Android Studio → File → New → Vector Asset → Material Icons

### 5. Créer le fichier de transition
`res/transition/fade_enter.xml` :
```xml
<?xml version="1.0" encoding="utf-8"?>
<fade xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="400"/>
```
Idem pour `fade_exit.xml`.

### 6. Sync Gradle et Run

---

## Connecter une vraie API

Dans `MemoryRepository.kt`, remplacer `getMockMemory()` par :

```kotlin
// 1. Ajouter dans build.gradle :
// implementation 'com.squareup.retrofit2:retrofit:2.9.0'
// implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

// 2. Créer l'interface API :
interface MemoryApiService {
    @GET("memories/{id}")
    suspend fun getMemory(@Path("id") id: String): Memory
}

// 3. Dans MemoryRepository :
private val retrofit = Retrofit.Builder()
    .baseUrl("https://votre-api.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val apiService = retrofit.create(MemoryApiService::class.java)

suspend fun fetchMemory(memoryId: String): Memory {
    return apiService.getMemory(memoryId)
}
```

---

## Format du tag NFC

Écrire sur le tag un enregistrement NDEF de type URI :
```
https://memorytag.app/memory/PARIS_001
```

L'app extrait automatiquement `PARIS_001` comme ID.

---

## Notes de design

| Élément         | Choix                              |
|-----------------|------------------------------------|
| Fond            | `#080808` (pas noir pur)           |
| Accent          | `#FF3B30` (rouge Apple)            |
| Typographie     | SF Pro (ou Roboto en fallback)     |
| Animations      | 600ms fade-in, 300ms crossfade     |
| Photos          | Glide avec crossfade               |
| Globe           | Canvas custom — projection ortho   |

---

## Dépendances principales

| Lib                    | Version | Usage                        |
|------------------------|---------|------------------------------|
| AndroidX Lifecycle     | 2.7.0   | ViewModel + StateFlow        |
| Kotlin Coroutines      | 1.7.3   | Async / API calls            |
| Glide                  | 4.16.0  | Chargement images            |
| Material Components    | 1.11.0  | Thème + composants           |
| ConstraintLayout       | 2.1.4   | Layouts complexes            |
