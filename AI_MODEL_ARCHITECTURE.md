# AI MODEL & MULTIMODAL INFERENCE ARCHITECTURE
## RakshaCall: Real-Time Multilingual Multimodal Conversation Safety Platform
**Document Version:** 3.0.0-PROD  
**Target Execution Environment:** Edge-Android & Async Backend Inference Mesh  
**Classification:** Core Technical Model Specification  

---

## 1. ARCHITECTURAL OVERVIEW

The RakshaCall AI engine discards simplistic keyword-based counting (`keyword -> score`) in favor of a **deep temporal, semantic, and contextual inference pipeline**. 

In high-stakes cyber fraud—specifically **"Digital Arrest" scams, fake CBI/police extortion, TRAI disconnection threats, and courier narcotics fraud**—scammers manipulate cognitive load through progressive psychological coercion. 

The inference architecture decomposes this manipulation across 7 specialized intelligence layers:

```
[LAYER 1: ACOUSTIC & SPEECH INGESTION]
  User-Consented 16kHz Audio Stream -> Chunking -> Pre-Emphasis -> Normalization
          ↓
[LAYER 2: MULTILINGUAL SPEECH RECOGNITION (ASR) & LID]
  Language Identification (ta, en, hi, code-mixed) -> HuBERT Acoustic Features -> Real Indic ASR Decoding
          ↓
[LAYER 3: CONVERSATION CONTEXT & WINDOWING]
  Multi-Turn Dialogue Window (T-4 ... T) -> Speaker Diarization -> Semantic Normalization
          ↓
[LAYER 4: 9-TACTIC PROBABILISTIC CLASSIFICATION & JEV PROVIDER]
  JEV Semantic Engine -> Intent Embeddings -> Probabilistic 9-Tactic Scoring + Supporting Evidence
          ↓
[LAYER 5: TEMPORAL STAGE & MANIPULATION VELOCITY]
  Scam Stage State Machine (Contact -> Escalation) + Velocity Gradient (d(Coercion)/dt)
          ↓
[LAYER 6: MULTIMODAL CONTEXT FUSION & YOLO11 SUPPORT]
  Primary (Speech/NLP/Stage/Velocity) + Supporting (YOLO11 Contextual Signals) + Disagreement Guard
          ↓
[LAYER 7: EXPLAINABLE RISK ENGINE & SAFETY BRAKE]
  0-100 Calibrated Risk Score + Contributing Factors + Safety Brake Execution (Tamil/English Audio)
```

---

## 2. MULTILINGUAL SPEECH RECOGNITION & HuBERT SPECIFICATION

### 2.1. The Role of HuBERT in Speech Processing
**Critical Technical Distinction:**  
HuBERT (*Hidden-Unit BERT*) is a **self-supervised speech representation model**, **NOT** an end-to-end Automatic Speech Recognition (ASR) transcription engine on its own. 

In RakshaCall's architecture:
1. **HuBERT / Acoustic Representation Backbone:**
   - Processes raw 16kHz waveform audio through a 7-layer temporal convolutional encoder ($CNN$) followed by a 12-layer Transformer encoder.
   - Generates 768-dimensional latent phonetic frame embeddings every 20ms.
   - Robust to noisy telephone audio, low bit-rate VoIP codecs, packet loss, and acoustic distortion.
2. **ASR Decoding Head:**
   - Downstream CTC (Connectionist Temporal Classification) or RNN-Transducer decoder maps HuBERT latent representations into character/subword tokens.
   - Coupled with an Indic multilingual language model supporting Tamil, Hindi, and English phonology.

### 2.2. Language Identification (LID) & Code-Switching Architecture
In Indian scam encounters, speech is predominantly code-switched (Tanglish: Tamil + English; Hinglish: Hindi + English). 

The pipeline runs a streaming acoustic Language Identification model concurrently with frame ingestion:
- **Frame Size:** 250ms chunks, 50ms overlap.
- **Languages Supported:**
  - `ta-IN` (Tamil)
  - `en-IN` (Indian English)
  - `hi-IN` (Hindi)
  - `ta-Latn` (Tanglish - Romanized Tamil)
  - `hi-Latn` (Hinglish - Romanized Hindi)
