# C4rds Wallet 💳

A native, ultra-fast, and 100% offline-first Android application to manage your loyalty cards and barcodes (supermarkets, retail stores, gyms, memberships).

Built with modern Android engineering: Kotlin, Jetpack Compose, Material 3, Room Database, CameraX, and ZXing.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://android.com)
[![F-Droid Ready](https://img.shields.io/badge/F--Droid-Ready-brightgreen.svg)](https://f-droid.org)

---

## 🚀 Key Features

- 🔒 **Privacy-First & 100% Offline**: Your cards, barcodes, and backups remain strictly on your device. No user accounts, no analytics, no ad tracking, and no proprietary cloud SDKs.
- 📸 **100% Open-Source Barcode & QR Scanner**: Powered by **CameraX** and **ZXing Core** (`com.google.zxing:core`).
  - Sensor-rotation aware (handles 0°, 90°, 180°, and 270° orientations seamlessly).
  - Handles CameraX memory rowStride buffer alignment cleanly.
  - Two-stage detection: focused central viewfinder crop (75% width × 40% height) for instant checkout targeting without picking up neighboring barcodes, with an automatic full-frame fallback.
  - Gallery scanner support: decode barcodes and QR codes directly from imported images.
  - Automatic normalization of 12-digit UPC-A codes to standard European EAN-13.
- 📐 **High-Resolution Barcode Rendering**: Dynamic ZXing vector/bitmap rendering that stretches 1D barcodes (EAN-13, Code 128) across full width for grocery laser scanners while keeping 2D barcodes (QR Codes, DataMatrix) perfectly proportioned.
- 💡 **Automatic Temporary Peak Brightness**: When viewing a card, screen brightness automatically boosts to 100% (`FLAG_KEEP_SCREEN_ON`) for instant readability at store self-checkouts. The original system brightness is immediately restored upon exiting.
- ⚡ **Android App Shortcuts (Dynamic & Pinned)**: Long-press the app icon on the Android launcher to access your favorite cards instantly, or pin dedicated 1-tap shortcuts with official store icons directly to your home screen.
- 🎨 **Modern Material 3 UI & Cloud Logos**: Clean, responsive layout with glassmorphic accents, brand search bar, and custom card reordering. Official store logos are cached locally and synchronized from a public static database (`brands.json`) with procedural color avatar fallbacks.
- 📦 **Open JSON Backup & Cross-Device Sharing**:
  - Full backup/restore via Android Storage Access Framework (SAF) using open `.c4rds` JSON format (with embedded Base64 custom logos).
  - Selective import options: "Add to existing" or "Replace all".
  - Deep Link & Web Sharing: Share individual cards with family via web links (`https://c4rds.app/create?...`) or shared `.c4rds` files.

---

## 🛠️ Tech Stack

- **Package ID**: `io.github.ar3ac.c4rds`
- **License**: GNU General Public License v3.0 ([GPL-3.0](LICENSE))
- **Language**: Kotlin 1.9+
- **UI Toolkit**: Jetpack Compose + Material 3
- **Local Persistence**: Room Database + Coroutines & Flow
- **Camera & Scanning**: CameraX (`androidx.camera`) + ZXing Core (`com.google.zxing:core`)
- **Image Loading**: Coil (`io.coil-kt:coil-compose`)
- **Serialization**: Google Gson
- **Shortcuts**: AndroidX `ShortcutManagerCompat`

---

## 🏛️ Architecture & Engineering Notes

- **Unified Open-Source Scanner**: In [`BarcodeScannerEngine.kt`](app/src/main/java/io/github/ar3ac/c4rds/ui/scan/BarcodeScannerEngine.kt), CameraX YUV_420_888 luminance planes are extracted and transposed according to the hardware sensor rotation (`rotationDegrees`). ZXing binarizes the luminance directly in memory without RGB overhead, ensuring sub-10ms decoding.
- **Fastlane Metadata**: Packaged in `fastlane/metadata/android/` (`en-US` and `it-IT`) compliant with F-Droid and Google Play automated submission pipelines.
- **Deep Link Navigation**: Handled in `NavGraph.kt` and `AndroidManifest.xml` supporting `c4rds://`, `mycards://`, and `https://c4rds.app/` URI schemes.
- **Brand Fallback System**: Implemented in `BrandIcon.kt`, providing three tiers of visual representation: local custom asset -> cloud cache -> deterministic colored monogram badge.

---

## 💻 Building and Development

### Prerequisites
- Android SDK (API 34 compileSdk, minSdk 26)
- JDK 17
- Gradle 8.5+

### Build Commands

```bash
# Build Debug APK
gradle assembleDebug

# Build Release APK
gradle assembleRelease
```

### Automation Script

The included `build_and_deploy.sh` script automates compilation, copies the output APK to Dropbox for immediate phone testing, and commits/pushes changes to GitHub:

```bash
./build_and_deploy.sh "commit message"
```

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.
