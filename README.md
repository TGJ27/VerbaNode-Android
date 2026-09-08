# VerbaNode Android

## v0.5.3 Release + Diagnostics

Turns the existing Core diagnostics backend into a structured mobile troubleshooting surface: Android/Core compatibility and pinned contract fingerprint, TLS trust identity, Core engine/pipeline health, recent sanitized logs, structured self-test results, and privacy-explicit diagnostics export. CI also builds a separate minified/R8 smoke APK while keeping the production release unminified. Requires Core v0.12.6+.

## v0.5.2 Connection + Architecture

Replaces one-shot mDNS-only discovery with a verified multi-path LAN scan: saved-server probes, mDNS/NSD with resolve retries, active UDP discovery, and a bounded HTTPS subnet fallback. Every candidate is verified through Core's public `/api/client-info` endpoint and TLS identity before it can be used for authentication. Requires Core v0.12.5+ for active UDP discovery; mDNS, saved profiles, subnet fallback, QR pairing, and manual connection remain available.

## v0.5.1 Agent Management + Mobile Parity

Brings the mobile agent editor much closer to the web dashboard: search/refresh states, AI role generation, full Edge/Kokoro speech controls, sampling/context settings, plugin-tool assignment, Knowledge Library assignment, and safer memory/delete confirmations. Also repairs Android CI SDK/report handling. Requires Core v0.12.4+ for the advertised agent-role generation contract.

## v0.5.0 Knowledge Management Phase 2

Adds catalog search and status/source filters, ingestion-job visibility, explicit file reprocessing versus retrieval-only reindexing, destructive-action confirmations, and direct agent ↔ library assignment from the Knowledge screen. Requires Core v0.12.3+ so the existing re-ingest/jobs/agent-library routes are advertised in the mobile contract.

## v0.4.9 Knowledge Management Phase 1

Makes migrated legacy knowledge visible across all libraries on Android instead of only inside the currently selected library. The Knowledge screen now has All / Legacy / Current / Selected views, migration and source counts, explicit refresh/loading/error/empty states, richer source badges and document inspection, while preserving the existing create/upload/edit/reindex management controls. Core v0.12.2 already exposes the required normalized Knowledge APIs, so no Core change is required.

## v0.4.8 Cleanup + release hardening

Streams Audio Library uploads and backup restores directly from Android document storage instead of materializing full files in memory, centralizes file-transfer plumbing outside `AppViewModel`, adds Android lint as a CI/release gate, and verifies signed release APKs with SHA-256 artifacts. R8 remains intentionally disabled until a dedicated minified release/device verification pass.

## v0.4.7 Phase 4 Core ↔ Android contract hardening

Adds an explicit Core ↔ Android compatibility boundary. Android validates the Core v0.12.2+ mobile contract before authentication, checks every REST method/path against the declared operation set, validates critical auth/pairing fields, uses negotiated heartbeat timing, and treats WebSocket 4403/4406 as terminal protocol failures instead of reconnecting forever.

## v0.4.6 Phase 3 test restoration

Restores useful legacy test intent into the canonical `src/test/kotlin` tree and expands coverage for protocol models/API requests, Push-to-Talk WAV encoding, connection reconnect policy, TLS identity validation, and QR pairing parsing. CI now runs JVM unit tests as an explicit gate, publishes their reports, and only builds the APK after the test task succeeds.

## v0.4.5 Phase 2 protocol correctness + Home UX

Successful REST responses are now parsed strictly instead of collapsing malformed JSON into empty objects/arrays. Required client-info, bootstrap, device, session, pairing and WebSocket fields raise typed protocol failures when the Core contract is malformed or incompatible. Type to Talk is now available directly from Home, and feature-card descriptions are constrained to one line with ellipsis so card heights stay aligned.

## v0.4.4 Phase 1 architecture foundation

Production and test Kotlin now each have one canonical source root. Gradle compiles `app/src/main/kotlin`, `app/src/test/kotlin`, and `app/src/androidTest/kotlin` explicitly, so stale Kotlin files left under older `src/*/java` layouts by changed-files overlays cannot be mixed with the current app or test architecture. The v0.4.1 root `clean` lifecycle fix remains included.

Native Android management client for VerbaNode.

**Current version:** v0.5.3  
**Required Core:** VerbaNode v0.12.6+  
**Transport:** local-network HTTPS/WSS only

## What it manages

The Android app uses the same VerbaNode Core REST/WebSocket APIs as the web dashboard. v0.5.3 adds structured diagnostics, compatibility visibility and release smoke gates while preserving the Agent, Knowledge, audio, plugin, settings, diagnostics, and recovery management added in earlier releases.