- **Code-Switching Preservation:** The transcription engine outputs dual representations:
  1. `canonical_transcript`: Original vernacular script (e.g., தமிழ் / हिन्दी / English).
  2. `normalized_romanized`: Phonetically normalized Latin representation to enable cross-lingual semantic matching.

---

## 3. SEMANTIC CONVERSATION UNDERSTANDING & JEV PROVIDER

### 3.1. Repository Inspection & The "JEV" Engine
**Rigorous Audit Finding:**  
Inspection of the existing repository confirms **no pre-existing "JEV" library, proprietary binary, or API key** was present. The acronym JEV as referenced by mentors could denote a *Joint Embedding Vision-Language*, *Judicial Enforcement Verifier*, or an external research model.

**Design Decision (Zero Guesswork / Zero Fabrication):**
RakshaCall encapsulates JEV behind a strict **`JEVProvider` interface**.
- The interface contracts semantic intent extraction, tactic probability distribution, and reasoning traces.
- Pluggable provider implementations are supported:
  1. `LocalSemanticJEVProvider`: Offline vector embedding matcher and intent classifier using sentence embeddings and cosine similarity.
  2. `HuggingFaceJEVProvider`: Connects to local or remote ONNX/Transformers model endpoints.
  3. `CloudLLMJEVProvider`: Backend-isolated provider leveraging Groq (LLaMA-3) or Gemini for deep contextual reasoning.
  4. `MockJEVProvider`: Controlled, deterministic provider for unit testing and CI validation.

### 3.2. Conversation Context Windows vs. Isolated Sentences
Keyword matching fails because:
- Words like *"police"* or *"OTP"* occur legitimately in normal life.
- Scam coercion is cumulative.

RakshaCall maintains a sliding conversation context $\mathcal{W}$ of the last $K$ turns (default $K=5$ turns, spanning 30–90 seconds):

$$\mathcal{W}_t = \{ (s_1, u_1, \tau_1), (s_2, u_2, \tau_2), \dots, (s_t, u_t, \tau_t) \}$$

Where $s_i \in \{\text{CALLER}, \text{USER}\}$, $u_i$ is the utterance text, and $\tau_i$ is the timestamp.

**Context Escalation Example:**
- **Turn 1 (CALLER):** *"Your Aadhaar card was found in a suspicious parcel seized at Mumbai airport."*  
  $\rightarrow$ Intent: Criminal Allegation. Stage: FEAR.
- **Turn 2 (CALLER):** *"We are connecting you directly to the CBI Cyber Crime headquarters."*  
  $\rightarrow$ Intent: Authority Impersonation. Escalates Stage to: AUTHORITY.
- **Turn 3 (CALLER):** *"Do not disconnect this call or inform your family. You are in digital custody."*  
  $\rightarrow$ Intent: Isolation & Secrecy. Escalates Stage to: ISOLATION.
- **Turn 4 (CALLER):** *"To verify your innocence, transfer ₹50,000 to the RBI clearance account immediately."*  
  $\rightarrow$ Intent: Payment Demand + Urgency. Irreversible Action Flag triggered! Safety Brake deployed!

---

## 4. 9-TACTIC PROBABILISTIC CLASSIFICATION

Each tactic $T_k$ ($k \in [1, 9]$) produces a structured inference tuple:
$$\mathcal{O}(T_k) = \langle P(T_k), C(T_k), E(T_k), \tau_k, L_k, S_k, \text{Reason}_k \rangle$$

Where:
- $P(T_k) \in [0.0, 1.0]$: Probability of tactic presence based on semantic intent similarity.
- $C(T_k) \in [0.0, 1.0]$: Model confidence (accounting for audio quality, ASR confidence, and context clarity).
- $E(T_k)$: Precise supporting evidence snippet extracted from the conversation window.
- $\tau_k$: Timestamp of observation.
- $L_k$: Language/dialect detected (`ta`, `tanglish`, `en`, `hi`, `hinglish`).
- $S_k$: Speaker attribution (`CALLER` vs `USER`).
- $\text{Reason}_k$: Human-readable explanation of why this tactic was flagged.

### 4.1. Tactic Definitions & Semantic Intent Representations

