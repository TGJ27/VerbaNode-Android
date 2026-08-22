# VerbaNode Android

Native Android management client for VerbaNode.

**Current version:** v0.3.6  
**Required Core:** VerbaNode v0.9.2+  
**Transport:** local-network HTTPS/WSS only

## What it manages

The Android app uses the same VerbaNode Core REST/WebSocket APIs as the web dashboard. It provides mobile management for:

- Dashboard/system state
- Agents
- Chat and Push-to-Talk
- Information/knowledge
- Scripts and playback queue
- Plugins
- Conversation/STT settings
- Host audio devices and tests
- AI/Ollama models and engine actions
- Trusted devices/pairing
- Diagnostics/export
- Backup/restore
- Core/client status

The app intentionally does **not** provide cloud remote access.

## Connection flow

1. Start VerbaNode Core on the Windows PC.
2. Put the phone and PC on the same LAN/Wi-Fi.
3. Open VerbaNode Android.
4. Use **Scan Wi-Fi** (single 6.5-second scan), scan a QR pairing code, select a saved server, or enter the HTTPS address manually.
5. Verify/trust the server identity on first connection.
6. Enter the controller PIN or use a previously trusted-device credential.
7. Manage VerbaNode from the Android dashboard.

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
VerbaNode Core (Windows)
        │
        │ HTTPS / WSS on LAN
        │ REST API v1 + WS v1
        ▼
VerbaNode Android
        │
        ├── Home / system dashboard
        ├── Chat / PTT
        ├── Agents
        └── Management
            ├── Information
            ├── Scripts & Queue
            ├── Plugins
            ├── Settings / Audio / Models
            ├── Devices
            ├── Diagnostics
            └── Data & Recovery
```

Core remains authoritative for AI, audio, plugins, database, device credentials and all management state. The Android app is a client; it does not duplicate backend logic.

### v0.3.6 Type to Talk 500 fix

- Removes the unnecessary `/api/configuration-options` dependency from Type to Talk startup.
- Loads Edge voices directly and safely from Core.
- Adds built-in fallback language and TTS-mode choices.

### v0.3.5 Edge voice selectors and remembered Script config

- Type to Talk and Create/Edit Script now use Core-backed **Edge voice** dropdowns instead of free-text voice entry.
- Edge voice choices are filtered by the selected language and show voice name, locale, and gender.
- New Script dialogs inherit the last speech configuration remembered by Core; normal defaults are used when no prior configuration exists.
- No Core update is required beyond VerbaNode v0.9.2.

### v0.3.4 chat status and Type-to-Talk layout

- Restored live Chat status from Core WebSocket/pipeline events: Recording, Transcribing, Generating, Preparing speech, Speaking, Listening, and Ready.
- Type-to-Talk model configuration is collapsed by default and can be shown/hidden on demand.
- Type-to-Talk history is height-capped instead of expanding to consume the screen.
- Type-to-Talk composer and playback controls stay anchored at the bottom and use IME padding so the typing area remains visible with the keyboard open.

### v0.3.3 direct speech and workflow UX

- Dedicated **Audio** bottom-nav area with broad/common-format uploads handled by Core
- Agent LLM model dropdown populated from shared configuration plus the live installed Ollama model catalog
- **Type to Talk** under More: queue multiple typed announcements directly to Core TTS without LLM processing
- Persistent script speech defaults outside the add dialog so language, TTS mode/voice, rate, and volume are reused
- Home remains focused on Chat, Agents, Plugins, Scripts, Audio, and Diagnostics; Devices/Backup stay under More
- Larger Chat transcript area with Auto-scroll beside the Chat title
- Script queue loop, per-item pause, and drag reorder
- RAG/large-knowledge retrieval is intentionally deferred to a later release
- Type to Talk now uses a chat-style composer/queue with remembered independent TTS settings.
- Audio picking includes MPEG MIME variants and MPEG-family filenames supported by Core v0.9.2.
- Fixed the Android Type-to-Talk/Dashboard compile regressions from the previous work-in-progress patch.

