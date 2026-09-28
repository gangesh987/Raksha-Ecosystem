# RAKSHA ECOSYSTEM — DUAL-APP SYSTEM ARCHITECTURE
**Version:** 1.0.0 Final Release  
**Ecosystem Paradigm:** Two Separate Android Applications + Shared Backend & Intelligence Infrastructure  

---

## 1. High-Level Architecture

The Raksha Ecosystem decomposes real-time communication and safety intervention into two independently installed, independently lifecycle-managed Android applications coordinated via a shared backend and secure Android app-to-app deep linking.

```text
                                  RAKSHA ECOSYSTEM
                                         │
                    ┌────────────────────┴────────────────────┐
                    │                                         │
             📱 RAKSHA VIDEO                            📱 RAKSHACALL
             Separate APK                               Separate APK
       Package: com.raksha.video                  Package: com.rakshacall.safety
       Label: "Raksha Video"                      Label: "RakshaCall"
       Icon: Video Camera Theme                   Icon: Shield / Safety Theme
                    │                                         │
                    │ WebRTC Streams                          │ Ambient Audio / SMS
                    │ & In-Call Signals                       │ & Device Defense
                    │                                         │
                    └────────────────────┬────────────────────┘
                                         │
                                         ▼
                                  RAKSHA BACKEND
                        (FastAPI / Uvicorn / gRPC / DB)
                                         │
              ┌──────────────────────────┼──────────────────────────┐
              │                          │                          │
      WebRTC Signaling            Protection Service          Intelligence Fusion
    /ws/signaling/{room}        /api/ws/sessions/{sid}        • Scam Classification
    STUN/TURN ICE config        Session Ledger & Vault        • Manipulation Velocity
                                                              • Safety Brake Interlock
              │                          │                          │
              └──────────────────────────┼──────────────────────────┘
                                         │
                             Shared Protection Layer
                         (SQLite / PostgreSQL / Vault)
```

---

## 2. Component Responsibilities

### 📱 Application 1: Raksha Video (`com.raksha.video`)
- **Primary Function:** Secure, peer-to-peer WebRTC video and audio calling.
- **Hardware Integration:** Camera (front/back), microphone, speaker, Bluetooth audio routing.
- **Signaling:** Real-time WebSocket signaling (`/ws/signaling/{call_id}`) with ICE candidate exchange.
- **UI:** In-call controls, video surface views, mute/unmute, camera switch, participant status.
- **Protection Integration:** Emits conversational speech chunks and visual perception frames to the backend protection session.
- **Safety Center:** In-app safety indicator displaying real-time risk level during active video sessions.

### 📱 Application 2: RakshaCall (`com.rakshacall.safety`)
- **Primary Function:** Continuous ambient fraud protection, scam intelligence, and safety interlocks.
- **Hardware Integration:** Audio recording service, foreground notification service, hardware Keystore.
- **Core Intelligence:**
  - Multi-Turn Scam Progression Engine (9 fraud tactics across 6 operational stages).
  - Manipulation Velocity Engine (temporal pressure acceleration monitoring).
  - Multimodal Risk Fusion (YOLO11 visual signals + text/acoustic embeddings).
- **Intervention UI:**
  - Full-Screen Safety Brake with localized voice prompts (*"STOP! DO NOT SEND MONEY"*).
  - 7-Step Verification Coach.
  - Trusted Family Contact SOS alerts.
  - SHA-256 Tamper-Evident Evidence Vault.

---

## 3. Communication & Integration Vectors

1. **Shared Cloud / LAN Backend:**
   - Both applications communicate with the same Raksha backend deployment (`https://poor-keys-like.loca.lt` or LAN `http://172.17.35.95:8000`).
   - Raksha Video connects to WebRTC signaling and stream session endpoints.
   - RakshaCall connects to real-time risk monitoring WebSockets (`/api/ws/sessions/{sid}`) and REST safety endpoints.

2. **App-to-App Deep Linking:**
   - When Raksha Video establishes a protected call session, it can hand off or invoke RakshaCall via explicit Intent / URI:
     `rakshacall://protection/session?id={session_id}&call={call_id}`
   - RakshaCall accepts the session ID and launches its live protection HUD, providing seamless overlay defense without merging the codebases.

3. **Cryptographic Independence:**
   - Each app uses independent signing keystores, package sandboxes, and Room databases.
   - No shared Linux UID or shared process space is required.