| Tactic | Display Name | Core Semantic Intents | Base Weight | Irreversible? |
|:---:|:---|:---|:---:|:---:|
| **T1** | Authority Impersonation | Pretending to be law enforcement, CBI, police, customs, court judges, RBI, TRAI | 15 | No |
| **T2** | Criminal Allegation / Fear | Accusing victim of narcotics smuggling, money laundering, fake passports, arrest warrants | 15 | No |
| **T3** | Urgency | Restricting time to deliberate, forcing instant compliance ("15 minutes", "right now") | 10 | No |
| **T4** | Isolation | Digital arrest, prohibiting talking to family, demanding closed room, camera continuous on | 15 | No |
| **T5** | Payment Demand | Direct money transfer, security deposit, escrow verification account, bail clearance | 20 | **YES** |
| **T6** | Credential / OTP Pressure | Demanding OTP, UPI PIN, CVV, netbanking password, biometric or Aadhaar verification code | 20 | **YES** |
| **T7** | Remote Access Pressure | Insisting on installing AnyDesk, TeamViewer, QuickSupport, RustDesk, screen sharing | 15 | **YES** |
| **T8** | Suspicious Action / Link | Sending APK files, unknown links, short URLs, suspicious forms | 10 | No |
| **T9** | Coercive Escalation | Threatening physical home raid, police team dispatch, asset seizure, immediate arrest | 10 | No |

### 4.2. Negation & Benign Intent Disambiguation
The semantic engine applies negative dependency parsing:
- If an utterance matches an intent pattern but is governed by a negation or protective predicate:
  - *"Never share your OTP with anyone."* $\rightarrow P(\text{Credential}) = 0.0$
  - *"Do not send money, it is a scam."* $\rightarrow P(\text{Payment}) = 0.0$
  - *"The police warned me about digital arrests."* $\rightarrow P(\text{Authority}) = 0.0$

---

## 5. SCAM STAGE STATE MACHINE

The Scam Stage Machine models the structured temporal entrapment used by fraudsters:

```
[0: CONTACT] 
     ↓
[1: AUTHORITY] (CBI, Police, Customs claim)
     ↓
[2: FEAR] (Drugs in courier, money laundering, FIR)
     ↓
[3: ISOLATION] (Digital custody, do not disconnect, stay in room)
     ↓
[4: DEMAND] (Resolution requirements, verification protocol)
     ↓
[5: PAYMENT / CREDENTIAL] (Money transfer, OTP disclosure, Remote access)
     ↓
[6: CRITICAL BRAKE] (Active intervention, irreversible harm prevention)
```

### 5.1. Transition Dynamics & Hysteresis
- A single isolated utterance cannot force a jump from `CONTACT` directly to `PAYMENT/CREDENTIAL` unless corroborating contextual signals exist.
- Stage progression requires:
  1. Tactic confidence $C(T) \ge 0.65$.
  2. Coercive intent confirmation across at least 2 consecutive window evaluations.
  3. Decay over benign conversational pauses (stages gradually de-escalate if caller pivots to benign topics for $> 120$ seconds).

---

## 6. MANIPULATION VELOCITY ENGINE

### 6.1. Mathematical Formulation
Manipulation velocity measures the **temporal acceleration of psychological pressure**:

$$\mathcal{V}(t) = \frac{1}{\max(1.0, \Delta t)} \left( \sum_{i \in \text{recent}} w(T_i) \cdot P(T_i) \cdot \lambda^{\frac{t - \tau_i}{\tau_{\text{half}}}} + \Delta \text{Stage} \cdot \alpha \right)$$

Where:
- $\Delta t$: Elapsed time in minutes over the sliding analysis window (typically 90s).
- $w(T_i)$: Standard weight of tactic $T_i$.
- $P(T_i)$: Probability of tactic $T_i$.
- $\lambda = 0.5$: Exponential decay factor.
- $\tau_{\text{half}} = 45$ seconds: Half-life of coercive pressure.
- $\Delta \text{Stage}$: Difference in scam stage progression within the window.
- $\alpha = 10.0$: Escalation penalty constant.

### 6.2. Classification
- **LOW:** $\mathcal{V} < 10$ points/min (Normal deliberate conversation).
- **MODERATE:** $10 \le \mathcal{V} < 25$ points/min (Noticeable escalation).
- **HIGH:** $\mathcal{V} \ge 25$ points/min (Aggressive coercion; rapid multi-tactic barrage).

---

