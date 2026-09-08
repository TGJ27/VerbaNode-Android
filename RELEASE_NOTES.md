# VerbaNode Android v0.5.1 — Agent Management + Mobile Parity

## Agent workspace

- Adds search and Active-only filtering with explicit refresh, loading, error, empty, and no-match states.
- Expands agent editing to full identity, LLM, STT/TTS, Kokoro voice, speech rate/volume, sampling, token/context, tools, and Knowledge Library controls.
- Adds AI-assisted role/system-prompt/greeting generation through Core's existing `/api/agents/generate-role` endpoint.
- Uses Core configuration choices for Edge/Kokoro voices and model/STT selections instead of free-form voice entry.
- Shows per-agent Knowledge/tool counts and keeps activation and backup actions on the agent card.
- Adds explicit confirmations before clearing memory or deleting an agent, with the retained/deleted data called out.

## Core contract and CI

- Requires VerbaNode Core v0.12.4+ so AI role generation is advertised in the versioned mobile contract.
- Installs the API-37 SDK package as `platforms;android-37.0` in GitHub Actions.
- Updates checkout, Java, Android setup, Gradle setup, and artifact-upload actions to current Node-24-native majors.
- Uses the repository Gradle wrapper in CI.
- Unit-test/lint report uploads warn rather than fail when an earlier build step produced no report files.

## Release

- Version name: `0.5.1`
- Version code: `22`
- REST API v1 and WebSocket protocol v1 remain unchanged.
