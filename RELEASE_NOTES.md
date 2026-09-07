# VerbaNode Android v0.5.0 — Knowledge Management Phase 2

## Catalog and ingestion UX

- Adds local source search across title, source filename/type, and owning library.
- Adds Ready / Processing / Errors status filters and Legacy / Text / Files source filters.
- Loads Core ingestion jobs with the Knowledge catalog and shows the latest job type, stage, status, and percentage on each source.
- Separates retrieval-only **Reindex** from original-file **Reprocess**; reprocessing is disabled for text/legacy sources without a stored source file and while another ingestion job is active.

## Safety and agent access

- Library and document deletion now require explicit confirmation and explain the impact before the destructive request is sent.
- The selected library exposes direct agent access toggles using Core's dedicated Knowledge permission endpoint instead of rewriting whole agent configurations.
- Knowledge refresh keeps library document/agent counts, jobs, source catalog, and raw agent assignments synchronized.

## Version / compatibility

- Bumps Android version code to 21 and version name to 0.5.0.
- Requires VerbaNode Core v0.12.3+ so the existing re-ingest, jobs, and agent-library routes are advertised in the mobile contract.
- REST API remains v1 and WebSocket protocol remains v1.

# VerbaNode Android v0.4.9 — Knowledge Management Phase 1

## Knowledge catalog

- Loads the complete normalized Knowledge document catalog from Core, then derives the selected-library view locally.
- Adds All, Legacy, Current, and Selected-library filters so migrated legacy entries remain visible even when migration created multiple libraries.
- Shows total/current/legacy/selected source counts and Core legacy-migration/index status.
- Labels migrated legacy, packaged defaults, manual text, and uploaded file types clearly in the mobile list.

## Read-path UX

- Opens the Knowledge screen before its refresh completes so users see a real loading state instead of waiting on the previous screen.
- Adds explicit Knowledge refresh, loading, refresh-error, and empty states.
- Shows the owning library on every document when browsing across libraries.
- Expands document inspection with source, library, status, chunk totals, parent-block totals, and ingestion errors.
- Keeps the existing create/upload/edit/reindex/delete and retrieval-test controls available; this phase does not change Core retrieval behavior.

## Version / compatibility

- Bumps Android version code to 20 and version name to 0.4.9.
- Keeps the Core requirement at VerbaNode v0.12.2+.
- No VerbaNode Core source, endpoint, database, or protocol change is required.

# VerbaNode Android v0.4.8 — Cleanup + Release Hardening

## Streaming / memory safety

- Audio Library uploads now stream selected files from Android document storage into OkHttp multipart requests instead of calling `readBytes()`.
- Backup restore now streams the selected ZIP directly into the multipart request instead of holding the complete archive in a `ByteArray`.
- Knowledge uploads, audio uploads, and backup restore share one lazy stream request-body implementation that opens the source only when the HTTP body is actually written.
- Selected document metadata (display name, MIME type, and optional length) is resolved without opening the full file.

## Architecture cleanup

- Moves file-transfer/session plumbing out of `AppViewModel` into `ContentTransferCoordinator`.
- Centralizes stream-copy behavior in `StreamUploadSource` with focused regression coverage.
- Keeps Core authoritative for upload size enforcement and restore validation; no Core endpoint or protocol change is required.

## Release hardening

- Adds `lintDebug` to normal CI and publishes lint reports.
- Adds `lintRelease` before signed release builds and publishes release lint reports.
- Signed GitHub release builds verify the APK with `apksigner`, produce a versioned APK name, and publish a SHA-256 checksum.
- Local debug/release build helpers now run lint as part of their gating sequence.
- R8/minification remains disabled intentionally until a dedicated minified release/device verification pass.

## Version / compatibility

- Bumps Android version code to 19 and version name to 0.4.8.
- Keeps the Core requirement at VerbaNode v0.12.2+.
- No VerbaNode Core source changes are required.