## 7. MULTIMODAL CONTEXT FUSION & YOLO11 PERCEPTION

### 7.1. Signal Weighting & Fusion Policy
The risk score is a synthesis of distinct, explainable modalities:

$$\text{Risk}_{\text{Fused}} = \min\left(100, \; S_{\text{conv}} \cdot 0.65 + S_{\text{stage}} \cdot 0.15 + S_{\text{velo}} \cdot 0.10 + S_{\text{visual}} \cdot 0.10\right)$$

Where:
- $S_{\text{conv}}$: Semantic conversation score derived from cumulative 9-tactic probabilities and weights.
- $S_{\text{stage}}$: Stage progression score ($[0, 10, 25, 45, 65, 90, 100]$ across stages $0 \dots 6$).
- $S_{\text{velo}}$: Velocity score mapped from $\mathcal{V}(t)$.
- $S_{\text{visual}}$: Supporting contextual vision score from YOLO11.

### 7.2. YOLO11 Contextual Perception Architecture
- **Model:** YOLO11-nano / YOLO11-small running on Edge (TFLite/NCNN on Android) or on Backend via PyTorch/TorchScript.
- **Input:** 640x640 consented camera or screen capture frames sampled at 1–2 fps.
- **Detected Classes & Contextual Semantics:**
  1. `person`: Person count (0 = caller away; 1 = 1-on-1 interaction; >1 = multiple observers/co-conspirators).
  2. `cell phone`: Secondary device usage (victim being coerced into using a second phone for banking/OTP).
  3. `laptop / monitor`: Screen share, AnyDesk, or fake video call environment.
  4. `document / paper`: Fake legal documents, forged arrest warrants, bogus police clearance certificates.
- **Strict Limitation:** YOLO11 signals are **strictly supporting**. A person holding a phone or document is **NEVER** classified as a scam unless conversation evidence confirms coercive extortion.

### 7.3. Model Disagreement Protocol
If $S_{\text{conv}} \ge 60$ (High Coercion) but $S_{\text{visual}} \le 10$ (Visual looks completely normal):
- **Decision:** Speech coercion takes **PRIMARY PRECEDENCE**.
- **Explanation Exogenous Log:** *"Visual environment appears normal, but conversational evidence indicates severe psychological coercion. Safety protocols prioritize speech evidence."*

---

## 8. SAFETY BRAKE & VERNACULAR INTERVENTION SPECIFICATION

### 8.1. Activation Invariant
The Safety Brake triggers if and only if:
$$\text{Risk Level} \ge \text{HIGH (60+)} \quad \land \quad \text{Irreversible Action Tactic Present (Payment / OTP / Remote Access)}$$

### 8.2. Triple-Action Intervention Protocol
1. **Immediate Execution Pause:** Intercepts user action before funds are transferred or credentials divulged.
2. **Tamil & English Audio Prompts:**
   - Designed for low-literacy users under intense panic.
   - Short, punchy, unambiguous voice alerts.
3. **Verification Coach Deployment:** Launches structured 7-step independent verification workflow.

---

## 9. HARDWARE & LATENCY TARGETS

| Subsystem | Execution Target | Measured Latency Target | Budget Cap |
|:---|:---|:---:|:---:|
| **Audio Capture & Framing** | Android Edge | 15 ms | 30 ms |
| **gRPC Transmission (HTTP/2)** | Network (Wi-Fi/4G) | 25 ms | 60 ms |
| **Indic ASR / Transcription** | Backend Engine | 180 ms | 350 ms |
| **JEV / Semantic Intent Engine** | Backend Engine | 95 ms | 200 ms |
| **Stage & Velocity Calculation** | Backend Engine | 5 ms | 15 ms |
| **YOLO11 Contextual Vision** | Backend / Edge | 35 ms | 70 ms |
| **Multimodal Risk Fusion** | Backend Engine | 5 ms | 10 ms |
| **gRPC RiskUpdate Response** | Network | 25 ms | 60 ms |
| **Safety Brake UI Trigger** | Android Edge | 10 ms | 25 ms |
| **TOTAL END-TO-END WARNING LATENCY** | Full Loop | **395 ms** | **< 800 ms** |

*All pipeline latencies remain well under the 1000ms threshold necessary to intercept live conversational fraud before irreversible user action.*
