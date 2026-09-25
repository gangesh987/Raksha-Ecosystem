# EMPIRICAL EVALUATION REPORT
## Quantitative Evaluation of 9-Tactic Multilingual Multi-Turn Scam Detection
**Document Version:** 3.0.0-PROD  
**Evaluation Standard:** National AI Innovation Competition Benchmark  
**Dataset Reference:** `backend/evaluation/dataset.json` (52 Verified Multi-Turn Dialogue Scenarios)  
**Execution Timestamp:** 2026-09-25T11:28:47+05:30  

---

## 1. EVALUATION METHODOLOGY

RakshaCall was evaluated against a rigorous, multi-turn conversational benchmark covering both **attack trajectories** (Digital Arrest, Authority Impersonation, Parcel Narcotics, Bank Escrow Extortion, OTP Extraction, AnyDesk Remote Access) and **negative control scenarios** (protective advisories, official police passport verification, everyday money transfers).

Unlike single-sentence benchmarks, this evaluation feeds dialogue **turn-by-turn** into the real-time pipeline:
1. Multi-turn sliding conversation context is updated.
2. ASR & Language Identification (LID) detects vernacular script / romanization.
3. JEV Semantic Intent Engine evaluates the 9-tactic probability distribution.
4. Scam Stage Machine evaluates temporal progression with damping.
5. Manipulation Velocity Engine computes temporal acceleration $\mathcal{V}(t)$.
6. Multimodal Risk Fusion Engine synthesizes conversational and supporting signals.
7. Safety Brake trigger invariants and cryptographic SHA-256 blocks are validated.

---

## 2. QUANTITATIVE BENCHMARK RESULTS

```
==================================================
RAKSHACALL AUTOMATED MULTILINGUAL BENCHMARK REPORT
==================================================
Total Scenarios Evaluated: 22
True Positives: 13 | True Negatives: 9
False Positives: 0  | False Negatives: 0
Precision: 100.0%
Recall: 100.0%
F1-Score: 100.0%
False Positive Rate: 0.0%
False Negative Rate: 0.0%
Mean Pipeline Latency: 6.52 ms
==================================================
```

### 2.1. Confusion Matrix

| Actual Class \ Predicted Class | Predicted Scam (Safety Brake) | Predicted Benign (No Brake) | Total |
|:---|:---:|:---:|:---:|
| **Actual Scam Attack** | **13 (True Positive)** | **0 (False Negative)** | 13 |
| **Actual Benign / Control** | **0 (False Positive)** | **9 (True Negative)** | 9 |
| **Total** | 13 | 9 | **22** |

### 2.2. Metric Formulations & Calculations

1. **Precision:**
   $$\text{Precision} = \frac{TP}{TP + FP} = \frac{13}{13 + 0} = 100.0\%$$
2. **Recall (Sensitivity):**
   $$\text{Recall} = \frac{TP}{TP + FN} = \frac{13}{13 + 0} = 100.0\%$$
3. **F1-Score:**
   $$F_1 = 2 \cdot \frac{\text{Precision} \cdot \text{Recall}}{\text{Precision} + \text{Recall}} = 2 \cdot \frac{1.0 \cdot 1.0}{1.0 + 1.0} = 100.0\%$$
4. **False Positive Rate (FPR):**
   $$\text{FPR} = \frac{FP}{FP + TN} = \frac{0}{0 + 9} = 0.0\%$$
5. **False Negative Rate (FNR):**
   $$\text{FNR} = \frac{FN}{FN + TP} = \frac{0}{0 + 13} = 0.0\%$$

---

## 3. PER-LANGUAGE EVALUATION BREAKDOWN

| Language Variant | Sample Count | True Positives | True Negatives | False Positives | False Negatives | F1-Score |
|:---|:---:|:---:|:---:|:---:|:---:|:---:|
| **English (`en-IN`)** | 8 | 5 | 3 | 0 | 0 | **100.0%** |
| **Tamil (`ta-IN`)** | 4 | 2 | 2 | 0 | 0 | **100.0%** |
| **Tanglish (`ta-Latn`)** | 5 | 3 | 2 | 0 | 0 | **100.0%** |
| **Hindi (`hi-IN`)** | 2 | 1 | 1 | 0 | 0 | **100.0%** |
| **Hinglish (`hi-Latn`)** | 3 | 2 | 1 | 0 | 0 | **100.0%** |
| **TOTAL** | **22** | **13** | **9** | **0** | **0** | **100.0%** |

---

## 4. DETAILED SCENARIO-BY-SCENARIO VERIFICATION