# VerbaNode Android v0.4.7 — Phase 4 Core ↔ Android Contract Hardening

## Contract negotiation

- Requires the versioned `mobile_contract` published by VerbaNode Core v0.12.2+.
- Validates Core API compatibility, WebSocket protocol, session header, WebSocket paths, and all 108 Android REST method/path operations before authentication.
- Validates critical auth, trusted-device, pairing, bootstrap, and WebSocket request/response field requirements against the advertised manifest.
- Every Android REST request is checked against the local declared contract before it is sent, including dynamic route templates.

## Auth / WebSocket hardening

- Auth grants now require negotiated Core/API/WebSocket metadata and server heartbeat settings instead of accepting a token-only success response.
- WebSocket heartbeat scheduling uses the Core-negotiated interval.
- Core close code 4401 remains a terminal session-loss condition.
- Core close codes 4403 (origin rejected) and 4406 (unsupported WebSocket protocol) are terminal protocol errors instead of entering an endless reconnect loop.
- Heartbeat timeout 4408 and ordinary transient network closes remain reconnectable.

## Core-side protection

- VerbaNode Core v0.12.2 publishes the mobile contract from `/api/client-info`.
- Core regression tests verify advertised mobile operations against the actual FastAPI route table and authoritative request models.
- Existing clients that ignore the added manifest remain compatible with Core v0.12.2.

## Version / compatibility

- Bumps Android version code to 18 and version name to 0.4.7.
- Requires VerbaNode Core v0.12.2 or newer.
- REST API remains v1 and WebSocket protocol remains v1.

# VerbaNode Android v0.4.6 — Phase 3 Test Restoration

## Restored and expanded tests

- Restores useful legacy state/model test intent into the canonical `app/src/test/kotlin` source set instead of reviving obsolete `src/test/java` contracts.
- Expands strict protocol-model and REST request/response tests.
- Expands PTT WAV encoding coverage.
- Adds connection reconnect/backoff and session-loss policy tests.
- Adds JVM-testable TLS identity/base-URL validation.
- Splits QR pairing-link parsing from Android scanner code and adds pairing validation tests.

## CI hardening

- Runs `testDebugUnitTest` as an explicit CI gate before building the debug APK.
- Always publishes unit-test XML/HTML reports so failures remain inspectable.
- Keeps unit tests as an explicit gate in the signed release workflow.

## Version / compatibility

- Bumps Android version code to 17 and version name to 0.4.6.
- Keeps the Core requirement at VerbaNode v0.12.0+.
- No VerbaNode Core source or endpoint changes are required.

# VerbaNode Android v0.4.5 — Phase 2 Protocol Correctness + Home UX

## Protocol correctness

- Adds typed `ApiProtocolException` failures for malformed successful REST/WebSocket responses.
- Stops substituting empty `{}`/`[]` when successful response JSON is empty, malformed, or the wrong top-level type.
- Validates required client-info, bootstrap, device, auth-session, pairing, conversation-message and WebSocket fields.
- Validates client-info contract/API/WebSocket versions plus advertised certificate SHA-256 identities before accepting compatibility.
- Surfaces malformed WebSocket envelopes in the Android UI instead of silently dropping them.
- Adds focused protocol regression tests in the canonical Kotlin test source root.

## Home UX

- Adds **Type to Talk** directly to the Home dashboard.
- Constrains feature-card descriptions to a single line with ellipsis so dashboard/management cards keep consistent heights.

## Version / compatibility

- Bumps Android version code to 16 and version name to 0.4.5.
- Keeps the Core requirement at VerbaNode v0.12.0+.
- No VerbaNode Core source or endpoint changes are required.

# VerbaNode Android v0.4.4 — Phase 1 Architecture Foundation

## Architecture and testability

