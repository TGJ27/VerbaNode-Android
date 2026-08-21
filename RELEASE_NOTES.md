# VerbaNode Android v0.3.3 — Direct Speech & Workflow UX


- Type to Talk now uses a chat-style direct-speech screen with Send/keyboard-send behavior, shared queued transcript, and TTS language/engine/voice/rate/volume controls.
- Script speech controls are back inside Create/Edit Script. New scripts are prefilled from the last script configuration saved on Core; when no previous configuration exists, normal defaults are used.
This release is designed for **VerbaNode Core v0.9.2+**. RAG/large-knowledge retrieval is intentionally deferred.

## Navigation and chat

- Bottom navigation remains **Home · Chat · Script · Audio · More**.
- Home stays intentionally compact: Chat, Agents, Plugins, Scripts, Audio, and Diagnostics.
- Devices and Backup remain under More.
- Chat keeps the larger transcript layout with Auto-scroll beside the Chat title.

## Agent configuration

- Agent LLM model is a real dropdown.
- Choices combine `/api/configuration-options` with the live installed-model catalog from Core so models are not lost when the static/shared list is incomplete.
- Language, STT, and TTS selectors continue to use Core-provided options.

## Type to Talk

- Added a Type-to-Talk area under More.
- Typed text is queued directly to Core TTS and does not pass through the LLM.
- Multiple entries can be added while speech is already playing.
- The shared Core queue can be played, stopped, cleared, removed, and reordered from Android.

## Script authoring

- Moved reusable speech configuration outside the create-script dialog.
- Language, TTS mode/voice, speech rate, and volume are saved as persistent defaults.
- New scripts inherit the last saved configuration instead of resetting after every entry.
- Editing an existing script preserves its saved speech configuration.

## Audio Library

- Audio remains a primary bottom-nav section.
- The picker accepts generic audio files instead of being limited to MP3/WAV.
- Core v0.9.2 advertises/accepts common formats including WAV, MP3, FLAC, OGG/OGA, Opus, M4A, AAC, WMA, AIFF/AIF, WebM audio, MKA, and AMR.

## Compatibility

- Required Core: v0.9.2+
- REST API: v1
- WebSocket protocol: v1
- Pairing remains LAN-only and the single-active-controller policy is unchanged.
- Existing trusted-device identity is not based on application version.

## Release build note

The Android source changes are statically validated here, but the final Gradle compile/release APK build must be run on the Windows Android build machine because this environment cannot download the Gradle distribution/dependencies.

- Audio upload picker now exposes MPEG audio/container MIME types and uploads `.mpeg`, `.mpg`, `.mpga`, and `.mp2` files to Core v0.9.2.
- Fixed the Type-to-Talk Compose build issue caused by importing the internal `weight` extension directly.
