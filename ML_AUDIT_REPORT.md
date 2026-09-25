# RAKSHACALL ML AUDIT REPORT: EMPIRICAL GENERALIZATION VS. SYNTHETIC SHORTCUTS

**Document Version:** 1.0.0-AUDIT  
**Audit Date:** September 25, 2026  
**Auditor:** Antigravity Autonomous ML & Safety Engineering Team  
**Audit Objective:** Perform an unvarnished, empirical forensic audit of the RakshaCall Machine Learning pipeline. Determine whether the previously reported results (0.00% WER, 1.000 scam F1, 100% stage accuracy, 0% false alarms) represent genuine generalization or data leakage, synthetic shortcuts, hardcoded fallbacks, and evaluation pipeline artifacts.

---

## EXECUTIVE SUMMARY & AUDIT VERDICT

| Component | Claimed Metric | Empirical Reality | Root Cause / Forensic Finding | Audit Verdict |
| :--- | :--- | :--- | :--- | :--- |
| **ASR (HuBERT)** | 0.00% WER across 5 languages | **Zero actual audio transcribed** | In `ml/evaluation/eval_asr.py`, hypothesis string was hardcoded character-for-character to equal the reference text (`hyp = pair["hypotheses"].get(p, ref)`). HuBERT outputs phonetic latent representations, not speech-to-text transcriptions. | ❌ **INVALID (FABRICATED TEST)** |
| **Scam Classifier** | 1.000 Scam F1, 0.720 Tactic F1 | **0.00 F1 on test split; trained on 20 turns** | The model is NOT a multilingual Transformer. It is a 2-layer MLP over a 2,500-feature scikit-learn `TfidfVectorizer`. Evaluated on an untouched 9-turn test set, `eval_tactics.py` scored **0.00 Precision, Recall, and F1**. | ❌ **INVALID (N-GRAM / TOY DATA)** |
| **Stage Model** | 100% Validation Accuracy | **Trained on 6 conversations; validated on 1** | Validation set consisted of exactly **one** synthetic conversation with **3 turns**. Transitions were deterministic. 100% accuracy appeared at Epoch 1. | ❌ **INVALID (TRIVIAL TOY SPLIT)** |
| **External Datasets** | Ingested IndicVoices, ScamBench, scam-dialogue, Hinglish, Phishing | **0 external samples reached final training splits** | `normalize_datasets.py` looked for `item["text"]`. `scambench` uses `item["messages"]`, `scam_dialogue` uses `item["dialogue"]`. 1,500 downloaded samples were silently skipped! | ❌ **FAILED INGESTION** |
| **JEV Engine** | "Joint Embedding Intent Verifier" | **Keyword/Regex heuristic dictionary** | `LocalSemanticJEVProvider` is a Python class containing dictionary word-matching (`INTENT_PROTOTYPES`, `TACTIC_PHRASES`) and regex. It is not an external proprietary neural network. | ⚠️ **HEURISTIC MISLABELED AS NEURAL** |
| **YOLO11 Vision** | Custom scam visual detection | **Standard COCO-pretrained `yolo11n.pt`** | Ran against a blank synthetic image (`np.zeros((640,640,3))`). No cyber-scam or KYC visual fine-tuning took place. Acts as generic object detection. | ⚠️ **OFF-THE-SHELF (NO DOMAIN TRAINING)** |

---

## 1. DATASET AUDIT

### 1.1 Ingestion Forensic Analysis

We audited all datasets configured in `ml/configs/datasets.yaml` and staged in `ml/datasets/raw/`:

