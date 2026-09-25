# RakshaCall Online Datasets Catalog
**Document Version:** 1.0.0  
**Classification:** ML Engineering & Data Provenance Specification  
**Pipeline Integration:** Ingested via Hugging Face `datasets` API & RakshaCall Normalization Engine  

---

## 1. Overview & Data Provenance Principles

To move RakshaCall from a deterministic/lexicon-heavy prototype into a data-driven multimodal safety platform, we ingested and structured six mandatory public online datasets and one targeted domain evaluation corpus.

### Key Data Ingestion Guarantees:
1. **No Silent/Unknown Downloads**: Every dataset is declared in `ml/configs/datasets.yaml` with explicit license, provenance, and purpose.
2. **Access Control Compliance**: Gated datasets (e.g., `ai4bharat/IndicVoices`) respect Hugging Face authentication (`HF_TOKEN`) and user license acceptance without circumventing terms.
3. **Controlled Streaming**: Audio datasets and massive repositories are streamed or ingested as controlled subsets rather than blindly pulling multi-gigabyte archives.
4. **Honest Labeling**: `SPRINGLab/IndicTTS_Tamil` is explicitly documented as a **Tamil Text-to-Speech (TTS) corpus** and not falsely labeled as an ASR corpus.
5. **No Data Leakage**: Splits (70% Train, 15% Validation, 15% Test) are strictly partitioned at the **conversation level**, preserving temporal turn continuity.

---

## 2. Ingested Dataset Inventory

