# INDIC DATASET AUDIT
**RakshaCall Phase 2 — Multilingual Holdout Corpus**
**Created: 2026-09-26**

---

## Purpose

Document the composition, provenance, disjointness checks, and leakage guarantees for the
newly created `ml/datasets/evaluation/indic_holdout/` corpus.

This corpus supplements (but NEVER replaces) the frozen test set at `ml/datasets/final/test_conversations.jsonl`.

---

## Corpus Composition

| Language     | Code     | Scam | Benign | Total | Source Type |
|--------------|----------|------|--------|-------|-------------|
| Tamil        | `ta`     | 5    | 5      | 10    | SYNTHETIC   |
| Tanglish     | `ta-Latn`| 5    | 3      | 8     | SYNTHETIC   |
| Hindi        | `hi`     | 5    | 3      | 8     | SYNTHETIC   |
| Hinglish     | `hi-Latn`| 3    | 2      | 5     | SYNTHETIC   |
| **Total**    |          | **18** | **13** | **31** | SYNTHETIC |

### Source Labels
- **SYNTHETIC**: Manually authored for linguistic diversity, not derived from real calls.
- **REAL**: Not present in this corpus — no real call recordings were available.
- **AUGMENTED**: Not present — no augmentation applied in v1.

---

## Conversation Disjointness Checks

### Overlap With Frozen Test Set
- All `conversation_id` values in this corpus are unique (format: `indic_holdout_*`).
- The frozen test set uses formats: `scambench_generated::*`, `scam_dialogue_*`, `hinglish_*`.
- **No shared conversation IDs detected.**

### Text Deduplication
- Phrases in this corpus were authored independently.
- No verbatim copy from `TACTIC_PHRASES` dictionary in `jev_provider.py`.
- No verbatim copy from `tests/ml/adversarial_paraphrases.jsonl` (Phase 1).

### Speaker Overlap
- All conversations are synthetic — no real speaker identities.
- No speaker metadata from the training or test set was reused.

---

## Coverage Rationale

The frozen test set has the following gap:

| Language | Test Set Count | Coverage Status |
|----------|---------------|-----------------|
| English  | 174           | Strong          |
| Hindi    | 4             | **Critically Insufficient** |
| Tanglish | 2             | **Critically Insufficient** |
| Hinglish | 1             | **Critically Insufficient** |
| Tamil    | 1             | **Critically Insufficient** |

This indic holdout corpus directly targets the Critically Insufficient languages.

> **Warning**: With 5–10 examples per language, this corpus supports directional analysis only.
> It does NOT support statistically robust generalization claims.

---

## Tactic Coverage

| Tactic                    | Covered |
|---------------------------|---------|
| AUTHORITY_IMPERSONATION   | Yes     |
| CRIMINAL_ALLEGATION_FEAR  | Yes     |
| URGENCY                   | Yes     |
| ISOLATION                 | Yes     |
| PAYMENT_DEMAND            | Yes     |
| CREDENTIAL_OTP_PRESSURE   | Yes     |
| REMOTE_ACCESS_PRESSURE    | Yes     |
| SUSPICIOUS_LINKS          | No (not authored; rare in voice calls) |
| ESCALATION_COERCION       | Yes     |

---

## Limitations

1. **SYNTHETIC data**: These examples were hand-authored, not collected from real scam calls.
   They may not capture the full variety of actual attacker phrasing.

2. **Small N**: 31 total conversations is far too small for robust statistical evaluation.
   Any per-language F1 on this corpus has extremely wide confidence intervals.

3. **No retraining boundary**: This corpus MUST NOT be used as training data.
   It is strictly for evaluation / ablation analysis.

4. **Language code inconsistency**: The existing dataset uses `hi-Latn` for Hinglish
   while some toolchains use `hinglish`. We standardize to `hi-Latn` in this corpus.

---

## Usage Rules

- NEVER modify labels to improve metrics.
- NEVER add examples from this corpus to training.
- Use only for ablation comparisons and language gap analysis.
- Report results only as "directional" given the small N.
