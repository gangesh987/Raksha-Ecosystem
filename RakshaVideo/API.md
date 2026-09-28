# API Surface

## Public health
- `GET /health`
- `GET /ready`
- `GET /api/webrtc/config`

## Authenticated calling
- `POST /api/calls`
- `WS /ws/call`

## Protection
- `POST /api/protection/sessions`
- `GET /api/protection/sessions/{session_id}`
- `POST /api/protection/sessions/{session_id}/events`
- `POST /api/protection/sessions/{session_id}/signals`
- `POST /api/protection/sessions/{session_id}/transcript`
- `POST /api/protection/sessions/{session_id}/visual`
- `POST /api/protection/sessions/{session_id}/end`
- `WS /api/ws/sessions/{session_id}`

## Safety/evidence
- `GET/POST/PATCH/DELETE /api/trusted-contacts...`
- `POST /api/protection/sessions/{session_id}/safety/actions`
- `GET /api/protection/sessions/{session_id}/timeline`
- `GET /api/protection/sessions/{session_id}/evidence`
- `POST /api/protection/sessions/{session_id}/evidence/export`

The development-only synthetic risk route is disabled by default.
