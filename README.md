# VerbaNode Android

Native Android management client for VerbaNode.

**Current version:** v0.3.2  
**Required Core:** VerbaNode v0.9.1+  
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

### v0.3.2 media and queue UX

- Host Audio Library management (MP3/WAV upload, play, stop, rename, delete)
- Core-provided dropdowns for agent/script model and language configuration
- Stable paired-device identity across Android/Core version updates
- Larger chat area with Auto-scroll toggle
- Script queue loop, per-item pause, and drag reorder

