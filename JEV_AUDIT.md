# RAKSHACALL JEV (JOINT EMBEDDING / INTENT VERIFIER) AUDIT

**Document Version:** 1.0.0-AUDIT  
**Date:** September 25, 2026  
**Auditor:** Antigravity Autonomous ML & Safety Engineering Team  
**Audit Objective:** Technical and architectural inspection of the JEV engine across `backend/app/ai/jev_provider.py` and downstream consumers.

---

## 1. EXECUTIVE SUMMARY & FORENSIC VERDICT

| Component | Marketing / Documented Name | Actual Implementation | Forensic Reality | Audit Verdict |
| :--- | :--- | :--- | :--- | :--- |
| **Local JEV Engine** | "Joint Embedding Intent Verifier" | `LocalSemanticJEVProvider` | A Python class containing hardcoded keyword lists (`INTENT_PROTOTYPES`), regex patterns (`TACTIC_PHRASES`), and string overlap scoring. | ⚠️ **HEURISTIC DECISION ENGINE** (Not a Neural Model) |
| **Cloud JEV Engine** | "Cloud Semantic JEV" | `CloudLLMJEVProvider` | An HTTP wrapper calling Groq (LLaMA-3-70B/8B) or Google Gemini API. | ✅ **GENUINE CLOUD LLM WRAPPER** |
| **JEV Neural Weights** | Purported proprietary JEV weights | **None exist** | No PyTorch `.pt`, TensorFlow `.pb`, or ONNX model exists named "JEV". | ❌ **NON-EXISTENT PROPRIETARY MODEL** |

---

## 2. SOURCE CODE INSPECTION OF `backend/app/ai/jev_provider.py`

### 2.1 Class Structure & Interfaces

```python
class JEVProvider(ABC):
    @abstractmethod
    def analyze_window(self, current_utterance, conversation_context, detected_language) -> JEVAnalysisResult:
        pass
```

The system implements two concrete providers:
1. `LocalSemanticJEVProvider` (Default offline provider)
2. `CloudLLMJEVProvider` (Cloud provider, active if `GROQ_API_KEY` is present)
3. `JEVProviderFactory.get_provider()` (Factory pattern selecting between the two)

### 2.2 Forensic Analysis of `LocalSemanticJEVProvider`

Inspection of lines 58–277 in `backend/app/ai/jev_provider.py`:

1. **`INTENT_PROTOTYPES`**: A Python dictionary containing prototype word lists for 9 tactics:
   - `AUTHORITY`: `"cbi officer cyber crime branch supreme court high court mumbai police ..."`
   - `FEAR`: `"illegal parcel seized narcotics drugs found money laundering ..."`
   - `URGENCY`: `"immediately right now within 10 minutes within 15 minutes ..."`
   - `ISOLATION`: `"do not disconnect stay on the call keep camera on do not tell anyone ..."`
   - `PAYMENT`: `"transfer money funds to clearance account escrow account ..."`
   - `CREDENTIAL`: `"give me otp share the otp enter upi pin card cvv ..."`
   - `REMOTE_ACCESS`: `"install download anydesk teamviewer quicksupport ..."`
   - `SUSPICIOUS_LINK`: `"click link download apk verify account fill this form ..."`
   - `ESCALATION`: `"sending police patrol to your house raid your home physical arrest ..."`
2. **`TACTIC_PHRASES`**: High-signal multi-word regex phrases.
3. **`PROTECTIVE_PATTERNS`**: Negation regexes (e.g. `r'(never share|do not share|don\'t share)'`).
4. **Scoring Algorithm (`_compute_intent_similarity`)**:
   - Compares input tokens against prototype words.
   - If a multi-word phrase matches: assigns fixed score `0.92` (or `0.75` if in historical window).
   - If discriminative terms (e.g. `"cbi"`, `"otp"`, `"anydesk"`) match: computes $0.35 + 0.15 \times \text{count}$, capped at $0.96$.
   - Does not perform dot products over continuous neural embedding spaces (e.g., BERT, RoBERTa).
   - Does not use GPU/TPU acceleration.

---

## 3. WHAT JEV ACTUALLY IS (HONEST RECLASSIFICATION)

To present this system honestly to technical juries, researchers, and regulatory bodies:

1. **JEV is a Semantic Decision & Tactic Policy Engine:**
   - It is a rule-and-heuristic arbitration layer designed to run with zero dependencies and sub-5 millisecond latency.
   - It excels at catching explicit scam coercion keywords across Indian vernacular scripts and Romanized transliterations.
2. **JEV is NOT an Deep Neural Network:**
   - It should never be described as "a joint embedding neural network" or "JEV AI model".
   - It is a **deterministic heuristic classifier with negation suppression**.
3. **Cloud Fallback is Real:**
   - When configured with `GROQ_API_KEY`, `CloudLLMJEVProvider` genuinely invokes cloud LLMs (LLaMA-3 or Gemini) to perform zero-shot conversational reasoning.

---

## 4. CODEBASE INTEGRATION RECOMMENDATION

- Maintain `LocalSemanticJEVProvider` as the **Deterministic Safety Guardrail** in the pipeline.
- Pair it with the **trained Multilingual Semantic Model** (MuRIL / XLM-R / Multi-Task Transformer).
- **Architecture Hierarchy:**
  ```
  [ Raw Transcript ]
          │
          ├──> [ Trained Multilingual Semantic Transformer ] ──> Probabilistic Intent (0.0 - 1.0)
          │                                                            │
          └──> [ JEV Heuristic Guardrail Engine ] ────────────────────┤
                                                                       ▼
                                                       [ Multimodal Risk Fusion ]
                                                                       │
                                                       [ Safety Brake Arbitrator ]
  ```
- Clearly distinguish in all architecture documentation between **Probabilistic ML Inference** and **Deterministic Safety Guardrails**.
