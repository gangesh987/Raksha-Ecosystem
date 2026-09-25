# RakshaCall Complete Architecture Diagrams Suite
**Document Version:** 1.0.0  
**Scope:** Complete Architectural, Data Flow, AI Inference, and Safety Intervention Diagrams  

---

## 1. System Topology & Transport Architecture (`ARCHITECTURE_DIAGRAM`)

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

## 2. End-to-End Real-Time Data Flow (`DATA_FLOW_DIAGRAM`)

```mermaid
sequenceDiagram
    autonumber
    actor Caller as Suspicious Caller / Scammer
    actor User as Citizen (User)
    participant Client as RakshaCall Client (Android / Flutter)
    participant gRPC as ProtectionService (gRPC Streaming)
    participant ASR as Multilingual ASR (HuBERT + LID)
    participant JEV as Semantic & 9-Tactic Engine (JEV)
    participant State as Stage & Velocity Analyzers
    participant Fusion as Multimodal Risk Fusion
    participant Brake as Safety Brake & Evidence Vault

    Caller->>User: "Ungal Aadhaar cyber crime-la maatikuchu! Panatha ippove anuppunga!"
    Note over User,Client: Acoustic Mic / MediaProjection captures audio
    Client->>Client: Normalize to 16kHz mono PCM (RAM buffer)
    Client->>gRPC: Stream ClientFrame(audio_chunk, sequence_id, consent=True)
    
    gRPC->>ASR: Ingest 300ms PCM chunk
    ASR->>ASR: HuBERT acoustic embedding + Language ID (Tamil / Tanglish)
    ASR->>JEV: Output Normalized Transcript ("Ungal Aadhaar cyber crime...")
    
    JEV->>JEV: Intent & Negation Check (Rule out protective guidance)
    JEV->>JEV: 9-Tactic Classification (Authority: 0.94, Criminal Allegation: 0.92, Payment: 0.95)
    
    JEV->>State: Emit Tactic Probabilities & Timestamps
    State->>State: Advance Stage: CONTACT -> AUTHORITY -> FEAR -> DEMAND
    State->>State: Calculate Manipulation Velocity (High acceleration: 0.72)
    
    State->>Fusion: Feed Context Window + Stage State + Velocity + Tactic Probs
    Opt YOLO11 Vision Frame Present
        Client->>gRPC: ClientFrame(visual_metadata: person=1, phone=1)
        gRPC->>Fusion: Feed Supporting Visual Context (Weight: 0.15)
    end
    
    Fusion->>Fusion: Compute Explainable Risk Score (84/100, CRITICAL)
    Fusion->>Brake: Evaluate Invariant (Risk >= 80 + Irreversible Action)
    
    Brake->>Brake: Append Tamper-Evident SHA-256 Evidence Block
    Brake->>gRPC: Generate RiskUpdate(Risk=84, Stage=CRITICAL_BRAKE, Action=EMERGENCY_INTERLOCK)
    gRPC-->>Client: Stream RiskUpdate frame over HTTP/2
    
    Client->>User: 🚨 FULL-SCREEN RED SAFETY BRAKE INTERLOCK
    Client->>User: 🔊 Tamil Voice Alert: "STOP! PANAM ANUPPATHINGA!"
    Client->>User: 📋 7-Step Verification Coach
    Client->>User: 📲 One-Tap Emergency Family Alert (ALERT_REQUESTED)
```

---

## 3. Multimodal AI Inference Pipeline (`AI_PIPELINE_DIAGRAM`)

