# VerbaNode Android v0.3.1

Maintenance and mobile UX patch for VerbaNode Core v0.9.0.

## Fixes

- Fixed **Start Conversation** to call Core's real `/api/conversation/start` endpoint instead of creating a new chat record.
- Fixed **Stop Conversation** to cancel Android PTT, cancel browser/mobile PTT on Core, stop continuous conversation mode, and refresh the authoritative mode state.
- Push-to-Talk now works independently of continuous conversation mode, matching the web dashboard behavior.
- Text chat no longer requires continuous conversation mode to be enabled.
- Conversation controls now show clearer enabled/disabled and mode state.
- Replaced the bottom navigation **Agents** shortcut with **Scripts**. Agent management remains available from Home and More.
- Scripts & Queue now uses the main dashboard shell and bottom navigation.
- Added explicit plugin **Refresh** and clarified **Reload external** behavior.
- Added **Windows Default** microphone and speaker choices to Host Audio.
- Audio device rows now show host API, Windows-default, and recommended-device flags.
- Rebuilt the Home **System status** layout as a responsive 2x2 status grid and normalized internal engine mode strings to user-facing states.
- Added global Android safe-drawing insets so the phone status bar and navigation/gesture bar do not cover VerbaNode UI.
- Improved the connection landing page, one-shot scan presentation, discovered-server cards, saved-server cards, and manual address field.
- Bumped Android app version to **v0.3.1**.

## Compatibility

- VerbaNode Core: **v0.9.0**
- REST API: **v1**
- WebSocket protocol: **v1**
- LAN-only; no cloud relay or Internet remote control.

## Apply

Apply this patch over the VerbaNode Android **v0.3.0** source tree and overwrite matching files.

Then run:

```bat
build_apk.bat
```

Debug APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```
