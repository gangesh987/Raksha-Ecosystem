# RakshaCall Held-Out Dataset Audit (Step 3)

**Audit Date:** 2026-09-26  
**Auditor:** Antigravity Autonomous Hardening Agent  
**Objective:** Formally freeze and verify the independence, provenance, and leakage status of all candidate evaluation datasets prior to held-out evaluation.

---

## 1. Candidate Evaluation Datasets Inventory

| Dataset Identifier | File Path | Format | Records | Total Turns | Unique Utterances | Internal Dups | Primary Labels | Languages | SHA-256 Hash |
|---|---|---|---|---|---|---|---|---|---|
| **Test Conversations** | `ml/datasets/final/test_conversations.jsonl` | JSONL (convo) | 182 | 1,292 | 1,193 | 99 | 102 Scam / 80 Benign | en (174), hi (4), ta-Latn (2), hi-Latn (1), ta (1) | `6696db171f4e33d76ac01c622ee4c89ac71de9ffa07b137818e36f806bfa56a0` |
| **Test Turns** | `ml/datasets/final/test_turns.jsonl` | JSONL (turn) | 1,292 | 1,292 | 1,193 | 99 | 1,144 Scam / 148 Benign | en (1236), hi (38), ta-Latn (10), hi-Latn (4), ta (4) | `df06169d5a6196d21522eaf6de7a5c0e274eff82d3adc5e4a2718098b37a87ba` |
| **Hard Negatives** | `evaluation/hard_negatives.jsonl` | JSONL (utterance) | 30 | 30 | 30 | 0 | 0 Scam / 30 Benign | en (10), ta (5), ta-Latn (5), hi (5), hi-Latn (5) | `c4b9d59f46dd4e6ec5071dd0ccac9c20cb28550a62ab77485b041c70fd31461f` |
| **Unseen Scam Convs** | `evaluation/unseen_scam_conversations.jsonl` | JSONL (convo) | 5 | 22 | 22 | 0 | 5 Scam / 0 Benign | en (1), ta (1), ta-Latn (1), hi (1), hi-Latn (1) | `80b9746888cbc1de60ed97f19660dbea20928850a696cc5b2336f2a43635be02` |
| **Unseen Benign Convs** | `evaluation/unseen_benign_conversations.jsonl` | JSONL (convo) | 4 | 12 | 12 | 0 | 0 Scam / 4 Benign | en (1), ta (1), ta-Latn (1), hi (1) | `3448a5e0477a3813c1edaefae70366351edeec871c1a7c355bef4de79decae7d` |
| **English Slice** | `evaluation/english_cases.jsonl` | JSONL (turn) | 21 | 21 | 21 | 0 | 7 Scam / 14 Benign | en (21) | `153c6efde11a0448f797ace0a0ace5991ab2cf8a2c9f0dcca1cad55753cedec4` |
| **Tamil Slice** | `evaluation/tamil_cases.jsonl` | JSONL (turn) | 12 | 12 | 12 | 0 | 4 Scam / 8 Benign | ta (12) | `e3c5fb119c13400b48933d318c9ac67ff1b772fad76c57830b573738a8c163fc` |
| **Tanglish Slice** | `evaluation/tanglish_cases.jsonl` | JSONL (turn) | 12 | 12 | 12 | 0 | 4 Scam / 8 Benign | ta-Latn (12) | `75f8f2327b5c38647413b1449215edbd6211d008401065bae59b8d570b66e6fa` |
| **Hindi Slice** | `evaluation/hindi_cases.jsonl` | JSONL (turn) | 10 | 10 | 10 | 0 | 3 Scam / 7 Benign | hi (10) | `33b6564349cfd0123730bab538ef9fb15822bea45017b3572b9ea53b3832afc9` |
| **Hinglish Slice** | `evaluation/hinglish_cases.jsonl` | JSONL (turn) | 9 | 9 | 9 | 0 | 4 Scam / 5 Benign | hi-Latn (9) | `5d0de8f24b19e58f67977f0caae6f34851bc8e4303ea6ba3ed6108dd7dd701d6` |
| **Indic Holdout** | `ml/datasets/evaluation/indic_holdout/indic_holdout_conversations.jsonl` | JSONL (convo) | 31 | 90 | 90 | 0 | 18 Scam / 13 Benign | ta (10), ta-Latn (8), hi (8), hi-Latn (5) | `c26f3c0208fbded9cf4b16f5dfe99988450a9a2d66b20147ae739639a947fa7e` |
| **Legacy 22-Scenario** | `backend/evaluation/dataset.json` | JSON (scenario) | 22 | 60 | 60 | 0 | 13 Scam / 9 Benign | en-IN (8), ta-IN (4), ta-Latn (5), hi-IN (2), hi-Latn (3) | `0649d9a416bcb81bdb2d739413e8accd97e3b303621222f54c7dc5894862ae85` |

