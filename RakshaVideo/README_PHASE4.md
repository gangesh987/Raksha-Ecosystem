# Raksha Video — Phase 4

Phase 4 extends the Phase 3 real WebRTC + protection project with modular conversation/visual intelligence and explainable risk fusion.

## Important

This package does **not** pretend that a production speech-to-text or liveness model exists when it does not. Transcript analysis accepts text from a real STT source; the Android video path samples real WebRTC frames and sends frame metadata. Face detection and liveness remain explicit `UNAVAILABLE` interfaces until a real model is integrated.

## Run backend

```bash
cd server
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

For Android emulator, the default backend is `http://10.0.2.2:8000`.

For physical devices, configure the app/backend host to a reachable LAN/server address.

## Phase 4 endpoints

- `POST /api/protection/sessions/{session_id}/signals`
- `POST /api/protection/sessions/{session_id}/transcript`
- `POST /api/protection/sessions/{session_id}/visual`

## Verification performed for this archive

- Python syntax check passed.
- Backend route smoke test passed for session creation, transcript analysis and visual signal ingestion.
- ZIP integrity check is performed before delivery.
- Android Gradle build was not performed because the source package does not include a Gradle wrapper and this environment does not have a Gradle installation.
- Two-physical-device WebRTC acceptance testing was not performed in this environment.
