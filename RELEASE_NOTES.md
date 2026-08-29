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