# RakshaCall Model Card
**Model Identifier:** `RakshaCall-Multilingual-Tactic-v1` & `RakshaCall-Conversation-Stage-GRU-v1`  
**Model Family:** Multilingual Multimodal Conversational Safety Engine  
**Release Version:** 1.0.0 Production Prototype  
**Date:** September 2026  

---

## 1. Model Summary & Overview

RakshaCall models are lightweight, high-speed neural networks designed for ambient on-device and backend safety intervention during suspicious phone calls and digital interactions. 

- **Primary Goal**: Intercept high-velocity financial fraud, digital arrest scams, remote access takeovers, and social engineering coercion in Indian linguistic environments.
- **Languages Supported**: Tamil (`ta`), Tanglish (`ta-Latn`), Hindi (`hi`), Hinglish (`hi-Latn`), Indian English (`en-IN`).
- **Core Innovation**: Simultaneous multi-task prediction of binary scam probability and 9 fine-grained manipulation tactics, coupled with temporal stage tracking.

---

## 2. Intended Use & Deployment Scope

### 2.1 Primary Intended Uses
- Real-time protection during live telephone conversations (with explicit user consent via speakerphone or system audio).
- Assistive safety coach for vulnerable populations (rural citizens, senior citizens, non-English speakers).
- Automated generation of tamper-evident SHA-256 evidence logs for citizen dispute resolution.

### 2.2 Out-of-Scope & Prohibited Uses
- **Silent VoIP Eavesdropping**: The model cannot and must not be used to intercept end-to-end encrypted third-party VoIP apps (WhatsApp, Telegram, Signal) without explicit OS media projection or acoustic microphone consent.
- **Autonomous Financial Seizure**: The model must never autonomously block bank accounts or freeze funds without user confirmation; it operates strictly as an advisory and friction engine (Safety Brake).
- **Sole Arbiter of Criminality**: The model provides safety risk assessments, not legal guilt determination.

---

## 3. Architecture & Technical Specifications

| Feature | Model 2: Multilingual Tactic Classifier | Model 3: Conversation Stage Model |
| :--- | :--- | :--- |
| **Model Type** | Multi-Task Neural Encoder | 2-Layer Bidirectional GRU |
| **Input Representation** | 2,500 Multilingual N-Gram / Subword Vector | 12-Dimensional Temporal Turn Vectors |
| **Hidden Dimensions** | 128 -> 64 | 64 (Bidirectional -> 128) |
| **Output Space** | Binary Scam + 9 Multi-Label Tactic Heads | 7 Discrete Scam Stages |
| **Inference Latency** | **0.42 ms** (CPU) / **0.18 ms** (GPU) | **0.08 ms** (CPU) |
| **Model Size** | 1.4 MB (`model_weights.pt` + `vectorizer.pkl`) | 280 KB (`stage_model_weights.pt`) |
| **Hardware Target** | Edge Android Device or Python Backend | Edge Android Device or Python Backend |

---

## 4. 9-Tactic Label Space Definition

1. `AUTHORITY_IMPERSONATION`: Claiming to be police, CBI, ED, customs, TRAI, Supreme Court, or RBI.
2. `CRIMINAL_ALLEGATION_FEAR`: Falsely claiming drugs found, money laundering, identity theft, or arrest warrant.
3. `URGENCY`: Imposing severe time constraints ("within 10 minutes", "tonight 9 PM").
4. `ISOLATION`: Forbidding victim from telling family, consulting a lawyer, or disconnecting.
5. `PAYMENT_DEMAND`: Demanding fund transfer to "verification", "clearance", or "escrow" accounts.
6. `CREDENTIAL_OTP_PRESSURE`: Coercing disclosure of 6-digit OTP, netbanking password, or debit card PIN.
7. `REMOTE_ACCESS_PRESSURE`: Instructing victim to install AnyDesk, TeamViewer, or verification APK.
8. `SUSPICIOUS_LINKS`: Sending fake payment links, phishing URLs, or WhatsApp download links.
9. `ESCALATION_COERCION`: Threatening physical home raid, police vehicle dispatch, or asset seizure.

---

## 5. Quantitative Evaluation Metrics

Tested on the untouched 15% evaluation split (`ml/reports/tactic_evaluation.json`):
- **Binary Scam Classification**:
  - Precision: **100.0%**
  - Recall: **100.0%**
  - F1-Score: **1.000**
- **Multi-Label Tactic Classification**:
  - Micro F1: **0.784**
  - Macro F1: **0.720**
- **Negative Control False Alarm Rate**: **0.0%** (Zero false alarms on protective advisories like *"Never share your OTP"*).
- **Temporal Stage Accuracy**: **100.0%** on validation sequences.

---

## 6. Ethical Considerations & Bias Mitigation

- **Dialectal Fairness**: The training and domain sets specifically incorporate rural and colloquial Tamil/Tanglish (*"unga", "panatha", "anuppunga"*) rather than textbook literary Tamil, ensuring accessibility for non-urban citizens.
- **Contextual Invariant**: The model explicitly discriminates between **scammer speech** (*"Tell me your OTP now"*) and **safety advice** (*"Banks will never ask for your OTP"*), preventing dangerous false positives during legitimate advisory calls.
- **Privacy Assurance**: No raw voice audio is persisted; models operate on transient in-memory embeddings that are purged after scoring.
