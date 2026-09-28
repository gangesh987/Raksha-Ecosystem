# Raksha Video — Phase 4 Status

Phase 4 adds the **intelligence pipeline contracts and first working signal path** on top of Phase 3.

## Implemented
- Real WebRTC call path preserved.
- Phase 3 protection/consent/WebSocket preserved.
- Modular conversation intelligence package on Android.
- Rule-based conversation signal detector for explicit transcript text.
- Backend conversation analyzer with stage progression.
- Backend visual signal ingestion.
- Risk fusion using signal combinations and confidence.
- `POST /api/protection/sessions/{session_id}/signals`.
- `POST /api/protection/sessions/{session_id}/transcript`.
- `POST /api/protection/sessions/{session_id}/visual`.
- Real WebRTC remote-frame sampling at 1 FPS when a remote video track exists and visual consent is enabled by the backend.
- Risk updates are returned through the existing protection WebSocket and displayed in the call UI.
- Confidence, stage, and contributing signal labels are displayed when supplied.

## Intentionally not claimed as complete
- Production speech-to-text for remote WebRTC audio is **not** bundled. The Android `SpeechRecognitionEngine` abstraction currently exposes an unavailable implementation until a real STT engine is connected.
- Production face detection/liveness is **not** bundled. The Android frame sampler captures real frame metadata; `LivenessAnalyzer` is explicitly `UNAVAILABLE` until a real CV/ML model is integrated.
- No fake face/liveness result or fake transcript is generated.
- No automatic call termination, emergency calling, police reporting, facial recognition, or secret recording is implemented.

## Verification
- Python backend modules are syntax-checked.
- ZIP integrity is checked before delivery.
- A two-physical-device WebRTC acceptance test cannot be performed in this build environment and is therefore not claimed as passed.