| Dataset Identifier | Hugging Face Repository | Declared Role | Raw Download Status | Ingested into Final Split | Actual Reason / Evidence |
| :--- | :--- | :--- | :---: | :---: | :--- |
| `indic_voices` | `ai4bharat/IndicVoices` | Multilingual Audio ASR | `AUTH_REQUIRED_STAGED` (0 samples) | **0 samples** | Gated dataset on Hugging Face; requires accepted user terms and `HF_TOKEN`. Staged placeholder only. |
| `scambench` | `shaw/scambench-training` | Multi-turn scam dialogues | `DOWNLOADED` (1,000 samples, 2.6 MB) | **0 samples** | Downloaded into `raw/scambench/samples.json`, but `normalize_datasets.py` evaluated `item.get("text")`, which was `None` because schema uses `item["messages"]`. Silently ignored! |
| `scam_dialogue` | `BothBosu/scam-dialogue` | Multi-turn dialogues | `DOWNLOADED` (500 samples, 902 KB) | **0 samples** | Downloaded into `raw/scam_dialogue/samples.json`, but `normalize_datasets.py` looked for `item["text"]` instead of `item["dialogue"]`. Silently ignored! |
| `hinglish_scam` | `bolewara/hinglish-scam-text-dataset` | Hinglish scam text | `OFFLINE_FALLBACK_STAGED` (0 samples) | **0 samples** | Hugging Face raw file failed JSON parsing (`"Expected object or value"`). 0 samples staged. |
| `phishing_supplement` | `ealvaradob/phishing-dataset` | Phishing links | `STAGED_METADATA` (0 samples) | **0 samples** | Staged metadata only; full download was deferred. |
| `indic_tts_tamil` | `SPRINGLab/IndicTTS_Tamil` | Speech acoustic eval | `DOCUMENTED_TTS` (0 samples) | **0 samples** | TTS acoustic corpus; 0 samples staged in raw. |
| `rakshacall_domain_indian` | Local authored script (`build_rakshacall_dataset.py`) | High-priority Indian scam scenarios | `LOCAL_VERIFIED` (9 conversations, 32 turns) | **32 turns (100% of final data)** | Authored domain conversations in Tamil, Tanglish, Hindi, and English. |

### 1.2 Final Dataset Composition Dissection

Inspection of `ml/datasets/final/dataset_summary.json` reveals the exact composition used to train and evaluate previous models:

```json
{
  "total_conversations": 9,
  "splits": {
    "train": {
      "conversations": 6,
      "turns": 20
    },
    "val": {
      "conversations": 1,
      "turns": 3
    },
    "test": {
      "conversations": 2,
      "turns": 9
    }
  },
  "tactics_ontology": [
    "AUTHORITY_IMPERSONATION",
    "CRIMINAL_ALLEGATION_FEAR",
    "URGENCY",
    "ISOLATION",
    "PAYMENT_DEMAND",
    "CREDENTIAL_OTP_PRESSURE",
    "REMOTE_ACCESS_PRESSURE",
    "SUSPICIOUS_LINKS",
    "ESCALATION_COERCION"
  ]
}
```

**Key Finding:** The entirety of the training, validation, and test splits consisted of **32 individual sentences across 9 conversations**. Claims of large-scale dataset ingestion were false due to schema mismatches in the ingestion script.

---

## 2. DATA LEAKAGE AUDIT

### 2.1 Split Analysis

1. **Validation Split:** Consisted of exactly **one conversation** with **3 turns** (`scenario`: "TRAI SIM Blocking & Police Cyber Cell Coercion", Tamil).
2. **Test Split:** Consisted of **two conversations** with **9 turns** (`scenario`: "Electricity Bill Unpaid Urgent Threat" + "Casual Benign Chat").
3. **Training Split:** Consisted of **six conversations** with **20 turns**.

### 2.2 Leakage & Overfitting Findings
- While conversations were not duplicated across splits (conversation IDs were distinct), the vocabulary across these 9 scenarios was highly uniform.
- Because `TfidfVectorizer` was fitted on the 20 training turns, **out-of-vocabulary (OOV) rate on the test set was catastrophic**.
- In `ml/reports/tactic_evaluation.json`, when evaluated on the 9 test turns, the model predicted negative for every single sample:
  $$\text{True Positives} = 0, \quad \text{False Negatives} = 9, \quad \text{Precision} = 0.0, \quad \text{Recall} = 0.0, \quad \text{F1} = 0.0$$
- The earlier reported "1.000 F1" in summary markdown did NOT come from the test split. It was either an artifact of training set re-evaluation or evaluation against handcrafted heuristic dictionaries.

---

## 3. MODEL ARCHITECTURE AUDIT

### 3.1 Actual Architecture of `RakshaCall-Multilingual-Tactic-v1`

Inspection of `ml/models/scam_classifier/v1/model_config.json` and `ml/training/train_multilingual_tactic.py`:

- **Model Name:** `RakshaCall-Multilingual-Tactic-v1`
- **Claimed in Documentation:** "Multilingual Semantic Neural Model"
- **Actual Architecture:** **Bag-of-Words / N-Gram MLP** (Multi-Layer Perceptron)
- **Feature Extractor:** `sklearn.feature_extraction.text.TfidfVectorizer(max_features=2500, ngram_range=(1, 3))`
  - Saved artifact: `vectorizer.pkl` (55 KB)
- **Input Dimension:** 1,207 (actual unique n-grams extracted from 20 training turns)
- **Neural Structure:**
  - Shared Encoder: `Linear(1207, 128) -> BatchNorm1d -> ReLU -> Dropout(0.20) -> Linear(128, 64) -> BatchNorm1d -> ReLU -> Dropout(0.20)`
  - Scam Head: `Linear(64, 1)`
  - Tactic Head: `Linear(64, 9)`
- **Total Parameters:**
  $$\text{Linear}_1: 1207 \times 128 + 128 = 154,624$$
  $$\text{BatchNorm}_1: 2 \times 128 = 256$$
  $$\text{Linear}_2: 128 \times 64 + 64 = 8,256$$
  $$\text{BatchNorm}_2: 2 \times 64 = 128$$
  $$\text{Scam Head}: 64 \times 1 + 1 = 65$$
  $$\text{Tactic Head}: 64 \times 9 + 9 = 585$$
  $$\textbf{Total Trainable Parameters}: \mathbf{163,914}$$
- **Transformer Encoder:** **NONE.** No Self-Attention layers, no Transformer blocks, no subword embeddings (BPE/WordPiece/SentencePiece).
- **Training Epochs:** 25
- **Optimizer:** AdamW ($lr = 0.005$)

**Conclusion:** Calling this model a "multilingual semantic Transformer" was a misrepresentation. It is a small MLP on top of TF-IDF n-grams.

---

## 4. STAGE MODEL AUDIT

Inspection of `ml/training/train_conversation_stage.py` and `ml/models/stage_model/v1/stage_config.json`:

- **Model:** `RakshaCall-Conversation-Stage-GRU-v1`
- **Architecture:** 2-Layer Bidirectional GRU (Hidden Dim: 64, Input Dim: 12)
- **Claimed Metric:** "100% Validation Accuracy"
- **Forensic Investigation:**
  - The training dataset consisted of sequences constructed from the 6 training conversations.
  - The validation dataset was constructed from **exactly 1 validation conversation**.
  - That 1 conversation had 3 sequential turns with deterministic stage labels (`CONTACT -> AUTHORITY -> FEAR`).
  - At Epoch 5, training loss was 1.4912, yet validation accuracy was already **1.000 (100%)** because predicting the monotonic stage sequence on a single 3-turn sample is trivial.
- **Verdict:** The "100% accuracy" is an artifact of having a single synthetic validation sample.

---

## 5. ASR AUDIT: THE 0.00% WER FORENSICS

Inspection of `ml/evaluation/eval_asr.py`:

```python
# Lines 58-104 of ml/evaluation/eval_asr.py
EVAL_SPEECH_PAIRS: List[Dict[str, Any]] = [
    {
        "lang": "ta",
        "reference": "உங்கள் ஆதார் எண் சைபர் கிரைம் வழக்கில் சிக்கியுள்ளது",
        "hypotheses": {
            "hubert_phonetic": "உங்கள் ஆதார் எண் சைபர் கிரைம் வழக்கில் சிக்கியுள்ளது",
            "indic_asr": "உங்கள் ஆதார் எண் சைபர் க்ரைம் வழக்கில் சிக்கியுள்ளது",
            "cloud_asr": "உங்கள் ஆதார் என் சைபர் கிரைம் வழக்கில் சிக்கி உள்ளது"
        }
    },
    ...
```

And in the evaluation loop (line 123):
```python
hyp = pair["hypotheses"].get(p, ref)
wer = compute_wer(ref, hyp)
cer = compute_cer(ref, hyp)
```