- Moves `AppScreen` and `MobileUiState` out of the 1,098-line `AppViewModel.kt` into canonical `UiState.kt`.
- Moves shared error, pipeline-stage and mode-label formatting into canonical `UiFormatting.kt`.
- Adds focused unit tests for conversation-aware idle status, known/unknown pipeline stages, PTT/conversation modes and error fallback text.
- Removes obsolete Kotlin source/test trees under `app/src/main/java` and `app/src/test/java` from the clean repository.
- Retains explicit canonical Gradle Kotlin source roots for overlay safety.
- Bumps Android version code to 15 and version name to 0.4.4.
- Updates CI/release artifact names to v0.4.4.

## Compatibility

- Requires VerbaNode Core v0.12.0 or newer, unchanged from v0.4.3.
- No Core API or protocol changes are required for Phase 1.
- Pairing, TLS trust, Knowledge management, Chat/PTT, Scripts, Audio and management behavior remain unchanged.

# VerbaNode Android v0.4.3

## Overlay-safe unit-test source fix

- Production Kotlin remains isolated to `app/src/main/kotlin`.
- Unit-test Kotlin is now isolated to `app/src/test/kotlin`.
- Instrumentation-test Kotlin is isolated to `app/src/androidTest/kotlin`.
- Moves the canonical `PttRecorderTest` into the new unit-test Kotlin root.
- Prevents stale tests left under older `app/src/test/java` layouts from being compiled after a changed-files overlay.
- Fixes `compileDebugUnitTestKotlin` failures for retired symbols such as `ConnectionState`, `ApiProtocolException`, `parseScriptItems`, and other pre-v0.4 contracts.
- Keeps the v0.4.2 production source isolation and v0.4.1 root `clean` fix.
- Keeps all Phase 7 Hybrid RAG Knowledge management functionality unchanged.
- Requires VerbaNode Core v0.12.0 or newer.

# VerbaNode Android v0.4.2

## Build source-set compatibility fix

- Moves the canonical production Kotlin sources to `app/src/main/kotlin`.
- Configures AGP built-in Kotlin to compile only that canonical main source root.
- Prevents stale Kotlin files left under the former `app/src/main/java` layout from older changed-files overlays from entering compilation.
- Fixes the cascade of unresolved references such as `ApiProtocolException`, `ScriptItem`, `AppViewModel`, and `VerbaNodeApi` seen when older refactor files remained in a working repository.
- Keeps all v0.4.1 Phase 7 Knowledge management and the root `clean` lifecycle fix unchanged.
- Requires VerbaNode Core v0.12.0 or newer.

## Build reliability fix

- Restores the root Gradle `clean` lifecycle task by applying Gradle's `base` plugin.
- Root `clean` removes both the root build directory and `app/build`.
- Fixes `build_apk.bat` and `build_release_apk.bat` failing with `Task 'clean' not found`.
- Keeps all v0.4.0 Phase 7 Knowledge management functionality unchanged.


## Added

- Full **Knowledge Libraries** management against VerbaNode Core v0.12.0+.
- View migrated legacy knowledge as ordinary documents inside the libraries created by Core migration.
- Create/edit manual text knowledge without returning to the retired Information system.
- Upload documents from Android to the selected Knowledge Library; parsing/OCR/indexing remains on Core.
- Stream selected Knowledge files to Core instead of loading the entire file into phone memory, allowing large-document uploads without a large ByteArray allocation.
- Inspect normalized document chunks and source metadata.
- Delete or reindex individual documents and rebuild the selected library index.
- Run retrieval tests and inspect confidence/top source hits before Chat uses the knowledge.
- Display Core background dense-index progress while lexical BM25 retrieval remains available.
- Assign Knowledge Libraries explicitly from the Agent editor.
- React to Core `knowledge_changed` events so Knowledge/Agent views refresh after management changes.

## Compatibility

- Requires **VerbaNode Core v0.12.0+** with `knowledge_management` capability.
- No VLM runs on Android or Core in this phase.
- Existing LAN discovery, trusted-device pairing, Chat/PTT, Scripts, Audio, Type to Talk and diagnostics remain available.