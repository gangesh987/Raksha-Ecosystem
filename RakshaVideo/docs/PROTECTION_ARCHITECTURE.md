# Protection Architecture

Raksha Video separates media from protection:

```
Android Call UI -> WebRTC -> Remote participant
        |
        +-> Protection Bridge -> Protection API/WebSocket -> Raksha backend -> risk events -> Call UI
```

Protection is optional. If the protection backend disconnects, the WebRTC call remains independent.

No raw audio/video is uploaded by the Phase 3 protection client. The client sends structured call/media telemetry and consumes backend events.
