# RakshaCall Architecture Diagram
**Document Version:** 1.0.0  
**Scope:** High-Level End-to-End System & Transport Topology  

---

## 1. Mermaid Architecture Diagram

```mermaid
graph TB
    subgraph ClientLayer ["1. Client Domain (Android Native / Flutter)"]
        UI["Material 3 Safety UI\n- High-Contrast Alerts\n- Large Button Interlocks\n- Low-Literacy Tamil Cues"]
        Mic["Acoustic Microphone / MediaProjection Ingestion"]
        Cam["Camera Sensor (Supporting Visual Context)"]
        Keystore["Android Keystore\n- MasterKey AES-256-GCM\n- Encrypted Room DB"]
        gRPCClient["gRPC Client (grpc.aio / Dart)\n- HTTP/2 Multiplexing\n- Bidirectional Streaming"]
        LocalBrake["Local Safety Fallback Guardrail"]

        Mic -->|16kHz PCM Frames| gRPCClient
        Cam -->|Sub-sampled Frames| gRPCClient
        gRPCClient -->|RiskUpdate Stream| UI
        LocalBrake -->|Emergency Interlock| UI
        Keystore -.->|Session Auth & Contacts| gRPCClient
    end

    subgraph TransportLayer ["2. Real-Time Transport (HTTP/2 + TLS 1.3)"]
        gRPCStream["ProtectionService.StreamProtectionSession\n- ClientFrame (Audio, Transcript, Vision, Consent)\n- RiskUpdate (Scores, Stage, Velocity, Interventions)"]
    end

    subgraph BackendDataPlane ["3. AI Data-Plane (Async Streaming Workers)"]
        Dispatcher["Stream Dispatcher & Flow Controller"]
        HuBERT["HuBERT Feature Extractor\n(16kHz 768-dim Acoustic Embeddings)"]
        LID["Multilingual LID\n(Tamil, Tanglish, Hindi, Hinglish, English)"]
        ASR["Phonetic / Indic ASR Engine\n(Multi-Turn Normalized Transcript)"]
        JEV["Semantic Engine (JEV Provider)\n- Intent & Negation Analysis\n- 9-Tactic Probabilistic Classifier"]
        YOLO["YOLO11 Contextual Vision\n(Person, Device, Screen Supporting Signals)"]
        StageMachine["Scam Stage Machine\n(CONTACT -> AUTHORITY -> FEAR -> ... -> BRAKE)"]
        Velocity["Manipulation Velocity Engine\n(Temporal Acceleration & Escalation)"]
        Fusion["Multimodal Risk Fusion Engine\n- Primary: Speech, Stage, Velocity\n- Supporting: YOLO11 Vision"]
        BrakeEngine["Safety Brake & Verification Coach\n(Voice Cues & 7-Step Protocol)"]
        Vault["Evidence Vault\n(SHA-256 Block Chaining)"]

        Dispatcher --> HuBERT
        HuBERT --> LID
        LID --> ASR
        ASR --> JEV
        JEV --> StageMachine
        JEV --> Velocity
        Dispatcher --> YOLO
        
        StageMachine --> Fusion
        Velocity --> Fusion
        JEV --> Fusion
        YOLO -.->|Supporting Signal| Fusion

        Fusion --> BrakeEngine
        BrakeEngine --> Vault
        Fusion --> Dispatcher
    end

    subgraph BackendControlPlane ["4. Control Plane (FastAPI REST APIs)"]
        FastAPI["FastAPI Control Plane\n- Session Lifecycle & Health\n- RBAC & Telemetry\n- Evidence Chain Verification"]
        DB[(PostgreSQL / SQLite Storage)]
        FastAPI --- DB
    end

    gRPCClient <===>|TLS 1.3 / HTTP/2| gRPCStream
    gRPCStream <===>|Bidirectional Stream| Dispatcher
    FastAPI -.->|Session State & Admin| Dispatcher
```

---

## 2. Textual / ASCII Topology Diagram