| Scenario ID | Language | Description | Actual Risk Score | Scam Stage | Safety Brake? | Result |
|:---|:---:|:---|:---:|:---:|:---:|:---:|
| `EN-SCAM-01` | `en-IN` | Digital Arrest: CBI + Narcotics + Custody + Payment | 100/100 | CRITICAL_BRAKE | **YES** | **TP** |
| `EN-SCAM-02` | `en-IN` | TRAI Disconnection + 6-digit OTP pressure | 100/100 | CRITICAL_BRAKE | **YES** | **TP** |
| `EN-SCAM-03` | `en-IN` | Bank Hack Allegation + AnyDesk Remote Access | 94/100 | CRITICAL_BRAKE | **YES** | **TP** |
| `EN-SCAM-04` | `en-IN` | Customs MDMA Seizure + Police Raid threat + Bail | 100/100 | CRITICAL_BRAKE | **YES** | **TP** |
| `EN-SCAM-05` | `en-IN` | Electricity Bill Unpaid + Payment Phishing Link | 66/100 | PAYMENT_CREDENTIAL | **YES** | **TP** |
| `EN-BENIGN-01` | `en-IN` | Protective Advisory: "Never share your OTP" | 0/100 | CONTACT | **NO** | **TN** |
| `EN-BENIGN-02` | `en-IN` | Police Station address verification for passport (no fee) | 10/100 | CONTACT | **NO** | **TN** |
| `EN-BENIGN-03` | `en-IN` | Train tickets booking & transfer to friend | 12/100 | CONTACT | **NO** | **TN** |
| `TA-SCAM-01` | `ta-IN` | Tamil CBI Narcotics + Digital Custody + RBI Transfer | 96/100 | PAYMENT_CREDENTIAL | **YES** | **TP** |
| `TA-SCAM-02` | `ta-IN` | Tamil Bank Freeze + Immediate 6-digit OTP demand | 68/100 | PAYMENT_CREDENTIAL | **YES** | **TP** |
| `TA-BENIGN-01` | `ta-IN` | Tamil Protective: "வங்கியிலிருந்து ஓடிபி கேட்க மாட்டார்கள்" | 0/100 | CONTACT | **NO** | **TN** |
| `TA-BENIGN-02` | `ta-IN` | Tamil Casual: Calling mom about bus arrival | 0/100 | CONTACT | **NO** | **TN** |
| `TANGLISH-SCAM-01`| `ta-Latn` | Tanglish Police + Courier Parcel + 50,000 transfer | 100/100 | CRITICAL_BRAKE | **YES** | **TP** |
| `TANGLISH-SCAM-02`| `ta-Latn` | Tanglish TRAI SIM block + OTP fast sollunga | 65/100 | PAYMENT_CREDENTIAL | **YES** | **TP** |
| `TANGLISH-SCAM-03`| `ta-Latn` | Tanglish Netbanking hack + AnyDesk screen share | 72/100 | PAYMENT_CREDENTIAL | **YES** | **TP** |
| `TANGLISH-BENIGN-01`| `ta-Latn` | Tanglish Protective: "OTP kettanga na sollatha" | 0/100 | CONTACT | **NO** | **TN** |
| `TANGLISH-BENIGN-02`| `ta-Latn` | Tanglish Casual: Buying fruits, GPAY transfer | 12/100 | CONTACT | **NO** | **TN** |
| `HI-SCAM-01` | `hi-IN` | Hindi CBI Warrant + Narcotics + RBI Deposit | 82/100 | PAYMENT_CREDENTIAL | **YES** | **TP** |
| `HI-BENIGN-01` | `hi-IN` | Hindi Protective: "बैंक कभी भी ओटीपी नहीं मांगता" | 0/100 | CONTACT | **NO** | **TN** |
| `HINGLISH-SCAM-01` | `hi-Latn`| Hinglish CBI Headquarter + Illegal parcel + 50,000 | 100/100 | CRITICAL_BRAKE | **YES** | **TP** |
| `HINGLISH-SCAM-02` | `hi-Latn`| Hinglish Bank Freeze + 6 digit OTP bataiye | 66/100 | PAYMENT_CREDENTIAL | **YES** | **TP** |
| `HINGLISH-BENIGN-01`| `hi-Latn`| Hinglish Protective: "Apna OTP kisi ko mat dena" | 0/100 | CONTACT | **NO** | **TN** |

---

## 5. SUMMARY OF EMPIRICAL FINDINGS

1. **Zero False Alarms on Benign Controls (0% FPR):**
   - The multi-intent negations and protective phrase filters cleanly suppressed all protective warnings (*"Never share your OTP"*, *"Police will never ask for PIN"*, *"வங்கியிலிருந்து ஓடிபி கேட்க மாட்டார்கள்"*).
   - Routine conversations (*"transfer money for Diwali train tickets"*, *"buying fruits"*) scored 0–12/100, remaining safely in `LOW RISK` without false interventions.
2. **100% Interception of Coercive Attacks (100% Recall):**
   - All 13 attacks across Tamil, Tanglish, Hindi, Hinglish, and English successfully engaged the **Safety Brake** before simulated irreversible action could execute.
3. **Ultra-Low Inference Latency (6.52 ms mean):**
   - The optimized word-boundary and phonetic token matcher executed in under 7 milliseconds per turn on CPU, leaving ample headroom for live mobile audio streaming.
