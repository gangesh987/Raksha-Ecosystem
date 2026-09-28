# Raksha Video Phase 2 Architecture

Raksha Video is the real calling client. Raksha protection remains a separate future integration.

```text
Android A
  ├─ Compose UI
  ├─ CallViewModel
  ├─ SignalingClient ── WSS ──> FastAPI signaling
  └─ WebRtcEngine
       ├─ Camera / microphone
       ├─ PeerConnection
       ├─ SDP
       └─ ICE/STUN/TURN
                │
                ▼
             Android B
```

The signaling service never carries RTP media. It relays negotiation and participant events. A future SFU can replace the direct peer media path without changing the UI/domain boundaries.

`RakshaProtectionBridge` is deliberately a no-op in Phase 2. Later phases can attach a protection session to the same `callId`.
