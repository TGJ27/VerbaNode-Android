# VerbaNode Android v0.5.4 — Phase 1: Connection + Home + Navigation + Chat

## Connection entry

- Connection remains outside the authenticated main menu.
- Connect screen is organized into Saved / Scan Wi-Fi / Manual views.
- Existing saved-server probing, mDNS/NSD, active UDP discovery, HTTPS subnet fallback, TLS trust, QR pairing and PIN/pairing-code authentication are preserved.

## Home + navigation

- Large server-status card is replaced by a compact connected-server indicator on Home.
- Home uses the approved equal-height one-line feature cards for Agents, Knowledge, Chat, Type to Talk, Push to Talk, Scripts, Audio, Backups and Diagnostics.
- Home bottom navigation is Home / Agents / Chat / More, matching the approved Phase 1 outlook.
- Type to Talk is described correctly as direct TTS, not agent chat or microphone input.

## Chat / Conversation

- Chat shows the selected active agent.
- Convo Mode is an explicit switch and is described accurately as continuous listening on the Windows host.
- New Chat and Clear Chat actions are visible.
- Composer draft is ViewModel-backed so recomposition does not lose it.
- Failed sends restore the draft and offer Retry.
- A successful send is not marked failed if only the post-send refresh later fails.
- Existing Push-to-Talk remains available in Chat until its dedicated Phase 2 UX pass.

## Compatibility

Requires VerbaNode Core v0.12.6+. REST API v1, WebSocket protocol v1 and mobile contract v1 are unchanged.

## Phase 1 visual hotfix

- Connect / Scan screen visually redesigned with a clearer segmented Saved / Wi-Fi Scan / Manual selector.
- Saved-server and discovered-server rows are now compact, action-oriented connection cards.
- Pairing / Authentication screen now uses a clearer segmented PIN / Enter Code / Scan QR presentation.
- Home feature cards use a more polished equal-height card treatment.
- Chat header, convo-mode section, message composer and retry state have been visually tightened to better match the approved UI/UX outlook.

## Phase 1 replacement UI build (versionCode 26)

- Replaces the previous Connect card stack with a visually distinct connection landing experience.
- Connect now uses a blue hero panel plus dedicated Saved / Wi-Fi Scan / Manual segmented navigation.
- Saved and discovered servers use compact connection rows with leading server icons and right-side Connect actions.
- Trust and Pairing / Authentication screens now use dedicated hero sections and focused authentication cards.
- Pairing supports PIN, short code, and QR scanning directly from the authentication screen.
- Home server status is a compact tappable server row and Home feature cards use the approved lighter card hierarchy.
- Chat uses the approved compact agent / Convo Mode header and a stronger message-composer container.
- Build number is raised to versionCode 26 so this replacement build is distinguishable from the earlier Phase 1 APK.


## Phase 1 exact mock-alignment build (versionCode 27)

- Connect / Scan this Wi-Fi now follows the approved white layout directly: centered Wi-Fi icon/title, Saved / Wi-Fi Scan / Manual underline tabs, compact server rows, right-side Connect actions and the discovery info strip.
- Pairing / Authentication now follows the approved two-tab Enter Code / Scan QR layout with the trusted-connection panel. Controller PIN remains available as a secondary Enter Code option so existing authentication capability is preserved.
- Home now matches the approved Phase 1 outlook with the compact connection line, nine-card two-column grid, Push to Talk entry, and Home / Agents / Chat / More bottom navigation.
- Chat now uses the approved standalone top bar, selected-agent chip, Convo Mode placement, compact message bubbles and pill composer. New/Clear chat, auto-scroll and temporary Push-to-Talk access move into the overflow menu to preserve functionality without changing the default mock layout.
- Core remains v0.12.6; API, WebSocket and mobile-contract versions are unchanged.
- Build number is versionCode 27 so this exact-alignment APK can be distinguished from the earlier versionCode 26 replacement.


## Phase 1 exact-mock compile hotfix (versionCode 28)

- Restored the missing Material3 imports required by the exact-mock Chat UI: `HorizontalDivider` and `TextButton`.
- Fixes the Kotlin compile errors at the Chat top divider and Retry action.
- No behavior or Core contract changes.
- Build number is versionCode 28 so the corrected APK is distinguishable from the broken versionCode 27 package.


