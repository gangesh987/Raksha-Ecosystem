# PHASE 2: MULTILINGUAL ROBUSTNESS & ADVERSARIAL HARDENING REPORT
**RakshaCall Scam Detection System**
**Report Date:** 2026-09-26
**Git Commit:** 511e6f7
**Phase 1 Baseline Commit:** 6bb6451

---

> [!IMPORTANT]
> **FROZEN TEST SET INTEGRITY:** SHA-256 `6696db171f4e33d76ac01c622ee4c89ac71de9ffa07b137818e36f806bfa56a0`
> The 182-conversation test set at `ml/datasets/final/test_conversations.jsonl` was NOT modified.
> All new evaluations use separate datasets.

---

## Status Legend
| Badge | Meaning |
|-------|---------|
| 🟢 GREEN | Independently verified, reproducible |
| 🟡 YELLOW | Directional only — small N, not statistically robust |
| 🔴 RED | Unsupported, failed, or requires action |

---

## 1. Dataset Composition

### 1.1 Frozen Test Set (Baseline — Phase 1)
- **File:** `ml/datasets/final/test_conversations.jsonl`
- **Conversations:** 182 | **Turns:** 1,292
- **Language gap (critical):** English 174, Hindi 4, Tanglish 2, Hinglish 1, Tamil 1

### 1.2 New Indic Holdout Corpus 🟡
- **File:** `ml/datasets/evaluation/indic_holdout/indic_holdout_conversations.jsonl`
- **Type:** SYNTHETIC — hand-authored, NOT from real calls
- **Total:** 31 conversations (18 scam, 13 benign)
- **Languages:** Tamil (`ta`), Tanglish (`ta-Latn`), Hindi (`hi`), Hinglish (`hi-Latn`)
- **Warning:** N=5–10 per language/class. Only directional conclusions supported.

### 1.3 Adversarial Benchmark v2 🟢
- **File:** `tests/ml/adversarial_benchmark_v2.jsonl`
- **Total:** 70 examples across 14 categories (A–N)
- **Purpose:** Test model on phrasing that avoids the `TACTIC_PHRASES` dictionary

### 1.4 Hard Negatives 🟢
- **File:** `tests/ml/hard_negatives.jsonl`
- **Total:** 50 benign examples with high-density scam vocabulary
- **Purpose:** Measure false positive rate on legitimate calls and protective language

---

## 2. Leakage Checks

| Check | Result |
|-------|--------|
| Indic holdout IDs overlap with test set | None (disjoint `indic_holdout_*` prefix) |
| Adversarial v2 copies from TACTIC_PHRASES | No — verified by author inspection |
| Hard negatives sourced from training data | No — independently authored |
| Test set hash post-Phase-2 | ✅ `6696db17...` unchanged |

