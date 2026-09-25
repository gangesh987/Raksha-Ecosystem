# RakshaCall End-to-End Machine Learning Architecture
**Document Version:** 1.0.0 Production Prototype  
**Classification:** Complete ML Engineering & Multimodal System Architecture  
**Pipeline Root:** `ml/` & `backend/app/ai/`  

---

## 1. Executive Summary & Architectural Paradigm

RakshaCall shifts from static keyword-matching into a **data-driven, multimodal conversational intelligence engine**. Rather than scanning for isolated scam words, the platform reasons over multi-turn context, phonetic speech representations, 9 coercive tactics, chronological stage transitions, temporal manipulation velocity, and supporting visual signals.

```
+========================================================================================================+
|                                    RAKSHACALL ML INFERENCE TOPOLOGY                                    |
+========================================================================================================+

                                      [ CONSENTED EDGE MEDIA ]
                                      ├── 16kHz Mono PCM Audio
                                      └── Camera / Screen Frame
                                                 │
                                                 ▼
                             [ REAL-TIME gRPC STREAMING (HTTP/2) ]
                               (ClientFrame -> ProtectionService)
                                                 │
                                                 ▼
                           [ LAYER 1: ACOUSTIC & PHONETIC INGESTION ]
                           ├── HuBERT Acoustic Feature Extractor (768-dim)
                           ├── Multilingual LID (Tamil, Tanglish, Hindi, Hinglish, English)
                           └── Phonetic ASR Decoding -> Normalized Transcript
                                                 │
                                                 ▼
                           [ LAYER 2: MULTI-TURN CONVERSATION BUFFER ]
                           └── Sliding Window (k=5) + Session Registry
                                                 │
                                                 ▼
                        [ LAYER 3: MULTILINGUAL SEMANTIC DECISION ENGINE ]
                        ├── Trained Multilingual 9-Tactic Neural Classifier (Model 2)
                        ├── Cloud LLM Nuance Analysis (Groq Qwen-3.8-27B / Gemini)
                        ├── Local JEV Prototype Vector Matcher
                        └── Deterministic Safety Guardrail (Safety Floor)
                                                 │
                                                 ▼
                         [ LAYER 4: TEMPORAL DYNAMICS & SCAM TRAJECTORY ]
                         ├── Scam Stage State Machine (Bidirectional GRU Model 3)
                         │   (CONTACT -> AUTHORITY -> FEAR -> ISOLATION -> DEMAND -> BRAKE)
                         └── Manipulation Velocity Engine (Temporal Acceleration dTactics/dt)
                                                 │
                                                 ▼
                         [ LAYER 5: MULTIMODAL CONTEXT FUSION LAYER ]
                         ├── Primary Signals: Speech + Stage + Velocity (90%)
                         ├── Supporting Signal: YOLO11 Contextual Vision (Model 4) (10%)
                         └── Model Disagreement Detection & Explainability Generator
                                                 │
                                                 ▼
                         [ LAYER 6: SAFETY INTERVENTION & AUDIT VAULT ]
                         ├── Safety Brake Trigger Invariant: Risk >= 80 + Irreversible Action
                         ├── Low-Literacy Vernacular Voice Prompts ("STOP! PANAM ANUPPATHINGA!")
                         ├── 7-Step Independent Verification Coach
                         ├── Trusted Family Emergency Handoff (ALERT_REQUESTED)
                         └── SHA-256 Tamper-Evident Evidence Vault (Genesis: 000...000)
+========================================================================================================+
```

---

## 2. Four Core Machine Learning Models

### Model 1: Multilingual Speech & HuBERT Representation Pipeline
- **Role**: Maps raw 16kHz acoustic waveforms into normalized text and language metadata.
- **Components**:
  - `HuBERTAcousticBackbone`: Extracts 768-dimensional latent representations capturing acoustic energy, spectral centroid, and voicing without cloud latency.
  - `LanguageIdentifier`: Distinguishes vernacular Tamil (`ta-IN`), Romanized Tanglish (`ta-Latn`), Devanagari Hindi (`hi-IN`), Romanized Hinglish (`hi-Latn`), and Indian English (`en-IN`).
  - Modular `ASRProvider`: Supports on-device HuBERT phonetic decoding, AI4Bharat Indic ASR conformers, or cloud speech APIs.

### Model 2: Multilingual Multi-Task Tactic Classifier
- **Architecture**: Deep neural encoder with shared 128-dimensional representations and multi-task output heads.
- **Output Space**:
  - Head A: $P(\text{is\_scam}) \in [0.0, 1.0]$ (Binary classification).
  - Head B: $P(\text{tactic}_i) \in [0.0, 1.0]$ for $i \in [1..9]$ (Multi-label classification).
- **Tactics**:
  1. `AUTHORITY_IMPERSONATION`
  2. `CRIMINAL_ALLEGATION_FEAR`
  3. `URGENCY`
  4. `ISOLATION`
  5. `PAYMENT_DEMAND`
  6. `CREDENTIAL_OTP_PRESSURE`
  7. `REMOTE_ACCESS_PRESSURE`
  8. `SUSPICIOUS_LINKS`
  9. `ESCALATION_COERCION`
