# TURN Setup

Phase 2 supports TURN credentials but does not bundle a TURN server. Use a TURN service or self-hosted coturn.

Set:

```text
WEBRTC_TURN_URL=turn:your-turn-host:3478
WEBRTC_TURN_USERNAME=temporary-user
WEBRTC_TURN_CREDENTIAL=temporary-password
```

Do not commit real credentials. Production credentials should be short-lived and delivered by an authenticated backend endpoint.
