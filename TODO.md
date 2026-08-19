# c4rds - App Android Fedeltà / Barcode

## Roadmap di Sviluppo V1 (Completata)

- [x] Step 1: Analisi requisiti e architettura del progetto
- [x] Step 2: Creazione struttura progetto Android Gradle e dipendenze (Compose M3, Room, CameraX, ML Kit, ZXing, ShortcutManagerCompat)
- [x] Step 3: Implementazione strato Dati (Entity Room, DAO, Database e Repository con migrazione Room)
- [x] Step 4: Utility per Barcode (ZXing Generator), Luminosità Schermo e Shortcut Dinamiche Android
- [x] Step 5: Implementazione Fotocamera e Scansione Barcode (CameraX + ML Kit)
- [x] Step 6: Schermate UI (Home list, Aggiungi/Modifica con auto-detection CODE 128/EAN-13, Dettaglio Barcode ad alto contrasto con pulsante Copia, Impostazioni & Backup JSON SAF)
- [x] Step 7: Brand Identity & Loghi Ufficiali ad Alta Risoluzione per 19 supermercati/brand (Esselunga, Conad, Coop, Lidl, Eurospin, Iperal, Carrefour, Decathlon, IKEA, Tigotà, Crai, MD, Pam, Bennet, Despar, Sephora, Unieuro, MediaWorld, Leroy Merlin).
- [x] Step 8: Nickname / Note ("es. Luca") con visualizzazione elegante senza prefissi fisso e campo Form "Nickname / Note".
- [x] Step 9: Ingrandimento e presentazione ad alto impatto visivo del Logo Ufficiale del brand nella schermata Dettaglio Tessera (Card bianca elevata 3D).
- [x] Step 10: "Aggiungi alla Home" (Pin Shortcut) con icona ufficiale del brand e avvio diretto al dettaglio via Deep Link (`mycards://card/{id}`).
- [x] Step 11: Implementazione nuovo Logo App "Premium" in stile glassmorphism nella TopAppBar.
- [x] Step 12: Condivisione Tessera (Intent ACTION_SEND per WhatsApp/Telegram).
- [x] Step 13: Migrazione Database v3 (Aggiunto `displayOrder` per futuro Drag & Drop manuale).
- [x] Step 14: Compilazione APK Debug v1.5.0.

---

## 💡 Idee per V2 (Roadmap Futura)

- [ ] **Drag & Drop Manuale (UI)**:
  - Il database è già pronto (campo `displayOrder`), manca solo l'implementazione visiva dei gesture detector sulla `LazyColumn` per spostare le tessere a piacimento.
- [ ] **Importazione da Immagine / Galleria**:
  - Scansione e rilevamento barcode da foto salvate nella galleria.
