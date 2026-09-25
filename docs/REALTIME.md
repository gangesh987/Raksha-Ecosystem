# Real-Time Streaming & WebSocket Architecture

## 1. Overview
RakshaCall provides real-time bi-directional telemetry across Android devices, cloud/local backend services, and AI inference engines.

```text
Android Audio Record (PCM16 16kHz)
  ↓
Android SpeechRecognizer / Gemini Live
  ↓
RakshaRealtimeWebSocketClient (OkHttp)
  ↓ (ws://localhost:8000/api/ws/sessions/{sid})
FastAPI WebSocket Router
  ↓
Coercion Risk Engine + Gemini 1.5 Flash Provider
  ↓
Risk / Stage / Velocity JSON Frame Broadcast
  ↓
Compose UI Live Protection Screen (Reactive StateFlow)
```

---

## 2. Android WebSocket Client (`RakshaRealtimeWebSocketClient.kt`)
The Android client is built on **OkHttp WebSocket** with resilience patterns:

### Connection Lifecycle States
- `DISCONNECTED`: Initial idle state before protection starts.
- `CONNECTING`: Attempting initial handshake with backend.
- `CONNECTED`: Active full-duplex session with heartbeat ping/pong.
- `RECONNECTING`: Network dropped; executing exponential backoff retry.
- `OFFLINE`: Maximum retries exhausted or network unavailable; operating purely on local deterministic safety engine.

### Exponential Backoff
- Initial delay: 1,000ms
- Multiplier: 2.0x
- Max delay: 30,000ms
- Max reconnect attempts: 5 (then gracefully marks connection as `OFFLINE` and notifies user)

### Heartbeat & Liveness
- 15-second Ping/Pong heartbeat interval
- Read/write timeouts configured to 10 seconds to detect dead sockets early

---

## 3. Real-Time Connectors & Platform Boundaries
In accordance with platform security and privacy policies:
- **No Silent Interception**: RakshaCall strictly obeys Android OS boundaries and never intercepts third-party encrypted voice/video calls (WhatsApp, Signal, Telegram, Skype) without explicit OS permission.
- **Permitted Input Modes**:
  1. Real-time microphone audio capture via `SpeechRecognizer` (Android standard permission).
  2. CameraX real-time preview analysis (with explicit user toggle and camera permission).
  3. Screen capture via `MediaProjection` API (user-approved projection dialogue).
  4. Google Meet Media API consented connector boundary (via backend OAuth).
  5. Web extension consented browser audio capture boundary.

---

## 4. Degraded Mode & Fallback Assurance
If network drops, WebSocket disconnects, or the remote AI provider fails:
1. The **Local Safety Provider** (`LocalSafetyProvider.kt`) immediately takes over with 0ms interruption.
2. The UI honestly transitions from `CONNECTED` to `OFFLINE` or `AI UNAVAILABLE`.
3. Deterministic regex-based tactic detection (all 9 coercive tactics) continues locally.
4. Scam Stage transitions and the Safety Brake trigger without relying on any external network call.
5. All evidence events are stored locally in Room database and queued for sync once connectivity is restored.