### Forensic Findings:
1. `eval_asr.py` contained **5 hardcoded text sentences**.
2. For provider `hubert_phonetic`, the string in `hypotheses["hubert_phonetic"]` was **character-for-character identical to the reference text**.
3. No audio file was ever read from disk or processed.
4. No acoustic feature extraction was executed.
5. No CTC decoder or language model was invoked.
6. The script simply computed `compute_wer(ref, ref)` which mathematically equals `0.00%`.
7. **HuBERT Reality:** Hidden-Unit BERT (HuBERT) is a self-supervised acoustic representation model that converts audio into 768-dimensional cluster embeddings. **It cannot output text on its own.** Claiming HuBERT has "0.00% WER" is a severe evaluation violation.

---

## 6. JEV ARCHITECTURAL AUDIT

Inspection of `backend/app/ai/jev_provider.py`:

1. `LocalSemanticJEVProvider` does not load PyTorch weights or ONNX tensors.
2. It contains hardcoded string arrays:
   - `INTENT_PROTOTYPES`: 9 dictionary keys containing concatenated strings in English, Tamil, Tanglish, Hindi, and Hinglish.
   - `TACTIC_PHRASES`: High-signal phrases (e.g., `"cbi officer"`, `"arrest warrant"`, `"anydesk"`, `"panam anuppunga"`).
   - `PROTECTIVE_PATTERNS`: Regex patterns detecting negation (e.g., `r'(never share|do not share)'`).
3. `_compute_intent_similarity` searches for substrings with `re.search` and counts token overlaps.
4. If `GROQ_API_KEY` is present, `CloudLLMJEVProvider` routes requests to Groq (LLaMA-3) or Gemini.
5. **Verdict:** JEV is an intelligent multi-intent rule and pattern heuristic provider with optional Cloud LLM fallback. It is **NOT** a custom-trained multimodal joint embedding model.

---

## 7. YOLO11 AUDIT

Inspection of root directory and `ml/evaluation/eval_vision.py`:

1. Weights file `yolo11n.pt` (5,613,764 bytes) is the official generic COCO-pretrained checkpoint released by Ultralytics.
2. Classes used: Person (0), Cell phone (67), Laptop (63), Screen (62), Book (73).
3. In `eval_vision.py`, the test input is:
   ```python
   test_frame = np.zeros((640, 640, 3), dtype=np.uint8)
   cv2.circle(test_frame, (320, 240), 100, (200, 200, 200), -1)
   cv2.rectangle(test_frame, (200, 350), (440, 600), (100, 100, 100), -1)
   ```
4. No dataset of Indian cyber-crime coercion video streams, fake police badges, or KYC documents was used to fine-tune YOLO11.
5. **Verdict:** YOLO11 is a valid supporting context provider for off-the-shelf objects (phone, laptop), but **must not be claimed as a custom-trained cyber fraud vision model**.

---

## 8. ACTION PLAN TO REIFY SYSTEM TO JURY-GRADE STANDARDS

To transition RakshaCall from synthetic shortcuts to genuine, demonstrable ML excellence:

1. **Fix Ingestion Pipeline:** Correct `normalize_datasets.py` to properly parse `scambench` (1,000 multi-turn samples) and `scam_dialogue` (500 multi-turn samples). Expand the local Indian domain corpus to over 100 diverse, realistic multi-turn conversations across Tamil, Tanglish, Hindi, Hinglish, and English.
2. **Train a Real Multilingual Semantic Model:**
   - Benchmark multilingual encoders: `MuRIL` / `XLM-RoBERTa` / `mBERT` / `paraphrase-multilingual-MiniLM-L12-v2`.
   - Train a dual-head model (Binary Scam + 9-Tactic Multi-Label) using true multi-label BCE loss with class weighting.
   - Implement early stopping and save the full Hugging Face tokenizer and PyTorch state dict.
3. **Hard Negative Test Suite:** Build a 40+ sample hard-negative evaluation set of security advisories, bank KYC, and legitimate police verification that use scam vocabulary in a protective context.
4. **ASR Honesty:** Replace fabricated 0.00% WER with honest empirical benchmarks. Document that HuBERT provides acoustic feature framing while transcription requires an ASR engine (Indic ASR / Whisper / Cloud).
5. **Stage Model Realism:** Evaluate stage transitions on diverse multi-turn conversations, reporting honest validation accuracy.
6. **Realistic Acceptance Testing:** Build completely isolated test suites (`unseen_scam_conversations.jsonl`, `hard_negatives.jsonl`, language-specific files) that are never seen during training.
