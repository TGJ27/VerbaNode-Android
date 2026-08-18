# VerbaNode Android v0.3.2 — Media & Queue UX

This release is designed for **VerbaNode Core v0.9.1**.

## Added

- New **Audio Library** screen under More / Management.
- Upload MP3/WAV files from Android to the VerbaNode host.
- Play, stop, rename, and delete host audio files.
- Agent editor now uses Core-provided dropdown choices for LLM model, language, TTS mode, and STT model.
- Script editor now uses Core-provided dropdown choices for language and TTS mode.
- Script queue now exposes a persistent **Loop** switch.
- Queue items now support configurable pause-after-playback seconds.
- Queue items can be long-pressed and dragged up/down to reorder.
- Chat now gives more screen area to messages and includes an **Auto-scroll** toggle near the composer.

## Update identity hardening

App version and Core version are not used as device identity. Existing trusted-device credentials are preserved when the same Core `instance_id` is rediscovered after an update. If the Core certificate key genuinely changes, Android asks to trust the new TLS identity while retaining the existing paired credential for that same Core instance.

With Core v0.9.1, source-mode Core identity/state also persists outside replaceable source folders, preventing normal Core updates from appearing as a new server.

## Compatibility

- Recommended Core: v0.9.1
- REST API: v1
- WebSocket protocol: v1
- Pairing remains LAN-only.
- The single-active-controller policy is unchanged.
## Release build fixes

- Pins AndroidX Fragment to stable 1.8.9 so Activity Result APIs pass release lint.
- Uses AutoMirrored Material icons for Chat, Send, and Logout to remove current Compose deprecation warnings.
- Includes the self-contained signed APK release builder that reuses the permanent local signing identity.


## Mobile navigation and chat layout revision

- Bottom navigation is now **Home · Chat · Script · Audio · More**.
- Audio Library is directly accessible from the bottom bar instead of being hidden under More.
- Home is intentionally simplified to six primary areas: Chat, Agents, Plugins, Scripts, Audio, and Diagnostics.
- Devices and Backup remain available under More rather than taking Home dashboard space.
- Chat uses a compact top bar and moves the Auto-scroll switch onto the same row as the Chat title.
- Chat conversation controls are shorter and remove redundant helper/subtitle text, giving the transcript more vertical space.
- The empty-chat placeholder is reduced to a compact state so it does not consume the message area.