```
+========================================================================================================+
|                                    RAKSHACALL SYSTEM TOPOLOGY                                          |
+========================================================================================================+

[ ANDROID CLIENT TRUST DOMAIN ]
+-------------------------------------------------------------------------------------------------------+
|  +-------------------------------------------------------------------------------------------------+  |
|  | User Interface (Material 3)                                                                     |  |
|  | - Emergency Voice Alerts ("STOP. PANAM ANUPPATHINGA") - 7-Step Verification Coach - SOS Action   |  |
|  +-------------------------------------------------------------------------------------------------+  |
|                                                  ▲                                                    |
|                                                  │ (Live RiskUpdate: Risk, Stage, Velocity, Actions)  |
|  +----------------------+      +-----------------+---------------+      +--------------------------+  |
|  | Media Ingestion      |      | Real-Time gRPC Client Engine    |      | Android Keystore         |  |
|  | - Mic PCM (16kHz)    | ---> | - HTTP/2 Multiplexed Streaming  | <--- | - MasterKey AES-256-GCM  |  |
|  | - MediaProjection    |      | - Bidirectional Channel         |      | - Encrypted Room Cache   |  |
|  | - YOLO Camera Feed   |      | - Flow Control & Heartbeat      |      | - Trusted Contacts       |  |
|  +----------------------+      +---------------------------------+      +--------------------------+  |
+--------------------------------------------------┬----------------------------------------------------+
                                                   │
                                                   │ [TLS 1.3 / HTTP/2 Bidirectional gRPC Streaming]
                                                   │ (ClientFrame <===> RiskUpdate)
                                                   ▼
[ BACKEND STREAMING & INTELLIGENCE CLUSTER ]
+-------------------------------------------------------------------------------------------------------+
|  +-------------------------------------------------------------------------------------------------+  |
|  | gRPC ProtectionService Dispatcher (Port 50051)                                                  |  |
|  +-------------------------------------------------------------------------------------------------+  |
|         │                                                                             ▲               |
|         │ (Audio Frames)                                                              │ (RiskUpdate)  |
|         ▼                                                                             │               |
|  +--------------------------+      +---------------------------+             +--------┴------------+  |
|  | HuBERT Representation    | ---> | Multilingual LID & ASR    |             | Safety Brake Engine |  |
|  | 768-dim Acoustic Vectors |      | Tamil, Tanglish, Hindi... |             | Voice Cues & Coach  |  |
|  +--------------------------+      +---------------------------+             +---------------------+  |
|                                                  │                                    ▲               |
|                                                  ▼ (Normalized Transcript)            │               |
|  +-------------------------------------------------------------+                      │               |
|  | Semantic Intelligence & JEV Provider Layer                  |                      │               |
|  | - Intent & Negation Analysis ("Never share OTP" suppressed) |                      │               |
|  | - 9-Tactic Classification (Authority, Fear, Urgency...)     |                      │               |
|  +-------------------------------------------------------------+                      │               |
|         │                                        │                                    │               |
|         ▼ (Tactic Probabilities)                 ▼ (Temporal Events)                  │               |
|  +--------------------------+      +---------------------------+             +--------┴------------+  |
|  | Scam Stage Machine       |      | Manipulation Velocity     |             | Multimodal Fusion   |  |
|  | CONTACT -> FEAR -> BRAKE |      | Temporal Acceleration     | ----------> | Primary: Speech     |  |
|  +--------------------------+      +---------------------------+             | Supporting: YOLO11  |  |
|                 │                                                            +---------------------+  |
|                 └─────────────────────────────────────────────────────────────────────▲               |
|                                                                                       │ (Supporting)  |
|  +-------------------------------------------------------------+                      │               |
|  | YOLO11 Contextual Perception Engine                         | ---------------------┘               |
|  | Person presence, phone usage, screen context                |                                      |
|  +-------------------------------------------------------------+                                      |
|                                                                                                       |
|  +-------------------------------------------------------------------------------------------------+  |
|  | SHA-256 Tamper-Evident Evidence Vault (Append-Only Hash-Linked Chain)                           |  |
|  +-------------------------------------------------------------------------------------------------+  |
+-------------------------------------------------------------------------------------------------------+
```