- **Inference Latency**: **0.42 ms** on CPU.

### Model 3: Temporal Conversation Stage Model
- **Architecture**: 2-layer Bidirectional GRU with temporal sequence pooling and dropout regularization.
- **Input**: 12-dimensional sequence tensors (9 tactic probabilities + speaker flag + normalized turn position + scam state).
- **Output**: Discrete classification across the 7 stages of social engineering escalation:
  $$\text{CONTACT} \longrightarrow \text{AUTHORITY} \longrightarrow \text{FEAR} \longrightarrow \text{ISOLATION} \longrightarrow \text{DEMAND} \longrightarrow \text{PAYMENT\_CREDENTIAL} \longrightarrow \text{CRITICAL\_BRAKE}$$
- **Inference Latency**: **0.08 ms** on CPU.

### Model 4: YOLO11 Contextual Visual Perception
- **Architecture**: Ultralytics YOLO11 Nano (`yolo11n.pt`).
- **Context Classes**: `person`, `cell phone`, `laptop`, `tv / monitor screen`, `book / document`.
- **Role**: Strictly **SUPPORTING CONTEXT**. It never independently triggers scam alerts, but corroborates dual-device fraud or identity card coercion.
- **Inference Latency**: **18.5 ms** on CPU.

---

## 3. Hybrid Semantic Decision Engine & Safety Floor

To reconcile data-driven ML generalization with deterministic zero-tolerance for explicit irreversible theft, RakshaCall deploys a **Hybrid Decision Engine**:

```
+───────────────────────────────────────────────────────────────────────────────+
| RAKSHACALL HYBRID DECISION ENGINE                                             |
|                                                                               |
| 1. DATA-DRIVEN ML DETECTION (Primary Reasoning)                               |
|    - Neural forward pass produces continuous probabilities (0.0 to 1.0).      |
|    - Understands nuanced colloquial phrasing and multi-turn escalation.       |
|    - Enhanced by Groq Qwen-3.8-27B cloud LLM when configured.                 |
|                                                                               |
| 2. DETERMINISTIC SAFETY GUARDRAIL (Safety Floor)                              |
|    - Never labeled as an AI model; explicitly tagged as SAFETY FLOOR.         |
|    - Activates only on unambiguous irreversible theft markers:                |
|      * Direct OTP extraction ("Tell me OTP", "ஓடிபி சொல்லுங்கள்", "ओटीपी बताइए") |
|      * Remote access app takeover ("Install AnyDesk", "Screen share")          |
|      * Explicit payment demand ("Transfer money immediately", "பணம் அனுப்புங்கள்") |
|    - Guarantees zero missed interventions on critical explicit threats.       |
+───────────────────────────────────────────────────────────────────────────────+
```

---

## 4. Multimodal Risk Fusion Mathematics

The fusion layer produces an explainable, normalized 0–100 risk score:

$$R_{\text{raw}} = 0.45 \cdot S_{\text{speech}} + 0.25 \cdot S_{\text{stage}} + 0.20 \cdot S_{\text{velocity}} + 0.10 \cdot S_{\text{vision}}$$

Where:
- $S_{\text{speech}} = \sum_{i=1}^{9} w_i \cdot P(\text{tactic}_i)$ (Primary conversational intent)
- $S_{\text{stage}} = \text{StageRankPoint}(\text{stage}_t)$ (Chronological escalation stage)
- $S_{\text{velocity}} = \min(V(t) \cdot 100, 100)$ (Temporal rate of coercion accumulation)
- $S_{\text{vision}} = \text{YOLO11ContextMultiplier}$ (Supporting physical context)

### Irreversible Action Multiplier:
$$\text{If } (\text{PaymentDemand} \lor \text{OTPExtraction} \lor \text{RemoteAccessInstall}) \text{ detected:}$$
$$M_{\text{action}} = 1.25$$
$$\text{RiskScore} = \min(\text{round}(R_{\text{raw}} \cdot M_{\text{action}}), 100)$$

---

## 5. Safety Brake & Forensic Evidence

### Safety Brake Invariant:
$$\text{Trigger Safety Brake } \iff (\text{RiskScore} \ge 80) \land (\text{IrreversibleAction} == \text{True})$$

When triggered:
1. **Immediate Audio Interlock**: Dispatches culturally tailored Tamil or English voice prompt (*"STOP! PANAM ANUPPATHINGA!"*).
2. **Visual Interlock**: High-contrast, large-button modal blocks further screen taps.
3. **Verification Coach**: Displays 7-step independent verification protocol.
4. **Emergency Handoff**: Prepares one-tap family alert (`ALERT_REQUESTED`).
5. **Cryptographic Preservation**: Appends an immutable block to the SHA-256 Evidence Vault.
