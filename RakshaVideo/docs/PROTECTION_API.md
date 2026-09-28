# Protection API

## Create session
`POST /api/protection/sessions`

Authorization: `Bearer <token>`

Body includes `call_id`, `media`, and granular `consent`.

## Session WebSocket
`WS /api/ws/sessions/{session_id}`

All envelopes use `version`, `type`, `event_id`, `session_id`, `call_id`, `timestamp`, and `payload`.

## End session
`POST /api/protection/sessions/{session_id}/end`

## Risk event
The client accepts a backend-generated `risk_update` event. The Phase 3 development server does not generate risk itself.

## Phase 4 intelligence endpoints

### `POST /api/protection/sessions/{session_id}/signals`
Submit structured, consent-gated signals for fusion.

### `POST /api/protection/sessions/{session_id}/transcript`
Submit transcript text from an actual STT provider/client. Requires conversation-analysis consent.

### `POST /api/protection/sessions/{session_id}/visual`
Submit visual signal metadata. Requires visual-analysis consent.

Risk updates are emitted through the existing authenticated protection WebSocket.
