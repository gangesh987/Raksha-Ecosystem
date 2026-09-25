# RAKSHACALL DATA LEAKAGE & EVALUATION INTEGRITY AUDIT

**Document Version:** 2.0.0-VERIFIED  
**Date:** September 25, 2026  
**Auditor:** Antigravity Autonomous ML & Safety Engineering Team  
**Evaluation Standard:** National AI Innovation Competition / Jury-Grade ML Acceptance  

---

## 1. EXECUTIVE SUMMARY & FORENSIC FINDING

The core question of this audit:
> **"Did the reported 1.000 F1, 100% stage validation accuracy, and 0.00% WER stem from true generalization, or from data leakage, synthetic shortcuts, and evaluation artifacts?"**

### Forensic Verdict:
The previous reported metrics were **not** caused by classic train/test string duplication; instead, they were caused by **severe synthetic shortcuts, trivial split sizing, and evaluation against handcrafted heuristic dictionaries**:

1. **Validation Split Sizing & Trivial Sequence Fit:** The original validation set for the stage model contained **exactly one conversation with three turns** (`scenario`: "TRAI SIM Blocking"). A 2-layer GRU easily memorized this 3-turn sequence at Epoch 1, producing a permanent, unvarying "100.0% Validation Accuracy" throughout all 25 epochs.
2. **Cherry-Picked Test Reporting vs. Real Artifact Reality:** While summary markdowns claimed **"1.000 Scam F1"** and **"0.720 Macro Tactic F1"**, the actual untouched test split artifact (`ml/reports/tactic_evaluation.json`) scored:
   - **True Positives:** 0
   - **False Negatives:** 9
   - **Precision:** 0.00%
   - **Recall:** 0.00%
   - **F1-Score:** 0.00%
   Due to an extreme vocabulary bottleneck (20 training turns vs. 9 test turns in a 2,500-feature TF-IDF model), the N-gram classifier completely failed on unseen test turns.
3. **Synthetic Shortcut in End-to-End Evaluation:** The 100% F1 reported in `EVALUATION_REPORT.md` (22/22 correct) did not evaluate a trained neural model. It evaluated `LocalSemanticJEVProvider`—a Python dictionary of prototype strings—against 22 synthetic test scenarios authored with the exact matching keywords.
4. **ASR Hardcoding Shortcut:** In `ml/evaluation/eval_asr.py`, `hubert_phonetic` was evaluated by passing the reference text as the hypothesis text (`hyp = pair["hypotheses"].get(p, ref)`), trivially producing $0.00\%$ WER without any speech recognition occurring.

---

## 2. EMPIRICAL BEFORE VS. AFTER SPLIT AUDIT

We executed an automated forensic scan (`ml/scripts/audit_data_leakage.py`) across the old and newly rectified splits using SHA-256 exact hashing, normalized string hashing, and conversation ID intersection.

### 2.1 Comparative Audit Matrix

| Metric | Original Audited Split | Rectified Production Split | Change / Impact |
| :--- | :---: | :---: | :--- |
| **Total Conversations** | 9 | **1,204** | +1,195 conversations (Ingested ScamBench, ScamDialogue, HinglishScam, Expanded Domain) |
| **Train Turns** | 20 (6 convs) | **4,541 (841 convs)** | **+4,521 genuine turns** |
| **Validation Turns** | 3 (1 conv) | **1,042 (181 convs)** | **+1,039 genuine turns** (Replaces 1-conv toy set) |
| **Test Turns** | 9 (2 convs) | **1,292 (182 convs)** | **+1,283 genuine turns** (Replaces 2-conv toy set) |
| **Train/Test Text Collisions** | 0 (due to toy size) | **0 (Strictly Purged)** | 963 template collisions detected and eliminated. |
| **Train/Val Text Collisions** | 0 | **0 (Strictly Purged)** | All shared template phrases removed from train. |
| **Val/Test Text Collisions** | 0 | **0 (Strictly Purged)** | Completely disjoint turn sets. |
| **Conversation ID Overlap** | 0 | **0 (Strictly Disjoint)** | Guaranteed conversation-level partition. |
| **Acceptance Suite Leakage** | N/A | **0 (Strictly Isolated)** | `evaluation/` directory completely isolated from training. |

### 2.2 Final Verification Output from `audit_data_leakage.py`

```json
{
  "counts": {
    "train": 4541,
    "val": 1042,
    "test": 1292
  },
  "conversation_counts": {
    "train": 841,
    "val": 181,
    "test": 182
  },
  "exact_text_collisions": {
    "train_test": 0,
    "train_val": 0,
    "val_test": 0
  },
  "conversation_id_overlap": {
    "train_test": 0,
    "train_val": 0,
    "val_test": 0
  }
}
```

---

## 3. REMEDIATION COMPLETE

1. **Ingested Real Open Datasets:** `BothBosu/scam-dialogue` (500 multi-turn dialogues), `shaw/scambench-training` (1,000 multi-turn dialogues), `bolewara/hinglish-scam-text-dataset` (3,787 Hinglish samples), and expanded Indian domain corpus.
2. **Purged All Cross-Split Template Collisions:** Common conversational greetings and dialogue templates that spanned multiple conversation IDs were detected via SHA-256 hash sets and purged from the training set.
3. **Established Isolated Acceptance Suite:** The `evaluation/` directory contains 100% held-out multi-turn scenarios and hard negatives that were strictly excluded from training and validation.
