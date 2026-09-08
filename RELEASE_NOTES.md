# VerbaNode Android v0.5.3 — Release + Diagnostics

## Diagnostics

- Structured compatibility card with Android/Core/API/WebSocket/mobile-contract versions.
- Pinned SHA-256 mobile-contract fingerprint is checked before authenticated use and displayed in Diagnostics.
- Connection & Trust card shows the active LAN HTTPS endpoint and abbreviated public TLS SPKI hash only.
- Core health summarizes Audio Engine, AI Engine, pipeline state and queue state.
- Recent Core diagnostic logs are displayed with warning/error filtering and a second Android-side credential redaction pass.
- Self-test results are shown as individual pass/warn/fail checks instead of only raw JSON.
- Clearing the in-memory diagnostic log buffer requires confirmation.
- Diagnostics export explains the privacy boundary and keeps Core's sanitized ZIP as the authoritative report.

## Release hardening

- Production `release` remains unminified for v0.5.3.
- CI additionally builds an unsigned `minifiedRelease` smoke APK with R8 + resource shrinking to catch release-only regressions early.
- Signed-release CI verifies package ID, versionCode 24, versionName 0.5.3, APK signature and SHA-256 checksum.
- Android's canonical mobile-contract fingerprint is regression-tested against Core v0.12.6.

Requires VerbaNode Core v0.12.6+. REST API v1 and WebSocket protocol v1 remain unchanged.
