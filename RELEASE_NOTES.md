# VerbaNode Android v0.4.1 — Phase 3 Build Integration Fix

## Build fix

- Fixed the Agent editor save path to read the existing agent ID from `JSONObject` with `optInt("id")` instead of the typed-model `.id` accessor.
- Corrected the release packaging strategy so the changed-files ZIP is cumulative from v0.3.8 and includes the Phase 2 transport/streaming dependencies required by the Phase 3 ViewModel. This prevents `ConnectionState`, WebSocket callback, streaming upload/download, and file-export signature mismatches when applying changed files over an older checkout.

## Phase 3 retained

- Type to Talk remains available directly from the Home dashboard.
- Home feature cards remain equal height with one-line title/description truncation.
- `UiState`, `ManagementRepository`, typed management models, and formatting helpers remain separated from `AppViewModel`.
- Phase 1/2 WebSocket, PTT, protocol-error, concurrent-operation, and streaming-transfer hardening remains intact.

## Compatibility

- Recommended with VerbaNode Core v0.9.9.
- No Core change is required for this Android build correction.
