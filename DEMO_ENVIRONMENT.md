# RAKSHA ECOSYSTEM — LOCAL NETWORK TWO-PHONE DEMO GUIDE
**System Version:** 1.0.0 Unified Production Prototype  
**Audience:** Technical Evaluators, Live Demonstration Team  
**Hardware Topology:** 1 Host Laptop + 2 Physical Android Smartphones on the Same Wi-Fi  

---

## 1. Network Topology & IP Configuration

```text
                                 SHARED WI-FI ROUTER / HOTSPOT
                                              │
                      ┌───────────────────────┼───────────────────────┐
                      │                       │                       │
                      ▼                       ▼                       ▼
              💻 HOST LAPTOP             📱 PHONE A              📱 PHONE B
             172.17.35.95             172.17.35.X             172.17.35.Y
           (Raksha Backend)         (Caller Device)        (Receiver Device)
```

| Parameter | Value | Notes |
| :--- | :--- | :--- |
| **Host Laptop Active LAN IP** | `172.17.35.95` | Detected via `ipconfig` on Wireless LAN Adapter Wi-Fi |
| **Backend Port** | `8000` | Bound to `0.0.0.0` (all network interfaces) |
| **REST API Base URL** | `http://172.17.35.95:8000` | Replaces localhost / 127.0.0.1 on phones |
| **Health Check URL** | `http://172.17.35.95:8000/health` | HTTP 200 `{"status": "ok"}` |
| **Readiness Check URL** | `http://172.17.35.95:8000/ready` | HTTP 200 `{"status": "ready"}` |
| **Interactive API Docs** | `http://172.17.35.95:8000/docs` | Swagger UI |
| **WebRTC Signaling WebSocket** | `ws://172.17.35.95:8000/ws/call` | Real-time SDP offer/answer & ICE candidate relay |
| **Protection Stream WebSocket**| `ws://172.17.35.95:8000/api/ws/sessions/{id}` | Real-time risk event & speech signal stream |
| **Database** | SQLite (`./data/raksha.db`) | Automatically initialized persistent storage |
| **STUN Server** | `stun:stun.l.google.com:19302` | Public Google STUN server for ICE negotiation |
| **Auth API Token** | `demo-token-raksha-video` | Pre-configured demo bearer token |

---

## 2. Windows Defender Firewall Configuration

To allow physical Android phones on the Wi-Fi to reach port 8000, run this single command in an **Administrator PowerShell** window:

```powershell
netsh advfirewall firewall add rule name="RakshaDemoPort8000" dir=in action=allow protocol=TCP localport=8000
```

*(To remove the rule after the demonstration, run: `netsh advfirewall firewall delete rule name="RakshaDemoPort8000"`)*

---

## 3. Starting and Stopping the Backend

### Windows:
* **To Start:** Double-click [`start_demo_backend.bat`](file:///c:/Users/gangs/Downloads/APP%20OF%20RAKSHA/start_demo_backend.bat) or run in PowerShell:
  ```powershell
  .\start_demo_backend.bat
  ```
* **To Stop:** Double-click [`stop_demo_backend.bat`](file:///c:/Users/gangs/Downloads/APP%20OF%20RAKSHA/stop_demo_backend.bat) or press `Ctrl + C` in the server terminal.

### Linux / macOS:
* **To Start:**
  ```bash
  chmod +x start_demo_backend.sh
  ./start_demo_backend.sh
  ```

---

## 4. Physical Android Phone Setup

### Step 1: Install APKs
Connect each phone via USB or download via browser:
* **Phone A (Caller):**
  - Install `APK/RakshaVideo-debug.apk` (Package: `com.raksha.video`)
  - Install `APK/RakshaCall-debug.apk` (Package: `com.rakshacall.safety`)
* **Phone B (Receiver):**
  - Install `APK/RakshaVideo-debug.apk` (Package: `com.raksha.video`)

### Step 2: Configure Server URL on Devices
Both applications have direct one-tap network selectors:
1. In **RakshaCall**: On the Onboarding or Settings screen, tap **"PC LAN"** (`http://172.17.35.95:8000`).
2. In **Raksha Video**: Built with `http://172.17.35.95:8000` as the default local backend.

---

## 5. Live Two-Phone Demonstration Script

1. **Verify Backend Startup:**
   Open `http://172.17.35.95:8000/health` in any mobile browser on Phone A or Phone B. Confirm `status: ok`.
2. **Start Video Call on Phone A:**
   - Open **Raksha Video**.
   - Tap **"Create Call"**.
   - Note the generated Call ID (e.g. `RCV-A84F2E`).
3. **Join Call on Phone B:**
   - Open **Raksha Video** on Phone B.
   - Tap **"Join an existing call"** and enter `RCV-A84F2E`.
   - Confirm peer-to-peer video streams and audio transmission over WebRTC.
4. **Trigger Protection Handshake (Phone A):**
   - Tap **"Protection Active"** inside Raksha Video.
   - Or open **RakshaCall** and tap **"Start Protection"**.
5. **Simulate Scam Attack Progression:**
   - Speaker A says: *"I am Inspector Rajesh Kumar from the Cyber Crime Bureau. Your account is frozen."*
   - Observe real-time risk level elevation from **LOW** to **HIGH**.
   - Speaker A says: *"Immediately transfer the money and share your OTP."*
   - Observe **CRITICAL Safety Brake Interlock** trigger on screen.
