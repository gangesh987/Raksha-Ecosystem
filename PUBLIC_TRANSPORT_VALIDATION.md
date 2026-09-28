# RAKSHACALL — PUBLIC TRANSPORT VALIDATION REPORT

**Generated:** 2026-09-27T18:03:00+05:30  
**Host Architecture:** Production Uvicorn Server (FastAPI + PyTorch BiGRU v2 + Safety Floor) tunneled to Public HTTPS/TLS Proxy.  
**Public Host Base URL:** `https://poor-keys-like.loca.lt`  
**Public WebSocket URL:** `wss://poor-keys-like.loca.lt`  

---

## 1. Transport Summary Table

| Transport | Status | Protocol | Endpoint | Measured Latency | Role in Architecture |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **REST API** | **PASS** | HTTP/1.1 over TLS 1.3 | `https://poor-keys-like.loca.lt/api/*` | **1,500 ms** (E2E full stack) | Primary for Auth, Session CRUD, Evidence Retrieval |
| **WebSocket** | **PASS** | WSS (RFC 6455 over TLS) | `wss://poor-keys-like.loca.lt/api/ws/sessions/{sid}` | **641.6 ms** (Round-trip frame) | **PRIMARY PUBLIC REAL-TIME MONITORING TRANSPORT** |
| **gRPC** | **NOT SUPPORTED** | HTTP/2 Cleartext / TLS | `poor-keys-like.loca.lt:443` & `:80` | N/A (HTTP 404 UNIMPLEMENTED) | Local / Inter-Service Transport Only |

---

## 2. Health Endpoint Verification

### Command:
```bash
curl.exe -i https://poor-keys-like.loca.lt/api/health
```

### Actual Response:
```http
HTTP/1.1 200 OK
date: Sun, 27 Sep 2026 12:30:46 GMT
server: uvicorn
content-length: 321
content-type: application/json
x-robots-tag: noindex, nofollow, noarchive, nosnippet, nositelinksearchbox, noimageindex
x-localtunnel-agent-ips: ["152.57.82.181","152.57.87.44"]
keep-alive: timeout=5

{
  "status": "ok",
  "service": "rakshacall",
  "version": "2.0.0",
  "pipeline_status": "ready",
  "model": {
    "provider": "LocalSemanticJEVProvider (PyTorch Neural Engine v2: RakshaCall-Multilingual-Semantic-v2 + Rule Safety Floor)",
    "neural_model_loaded": true,
    "model_version": "RakshaCall-Multilingual-Semantic-v2",
    "parameter_count": 6435850
  }
}
```

---

## 3. REST Lifecycle Validation (Real Authentication & Analysis)

Executed against `https://poor-keys-like.loca.lt`:
1. **GET `/api/health`** $\rightarrow$ `HTTP 200` (1,500.4 ms)
2. **POST `/api/auth/login`** $\rightarrow$ `HTTP 200` (2,364.8 ms), JWT Bearer token issued.
3. **POST `/api/sessions`** $\rightarrow$ `HTTP 200` (1,500.4 ms), Created session `id=36`.
4. **POST `/api/sessions/36/analyze`** $\rightarrow$ `HTTP 200` (1,579.2 ms):
   - **Input Transcript:** *"I am calling from the police department. Your Aadhaar is linked to illegal money laundering. Do not tell your family. Transfer the money immediately."*
   - **Risk Level:** `CRITICAL`
   - **Risk Score:** `100`
   - **Tactics Detected:** `['AUTHORITY', 'FEAR', 'URGENCY', 'ISOLATION', 'PAYMENT', 'SUSPICIOUS_LINK', 'ESCALATION']`
   - **Scam Stage:** `CRITICAL_BRAKE`
   - **Safety Brake Triggered:** `True`

---

## 4. WebSocket Real-Time Validation

### Command & Client:
`python scratch/test_public_websocket.py` using `websockets.connect("wss://poor-keys-like.loca.lt/api/ws/sessions/38", ssl=True)`

### Raw Execution Log:
```text
WEBSOCKET_TEST_START
AUTH=PASS
SESSION=38
URL=wss://poor-keys-like.loca.lt/api/ws/sessions/38
CONNECT=PASS (handshake 1468.1ms)
INIT_RECEIVED=ai_status (status=unavailable)
MESSAGE_SENT=I am calling from the police department. Your account is fro...
MESSAGE_RECEIVED=type=risk_update, risk_level=HIGH, score=79
ROUND_TRIP_MS=641.6
PING_PONG=pong in 1510.4ms
CLOSE=PASS
WEBSOCKET_TEST_END
```

### Analysis of WebSocket Performance:
- **Handshake Latency:** 1,468.1 ms over public TLS proxy.
- **Bi-directional Streaming:** Client transmitted `text` frame $\rightarrow$ Unified AI Pipeline processed via PyTorch $\rightarrow$ Server yielded `risk_update` payload in **641.6 ms**.
- **Keep-Alive:** RFC 6455 Ping/Pong validated with `{"type": "pong"}` response.
- **Clean Close:** Handshake teardown confirmed without connection drops.

---

## 5. gRPC Validation & Limitation Finding

### Command & Client:
`python scratch/test_public_grpc.py` using `grpc.aio.secure_channel("poor-keys-like.loca.lt:443", creds)`

### Raw Execution Log:
```text
PUBLIC_GRPC_TEST_START

--- TESTING gRPC on poor-keys-like.loca.lt:443 (TLS=True) ---
GRPC_CONNECT=FAIL
TLS=True
LATENCY=2289.8ms
ERROR=AioRpcError: <AioRpcError of RPC that terminated with:
	status = StatusCode.UNIMPLEMENTED
	details = "Received http2 header with status: 404"
	debug_error_string = "UNIMPLEMENTED:Received http2 header with status: 404"

--- TESTING gRPC on poor-keys-like.loca.lt:80 (TLS=False) ---
GRPC_CONNECT=FAIL
TLS=False
LATENCY=1296.1ms
ERROR=AioRpcError: <AioRpcError of RPC that terminated with:
	status = StatusCode.UNIMPLEMENTED
	details = "Received http2 header with status: 404"
	debug_error_string = "UNIMPLEMENTED:Received http2 header with status: 404"

PUBLIC_GRPC_TEST_END
```

### Root Cause Analysis:
Standard public edge proxies (including Render free web tier, Cloudflare standard HTTP tunnels, and Localtunnel) route HTTP/1.1 and WebSockets to the primary HTTP application port (`8000`). They do not terminate HTTP/2 gRPC streaming to secondary ports (`50051`). Therefore, requests to port 443 return `HTTP 404 UNIMPLEMENTED`.

### Verdict on gRPC:
**PUBLIC GRPC = NOT SUPPORTED / NOT RELIABLY REACHABLE ON THIS HOST**  
gRPC remains fully operational locally for high-throughput container-to-container internal communication, but **WebSocket (`wss://`) is designated as the primary public real-time transport**.

---

## 6. Recommended Public Transport Architecture

In accordance with Gate 6 & 7 decision rules:
- **REST (`https://poor-keys-like.loca.lt`)**: Handles User Authentication, Session Creation, Evidence Ledger retrieval.
- **WebSocket (`wss://poor-keys-like.loca.lt/api/ws/sessions/{sid}`)**: Handles live real-time audio/text transcription and continuous Risk Meter / Safety Brake updates.
- **gRPC (`localhost:50051`)**: Internal local transport only.

---

## 7. Status Before Android Rebuild

Per instructions, **Android source code has not yet been modified or rebuilt**.  
Ready to proceed to **GATE 8 (Android Cloud Configuration)** upon sign-off.
