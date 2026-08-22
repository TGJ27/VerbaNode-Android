# VerbaNode Android v0.3.6 — Type to Talk 500 Fix

## Fixed

- Type to Talk no longer depends on `/api/configuration-options` just to open the screen.
- Edge voice choices are loaded directly from Core via `/api/tts/edge-voices`.
- A failure while loading the Edge voice catalogue is isolated and will not prevent Type to Talk from opening.
- Language and TTS mode selectors now have built-in fallback choices, so the screen remains usable even if auxiliary configuration metadata is unavailable.

## Compatibility

- VerbaNode Core v0.9.2 remains compatible and does not require a code change for this Android fix.