```mermaid
graph TD
    RawAudio["Consented Raw Audio (16kHz PCM Chunk)"] --> HuBERT["HuBERT Acoustic Representation\n(768-dim Embedding Vectors)"]
    HuBERT --> LID["Language Identification (LID)\nDetects: ta, ta-Latn, hi, hi-Latn, en"]
    LID --> ASR["Multilingual Indic ASR\nPhonetic Decoding & Text Normalization"]
    
    ASR --> Window["ConversationContext (Sliding Window k=5)\nMulti-Turn Dialogue History"]
    Window --> JEV["JEV Semantic Engine (Provider Interface)\n- Negation & Protective Intent Filter\n- Contextual Ambiguity Resolution"]
    
    JEV --> Tactics["9-Tactic Probabilistic Classifier\n1. Authority Impersonation\n2. Criminal Allegation / Fear\n3. Urgency Pressure\n4. Isolation Demand\n5. Payment Demand\n6. Credential / OTP Pressure\n7. Remote Access App Pressure\n8. Suspicious Link Injection\n9. Coercion & Escalation"]

    Tactics --> StageMachine["Scam Stage Machine\nCONTACT -> AUTHORITY -> FEAR ->\nISOLATION -> DEMAND -> CRITICAL_BRAKE"]
    Tactics --> Velocity["Manipulation Velocity Engine\nRate of Coercive Tactic Accumulation\nV(t) = sum(w_i * exp(-lambda * dt))"]

    RawVideo["Consented Camera / Screen Frame"] --> YOLO["YOLO11 Contextual Perception\n- Person Presence\n- Phone Device Presence\n- Screen / Document Context\n(SUPPORTING SIGNAL ONLY)"]

    Tactics --> Fusion["Explainable Multimodal Fusion Engine"]
    StageMachine --> Fusion
    Velocity --> Fusion
    YOLO -.->|Supporting Context (Weight: 0.10)| Fusion

    Fusion --> Decision["Explainable Risk Output (0-100)\n- Risk Level: LOW / MEDIUM / HIGH / CRITICAL\n- Contributing Factor Vector\n- Irreversible Action Invariant Check"]

    Decision --> Brake["Safety Brake & Verification Engine\n- Low-Literacy Tamil/English Voice Prompts\n- 7-Step Verification Coach\n- SHA-256 Tamper-Evident Evidence Vault"]
```

---

## 4. Safety Brake & Intervention Flow (`SAFETY_INTERVENTION_FLOW`)

```mermaid
stateDiagram-v2
    [*] --> MonitoringState: Consented Session Active

    state MonitoringState {
        direction TB
        IngestMedia --> ExtractFeatures
        ExtractFeatures --> EvaluateTactics
        EvaluateTactics --> UpdateStageAndVelocity
        UpdateStageAndVelocity --> CalculateRiskScore
    }

    CalculateRiskScore --> MonitoringState: Risk < 80 OR No Irreversible Action
    CalculateRiskScore --> SafetyBrakeTriggered: Risk >= 80 AND Irreversible Action Detected

    state SafetyBrakeTriggered {
        direction TB
        
        state "PHASE 1: PAUSE (Aural & Visual Interlock)" as Phase1 {
            direction LR
            VisualLock: Full-Screen Red Modal\n(High Contrast, Large Typography)
            AuralCue: Tamil/English Voice Alert\n("STOP! PANAM ANUPPATHINGA!")
            HapticFeedback: Triple Emergency Pulse
        }

        state "PHASE 2: VERIFY (Verification Coach)" as Phase2 {
            direction TB
            Step1: "1. Take a breath and pause."
            Step2: "2. Disconnect the call immediately."
            Step3: "3. Do not dial numbers provided by the caller."
            Step4: "4. Search for the institution's official helpline independently."
            Step5: "5. Confirm whether an actual investigation exists."
            Step6: "6. Consult your family or trusted contact."
            Step7: "7. Resume financial actions only after verification."
            Step1 --> Step2 --> Step3 --> Step4 --> Step5 --> Step6 --> Step7
        }

        state "PHASE 3: ESCALATE & PRESERVE" as Phase3 {
            direction LR
            AlertFamily: One-Tap Family SOS Dispatch\n(Status: ALERT_REQUESTED)
            HashVault: Append SHA-256 Tamper-Evident Evidence Block
        }

        Phase1 --> Phase2
        Phase2 --> Phase3
    }

    SafetyBrakeTriggered --> CitizenDecides: User Reviews Guidance

    state CitizenDecides <<choice>>
    CitizenDecides --> SessionSafelyEnded: User Disconnects & Verifies
    CitizenDecides --> SafeOverride: User Explicitly Overrides After Cooldown
```