---

## 2. Holdout Status Determination

### Reference Training & Validation Sets
- **Training Set:** `ml/datasets/final/train_turns.jsonl` (4,541 turns, 4,244 unique normalized utterances, 841 unique `conversation_id`s, SHA-256: `57657bf7...`).
- **Validation Set:** `ml/datasets/final/val_turns.jsonl` (1,042 turns, 1,001 unique normalized utterances, 181 unique `conversation_id`s, SHA-256: `6e026c93...`).

### Holdout Categories
- **Category A (Used for Training):** Any sample present in `train_turns.jsonl` or used in optimizer gradient updates.
- **Category B (Used for Validation):** Any sample present in `val_turns.jsonl` used for early stopping or tokenizer vocabulary fitting.
- **Category C (INDEPENDENT HELD-OUT TEST DATA):** Completely unseen during tokenizer fitting, training, and early stopping.
- **Category YELLOW (Holdout Status Unverified):** Samples whose provenance or overlap cannot be verified.

### Verification Results

| Dataset | Training Overlap (IDs / Texts) | Validation Overlap (IDs / Texts) | Subword Tokenizer Fit | Category | Holdout Classification |
|---|---|---|---|---|---|
| `test_conversations.jsonl` / `test_turns.jsonl` | **0 / 0** | **0 / 0** | Excluded | **Category C** | **INDEPENDENT HELD-OUT TEST DATA** |
| `evaluation/hard_negatives.jsonl` | **0 / 0** | **0 / 0** | Excluded | **Category C** | **INDEPENDENT HELD-OUT TEST DATA (Hard Negatives)** |
| `evaluation/unseen_scam_conversations.jsonl` | **0 / 0** | **0 / 0** | Excluded | **Category C** | **INDEPENDENT HELD-OUT TEST DATA (Multi-Turn Scams)** |
| `evaluation/unseen_benign_conversations.jsonl` | **0 / 0** | **0 / 0** | Excluded | **Category C** | **INDEPENDENT HELD-OUT TEST DATA (Multi-Turn Benign)** |
| `evaluation/english_cases.jsonl` | **0 / 0** | **0 / 0** | Excluded | **Category C** | **INDEPENDENT HELD-OUT TEST DATA (English Slice)** |
| `evaluation/tamil_cases.jsonl` | **0 / 0** | **0 / 0** | Excluded | **Category C** | **INDEPENDENT HELD-OUT TEST DATA (Tamil Slice)** |
| `evaluation/tanglish_cases.jsonl` | **0 / 0** | **0 / 0** | Excluded | **Category C** | **INDEPENDENT HELD-OUT TEST DATA (Tanglish Slice)** |
| `evaluation/hindi_cases.jsonl` | **0 / 0** | **0 / 0** | Excluded | **Category C** | **INDEPENDENT HELD-OUT TEST DATA (Hindi Slice)** |
| `evaluation/hinglish_cases.jsonl` | **0 / 0** | **0 / 0** | Excluded | **Category C** | **INDEPENDENT HELD-OUT TEST DATA (Hinglish Slice)** |
| `indic_holdout_conversations.jsonl` | **0 / 0** | **0 / 0** | Excluded | **Category C** | **INDEPENDENT HELD-OUT TEST DATA (Indic Multi-Turn)** |
| `backend/evaluation/dataset.json` (22 scenarios) | **0 / 0** | **0 / 0** | Excluded | **LEGACY SMOKE TEST** | **LEGACY RULE-ORIENTED FUNCTIONAL SMOKE TEST** |

