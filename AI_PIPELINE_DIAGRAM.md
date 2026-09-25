# RakshaCall AI Intelligence Pipeline Diagram
**Document Version:** 1.0.0  
**Scope:** Deep-Dive Technical Flow of Multimodal AI Models & Inference Engines  

---

## 1. Mermaid AI Pipeline Architecture

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

## 2. Mathematical & Algorithmic Layer Breakdown

```
========================================================================================================
                               RAKSHACALL MULTIMODAL INFERENCE PIPELINE
========================================================================================================

[ LAYER 1: ACOUSTIC & PHONETIC REPRESENTATION ]
Input: X_audio ∈ R^(16000 x t) (16kHz Mono PCM)
  │
  ├─> HuBERT Feature Extractor:
  │     z_t = HuBERT_Encoder(X_audio) ∈ R^(768)
  │
  ├─> Language Identification (LID):
  │     P(lang | z_t) ∈ {ta, ta-Latn, hi, hi-Latn, en}
  │
  └─> Multilingual Phonetic ASR:
        Y_transcript = ASR_Decoder(z_t, lang)
  │
  ▼
[ LAYER 2: MULTI-TURN CONVERSATION CONTEXT (k=5) ]
History Buffer: C_t = {u_(t-4), u_(t-3), u_(t-2), u_(t-1), u_t}
  │
  ├─> Eliminates single-word false triggers
  └─> Tracks semantic arc across alternating conversational turns
  │
  ▼
[ LAYER 3: SEMANTIC INTELLIGENCE & JEV PROVIDER ]
  │
  ├─> Protective Intent Filter:
  │     If "never ask OTP" OR "scam alert" ∈ C_t:
  │       Suppress OTP/Payment tactic activation (Protective Negation)
  │
  └─> 9-Tactic Probabilistic Classification:
        T_t = { (tactic_i, P_i, confidence_i) | i ∈ [1..9] }
  │
  ▼
[ LAYER 4: TEMPORAL STATE & VELOCITY ENGINES ]
  │
  ├─> Scam Stage Machine:
  │     S_t = f(S_(t-1), T_t, dwell_time)
  │     States: CONTACT(0) -> AUTHORITY(1) -> FEAR(2) -> ISOLATION(3) -> DEMAND(4) -> BRAKE(5)
  │
  └─> Manipulation Velocity Engine:
        V(t) = ∑ [ weight(T_k) * exp(-λ * (t - t_k)) ] / Δt
  │
  ▼
[ LAYER 5: MULTIMODAL RISK FUSION LAYER ]
Signals:
  - S_conv = ∑ (P_i * weight_i)           [PRIMARY: 45%]
  - S_stage = StageScore(S_t)              [PRIMARY: 25%]
  - S_velocity = min(V(t) * 100, 100)      [PRIMARY: 20%]
  - S_vision = YOLO11_ContextScore         [SUPPORTING: 10%]

Raw Risk Formulation:
  R_raw = (0.45 * S_conv) + (0.25 * S_stage) + (0.20 * S_velocity) + (0.10 * S_vision)

Irreversible Action Multiplier (M_action):
  If Payment Demand OR OTP Request active:
    M_action = 1.25 (Capped at 100)
  Else:
    M_action = 1.0

Final Score:
  RiskScore = min(round(R_raw * M_action), 100)
  │
  ▼
[ LAYER 6: EXPLAINABILITY & SAFETY INTERVENTION ]
Trigger Invariant:
  If (RiskScore >= 80) AND (IrreversibleActionDetected):
    Trigger SAFETY_BRAKE:
      1. Voice Prompt: "STOP! PANAM ANUPPATHINGA!" (Tamil)
      2. Full-Screen Red Interlock Display
      3. 7-Step Verification Coach
      4. Alert Family / Trusted Contact
      5. Append SHA-256 Tamper-Evident Evidence Block
========================================================================================================
```
