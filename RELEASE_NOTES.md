# VerbaNode Android v0.5.2 — Connection + Architecture

## Same-Wi-Fi discovery

- Probes remembered VerbaNode profiles immediately using their saved TLS SPKI.
- Keeps Android NSD/mDNS discovery and retries transient resolve failures instead of silently dropping them.
- Adds active UDP discovery protocol v1 against Core v0.12.5. UDP/mDNS metadata is treated only as a hint.
- Adds a bounded IPv4 HTTPS fallback over the phone's local /24 window when multicast/broadcast discovery does not produce a verified result quickly.
- Uses limited concurrency and short unauthenticated probe timeouts for subnet fallback.
- Deduplicates results by stable Core instance ID, then TLS SPKI, then normalized URL.

## Security

- Every discovery candidate must pass HTTPS `/api/client-info`, Android protocol compatibility checks, and TLS certificate/SPKI verification before it is shown as connectable.
- Discovery never sends a PIN, session token, trusted-device credential, or pairing secret.

## UX / architecture

- Scan window increases from 6.5 to 10 seconds and shows staged progress: saved servers, LAN discovery, subnet fallback, complete.
- Verified results remain visible after the scan.
- mDNS failure is no longer fatal to the entire scan; independent fallback transports continue.
- Connection discovery responsibilities stay isolated in the discovery package instead of growing `AppViewModel`.

## Release

- Version name: `0.5.2`
- Version code: `23`
- Recommended Core: VerbaNode v0.12.5+
- REST API v1 and WebSocket protocol v1 remain unchanged.
