# VirueditX

**Create. Convert. Simplify.**

A premium, clean and simple image / PDF / document conversion studio for Android — iOS-inspired design, a strong on-device conversion engine, and zero uploads. Your files never leave your phone.

- **Works on all Android phones:** Android 7.0 (API 24) up to Android 15 (API 35)
- **Fast & safe:** no accounts, no ads, no storage permissions — files come in only through the system picker, and the app works fully offline
- **Avatar branding:** Virendra's Viru avatar is the app's profile image (home top bar + About) and stars on the app poster

---

## Clean, user-friendly design (a core requirement)

- Lots of whitespace, one clear main action per screen, restrained colours (off-white surfaces, charcoal text, one indigo accent)
- Big, labelled buttons (56dp) in everyday words — "Select Files", "Create PDF", "Done"
- Every tool reads as 3 obvious steps: **1 Pick → 2 Choose (optional) → 3 Make it**
- Sensible defaults pre-chosen (A4 pages, 90% quality, "Under 1 MB" ready) — advanced settings live in an optional settings sheet, never blocking the main button
- Errors in plain language ("This file is locked with a password") with what to do next
- Friendly empty states, one-word bottom navigation: **Home · Tools · Files · Settings**
- Light, Dark and "Use phone setting" themes, remembered between launches

---

## Features

### Working tools (real conversions, verified by the engine before success is shown)

| Tool | What it does |
|---|---|
| **Image to PDF** | Multi-photo studio: pick or photograph images, tick/untick, reorder (drag or arrows), rotate, remove; A4 / Letter / Fit page size, portrait/landscape/auto, margins, quality, custom file name → genuine multi-page PDF |
| **PDF to Images** | Renders every page of a PDF to JPG / PNG / WEBP photos |
| **Image Converter** | JPG · PNG · WEBP · BMP (incl. HEIC photos decoded on phones that support them) |
| **Make it under 1 MB** ⭐ | For college & form uploads. PDF: cleans, then shrinks embedded photos pass by pass, checking the **real size after every pass** until ≤ 1,000,000 bytes. Photo: steps JPEG quality down, then size, until ≤ 1 MB. If 1 MB is truly impossible, you get the smallest file and the result screen says so honestly — *"Smallest possible: X MB — could not reach 1 MB"* — and shows a green **"✓ Under 1 MB"** when the target is met |
| **PDF Compressor** | Re-saves a PDF stripped of metadata bloat (and the "Under 1 MB" mode above) |
| **Merge PDFs** | Join 2+ PDFs in the order you choose |
| **Split PDF** | Take out pages by range, e.g. `1,3-5` → one new PDF |
| **Scan to PDF** | Camera capture straight into the Image-to-PDF studio |
| **Text to PDF** | A `.txt` file laid out as a clean multi-page PDF |

Plus: **PDF preview** (page by page, zoom levels), **image preview**, a **Files** manager (search, filter, sort, rename, delete with confirmation, share/open), **History** of past conversions (opens the output; moved/deleted files are labelled, never a crash), and a success screen ("Your file is ready") showing name, **final size** and page count with Open / Share / Save Another Copy / Convert More.

### Honestly not available (never faked)

| Tool | Why |
|---|---|
| Word (DOCX) → PDF | Needs a large computer-grade office engine; no honest on-device engine exists. Shown in the app with a clear "Not available yet" note. |
| PDF → Word (DOCX) | Same reason — shown as not available on-device yet. |

Encrypted/password PDFs and corrupted files produce plain-language failures, not crashes.

---

## Architecture — a strong engine under the clean UI

Strict MVVM separation — **no conversion code lives in any screen**:

```
UI (Compose screens)  →  ToolViewModel  →  ViruEngine  →  Converters / PdfBox / Android PDF
     clean & simple         state + jobs        typed results      pure functions
```

- **`engine/`** — the conversion backend, fully on-device:
  - `Converters.kt` — pure, testable operations (input + options → output file): `PdfDocument` for image→PDF & text→PDF, `PdfRenderer` for PDF→images & preview, **PdfBox-Android** for merge / split / compress, a hand-written BMP encoder, EXIF-aware image decoding with sampling (50MB+ photos and 500-page PDFs are processed page-by-page with bitmaps recycled — no freezing, no OOM)
  - `Engine.kt` — `ViruEngine` facade returning typed `EngineResult.Success / Failure` (plain-language reasons). **Every output is verified before success**: re-opened, size > 0, PDF page count checked (and matched to the expected count), images must decode. Failed verification deletes the partial file and reports failure — the success screen can never be shown falsely
  - Cooperative cancellation, temp-file cleanup on failure, all work on `Dispatchers.IO/Default`
- **`ui/`** — `App.kt` (all screens), `Theme.kt` (light/dark design system), `ToolViewModel.kt` (the only bridge from UI to engine)
- **`data/`** — `HistoryStore`, a small local JSON store for conversion history
- Scoped storage only: Photo Picker / Storage Access Framework in, FileProvider out. **The app declares no permissions at all.**

### Versions (all stable, from google() / mavenCentral())

| Piece | Version |
|---|---|
| Android Gradle Plugin | 8.6.1 |
| Kotlin (+ Compose compiler plugin) | 2.0.20 |
| Compose BOM | 2024.09.00 (Material 3) |
| Gradle (CI) | 8.9 |
| JDK | 17 (Temurin) |
| compileSdk / targetSdk | 35 (Android 15) |
| minSdk | 24 (Android 7.0) |
| PdfBox-Android | 2.0.27.0 (`com.tom-roush:pdfbox-android`) |
| Navigation Compose | 2.7.7 |
| Activity Compose / Lifecycle | 1.9.2 / 2.8.4 |

---

## Build the APK online (GitHub Actions — stage 2)

No Android SDK on your computer is needed. The workflow in `.github/workflows/build-apk.yml`:

1. Push this project to a GitHub repository (branch `main`)
2. Open the repo's **Actions** tab — the "Build VirueditX APK (online)" workflow runs on push, or start it manually with **Run workflow**
3. It installs JDK 17 + Gradle 8.9 + Android platform 35 on the runner, runs `gradle assembleDebug`, and uploads the APK as an artifact named **VirueditX-apk**
4. Download the artifact from the finished run, unzip, and install `app-debug.apk` on any Android phone (allow "install from this source" if Android asks — the APK is debug-signed, which is installable on every phone)

> Notes: CI uses the `gradle` provided by `gradle/actions/setup-gradle`, so the wrapper JAR is not required. To build locally instead, open the project in Android Studio (it will offer to generate the Gradle wrapper and download SDK 35), or run `gradle wrapper --gradle-version 8.9` once, then `./gradlew assembleDebug`.

## Project structure

```
virueditx/
├── .github/workflows/build-apk.yml   # online APK build
├── settings.gradle.kts / build.gradle.kts / gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml        # no permissions; FileProvider declared
        ├── java/com/virueditx/app/
        │   ├── MainActivity.kt
        │   ├── engine/  Converters.kt · Engine.kt
        │   ├── ui/      App.kt · Theme.kt · ToolViewModel.kt
        │   └── data/    HistoryStore.kt
        └── res/  (adaptive icon: document + X · avatar.jpg · themes · file_paths)
```

## Changing the avatar or icon

- Avatar: replace `app/src/main/res/drawable-nodpi/avatar.jpg` (square works best; the app crops it to a circle)
- Icon: edit `app/src/main/res/drawable/ic_launcher_foreground.xml` (adaptive icon) — legacy PNG icons live in the `mipmap-*` folders
