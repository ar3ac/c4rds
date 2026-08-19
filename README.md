# c4rds 💳

Un'applicazione Android nativa, semplice, ultra-veloce e 100% offline per gestire le tue tessere fedeltà e codici a barre (supermercati, negozi, palestre, club).

---

## 🚀 Caratteristiche Principali (V1)

- 🔒 **100% Offline & Privacy Friendly**: Nessun permesso `INTERNET` nel Manifest, nessun tracciamento, nessun server cloud o Firebase. I tuoi dati restano esclusivamente sul tuo smartphone.
- 📸 **Scansione Barcode & QR**: Scansione in tempo reale tramite **CameraX** e **ML Kit Barcode Scanning** con modello integrato offline.
- 📐 **Rendering Barcode Alta Qualità**: Generazione ad alta risoluzione tramite **ZXing** rispettando proporzioni, margini e quiet-zone originali per la massima leggibilità con gli scanner laser/ottici delle casse dei supermercati.
- 💡 **Luminosità Massima Temporanea**: All'apertura della schermata della tessera, la luminosità viene portata al 100% ed il display rimane acceso (`FLAG_KEEP_SCREEN_ON`). Alla chiusura, la luminosità di sistema originale viene ripristinata automaticamente.
- ⚡ **Android App Shortcuts Dinamiche**: Pressione prolungata sull'icona dell'app nel launcher Android per accedere istantaneamente alle tessere preferite (es. Esselunga, Conad, Lidl) o alla funzione *Aggiungi*, senza passare dalla Home.
- ⭐ **Tessere Preferite in Cima**: Organizzazione delle tessere con priorità alle preferite sia nella lista principale che nelle scorciatoie veloci.
- 📦 **Backup Doppio**:
  - **Android Auto Backup**: Configurazione di sistema per backup automatico Google (`backup_rules.xml` e `data_extraction_rules.xml`).
  - **Backup Manuale JSON via SAF**: Esportazione ed importazione manuale in formato JSON versionato con Storage Access Framework (SAF), gestione file corrotti/versioni e scelta tra unione o sostituzione del database.

---

## 🛠️ Stack Tecnologico

- **Linguaggio**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Database Locale**: Room Database + Kotlin Coroutines & Flow
- **Fotocamera**: CameraX (`androidx.camera`)
- **Riconoscimento Codici**: Google ML Kit Barcode Scanning (`com.google.mlkit:barcode-scanning`)
- **Generatore Codici**: ZXing Core (`com.google.zxing:core`)
- **Scorciatoie App**: AndroidX `ShortcutManagerCompat`

---

## 📁 Struttura del Progetto

```
MyCards/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/mycards/
│   │   │   ├── data/
│   │   │   │   ├── local/        # Room Database & DAO (CardEntity, CardDao, AppDatabase)
│   │   │   │   ├── repository/   # CardRepository e sync scorciatoie
│   │   │   │   └── backup/       # BackupManager (Export/Import JSON SAF)
│   │   │   ├── ui/
│   │   │   │   ├── theme/        # Material 3 Theme, Colori e Stili
│   │   │   │   ├── navigation/   # NavGraph, Rotte Compose e Deep Links
│   │   │   │   ├── home/         # HomeScreen & HomeViewModel
│   │   │   │   ├── add/          # AddEditCardScreen & AddEditViewModel
│   │   │   │   ├── scan/         # BarcodeScannerScreen (CameraX + ML Kit)
│   │   │   │   ├── detail/       # CardDetailScreen (Alto contrasto & Screen brightness)
│   │   │   │   └── settings/     # SettingsScreen (Backup SAF & Info Privacy)
│   │   │   ├── utils/            # BarcodeGenerator (ZXing), ShortcutHelper, BrightnessHelper
│   │   │   ├── MainActivity.kt
│   │   │   └── MyCardsApplication.kt
│   │   └── res/                  # Risorse XML, Drawable, Adaptive Icons e Backup Rules
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── TODO.md
```

---

## 💻 Come Aprire il Progetto in Android Studio

1. Avvia **Android Studio** (Hedgehog 2023.1.1 o più recente consigliato).
2. Seleziona **Open** e naviga fino alla cartella del progetto:
   `/home/ar3ac/projects/c4rds`
3. Attendi che Gradle completi l'importazione delle dipendenze e l'indicizzazione.
4. Seleziona un dispositivo virtuale (Emulator Android 8.0 / API 26+) o un dispositivo reale via USB.
5. Fai clic sul pulsante **Run 'app'** (o premi `Shift + F10`).

---

## 🔨 Come Compilare l'APK Debug da Terminale

Esegui il seguente comando nella root del progetto:

```bash
./gradlew assembleDebug
```

### 📍 Posizione dell'APK Generato

Dopo la compilazione, il file APK pronto per l'installazione si trova in:

```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Guida ai Test su Dispositivo Reale

Per verificare appieno l'applicazione alla cassa del supermercato, esegui i seguenti test:

1. **Scansione Barcode**:
   - Apri l'app e premi `+ Aggiungi carta` -> `Scansiona con Fotocamera`.
   - Concedi il permesso Fotocamera e inquadra una tessera reale (es. EAN-13 di Esselunga/Conad/Coop).
   - Rileva automaticamente valore e formato (EAN_13, CODE_128, ecc.) e inserisci il nome della tessera.

2. **Visualizzazione Barcode alla Cassa & Luminosità**:
   - Tocca la tessera salvata per aprirla.
   - Verifica che lo schermo aumenti immediatamente la luminosità al 100% e non si spenga (Keep Screen On).
   - Verifica che il barcode sia ben visibile in nero su rettangolo bianco ad alto contrasto.
   - Chiudi la schermata e nota il ripristino immediato della luminosità normale del tuo telefono.

3. **Android App Shortcuts Dinamiche**:
   - Vai nella schermata Home del tuo Android launcher e tieni premuta l'icona dell'app **MyCards**.
   - Verifica la comparsa delle tessere preferite (es. *Esselunga*, *Conad*) nel menu rapido.
   - Tocca direttamente una tessera: l'app si aprirà istantaneamente sulla schermata del barcode senza passare dalla Home!

4. **Esportazione & Importazione Backup JSON**:
   - Apri le Impostazioni dell'app (icona ingranaggio in alto a destra nella Home).
   - Premi **Esporta** e salva il file JSON nella cartella Download o Drive.
   - Cancella una carta o premi **Importa** per ricaricare le tessere da file selezionando "Sostituisci" o "Unisci".

---

## 📝 Limitazioni Note V1

- **Nessuna sincronizzazione cloud proprietaria**: I dati sono esclusivamente locali (design intentional per privacy e velocità).
- **Loghi ufficiali**: Si utilizzano badge cromatici generici con le iniziali del supermercato/negozio.
