- [x] Step 23: Ricetta metadati ufficiale F-Droid inclusa nel repository (`fdroid/io.github.ar3ac.c4rds.yml`).
- [x] Step 24: Tutela legale e marchi: inserito Trademark & Legal Disclaimer nominativo in README.md, Fastlane (EN/IT) e nelle Impostazioni dell'app con clausola di rimozione su richiesta.
- [x] Step 25: Allineamento ricetta F-Droid: commit SHA a 40 caratteri e rimozione di descrizioni duplicate in favore dei metadati Fastlane.
- [x] Step 26: Aggiunto icon.png (512x512) nella struttura Fastlane (globale e localizzata en-US / it-IT) per cataloghi F-Droid.
- [x] Step 27: Fix navigazione deep link / scorciatoie launcher da app già aperta in background (`navController.handleDeepLink`).

---

## 💡 Idee per V2 (Roadmap Futura)

- [ ] **Drag & Drop Manuale (UI)**:
  - Il database è già pronto (campo `displayOrder`), manca solo l'implementazione visiva dei gesture detector sulla `LazyColumn` per spostare le tessere a piacimento.
- [ ] **Wear OS Companion**:
  - Visualizzazione delle tessere fedeltà direttamente dallo smartwatch al polso.
- [ ] **Widget Android / Quick Settings Tile**:
  - Scorciatoia rapida alle carte preferite direttamente dal pannello notifiche o widget home.
