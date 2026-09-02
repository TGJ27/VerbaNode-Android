# VerbaNode Android

## v0.4.4 Phase 1 architecture foundation

Production and test Kotlin now each have one canonical source root. Gradle compiles `app/src/main/kotlin`, `app/src/test/kotlin`, and `app/src/androidTest/kotlin` explicitly, so stale Kotlin files left under older `src/*/java` layouts by changed-files overlays cannot be mixed with the current app or test architecture. The v0.4.1 root `clean` lifecycle fix remains included.

Native Android management client for VerbaNode.

**Current version:** v0.4.4  
**Required Core:** VerbaNode v0.12.0+  
**Transport:** local-network HTTPS/WSS only

## What it manages

The Android app uses the same VerbaNode Core REST/WebSocket APIs as the web dashboard. v0.4.4 includes full mobile management for the Hybrid RAG Knowledge Engine while keeping Core authoritative for parsing, indexing, retrieval, AI, audio, plugins, database state and device credentials.

- Dashboard/system state
- Agents, including Knowledge Library assignments
- Chat and Push-to-Talk
- **Knowledge Libraries and documents**
  - migrated legacy knowledge
  - create/edit manual text knowledge
  - upload mixed document files to Core
  - large uploads are streamed from Android storage instead of loaded fully into phone memory
  - inspect normalized chunks
  - delete/reindex documents
  - rebuild a selected Knowledge index
  - test retrieval/confidence before using it in Chat
  - see background dense-index progress
- Scripts and playback queue
- Audio Library and Type to Talk
- Plugins
- Conversation/STT/TTS settings
- Host audio devices and tests
- AI/Ollama models and engine actions
- Trusted devices/pairing
- Diagnostics/export
- Backup/restore
- Core/client status

The app intentionally does **not** provide cloud remote access and does not perform document parsing, OCR, embeddings or vector search on the phone. Those operations run in VerbaNode Core.

## Connection flow

1. Start VerbaNode Core v0.12.0 or newer on the Windows PC.
2. Put the phone and PC on the same LAN/Wi-Fi.
3. Open VerbaNode Android.
4. Use **Scan Wi-Fi** (single 6.5-second scan), scan a QR pairing code, select a saved server, or enter the HTTPS address manually.
5. Verify/trust the server identity on first connection.
6. Enter the controller PIN or use a previously trusted-device credential.
7. Manage VerbaNode from the Android dashboard.

## Source layout

Android v0.4.4 uses explicit canonical Kotlin roots for production and tests: `app/src/main/kotlin`, `app/src/test/kotlin`, and `app/src/androidTest/kotlin`. Older revisions used `src/*/java` for Kotlin, and changed-files overlays could leave obsolete production or test files behind. Gradle now ignores those legacy Kotlin files so an overlay cannot mix incompatible source generations.

## Build debug APK

Install Android Studio/JDK/Android SDK, then run:

```bat
build_apk.bat
```

The APK is produced at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Build signed release APK

Configure these environment variables:

```text
VN_KEYSTORE_PATH
VN_KEYSTORE_PASSWORD
VN_KEY_ALIAS
VN_KEY_PASSWORD
```

Then run:

```bat
build_release_apk.bat
```

## Architecture

```text
VerbaNode Core v0.12.0+ (Windows)
        │
        │ HTTPS / WSS on LAN
        │ REST API v1 + WS v1
        ▼
VerbaNode Android v0.4.4
        │
        ├── Home / system dashboard
        ├── Chat / PTT
        ├── Agents
        └── Management
            ├── Knowledge Libraries
            ├── Scripts & Queue
            ├── Audio / Type to Talk
            ├── Plugins
            ├── Settings / Audio / Models
            ├── Devices
            ├── Diagnostics
            └── Data & Recovery
```

Core remains authoritative. Android is a management client and never duplicates the Hybrid RAG backend.


### v0.4.4 Phase 1 architecture foundation

- Extracts `AppScreen` and `MobileUiState` from `AppViewModel.kt` into `UiState.kt`.
- Extracts shared error and pipeline/mode status formatting into `UiFormatting.kt`.
- Adds canonical Kotlin unit coverage for the extracted status/error behavior.
- Removes obsolete `app/src/main/java` and `app/src/test/java` Kotlin trees from the clean repository.
- Keeps explicit canonical source-set isolation so changed-files overlays remain safe even if an older working tree still contains legacy files.
- Makes no VerbaNode Core API, pairing, TLS, Knowledge, audio, or management behavior changes in Phase 1.

### v0.4.3 build isolation and Hybrid RAG Knowledge management

- Replaces the retired Information management surface with Knowledge Libraries.
- Lists migrated legacy knowledge as ordinary Knowledge documents.
- Creates/edits manual text documents and uploads files into the selected library.
- Inspects normalized document chunks and metadata returned by Core.
- Deletes/reindexes documents and rebuilds the selected library index.
- Adds a retrieval test with confidence/source preview before Chat consumes the evidence.
- Shows background dense-index progress while BM25 remains available.
- Adds explicit Knowledge Library assignment in the Agent editor.
- Requires Core v0.12.0+ and its `knowledge_management` client capability.

### v0.3.6 Type to Talk 500 fix

- Removed the unnecessary `/api/configuration-options` dependency from Type to Talk startup.
- Loaded Edge voices directly and safely from Core.
- Added built-in fallback language and TTS-mode choices.

### v0.3.5 Edge voice selectors and remembered Script config

- Type to Talk and Create/Edit Script use Core-backed Edge voice dropdowns.
- New Script dialogs inherit the last speech configuration remembered by Core.

### v0.3.4 chat status and Type-to-Talk layout

- Restored live Chat processing status.
- Made Type-to-Talk model configuration collapsible.
- Kept the composer anchored while capping history height.