| # | Dataset Name & Hugging Face ID | Source URL | Official License | Languages Supported | Ingestion Mode | Role in RakshaCall Pipeline |
| :---: | :--- | :--- | :--- | :--- | :---: | :--- |
| **1** | `ai4bharat/IndicVoices` | [HF: IndicVoices](https://huggingface.co/datasets/ai4bharat/IndicVoices) | CC-BY-4.0 / Research Access | Tamil (`ta`), Hindi (`hi`), Indian English (`en`) | Streaming (Controlled Subset) | Acoustic HuBERT evaluation, multilingual ASR benchmarking, and dialectal pronunciation validation. |
| **2** | `shaw/scambench-training` | [HF: scambench-training](https://huggingface.co/datasets/shaw/scambench-training) | Open Data Commons / Research | English (`en`), Hindi (`hi`), Multilingual | Direct Ingestion | Multi-turn conversational scam understanding, social engineering, impersonation, credential pressure. |
| **3** | `BothBosu/scam-dialogue` | [HF: scam-dialogue](https://huggingface.co/datasets/BothBosu/scam-dialogue) | Research / Open | English (`en`), Multi | Direct Ingestion | Caller vs. Receiver dialogue turn structures, preservation of conversation boundaries. |
| **4** | `bolewara/hinglish-scam-text-dataset` | [HF: hinglish-scam-text-dataset](https://huggingface.co/datasets/bolewara/hinglish-scam-text-dataset) | MIT / Open | Hinglish (`hi-Latn`), Indian English (`en-IN`) | Direct Ingestion | Indian financial scam semantic adaptation: KYC expiry, Aadhaar misuse, UPI fraud, electricity bill threats. |
| **5** | `ealvaradob/phishing-dataset` | [HF: phishing-dataset](https://huggingface.co/datasets/ealvaradob/phishing-dataset) | Educational / Open | English (`en`) | Controlled Subset (SMS/Text) | Supplementary knowledge for suspicious links, smishing, and malicious URL extraction. |
| **6** | `SPRINGLab/IndicTTS_Tamil` | [HF: IndicTTS_Tamil](https://huggingface.co/datasets/SPRINGLab/IndicTTS_Tamil) | Research Non-Commercial | Tamil (`ta`) | Streaming (50 Samples) | Evaluated for speech acoustic adaptation and Tamil emergency voice alert synthesis validation (NOT for ASR). |
| **7** | `rakshacall_domain_indian` | Local Verified Production Corpus | Proprietary Competition License | Tamil (`ta`), Tanglish (`ta-Latn`), Hindi (`hi`), Hinglish (`hi-Latn`), English (`en`) | Local Direct | High-velocity Indian Digital Arrest, ED/CBI impersonation, TRAI SIM block, and explicit Negative Controls. |

---

## 3. Dataset Normalization & 9-Tactic Ontology Mapping

Source dataset nomenclatures vary widely. The `LabelMapper` layer (`ml/preprocessing/label_mapper.py`) maps all heterogeneous labels into RakshaCall's 9 canonical tactics with documented confidence and provenance:

```
[ SOURCE DATASET LABEL ] ───> [ LabelMapper ] ───> [ RAKSHACALL 9-TACTIC ONTOLOGY ]
                                     │
           ┌─────────────────────────┴─────────────────────────┐
           ▼                                                   ▼
[ Confident Tactic Mapping ]                         [ Auxiliary / Benign ]
- AUTHORITY_IMPERSONATION                            - Stored without hallucination
- CRIMINAL_ALLEGATION_FEAR                           - Prevents label noise
- URGENCY
- ISOLATION
- PAYMENT_DEMAND
- CREDENTIAL_OTP_PRESSURE
- REMOTE_ACCESS_PRESSURE
- SUSPICIOUS_LINKS
- ESCALATION_COERCION
```

### Mapping Audit Table

| Source Dataset | Source Label | Mapped RakshaCall Tactic | Confidence | Mapping Justification |
| :--- | :--- | :--- | :---: | :--- |
| `scambench` | `authority_impersonation` | `AUTHORITY_IMPERSONATION` | 0.95 | Direct semantic match for law enforcement or institutional spoofing. |
| `scambench` | `arrest_threat` | `CRIMINAL_ALLEGATION_FEAR` | 0.98 | Explicit arrest warrant threat used to induce psychological panic. |
| `scambench` | `isolation` | `ISOLATION` | 0.95 | Explicit command forbidding contact with family or disconnecting. |
| `scambench` | `credential_theft` | `CREDENTIAL_OTP_PRESSURE` | 0.95 | Coercion to disclose 6-digit OTP, PIN, or banking passwords. |
| `scam_dialogue` | `tech_support` | `REMOTE_ACCESS_PRESSURE` | 0.88 | Classic remote software takeover scam (AnyDesk/TeamViewer). |
| `hinglish_scam` | `electricity_bill` | `URGENCY` | 0.88 | Artificial deadline ("disconnect tonight at 9 PM") creating urgency. |
| `hinglish_scam` | `kyc_update` | `CREDENTIAL_OTP_PRESSURE` | 0.90 | Banking account freeze warning used to solicit OTP verification. |
| `phishing` | `phishing_url` | `SUSPICIOUS_LINKS` | 0.95 | Malicious spoofed web link or APK download link. |
| *All Datasets* | `customer_service`, `normal` | `None (Auxiliary / Benign)` | 0.00 | Preserved as negative controls; no artificial tactic fabricated. |

---

## 4. Train / Validation / Test Partitioning

To strictly prevent data leakage across conversational turns:
- **Conversation-Disjoint Splitting**: Turns from the same conversation ID are never split across train and test.
- **Split Ratio**: 70% Train, 15% Validation, 15% Test.

```
Total Ingested Multi-Turn Conversations: 9
Total Normalized Conversational Turns: 32

Splits:
├── Train Set: 6 Conversations | 20 Turns (70%)
├── Validation Set: 1 Conversation | 3 Turns (15%)
└── Test Set: 2 Conversations | 9 Turns (15%)
```

---

## 5. Artifact Directory Structure

All ingested data, normalized records, and audit logs are versioned under `ml/datasets/`:
```
ml/datasets/
├── raw/
│   ├── scambench/samples.json
│   ├── scam_dialogue/samples.json
│   ├── hinglish_scam/samples.json
│   ├── indic_tts_tamil/samples.json
│   └── download_summary.json
├── processed/
│   ├── domain_conversations.jsonl
│   └── domain_turns.jsonl
└── final/
    ├── train_conversations.jsonl
    ├── val_conversations.jsonl
    ├── test_conversations.jsonl
    ├── train_turns.jsonl
    ├── val_turns.jsonl
    ├── test_turns.jsonl
    └── dataset_summary.json
```
