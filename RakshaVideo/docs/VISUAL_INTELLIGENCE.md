# Visual Intelligence

Phase 4 samples the real WebRTC remote video track at approximately 1 FPS and sends frame metadata to the backend when visual analysis consent is enabled.

Current frame signal:
- `FRAME_SAMPLE`
- width
- height
- rotation
- source

Face detection and liveness are represented by explicit interfaces, but no production model is bundled in Phase 4. Their state must remain `UNAVAILABLE` until a real implementation is connected.

Facial recognition/identity matching is intentionally not implemented.
