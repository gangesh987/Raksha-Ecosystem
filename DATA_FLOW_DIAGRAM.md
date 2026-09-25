# RakshaCall Data Flow Diagram
**Document Version:** 1.0.0  
**Scope:** Real-Time Data Pipeline from Edge Ingestion to Actionable Intervention  

---

## 1. Mermaid Data Flow Diagram

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

## 2. Textual / Data Pipeline Flowchart

```
[ PHYSICAL AUDIO ENVIRONMENT ]
  │
  ├─> Citizen Smartphone Mic (16kHz 16-bit PCM)
  └─> [Optional] Screen/Camera Capture (YOLO11 Input)
  │
  ▼
[ CLIENT DATA NORMALIZATION (RAM Ephemeral Buffer) ]
  │
  ├─> Windowing: 300ms rolling PCM chunks
  ├─> Monotonic Sequence Tagging (seq=1, 2, 3...)
  ├─> Explicit Consent State Assertion (consent_granted=True)
  │
  ▼
[ TRANSPORT: gRPC over HTTP/2 (TLS 1.3) ]
  │
  └─> Bidirectional Stream: ClientFrame Protobuf Message
  │
  ▼
[ BACKEND STREAM INGESTION & ACOUSTIC PIPELINE ]
  │
  ├─> 1. HuBERT Acoustic Representation: 768-dimensional temporal embeddings
  ├─> 2. Language Identification (LID): Detect Tamil, Tanglish, Hindi, Hinglish, English
  └─> 3. Multilingual ASR Phonetic Decoding: Normalized Multi-Turn Transcript
  │
  ▼
[ SEMANTIC UNDERSTANDING & TACTIC ENGINE (JEV Interface) ]
  │
  ├─> Negation & Protective Intent Filtering (Suppresses false alarms on "never give OTP")
  └─> 9-Tactic Probabilistic Classification (Authority, Fear, Urgency, Isolation, Payment...)
  │
  ▼
[ TEMPORAL STATE & DYNAMICS PROCESSING ]
  │
  ├─> Scam Stage State Machine (CONTACT -> AUTHORITY -> FEAR -> ISOLATION -> DEMAND)
  └─> Manipulation Velocity Engine (Temporal pressure rate = ΔTactics / ΔTime)
  │
  ▼
[ MULTIMODAL RISK FUSION LAYER ]
  │
  ├─> Primary Signals: Conversational Intent (0.45) + Stage (0.25) + Velocity (0.20)
  ├─> Supporting Signal: YOLO11 Contextual Visual Perception (0.10)
  ├─> Multiplier: Irreversible Action Flag (Payment / OTP demand detected)
  └─> Explainability Generator: "Risk 84/100 due to Authority + Fear + Payment Demand"
  │
  ▼
[ SAFETY INTERVENTION & FORENSIC AUDITING ]
  │
  ├─> Safety Brake Trigger: High Risk + Irreversible Action detected
  ├─> Evidence Vault: Appends SHA-256 Hash-Linked Block (Genesis: 000...000)
  └─> RiskUpdate Protobuf Stream dispatched to Client
  │
  ▼
[ CITIZEN INTERFACE (Low-Literacy Protection) ]
  │
  ├─> 🔊 Audio Prompt: "STOP! PANAM ANUPPATHINGA!" (Tamil) / "STOP! DO NOT PAY!" (English)
  ├─> 🚨 Visual: High-Contrast Red Interlock Screen
  ├─> 🛡️ Verification Coach: 7-Step Independent Verification Protocol
  └─> 👨‍👩‍👧 Family Safety Alert: Prepared Trusted Contact Dispatch (ALERT_REQUESTED)
```
