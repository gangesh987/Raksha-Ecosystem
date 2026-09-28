# Protection Testing

1. Start the FastAPI server.
2. Build/install Raksha Video.
3. Establish a real two-device WebRTC call.
4. Choose Enable Protection.
5. Confirm protection state reaches Monitoring.
6. Disconnect the backend and confirm the call continues while protection becomes unavailable.
7. Reconnect the backend and confirm protection can be re-established.
8. For UI-only development, enable `ALLOW_PROTECTION_TEST_EVENTS=true` and POST a clearly synthetic risk event to `/api/protection/sessions/{session_id}/test/risk`.

Synthetic events must never be enabled in production.
