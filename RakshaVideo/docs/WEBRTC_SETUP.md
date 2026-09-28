# WebRTC Setup

## Dependency

Phase 2 uses `org.webrtc:google-webrtc:1.0.32006`. The dependency is intentionally isolated in the Android module so the WebRTC provider can be changed later.

## Media

`LocalMediaManager` captures the front camera and microphone. `WebRtcEngine` creates the peer connection, adds local tracks, handles SDP, and forwards remote video tracks to the Compose renderer.

## STUN/TURN

The current development configuration uses Google public STUN by default. Production calls should provision TURN credentials from a trusted backend and return short-lived credentials from `/api/webrtc/config`.