**Audit document:** [`INDIC_DATASET_AUDIT.md`](file:///c:/Users/gangs/Downloads/APP%20OF%20RAKSHA/ml/datasets/evaluation/indic_holdout/INDIC_DATASET_AUDIT.md)

---

## 3. Language-Wise Metrics (Indic Holdout) 🟡

Evaluated with the Phase 1 threshold (0.50).

| Language | N | Neural F1 | Hybrid F1 | FNR (Neural) | FPR (Neural) |
|----------|---|-----------|-----------|--------------|--------------|
| Hindi (`hi`) | 8 | 0.7273 | **0.8333** | 0.2000 | 0.6667 |
| Hinglish (`hi-Latn`) | 5 | 0.6667 | 0.6667 | 0.3333 | 0.5000 |
| Tamil (`ta`) | 10 | 0.2857 | **0.7692** | 0.8000 | 0.2000 |
| Tanglish (`ta-Latn`) | 8 | 0.2500 | **0.9091** | 0.8000 | 0.6667 |

> [!WARNING]
> **Critical Finding — Tamil/Tanglish:** Neural model alone achieves only 0.2857 and 0.2500 F1.
> FNR = 80% — the model misses 4 out of 5 scam conversations in these languages.
> The RuleBasedSafetyFloor is essential: it raises Tanglish Hybrid F1 from 0.25 → **0.91**.
>
> **These N values (5–10) are too small for statistical confidence. Treat as directional.**

> [!NOTE]
> Phase 1 finding confirmed: Tamil/Tanglish neural recall failure is real and persistent.

---

## 4. Adversarial Benchmark v2 Metrics 🟢

**70 scam examples, 14 categories. No benign examples in this set.**

### Overall (All 70 examples)
| System | Precision | Recall | F1 | FNR |
|--------|-----------|--------|-----|-----|
| Neural Only | 1.0000 | 0.7429 | 0.8525 | 0.2571 |
| Rule Floor Only | 1.0000 | 0.6286 | 0.7719 | 0.3714 |
| **Hybrid JEV** | **1.0000** | **0.9571** | **0.9781** | **0.0429** |

**The hybrid architecture reduces adversarial FNR from 25.7% to 4.3% — a 6× improvement.**

### Per-Category Breakdown
| Category | Neural | Hybrid | Notes |
|----------|--------|--------|-------|
| A: Indirect payment | 100% | 100% | Neural captures financial framing |
| B: Indirect OTP | 80% | 100% | Rule floor catches 'digits from phone' |
| C: Indirect credential | 100% | 100% | |
| D: Indirect remote access | 100% | 100% | |
| E: Implied authority | 100% | 100% | |
| F: Fear without 'arrest' | 100% | 100% | |
| G: Urgency without keyword | 80% | 80% | **Miss: vague deadline language** |
| H: Isolation without keyword | 100% | 100% | |
| I: Polite professional scam | 80% | 100% | |
| J: Code-switching | 40% | 100% | Neural struggles with mixed language |
| K: Tamil | **0%** | **60%** | **Critical neural gap** |
| L: Tanglish | 60% | 100% | Rule floor rescues all |
| M: Hindi | 80% | 100% | |
| N: Hinglish | **20%** | **100%** | Neural nearly completely fails |

> [!CAUTION]
> **Category K (Tamil): Neural = 0/5 correct.** The BiGRU model trained predominantly on English data cannot generalize to Tamil script. Rule floor partially compensates but only achieves 60%.
>
> **Category N (Hinglish): Neural = 1/5 correct (20%).** Romanized Hindi confuses the English/subword tokenizer.
>
> **These are structural model weaknesses, not edge cases.**

---

## 5. Hard Negative Metrics (False Positive Rate) 🔴

**50 benign examples containing scam vocabulary in protective/legitimate contexts.**

| System | False Positives | FPR |
|--------|-----------------|-----|
| Neural Only | 37/50 | **74.0%** |
| Rule Floor Only | 19/50 | **38.0%** |
| Hybrid JEV | 31/50 | **62.0%** |

### Per-Category False Positive Rate (Neural | Hybrid)
| Category | Neural FPR | Hybrid FPR |
|----------|-----------|------------|
| Protective negation ("Police will never ask OTP") | 90% | 40% |
| Bank legitimate calls | 100% | 100% |
| Police legitimate calls | 67% | 100% |
| Government services | 100% | 100% |
| IT support (legitimate) | 100% | 67% |
| UPI legitimate | 100% | 67% |
| Family emergency | 33% | 33% |
| Security training | 67% | 33% |
| Fraud reporting | 33% | 67% |
| Protective Tamil | **0%** | **0%** |
| Protective Tanglish | 100% | 50% |
| Protective Hinglish | 50% | **0%** |

> [!CAUTION]
> **FPR = 74% on hard negatives is unacceptable for production deployment.**
>
> **Root cause:** The neural model was trained primarily on explicit scam dialogues. It has not learned the semantic context that distinguishes "police will never ask for OTP" (protective) from "police asks for OTP" (scam). The vocabulary overlap is too high for the subword tokenizer to differentiate.
>
> **Key asymmetry:** Tamil hard negatives have 0% FPR (model doesn't understand Tamil well enough to false-trigger), but Tamil scam FNR = 80%. This is a dangerous asymmetry — the model is neither catching Tamil scams NOR false-flagging Tamil benign calls.
>
> **The Rule Floor's PROTECTIVE_PATTERNS provide partial mitigation** — they correctly reject protective_negation calls at a higher rate than neural alone. But FPR remains unacceptably high for legitimate bank/government calls that contain authority vocabulary.

---

## 6. Calibration Results 🔴

**Brier Score:** 0.1888 (Baseline: 0.2460, Skill Score: +0.23)
**Expected Calibration Error (ECE):** **0.1791**
**Calibrated:** ❌ NO (ECE threshold = 0.10)

### Reliability Curve (key observations)
| Score Range | N | Mean Conf | Actual Positive Rate | Calibration Error |
|-------------|---|-----------|----------------------|-------------------|
| [0.0, 0.1) | 45 | 0.009 | 13.3% | 0.124 |
| [0.5, 0.6) | 3 | 0.580 | 0.0% | **0.580** |
| [0.6, 0.7) | 5 | 0.633 | 20.0% | **0.433** |
| [0.9, 1.0) | 65 | 0.991 | 67.7% | **0.314** |

> [!IMPORTANT]
> **`scam_probability` is NOT a calibrated probability.**
>
> The BiGRU model is highly overconfident — scoring 0.99 on many cases where the actual positive rate is only 67.7%. The "probability" output reflects the sigmoid activation, not a true frequency calibration.
>
> **Required Action:** Rename to `model_score` in all user-facing documentation, API responses, and UI until Platt scaling or isotonic regression calibration is applied.
>
> The field name in `JEVAnalysisResult` and gRPC response has been documented for this requirement. Code rename is deferred to Phase 3.

---

## 7. Threshold Analysis 🟢

**Evaluated on frozen validation set (181 conversations, 102 scam, 79 benign).**
**Test set NOT touched.**

| Threshold | Precision | Recall | F1 | FPR | FNR |
|-----------|-----------|--------|-----|-----|-----|
| 0.30 | 0.7344 | 0.9216 | 0.8174 | 0.4304 | 0.0784 |
| 0.40 | 0.7402 | 0.9216 | 0.8210 | 0.4177 | 0.0784 |
| 0.50 (current) | 0.7440 | 0.9118 | 0.8194 | 0.4051 | 0.0882 |
| 0.60 | 0.7623 | 0.9118 | 0.8304 | 0.3671 | 0.0882 |
| 0.70 | 0.7863 | 0.9020 | 0.8402 | 0.3165 | 0.0980 |
| 0.80 | 0.7982 | 0.8922 | **0.8426** | 0.2911 | 0.1078 |
| 0.90 | 0.7946 | 0.8725 | 0.8318 | 0.2911 | 0.1275 |

**Recommended threshold:** 0.40 (by safety objective: minimize FNR, subject to FPR ≤ 0.50)
- FNR at 0.40: 7.84% | FPR at 0.40: 41.8%

> [!NOTE]
> No threshold setting resolves the hard-negative FPR problem (which runs 62–74%). The threshold operates on a model that systematically confuses scam vocabulary in protective contexts with actual scams. Calibration + contrastive training are needed.

---

## 8. Multi-Turn Testing 🟡

**6 conversations tested against JEV with cumulative context window.**

| Scenario | Outcome | Notes |
|----------|---------|-------|
| Gradual escalation (English) | ✅ Detected all turns as SCAM | Neural score 0.9999 from turn 1 — extremely fast detection |
| OTP escalation (English) | ❌ MISSED turns 3-4 | "I am sending an OTP" — indirect phrasing scores 0.01. **Neural gap.** |
| Reversal (victim recognizes scam) | ❌ DOES NOT reduce risk | Score stays 0.9986 after protective turns. System remains high-risk permanently. |
| Slow-burn trust build (English) | ⚠️ Triggers too early | Scam detected at turn 1 ("Hello, how are you?") — false early flag |
| Direct Hindi scam | ⚠️ Partial | Turns 1+3 missed (score < 0.50), turns 2+4 caught |
| Direct Tanglish scam | ❌ ALL 4 TURNS MISSED | Scores 0.0008–0.0002 throughout. Complete Tanglish neural failure. |

> [!WARNING]
> **Three critical multi-turn failures:**
>
> 1. **Reversal failure:** The system permanently maintains high risk scores after scam vocabulary appears, even when the user explicitly identifies the scam and says protective phrases. The RuleBasedSafetyFloor's `is_protective()` check only works when the CURRENT utterance contains protective language — cumulative context contamination prevents risk reduction.
>
> 2. **OTP indirect phrasing miss:** "I am sending an OTP to your number. Please share it" scored 0.01. The model does not recognize this as a credential request.
>
> 3. **Complete Tanglish failure:** All 4 Tanglish turns scored < 0.001. The BiGRU cannot process romanized Tamil at all without the rule floor.

---

## 9. ASR Validation 🔴

**ASR section is NOT completed in Phase 2.**

Reasons:
- No test audio files are available in the repository.
- The Android `SpeechRecognizer` operates on-device and cannot be intercepted from the backend Python evaluation pipeline.
- Real voice-to-text latency measurement requires either physical device testing or a local ASR model (Whisper/Vosk).

**Document created:** `docs/REAL_VOICE_PIPELINE_BENCHMARK.md` — contains methodology and placeholder structure.

The JEV CPU latency from Phase 1 (270 ms text-only) remains the only measured latency.
It CANNOT be reported as voice-to-brake latency.

---

## 10. Audio Error Robustness 🔴

**Not evaluated in Phase 2 — blocked by same constraint as ASR.**

No audio test files available. Cannot simulate noise, fast speech, or accent variation without audio.

---

## 11. Model Ablation (Adversarial v2 — 70 examples) 🟢

| Configuration | F1 | Recall | FNR | FPR (Hard Neg) |
|---------------|----|--------|-----|-----------------|
| A: Neural Only | 0.8525 | 0.7429 | 25.7% | 74.0% |
| B: Rule Floor Only | 0.7719 | 0.6286 | 37.1% | 38.0% |
| C: Neural + Rule Floor (Hybrid) | **0.9781** | **0.9571** | **4.3%** | 62.0% |

**The hybrid architecture genuinely improves recall** — FNR reduction from 25.7% → 4.3% on adversarial examples is real and significant.

**Trade-off:** The hybrid does NOT improve false positives on hard negatives (FPR 62% vs 74% neural, 38% rule only). The rule floor's PROTECTIVE_PATTERNS provide some improvement for protective negation phrases, but cannot rescue legitimate bank/government calls that use authority vocabulary.

---

## 12. Failure Examples

### Confirmed Failure Cases (System misses scam)

| ID | Text | Neural Score | Hybrid | Root Cause |
|----|------|-------------|--------|------------|
| adv_K_001–005 | Tamil scam phrases | 0.00–0.05 | 0.60 | Out-of-vocabulary Tamil script |
| adv_N_001–004 | Hinglish phrases | 0.01–0.15 | 1.00 | Tokenizer confusion |
| multiturn_escalating_002 | "I am sending an OTP to your registered number. Please share it." | 0.01 | 0.01 | Indirect phrasing not in training |
| multiturn_tanglish | All 4 turns: Tanglish CBI/drugs/OTP | 0.0002–0.0008 | 0.0008 | Neural blind to Tanglish |

### Confirmed False Positive Cases (System flags benign)

| Category | Example | Neural Score | Problem |
|----------|---------|-------------|---------|
| Bank legitimate | "SBI calling about your home loan approval" | ~0.99 | Authority + financial vocabulary |
| Protective negation | "Police will never ask for OTP" | ~0.95 | OTP word triggers without context |
| Government service | "Income Tax refund pending — verify on portal" | ~0.99 | Refund + verify vocabulary |
| Security training | "In this exercise, imagine a CBI digital arrest call" | ~0.80 | Training scenario misidentified |

---

## 13. Remaining Limitations

### Structural Limitations (Not fixable by threshold tuning)

1. **🔴 Tamil/Tanglish blind spot:** Neural model cannot process Tamil script or romanized Tamil. Architecture requires either multilingual embedding (e.g., mBERT, IndicBERT) or a dedicated Indic language model.

2. **🔴 Calibration gap:** ECE = 0.18. Scores cannot be interpreted as probabilities. Platt scaling required before any probability claims in user-facing output.

3. **🔴 High hard-negative FPR (62–74%):** The model was not trained on contrastive protective/educational examples. It cannot distinguish "Police will never ask for OTP" from a scam requesting OTP. Requires retraining with negative examples.

4. **🔴 Reversal blindness:** Once scam vocabulary accumulates in context, the system cannot reduce risk even when protective language is introduced. The cumulative window design lacks a decay mechanism.

5. **🔴 Indirect OTP phrasing miss:** "I am sending a code to your number. Please share it" is not recognized as credential pressure. Model learned explicit "give me OTP" patterns, not indirect request patterns.

6. **🔴 ASR latency unmeasured:** Voice-to-brake latency is unknown. Text-only JEV latency (270 ms) is the only measurement.

### Scope-Limited Limitations

7. **🟡 Indic holdout N too small:** 5–10 examples per language is insufficient for statistical confidence. Directional findings only.

8. **🟡 SUSPICIOUS_LINKS tactic under-represented:** URL and APK-based scams are voice-call-rare but not covered in the synthetic holdout.

---

## Summary

| Component | Status | Evidence |
|-----------|--------|----------|
| Frozen test set integrity | 🟢 VERIFIED | SHA-256 unchanged |
| Indic holdout created | 🟢 DONE | 31 synthetic conversations |
| Adversarial v2 benchmark | 🟢 DONE | 70 examples, 14 categories |
| Hard negatives | 🟢 DONE | 50 examples |
| Threshold calibration | 🟢 DONE | Val set, 7 thresholds |
| Model calibration | 🔴 FAIL | ECE = 0.18, not calibrated |
| Hard negative FPR | 🔴 FAIL | 62–74% false positives |
| Tamil/Tanglish neural | 🔴 FAIL | 0–25% F1 |
| Hybrid JEV value | 🟢 CONFIRMED | FNR reduced 25.7% → 4.3% adversarial |
| Multi-turn reversal | 🔴 FAIL | System stays high-risk after protective turns |
| ASR evaluation | 🔴 NOT DONE | No audio data available |

### This system is NOT production-ready after Phase 2.

**What is confirmed working:**
- English scam detection (F1 0.8053 on independent test set)
- Hybrid architecture genuinely improves adversarial recall
- Rule floor correctly rescues Tanglish/Hindi where neural fails
- Test set integrity preserved throughout

**What must be fixed before production:**
- Tamil/Tanglish neural coverage (requires IndicBERT or multilingual model)
- Hard-negative false positive rate (requires contrastive retraining)
- Model calibration (requires Platt scaling)
- Reversal decay mechanism
- Real ASR latency measurement

---

*Report generated from empirical evaluation on 2026-09-26.*
*All evaluation scripts are reproducible from the committed repository.*
*Git commit: `511e6f7`*
