# PLATFORM LIMITATIONS & ARCHITECTURAL HONESTY
## Android Security Sandboxing, VoIP Audio Routing, and Operating System Realities
**Document Version:** 3.0.0-PROD  
**Classification:** Engineering Truth & Security Compliance  
**Target Platform:** Android 14+ / 15+ (API 34–36)  

---

## 1. THE IMPERATIVE OF ARCHITECTURAL HONESTY

A major vulnerability in AI security competitions is the tendency of prototypes to claim capabilities that violate operating system security guarantees. 

**RakshaCall adheres to strict platform honesty.** We explicitly document what the Android operating system permits, what it prohibits, and the exact architectural boundaries used to capture and protect conversational streams legally and transparently.

---

## 2. THE MYTH OF SILENT THIRD-PARTY VoIP INTERCEPTION

### 2.1. Android Security Sandbox & Cryptographic Guarantees
Several commercial and student projects falsely assert that their background application can *"silently listen to WhatsApp, Signal, or Telegram VoIP calls"* without user interaction.

**Technical Reality:**  
On modern Android (Android 10+, and enforced strictly in Android 14/15):
1. **UID-Level Process Isolation:** Each application runs in a distinct Linux UID sandbox. Memory spaces are completely segregated by the Linux kernel and SE-Linux policies.
2. **End-to-End Encryption (E2EE):** WhatsApp and Signal utilize the Signal Protocol (Curve25519, AES-256-GCM). Audio packets traversing the network interface are end-to-end encrypted; network packet sniffing yields only undecryptable ciphertext.
3. **Audio HAL & Capture Restrictions:** Android's AudioFlinger and AudioServer HAL explicitly forbid normal background apps from recording audio streams rendered by another app (`VOICE_COMMUNICATION` or `STREAM_VOICE_CALL`).
4. **Accessibility Exploit Rejection:** While malicious spyware misuses Android Accessibility Services to read screen text or scrape audio buffers, Google Play Store policies and Android 14+ restricted settings immediately terminate apps attempting unauthorized audio tapping.

**RakshaCall Policy:**  
RakshaCall **strictly disallows** illicit security bypasses, root exploits, or prohibited accessibility hacking.

---

## 3. RAKSHACALL'S 4 PERMITTED INGESTION BOUNDARIES

To deliver robust real-time conversational protection without violating platform boundaries, RakshaCall implements **4 legitimate, permitted media channels**:

```
+---------------------------------------------------------------------------------------------------+
|                            RAKSHACALL PERMITTED INGESTION BOUNDARIES                              |
+---------------------------------------------------------------------------------------------------+

[CHANNEL 1: IN-APP PROTECTED VIDEO CALL ROOMS]  <-- 100% Native End-to-End Control
  • Built-in WebRTC Audio/Video calling (RC-XXXXXX rooms)
  • Full, permitted real-time access to raw PCM16 audio and camera frames
  • Integrated live protection HUD, risk meter, and Safety Brake

[CHANNEL 2: ANDROID MediaProjection (USER CONSENTED)]  <-- System-Level Capture
  • Permitted system API: android.media.projection.MediaProjectionManager
  • Displays native Android OS consent dialog: "Start recording or casting with RakshaCall?"
  • Captures device display and internal system audio output legally
  • Bound to Foreground Service with explicit notification

[CHANNEL 3: SPEAKERPHONE ACOUSTIC CAPTURE]  <-- Physical Device Reality
  • Captures incoming caller audio through the physical microphone when call is on speaker
  • Works universally across regular cellular calls, WhatsApp, Telegram, and Meet
  • Requires only standard RECORD_AUDIO runtime permission

[CHANNEL 4: DESKTOP / BROWSER COMPANION EXTENSION]  <-- Multi-Platform Workspace
  • Chrome/Edge extension using chrome.tabCapture API
  • Consented capture of WhatsApp Web, Google Meet, or Zoom browser tabs
  • Streams Opus/PCM audio chunks to RakshaCall backend via authenticated gRPC
```

---

## 4. PERMISSION & LIFECYCLE ARCHITECTURE

### 4.1. Foreground Service Requirements (Android 14 & 15)
To ensure long-running protection during active phone calls, RakshaCall declares and manages modern foreground services:

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 4.2. Foreground Service Lifecycle Guardrails
- **Persistent Notification:** Android requires an ongoing, non-dismissible notification while protection is active (*"RakshaCall Active: Real-time conversation safety active"*).
- **Graceful Termination:** When the call ends, the service automatically releases the microphone/camera hardware, unbinds from the gRPC stream, and transitions to idle standby to prevent battery drain.

---

## 5. HARDWARE & ENVIRONMENTAL LIMITATIONS

| Limitation | Technical Manifestation | RakshaCall Mitigation |
|:---|:---|:---|
| **Earpiece Mode Audio** | In private earpiece mode, incoming voice is acoustic and physically muffled from the mic | App advises user: *"Enable Speakerphone to activate Call Shield"* |
| **Low-End Chipsets** | Heavy on-device LLM inference causes thermal throttling on sub-₹10,000 devices | Asymmetric architecture: Audio/Vision capture on edge, heavy inference on async gRPC backend |
| **Doze Mode & Standby** | OS aggressive battery optimization halts background threads after screen turn-off | Service requests `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for protection sessions |
| **Intermittent 4G/2G** | Rural Indian mobile networks experience severe packet jitter and momentary dropouts | Client circular audio buffer (1.5s) + Automatic local deterministic guardrail fallback |

---

## 6. PLATFORM FEATURE CLASSIFICATION MATRIX

| ID | Feature | Implementation Mode | Real-World Status | Platform Compliance |
|:---|:---|:---:|:---:|:---|
| 01 | **Deterministic Risk Engine** | Local Kotlin | `GREEN` / Verified | 100% Local, Zero Permission Required |
| 02 | **9-Tactic Intent Classifier** | Local / Backend | `GREEN` / Verified | Works on all permitted transcripts |
| 03 | **Scam Stage Machine** | Local / Backend | `GREEN` / Verified | Fully compliant state machine |
| 04 | **Manipulation Velocity Engine**| Local / Backend | `GREEN` / Verified | Sliding window, zero platform overhead |
| 05 | **Safety Brake Intervention** | Native Compose UI | `GREEN` / Verified | High-priority overlay / Full-screen intent |
| 06 | **Evidence Vault & SHA-256** | Room SQLite / Crypto| `GREEN` / Verified | Cryptographic append-only ledger |
| 07 | **In-App Protected Video Calling**| Native WebRTC | `GREEN` / Verified | Full native camera/audio permissions |
| 08 | **Microphone Acoustic Capture** | Android AudioRecord | `GREEN` / Verified | Uses `RECORD_AUDIO` with runtime consent |
| 09 | **MediaProjection Capture** | MediaProjection API | `BLUE` / Software | Requires physical user prompt tap |
| 10 | **Google Meet Connector** | Cloud OAuth API | `YELLOW` / External | Requires authorized Meet Media API tokens |
| 11 | **Twilio Emergency SMS** | Cloud REST API | `YELLOW` / External | Operates transparently when credentials set |
| 12 | **Silent WhatsApp VoIP Tapping**| N/A | `RED` / Disallowed | **PROHIBITED BY OS SANDBOX** (Uses permitted Mic/Projection instead) |

---

## 7. SUMMARY

RakshaCall solves the conversation safety challenge without security compromises, illegal accessibility tapping, or fraudulent marketing claims. It operates within legitimate operating system frameworks, treating privacy, user consent, and platform compliance as core engineering principles.