## Phase 2 — Type to Talk + Push to Talk UX (versionCode 29)

- Type to Talk is redesigned around its actual direct-TTS behavior: TTS / Voice configuration only, text input, speech queue and Play / Stop / Clear controls.
- Removed the incorrect Model terminology and any agent/listening/transcription concepts from Type to Talk.
- Added a dedicated Push to Talk screen using the existing browser-PTT pipeline.
- Push to Talk now presents Ready / Listening / Transcribing / Sending / Waiting / Speaking as user-facing states.
- Push to Talk shows the selected agent, active-agent STT usage, latest transcript and latest response.
- Home Push to Talk now opens the dedicated PTT screen.
- Chat overflow opens the dedicated PTT screen instead of embedding voice controls.
- Removed the unsupported Chat attachment button entirely.
- Core remains v0.12.6; API v1, WebSocket v1 and mobile contract v1 are unchanged.


## Phase 3 — Management Screens + Chat Status (versionCode 32)

- Agents is simplified into a searchable list with a clear active-agent indicator and a dedicated Agent Details action.
- Agent Details terminology is aligned around Model / Voice / STT, Tools / Plugins, Knowledge Libraries, role/system prompt and destructive actions.
- Knowledge keeps All / Legacy / Current / Selected scopes and search/status/source filters while using more compact source/status presentation.
- Scripts now has explicit Queue / Scripts tabs; playback controls and saved scripts are separated visually.
- Audio adds All / Uploaded / Generated views, compact playback state and file actions.
- Backups / Restore now presents separate Create Backup / Restore Backup cards plus the real recent Core recovery snapshots.
- Chat adds a compact status dot and user-facing pipeline label: Offline / Ready / Listening / Transcribing / Thinking / Speaking.
- Core remains v0.12.6; API v1, WebSocket v1 and mobile contract v1 are unchanged.
- Build number is versionCode 32.


## Phase 3 UI/fullscreen hotfix

- Agent actions use a responsive two-column layout so destructive and memory/backup actions never collapse into vertical text.
- Agent descriptions are constrained to one line with ellipsis on narrow phones.
- Script queue transport controls use compact icons with single-line labels for Play, Pause, Stop and Loop.
- Management screens use consistent 16 dp horizontal margins and tighter section spacing.
- VerbaNode now runs in immersive fullscreen: status and navigation bars are hidden, with transient swipe-to-reveal behavior and an API 23+ fallback.


### Phase 3 compile/test hotfix

- ImmersiveModeSourceTest now locates MainActivity.kt correctly whether Gradle runs tests from the app module or repository root.
- Replaced deprecated VolumeUp icons with the AutoMirrored variant.
- Hotfix build number: versionCode 32.


## Phase 4 — Diagnostics, Settings & final polish

- Removed the reserved status-bar/safe-drawing band so immersive fullscreen reaches the physical top edge.
- Main, Chat, and management scaffolds now use zero system-bar content insets while transient swipe-to-reveal system bars remain enabled.
- Settings keeps the current product sections only: Conversation, Audio, AI / Models, and Runtime.
- Settings section navigation is compact, horizontally scrollable, single-line, and keyboard-aware.
- Diagnostics adds a compact system overview for Android/Core version, connection, TLS identity, and contract health while retaining compatibility, trust, health, self-test, sanitized logs, and export controls.
- Final layout consistency uses 16 dp horizontal content padding and Material touch targets.
- Android build number is 33; versionName remains 0.5.4.

## v0.5.4 navigation consistency patch (versionCode 34)

- Standardized the primary bottom navigation to Home / Chat / Scripts / Audio / More across the main app shell.
- Chat now keeps the shared bottom navigation visible; More remains selected for child management screens such as Agents and Knowledge.
- Agents and Knowledge are surfaced from More, while Scripts and Audio remain first-class bottom-navigation destinations.
- Home is the stable root after configuration: losing the Core/controller session returns to Home in a disconnected state instead of forcing the Login screen.
- Home navigation no longer depends on a live API session before the screen can be shown.
- Android system Back returns secondary main screens to Home; Back from Home retains the normal system exit/minimize behavior.
- Added navigation-policy regression coverage for tab ordering, More-child selection, Back behavior, and session-loss routing.
- Core/API/WebSocket/mobile contract versions are unchanged.
- Android build number is 34; versionName remains 0.5.4.
