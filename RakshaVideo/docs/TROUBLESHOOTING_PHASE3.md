# Phase 3 Troubleshooting

## Protection unavailable
- Confirm FastAPI is reachable from the Android device.
- For an emulator, `10.0.2.2` maps to the development host.
- For a physical phone, replace the API host with the computer LAN IP and use the same network or a reachable server.
- Use HTTPS/WSS for non-local deployments.

## Protected indicator missing
Protection only becomes active after session creation and WebSocket connection succeed.

## Risk UI does not change
Confirm the backend sent a valid `risk_update` envelope. Phase 3 does not fabricate risk events.
