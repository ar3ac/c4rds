# c4rds - App Android Fedeltà / Barcode

## Roadmap di Sviluppo V1 & V1.2 (Completata)

- [x] Step 1: Analisi requisiti e architettura del progetto
- [x] Step 2: Creazione struttura progetto Android Gradle e dipendenze (Compose M3, Room, CameraX, ML Kit, ZXing, ShortcutManagerCompat)
- [x] Step 3: Implementazione strato Dati (Entity Room, DAO, Database versione 2 con migrazione MIGRATION_1_2 e campo `owner`)
- [x] Step 4: Utility per Barcode (ZXing Generator), Luminosità Schermo, BrandHelper (loghi vettoriali supermercati) e Shortcut Dinamiche
- [x] Step 5: Implementazione Fotocamera e Scansione Barcode (CameraX + ML Kit)
- [x] Step 6: Schermate UI:
  - **Home**: Lista pulita senza chip formato barcode, loghi vettoriali dei marchi ufficiali (Esselunga, Conad, Coop, Lidl, Eurospin, Carrefour, Decathlon, IKEA, Tigotà, ecc.), badge Proprietario/Nickname (`👤 Luca`) e menu con Copia Codice al volo.
  - **Aggiungi/Modifica**: Campo Proprietario/Nickname, auto-detection intelligente di CODE 128 / EAN-13 e gestione/pulizia spazi vuoti.
  - **Dettaglio Barcode**: Barcode gigante ad alto contrasto, logo del marchio, badge proprietario, pulsante di copia codice negli appunti con toast di conferma.
- [x] Step 7: Configurazione Android Auto Backup (`backup_rules.xml`, `data_extraction_rules.xml`, `hasFragileUserData`, `AndroidManifest.xml`)
- [x] Step 8: Icone vettoriali adattive c4rds per Pixel Launcher (Pixel 8)
- [x] Step 9: Compilazione APK Debug v1.2.0

---

## 💡 Idee per Future Release

- [ ] Supporto a widget nativi per la schermata home di Android.
- [ ] Riconoscimento intelligente tramite geolocalizzazione (es. suggerisce la carta Esselunga quando ci si trova nei pressi del supermercato).
