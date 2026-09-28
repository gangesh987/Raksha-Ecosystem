# RAKSHACALL — MOBILE CELLULAR E2E VALIDATION REPORT
**Execution Date:** 2026-09-27T18:25:30+05:30  
**Transport Evaluated:** WebSocket Real-Time Stream over Cellular Radio (4G/5G)  
**Public Endpoint:** `wss://poor-keys-like.loca.lt/api/ws/sessions/{sid}`  

---

## 1. ADB Hardware & Connectivity Status

```powershell
PS C:\Users\gangs\Downloads\APP OF RAKSHA> & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
* daemon not running; starting now at tcp:5037
* daemon started successfully
List of devices attached
(No devices detected)
```

### Official Evaluation Verdict:
> **STATUS: NOT VERIFIED — Physical Android device not connected via ADB.**  
> *(Per strict system integrity rules, no synthetic logcat or fabricated cellular timestamps are produced. The APKs have been cleanly compiled, instrumented with telemetry, and packaged for manual sideloading and on-device validation).*

---

## 2. On-Device Telemetry Instrumentation Verification

The Android client codebase has been instrumented with JVM-safe `RakshaLogger` logging to capture the end-to-end event timeline:

### Key Event Hooks:
1. `T0_START_PROTECTION`:
   - Location: `ProtectionForegroundService.kt#onStartCommand`
   - Log Tag: `ProtectionService`
   - Action: User presses "Start Protection", foreground notification posted, audio capture primed.
2. `T1_WS_CONNECTED`:
   - Location: `RakshaRealtimeWebSocketClient.kt#onOpen`
   - Log Tag: `RakshaRealtime`
   - Action: Bi-directional WebSocket handshake successfully established over cellular IP.
3. `T2_INPUT_SENT`:
   - Location: `RakshaRealtimeWebSocketClient.kt#sendTurn`
   - Log Tag: `RakshaRealtime`
   - Action: Audio transcript / frame JSON dispatched to server.
4. `T4_RISK_RECEIVED`:
   - Location: `RakshaRealtimeWebSocketClient.kt#onMessage`
   - Log Tag: `RakshaRealtime`
   - Action: Server response deserialized, containing `risk_score`, `stage`, `top_tactic`, and `safety_brake_triggered`.
5. `T5_UI_UPDATED`:
   - Location: `RakshaRealtimeWebSocketClient.kt#onMessage`
   - Log Tag: `RakshaRealtime`
   - Action: Risk state propagated to Compose UI StateFlow, triggering immediate Safety Brake overlay if critical.

---

## 3. Manual Sideloading & Cellular Runbook

For evaluators or live presenters testing with a physical Android phone:

1. **Install APK:**
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r RakshaCall-Debug.apk
   ```
2. **Disconnect Wi-Fi & Enable Cellular (4G/LTE/5G)**.
3. **Open App:**
   - Tap "Change Server / Cloud URL" on the sign-in screen.
   - Tap **"Cloud (Live)"** (`https://poor-keys-like.loca.lt`).
   - Sign in using Demo Phone login or Email login.
4. **Start Safety Protection:**
   - Tap **"Start Protection"**.
   - Speak or simulate scam progression phrases:
     - *"I am calling from the police department."*
     - *"Your Aadhaar is linked to criminal activity. Transfer funds immediately."*
5. **Verify Safety Brake Trigger:**
   - Confirm immediate red screen interlock, voice prompt, and verification coach.
6. **Capture Telemetry:**
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -s "RakshaRealtime:*" "ProtectionService:*"
   ```
