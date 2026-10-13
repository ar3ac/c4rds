# C4rds Wallet 💳

Un'applicazione Android nativa, semplice, ultra-veloce e 100% offline-first per gestire le tue tessere fedeltà e codici a barre (supermercati, negozi, palestre, club). 

---

## 🚀 Caratteristiche Principali (V1.0)

- 🔒 **Privacy Friendly**: C4rds è progettato per tutelare i tuoi dati. I backup e le tessere restano sul tuo dispositivo. L'app richiede il permesso `INTERNET` esclusivamente per scaricare (in sola lettura) i loghi dei brand da una repository statica open-source (nessun tracciamento, nessun SDK di terze parti).
- 📸 **Scansione Barcode & QR Mirata**: Scansione tramite **CameraX** e **ML Kit Barcode Scanning**. Implementa una logica di targeting avanzata che valuta l'orientamento del sensore fotocamera (portrait vs landscape) per leggere i codici *solo* quando passano nel 40% centrale del mirino. Supporto anche per la scansione di barcode da immagini della galleria fotografica.
- 📐 **Rendering Barcode Alta Qualità**: Generazione ad alta risoluzione tramite **ZXing**, con logiche dinamiche per espandere i codici 1D (EAN/Code128) a tutta larghezza e mantenere i 2D (QR Code) proporzionati, massimizzando la leggibilità con gli scanner laser delle casse dei supermercati.
- 💡 **Luminosità Massima Temporanea**: All'apertura della schermata della tessera, la luminosità viene portata al 100% (`FLAG_KEEP_SCREEN_ON`). Alla chiusura, la luminosità di sistema originale viene ripristinata automaticamente.
- ⚡ **Android App Shortcuts Dinamiche e Fisse**: Pressione prolungata sull'icona dell'app nel launcher Android per accedere istantaneamente alle tessere preferite (es. Esselunga, Conad, Lidl) o alla funzione *Aggiungi*. In caso di rimpiazzo del database, l'app si occupa di pulire le shortcut vecchie e "spegnere" (disable) i pinned shortcuts orfani per evitare crash.
- ⭐ **Estetica Premium e Loghi Cloud**: Header personalizzato in stile moderno con barra di ricerca integrata, design in *glassmorphism* leggero e icone generate vettorialmente via Python. I loghi ufficiali dei negozi vengono scaricati (se presenti) da un database JSON cloud condiviso (`brands.json`).
- 📦 **Backup Doppio e Condivisione Intelligente**:
  - **Android Auto Backup**: Configurazione di sistema per backup automatico Google.
  - **Backup JSON (.c4rds)**: Esportazione ed importazione manuale con Storage Access Framework (SAF). Scelta tra unione o sostituzione del database ("Aggiungi a esistenti" o "Sostituisci tutto").
  - **Deep Links & Web Sharing**: Condivisione sicura di una singola carta a familiari tramite link web (`https://c4rds.app/create?...`) o file condiviso, intercettato direttamente dai Deep Link dell'app.

---

## 🛠️ Stack Tecnologico

- **Package ID**: `io.github.ar3ac.c4rds`
- **Licenza**: GNU General Public License v3.0 (GPL-3.0)
- **Linguaggio**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Database Locale**: Room Database + Kotlin Coroutines & Flow
- **Fotocamera e Barcode Scanner**:
  - Flavor **`foss`**: 100% Open-Source tramite CameraX + ZXing Core (`com.google.zxing:core`), pronto per inclusione ufficiale su **F-Droid**.
  - Flavor **`full`**: CameraX + Google ML Kit Barcode Scanning per release proprietarie/Google Play.
- **Generatore Codici**: ZXing Core (`com.google.zxing:core`)
- **Scorciatoie App**: AndroidX `ShortcutManagerCompat` + `ShortcutManager` (per pulizia pinned shortcuts).

---

## 📁 Architettura e Design Decision (Per riferimenti futuri)

- **Doppio Flavor (F-Droid Ready)**: Il progetto è strutturato con la dimensione `distribution` divisa in:
  - `foss`: privo di qualsiasi binario proprietario o dipendenza Google Play Services. L'analisi dei frame della fotocamera e delle immagini da galleria è gestita da ZXing (`BarcodeScannerEngine.kt` in `src/foss/`).
  - `full`: include `com.google.mlkit:barcode-scanning` (`BarcodeScannerEngine.kt` in `src/full/`).
- **Centering & Targeting Mirino**: In entrambe le implementazioni, l'area di scansione valuta la rotazione del sensore (portrait vs landscape) ed esegue il crop/targeting nel mirino centrale, ignorando codici a barre periferici.
- **BrandIcon Fallback**: La funzione `BrandIcon` (in `ui/components/`) gestisce il caricamento asincrono via Coil, fallback su file locale, o fallback finale su un badge circolare con l'iniziale del negozio.
- **Metadati Fastlane**: Presenti in `fastlane/metadata/android/` (`it-IT` ed `en-US`) conformi agli standard di pubblicazione automatica per F-Droid e Google Play.
- **Automazione Icone**: Generazione delle icone Mipmap e Round (in `res/mipmap-*/`) tramite script Python nativo con `Pillow`.
- **DeepLink Routing**: Definito in `NavGraph.kt` e `AndroidManifest.xml` (scheme `c4rds://` e pathPattern `.*\.c4rds`).

---

## 💻 Sviluppo e Compilazione

L'intero ciclo di build e deploy è automatizzato dallo script di servizio `build_and_deploy.sh`, che compila i flavor debug, sposta gli APK risultanti nella cartella Dropbox per la distribuzione rapida, ed effettua il commit/push su GitHub in un unico passaggio:

```bash
# Compilazione debug e deploy
./build_and_deploy.sh "messaggio di commit"

# Compilazione singola via Gradle:
gradle assembleFullDebug   # Versione con ML Kit
gradle assembleFossDebug   # Versione 100% FOSS per F-Droid
```