---

## 3. Data Leakage Analysis

### Methodology
Every unique utterance in each candidate dataset was tested for:
1. **Exact string match** against `train_turns.jsonl` and `val_turns.jsonl`.
2. **Normalized string match** (case-folding, whitespace trimming, punctuation stripping).
3. **Conversation ID collision** against all 841 training and 181 validation conversations.
4. **Heuristic dictionary leakage**:
   - `TACTIC_PHRASES` (98 regex phrases in `RuleBasedSafetyFloor`)
   - `INTENT_PROTOTYPES` (279 distinct non-stopword tokens in `RuleBasedSafetyFloor`)
   - `DISCRIMINATIVE_TERMS` (21 high-signal scam terms in `RuleBasedSafetyFloor`)

### Leakage Audit Findings

```
Candidate Set: test_conversations.jsonl (1,193 unique utterances)
  Train text collision: 0 (0.00%)
  Val text collision: 0 (0.00%)
  Train conversation ID collision: 0 (0.00%)
  Tactic phrase overlap: 57 / 1,193 (4.8%)
  Discriminative terms overlap: 9 / 1,193 (0.8%)
  Conclusion: Clean independent test set. No data leakage detected.

Candidate Set: hard_negatives.jsonl (30 unique utterances)
  Train text collision: 0 (0.00%)
  Train conversation ID collision: 0 (0.00%)
  Tactic phrase overlap: 17 / 30 (56.7%)
  Discriminative terms overlap: 9 / 30 (30.0%)
  Conclusion: Legitimate adversarial negative set. Vocabulary overlap is INTENTIONAL
  to test whether the system over-relies on keywords when context is educational/protective.

Candidate Set: unseen_scam_conversations.jsonl (22 unique utterances)
  Train text collision: 0 (0.00%)
  Train conversation ID collision: 0 (0.00%)
  Tactic phrase overlap: 6 / 22 (27.3%)
  Discriminative terms overlap: 5 / 22 (22.7%)
  Conclusion: Clean multi-turn scam holdout.

Candidate Set: dataset.json (Legacy 22 scenarios, 60 unique utterances)
  Train text collision: 0 (0.00%)
  Train conversation ID collision: 0 (0.00%)
  Tactic phrase overlap: 32 / 60 (53.3%)
  Discriminative terms overlap: 16 / 60 (26.7%)
  Conclusion: HEAVY rule-dictionary alignment. The 22 scenarios were authored using
  the same verbatim strings as TACTIC_PHRASES. Must NOT be presented as ML generalization proof.
```

### Internal Duplication in Held-Out Test Set
Within `test_conversations.jsonl` (1,292 turns across 182 conversations):
- There are **1,193 unique turn texts** and **99 internal turn duplicates**.
- These duplicates represent natural conversational filler across different dialogues:
  - `"hello"` (18 occurrences)
  - `"yes"` (14 occurrences)
  - `"okay"` (9 occurrences)
  - `"who is this?"` (5 occurrences)
- Zero duplicated scam coercive turns exist. The test set is preserved verbatim without alteration.

---

## 4. Evaluation Protocol

All models and pipeline components are strictly frozen:
- **No retraining**
- **No threshold adjustments**
- **No rule-dictionary modifications**
- **No test set filtering or pruning**

Three ablation modes will be executed against `test_conversations.jsonl`:
1. **Mode A (Neural Only):** `ScamClassifier` PyTorch dual-head model directly.
2. **Mode B (Rule Floor Only):** `RuleBasedSafetyFloor` heuristic keyword and regex matcher.
3. **Mode C (Hybrid Pipeline):** Live `LocalSemanticJEVProvider` fusing neural + safety floor into risk evaluation.
