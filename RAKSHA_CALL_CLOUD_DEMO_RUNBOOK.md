# RAKSHACALL — PUBLIC CLOUD & CELLULAR DEMO RUNBOOK
**System Version:** 1.0.0 Production Prototype (Cloud & Cellular Hardened)  
**Target Environment:** Public Cloud Deployment + Real-Time Cellular Android Client  
**Active Public Base URL:** `https://poor-keys-like.loca.lt`  
**Active WebSocket Endpoint:** `wss://poor-keys-like.loca.lt/api/ws/sessions/{sid}`  
**Fallback LAN Base URL:** `http://172.17.35.95:8000`  
**Local Emulator Base URL:** `http://10.0.2.2:8000`  

---

## 1. Quick Demo Architecture

```
[ Android Mobile Device (4G / 5G Cellular) ]
                   │
                   │ HTTPS REST / WSS WebSocket (TLS 443)
                   ▼
       [ Public Edge / Reverse Proxy ]
       (e.g., Cloudflare, Render, Localtunnel: https://poor-keys-like.loca.lt)
                   │
                   │ HTTP/1.1 & WebSocket
                   ▼
     [ RakshaCall Production Backend ] (Port 8000)
       ├── UnifiedAnalysisPipeline (Canonical AI)
       ├── Manipulation Velocity Engine
       ├── Safety Brake Interlock State Machine
       └── Cryptographic SHA-256 Evidence Ledger
```

---

## 2. Pre-Demo Warm-Up Procedure (Mandatory for Free-Tier / Serverless Hosts)

Free-tier public cloud environments (Render, Cloud Run, Hugging Face Spaces) sleep when idle, incurring a 30–50 second cold start. Localtunnel and edge nodes may also take 2–3 seconds on the first TLS handshake.

### Step 1: Execute Cloud Warm-Up Probe (3 Minutes Before Demo)
Run this command from any terminal or open the URL in a browser on your phone:
```bash
curl -s -H "Bypass-Tunnel-Reminder: true" https://poor-keys-like.loca.lt/api/health
```

**Expected Response (HTTP 200):**
```json
{
  "status": "healthy",
  "service": "RakshaCall Safety API",
  "version": "1.0.0",
  "transports": {
    "rest": "operational",
    "websocket": "operational",
    "grpc": "internal_ipc"
  }
}
```

### Empirical Latency Characteristics:
- **Cold Start Request (First Call):** ~2,430 ms
- **Warm Requests (Subsequent):** ~1,560 ms – 1,840 ms (Includes edge proxy TLS overhead)
- **WebSocket Handshake:** ~1,460 ms (One-time connection setup)
- **WebSocket Risk Frame Round-Trip:** ~640 ms (Real-time bi-directional streaming)

---

## 3. Physical Android Device Setup (Cellular Testing)

### Step 1: Install the Signed APK
Connect the phone to PC via USB or download directly via mobile browser/Google Drive:
- **Recommended for Demo:** `RakshaCall-Debug.apk` (or `RakshaCall-Release.apk`)
- **Direct Sideload via ADB:**
  ```powershell
  & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r RakshaCall-Debug.apk
  ```

### Step 2: Disable Wi-Fi / Enable Cellular Data
1. Turn **OFF** Wi-Fi on the Android device.
2. Turn **ON** 4G / 5G Mobile Data.
3. Open the **RakshaCall** app.

### Step 3: Verify or Select Cloud Endpoint in App
1. On the Onboarding or Settings screen, tap **"Change Server / Cloud URL"**.
2. Tap the **"Cloud (Live)"** preset button:
   - Configures URL to `https://poor-keys-like.loca.lt`
   - Notice: No manual typing required.
3. If giving a local offline presentation where cellular reception is unavailable, tap **"PC LAN"** (`http://172.17.35.95:8000`).

---

## 4. Live Multi-Turn Scam Progression Demonstration

### Script / Telephony Simulator:
1. **Turn 1 (Contact / Authority):**
   > *"Hello, this is Inspector Rajesh Kumar from the Central Cyber Crime Investigation Bureau."*
   - **App Reaction:** Tactic identified as `Authority Impersonation`, Stage transitions to `AUTHORITY`, Advisory banner displayed.

2. **Turn 2 (Fear / Criminal Allegation):**
   > *"Your Aadhaar card and bank account have been linked to an international money laundering syndicate. An arrest warrant is active."*
   - **App Reaction:** Tactic identified as `Criminal Allegation / Fear`, Stage moves to `FEAR`, Manipulation Velocity increases, Risk Level: **MEDIUM** (Orange).

3. **Turn 3 (Isolation / Secrecy):**
   > *"This is a strictly confidential national security matter. Do not inform your family or friends or you will be arrested."*
   - **App Reaction:** Tactic identified as `Isolation`, Stage moves to `ISOLATION`, Manipulation Velocity spikes (>0.7), Risk Level: **HIGH** (Red Warning).

4. **Turn 4 (Urgent Financial Demand):**
   > *"You must immediately transfer Rs 50,000 to the Supreme Court verification escrow account to secure clearance."*
   - **App Reaction:** Tactic identified as `Payment Demand`, Velocity peaks.

5. **Turn 5 (Credential / OTP Extraction — Safety Brake):**
   > *"Quickly read out the OTP you just received from your bank to verify your payment."*
   - **App Reaction:** 
     - **SAFETY BRAKE ACTIVATED!**
     - Full-screen high-contrast visual interlock.
     - Urgent Voice Prompt: *"STOP. DO NOT SHARE OTP OR TRANSFER MONEY."*
     - Step-by-Step Verification Coach displayed.
     - Trusted Contact SOS prepared with SHA-256 verified event ledger snapshot.

---

## 5. Live Negative Control Demonstration (Zero False Positives)

Demonstrate resilience against false alarms using legitimate banking or educational speech:
> *"Hello sir, calling from your bank branch. Please remember never to share your OTP or password with anyone. Our branch refunded your charges."*

- **Result:**
  - Negation intent recognized.
  - Stage remains `CONTACT`.
  - Velocity remains `0.0`.
  - Risk Score remains `0 / SAFE`.
  - Safety Brake remains dormant.

---

## 6. Live Logcat & Real-Time Telemetry Inspection

To observe the real-time event pipeline on a connected Android device:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -s "RakshaCall:*" "RakshaRealtime:*" "ProtectionService:*"
```

**Key Event Timestamps Emitted:**
- `T0_START_PROTECTION`: Timestamp when user activates protection service.
- `T1_WS_CONNECTED`: Timestamp when WebSocket handshake completes over cellular data.
- `T2_INPUT_SENT`: Timestamp when conversation frame is transmitted.
- `T4_RISK_RECEIVED`: Timestamp when AI risk calculation is returned.
- `T5_UI_UPDATED`: Timestamp when Safety Brake or risk gauge updates on screen.
