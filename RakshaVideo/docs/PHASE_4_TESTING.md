# Phase 4 Testing

1. Build the Android app with the configured Android SDK.
2. Start the FastAPI server.
3. Configure a reachable backend URL for physical devices; the default `10.0.2.2` is for the Android emulator.
4. Place a real call between two devices.
5. Enable protection and grant the required consent.
6. Verify remote frame sampling appears as `FRAME_SAMPLE` signals when visual analysis is enabled.
7. Send a real transcript from a connected STT implementation to the transcript endpoint.
8. Verify risk updates arrive over the protection WebSocket.
9. Disable protection and confirm the WebRTC call remains independent.
