# RAKSHA ECOSYSTEM — INTER-APP & BACKEND INTEGRATION GUIDE
**Version:** 1.0.0 Final Release  
**Scope:** Integration protocols between Raksha Video, RakshaCall, and the Shared Protection Backend  

---

## 1. Architectural Philosophy: Separation of Concerns

Rather than bloating a single monolithic application, the Raksha Ecosystem cleanly separates:
1. **Communication Plane (`Raksha Video` - `com.raksha.video`):** High-framerate WebRTC peer-to-peer media streaming, hardware camera/mic controls, and in-call HUD.
2. **Safety & Protection Plane (`RakshaCall` - `com.rakshacall.safety`):** Ambient background protection, manipulation velocity tracking, 9-stage scam progression classification, Safety Brake interventions, and cryptographic evidence vaulting.

Both applications communicate via **authenticated backend protocols** and **Android OS Intent / Deep-Link bridges**.

---

## 2. Shared Backend Integration Architecture

Both applications connect to the single canonical Raksha Backend deployment:
- **Public Cloud Base URL:** `https://poor-keys-like.loca.lt`
- **LAN Base URL:** `http://172.17.35.95:8000`
- **Local Emulator Base URL:** `http://10.0.2.2:8000`

```text
┌───────────────────────┐                                 ┌───────────────────────┐
│     Raksha Video      │                                 │      RakshaCall       │
│  (com.raksha.video)   │                                 │ (com.rakshacall.safety│
└──────────┬────────────┘                                 └───────────┬───────────┘
           │                                                          │
           │ 1. Create Call (POST /api/calls)                         │
           │ 2. WebRTC Signaling (WSS /ws/signaling/{id})             │
           │ 3. Stream Telemetry (POST /api/signals/batch)            │
           │                                                          │ 4. Register/Login (/api/auth)
           │                                                          │ 5. Monitor Session (/api/ws/sessions/{id})
           │                                                          │ 6. Retrieve Evidence (/api/evidence)
           ▼                                                          ▼
    ═════════════════════════════════════════════════════════════════════════════
                              SHARED RAKSHA BACKEND
       FastAPI • Uvicorn • WebRTC Signaling • UnifiedAnalysisPipeline (AI)
    ═════════════════════════════════════════════════════════════════════════════
```

---

## 3. WebRTC Signaling & Protection Handshake

### Step 1: Call Creation (`Raksha Video`)
```http
POST /api/calls
Authorization: Bearer <api_token>

Response:
{
  "call_id": "RCV-A84F2E",
  "ice_servers": [
    {"urls": ["stun:stun.l.google.com:19302"]}
  ]
}
```

### Step 2: Protection Session Creation
```http
POST /api/protection/sessions
Content-Type: application/json
Authorization: Bearer <api_token>

{
  "call_id": "RCV-A84F2E",
  "consent": {
    "audio_analysis": true,
    "risk_detection": true,
    "visual_analysis": true
  }
}

Response:
{
  "session_id": "protect_9d81f204ba",
  "call_id": "RCV-A84F2E",
  "websocket_url": "wss://poor-keys-like.loca.lt/api/ws/sessions/protect_9d81f204ba"
}
```

### Step 3: Real-Time Stream Fusion & Risk Evaluation
During active calls, `Raksha Video` streams conversational transcripts and sampled visual perception vectors to the session:
```http
POST /api/protection/sessions/protect_9d81f204ba/signals
Content-Type: application/json

{
  "session_id": "protect_9d81f204ba",
  "call_id": "RCV-A84F2E",
  "signals": [
    {
      "source": "speech",
      "type": "tactic",
      "name": "Authority Impersonation",
      "score": 0.94,
      "timestamp": 1727453000000
    }
  ]
}
```
The backend routes these signals directly through `UnifiedAnalysisPipeline`, computing:
$$\text{Risk Score} = w_t \cdot \text{TacticProb} + w_v \cdot \text{Velocity} + w_m \cdot \text{MultimodalContext}$$

---

## 4. App-to-App Handshake & Deep Linking

When a user in `Raksha Video` receives a high-risk scam alert, `Raksha Video` provides a direct 1-tap handoff to launch `RakshaCall`'s dedicated Safety Brake & Verification Coach.

### Deep Link Intent Specification:
```xml
<!-- In RakshaCall AndroidManifest.xml -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data
        android:scheme="rakshacall"
        android:host="protection"
        android:pathPrefix="/session" />
</intent-filter>
```

### Invocation Code from Raksha Video:
```kotlin
val intent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("rakshacall://protection/session?id=${sessionId}&call=${callId}")
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}
if (intent.resolveActivity(context.packageManager) != null) {
    context.startActivity(intent)
} else {
    // Graceful fallback: Open browser dashboard or display in-app banner
}
```

---

## 5. Security & Isolation Matrix

| Dimension | Raksha Video | RakshaCall |
| :--- | :--- | :--- |
| **Android Package ID** | `com.raksha.video` | `com.rakshacall.safety` |
| **Android Process Space** | Independent Linux UID | Independent Linux UID |
| **Local Database** | Isolated Room DB (`raksha_video.db`) | Isolated Room DB (`rakshacall.db`) |
| **Keystore Keys** | WebRTC auth session | Hardware AES-256-GCM JWT key |
| **Foreground Service** | WebRTC Media / Call Service | Ambient Protection Service |
| **Permissions** | Camera, Audio, Network | Audio, Contacts, Notifications |