- Dashboard/system state
- Agents, including Knowledge Library assignments
- Chat and Push-to-Talk
- **Knowledge Libraries and documents**
  - All / Legacy / Current / Selected source views
  - local title/source/library search plus Ready / Processing / Errors and Legacy / Text / Files filters
  - ingestion/re-ingestion job stage and percent visibility
  - separate retrieval-only Reindex and original-file Reprocess actions
  - confirmed library/document deletion with impact text
  - direct agent access toggles on the selected Knowledge Library
  - migrated legacy knowledge visible across every migrated library
  - refresh plus loading/error/empty states
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
  - selected audio uploads stream from document storage instead of being fully buffered in phone memory
- Plugins
- Conversation/STT/TTS settings
- Host audio devices and tests
- AI/Ollama models and engine actions
- Trusted devices/pairing
- Diagnostics/export
- Backup/restore
  - restore ZIPs stream from document storage instead of being fully buffered in phone memory
- Core/client status

The app intentionally does **not** provide cloud remote access and does not perform document parsing, OCR, embeddings or vector search on the phone. Those operations run in VerbaNode Core.

## Connection flow

1. Start VerbaNode Core v0.12.6 or newer on the Windows PC.
2. Put the phone and PC on the same LAN/Wi-Fi.
3. Open VerbaNode Android.
4. Use **Scan Wi-Fi**. Android checks saved servers, mDNS, active LAN broadcast discovery, then a bounded HTTPS subnet fallback for up to 10 seconds. You can also scan a QR pairing code or enter the HTTPS address manually.
5. Verify/trust the server identity on first connection.
6. Enter the controller PIN or use a previously trusted-device credential.
7. Manage VerbaNode from the Android dashboard.

## Source layout

Android v0.5.3 uses explicit canonical Kotlin roots for production and tests: `app/src/main/kotlin`, `app/src/test/kotlin`, and `app/src/androidTest/kotlin`. Older revisions used `src/*/java` for Kotlin, and changed-files overlays could leave obsolete production or test files behind. Gradle now ignores those legacy Kotlin files so an overlay cannot mix incompatible source generations.

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
VerbaNode Core v0.12.6+ (Windows)
        │
        │ HTTPS / WSS on LAN
        │ REST API v1 + WS v1
        ▼
VerbaNode Android v0.5.3
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



### v0.4.7 Phase 4 Core ↔ Android contract hardening

- Requires Core v0.12.2+ and validates its versioned `mobile_contract` before authentication.
- Compares all 108 Android REST method/path operations against Core's advertised route manifest.
- Validates API/WS versions, session header, WebSocket paths, and critical auth/pairing/bootstrap field contracts.
- Rejects undeclared Android REST requests before network transmission.
- Requires negotiated metadata in auth grants and uses Core-provided heartbeat timing for WebSocket keepalive.
- Treats 4403 origin rejection and 4406 protocol incompatibility as terminal contract errors; 4408 heartbeat timeout remains reconnectable.
- Core v0.12.2 regression-tests the manifest against its real FastAPI route table and Pydantic request models.

### v0.4.6 Phase 3 test restoration

- Restores current UI-state coverage derived from the useful pre-isolation tests.
- Expands protocol model tests for compatibility, agent defaults, bootstrap conversation/message parsing and trusted-device defaults.
- Expands REST contract tests for session headers, mobile login fields and malformed conversation responses.
- Expands PTT WAV tests to verify PCM16 mono format fields and empty recordings.
- Adds deterministic reconnect-policy tests, including the 4401 session-loss no-reconnect rule and capped exponential backoff.
- Splits QR pairing-link parsing from Android scanner APIs so pairing validation runs in ordinary JVM unit tests.
- Adds TLS base-URL/SPKI identity validation tests and rejects explicit non-HTTPS base URLs.
- Makes GitHub CI run `testDebugUnitTest` as a distinct gate and publish JUnit/HTML reports before building APK artifacts.
- Keeps VerbaNode Core v0.12.0+ compatibility and requires no Core source changes.


### v0.4.5 Phase 2 protocol correctness + Home UX

- Adds `ApiProtocolException` for malformed successful REST/WebSocket responses.
- Rejects empty, malformed, or wrong top-level JSON response shapes instead of substituting empty `{}`/`[]`.
- Validates required `/api/client-info`, bootstrap, trusted-device, auth-session, pairing-claim, conversation-message and WebSocket fields.
- Validates client-info contract/API/WebSocket versions and certificate SHA-256 identities before accepting the Core connection.
- Surfaces malformed WebSocket envelopes to the UI instead of silently dropping them.
- Restores focused protocol regression tests under the canonical `src/test/kotlin` root.
- Adds Type to Talk directly to Home.
- Keeps Home/More feature-card descriptions to one line with ellipsis for consistent card heights.
- Requires Core v0.12.0+; no Core source changes are required.

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
