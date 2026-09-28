# Raksha Video — Final Phase 6 Package

Raksha Video is a real Android video-calling application with WebRTC calling, Raksha protection telemetry, conversation/visual intelligence, risk fusion, user-controlled safety actions, trusted contacts, and evidence/timeline handling.

## Final phase

Phase 6 hardens the Phase 5 implementation for release preparation:

- production endpoint/token configuration via Gradle properties or environment variables
- HTTPS/WSS enforcement for production Android configuration
- backend authentication without a committed default token
- persistent SQLite storage for trusted contacts, safety actions, and evidence events
- API rate limiting
- `/health` and `/ready` endpoints
- bounded request inputs and two-participant call-room enforcement
- safer WebSocket and deployment configuration
- non-root Docker image and health check
- release minification enabled
- release/signing documentation
- security, privacy, testing, deployment, and demo documentation

## Important verification boundary

The environment used to assemble this package does not contain an Android SDK/Gradle wrapper, so an Android APK/AAB build and physical two-device WebRTC test could not be executed here. The final reports explicitly mark these as `BLOCKED` or `NOT_TESTED` rather than claiming success.

Backend Python compilation and the existing backend protocol tests were executed successfully.

## Configure Android

Set these before building:

```text
RAKSHA_BACKEND_URL=https://your-host.example
RAKSHA_SIGNALING_WS_URL=wss://your-host.example/ws/call
RAKSHA_API_TOKEN=<deployment secret>
```

They can be supplied as environment variables or Gradle properties (`-PRAKSHA_BACKEND_URL=...`, etc.). Production configuration rejects blank endpoints/tokens and requires HTTPS/WSS.

## Configure backend

Copy `.env.example` to a deployment environment and set at minimum:

```text
RAKSHA_API_TOKEN=<long random secret>
RAKSHA_PUBLIC_WS_BASE=wss://your-host.example
```

For production, terminate TLS at a reverse proxy/load balancer and expose the backend through HTTPS/WSS.

## Backend

```bash
cd server
pip install -r requirements.txt
PYTHONPATH=. RAKSHA_API_TOKEN='...' uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Or:

```bash
docker compose -f server/docker-compose.yml up --build
```

The Docker Compose configuration requires `RAKSHA_API_TOKEN` and persists the SQLite safety/contact/evidence database in a named volume.

## Final documentation

See:

- `FINAL_ARCHITECTURE_AUDIT.md`
- `FINAL_SECURITY_AUDIT.md`
- `FINAL_PRIVACY_AUDIT.md`
- `FINAL_TEST_MATRIX.md`
- `FINAL_TEST_REPORT.md`
- `FINAL_PERFORMANCE_REPORT.md`
- `FINAL_RELEASE_CHECKLIST.md`
- `FINAL_RELEASE_REPORT.md`
- `FINAL_DEMO_SCRIPT.md`
- `HACKATHON_DEMO_CHECKLIST.md`
- `KNOWN_LIMITATIONS.md`
- `DEPLOYMENT.md`
- `SIGNING_GUIDE.md`
