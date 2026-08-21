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

- **Linguaggio**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Database Locale**: Room Database + Kotlin Coroutines & Flow
- **Fotocamera e ML**: CameraX (`androidx.camera`) + Google ML Kit Barcode Scanning
- **Generatore Codici**: ZXing Core (`com.google.zxing:core`)
- **Scorciatoie App**: AndroidX `ShortcutManagerCompat` + `ShortcutManager` (per pulizia pinned shortcuts).

---

## 📁 Architettura e Design Decision (Per riferimenti futuri)

- **MLKit Centering Logic**: In `BarcodeScannerScreen.kt`, il bounding box del barcode restituito da MLKit viene validato incrociandolo con `rotationDegrees` (90 o 270) per scambiare larghezza e altezza del buffer originale. Questo risolve il problema della fotocamera mobile in cui il sensore lavora in 1280x720 mentre lo schermo è 720x1280.
- **BrandIcon Fallback**: La funzione `BrandIcon` (in `ui/components/`) gestisce il caricamento asincrono via Glide/Coil, fallback su file locale, o fallback finale su un badge circolare con l'iniziale del negozio colorata pseudo-casualmente in base all'hash del nome.
- **Automazione Icone**: Generazione delle icone Mipmap e Round (in `res/mipmap-*/`) tramite script Python nativo con `Pillow` per massimizzare la nitidezza del logo vettoriale in stile LinkedIn.
- **DeepLink Routing**: Definito in `NavGraph.kt` e `AndroidManifest.xml` (scheme `c4rds://` e pathPattern `.*\.c4rds`).

---

## 💻 Sviluppo e Compilazione

L'intero ciclo di build e deploy è automatizzato dallo script di servizio `build_and_deploy.sh`, che esegue `gradlew assembleDebug`, sposta l'APK risultante nella cartella Dropbox per la distribuzione rapida, ed effettua il commit/push su GitHub in un unico passaggio.

```bash
./build_and_deploy.sh "messaggio di commit"
```
