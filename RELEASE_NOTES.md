# VerbaNode Android v0.3.8 — Phase 1 Build Compatibility Fix

## Fixed

- Fixes `compileDebugKotlin` failures in `VerbaNodeApi.kt` caused by use of kotlinx.coroutines internal APIs (`tryResume`, `tryResumeWithException`, and `completeResume`).
- The cancellable browser-PTT request now completes via the stable public Kotlin `Continuation.resumeWith(Result)` API while `invokeOnCancellation` continues to cancel the underlying OkHttp call.
- Retains all Phase 1 networking/PTT lifecycle hardening introduced in v0.3.7.

## Build note

Warnings that Gradle cannot strip `libandroidx.graphics.path.so` or `libdatastore_shared_counter.so` are non-fatal Android packaging warnings and are unrelated to the Kotlin compilation error fixed here.

## Compatibility

The client remains API-compatible with VerbaNode Core v0.9.2+, with **Core v0.9.7 recommended** for the coordinated host-PTT/WebSocket disconnect safety fix and structured unexpected-error diagnostics.
