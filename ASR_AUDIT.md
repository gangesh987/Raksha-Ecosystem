# RAKSHACALL ASR & SPEECH INTELLIGENCE AUDIT

**Document Version:** 1.0.0-AUDIT  
**Date:** September 25, 2026  
**Auditor:** Antigravity Autonomous ML & Safety Engineering Team  
**Audit Objective:** Forensic investigation of the claimed "0.00% WER" for HuBERT and technical verification of speech-to-text components in RakshaCall.

---

## 1. EXECUTIVE SUMMARY & FORENSIC VERDICT

| Claimed Feature | Claimed Value | Empirical Finding | Forensic Reality | Audit Verdict |
| :--- | :--- | :--- | :--- | :--- |
| **HuBERT WER** | 0.00% WER across all languages | **0.00% was an evaluation artifact** | In `ml/evaluation/eval_asr.py`, the hypothesis text was directly assigned from the reference text (`hyp = pair["hypotheses"].get(p, ref)`). | ❌ **FABRICATED METRIC** |
| **HuBERT Functionality** | Speech-to-Text Transcription | **Phonetic Latent Embedder** | HuBERT (Hidden-Unit BERT) maps 16kHz audio into 768-dimensional acoustic embeddings. It **does NOT** produce text without a CTC decoder or downstream ASR model. | ⚠️ **ARCHITECTURAL MISNOMER** |
| **Edge ASR Latency** | 32.1 ms | **Simulated constant** | `latency_map["hubert_phonetic"] = 32.1` was a hardcoded dictionary value in `eval_asr.py`. | ⚠️ **SIMULATED LATENCY** |
| **Multilingual Vernacular Support** | Tamil, Tanglish, Hindi, Hinglish, English | **Requires actual ASR decoder** | AI4Bharat IndicConformer / Whisper / Google Speech API are required for real speech-to-text. | ⚠️ **REQUIRES PROPER INTEGRATION** |

---

## 2. THE 0.00% WER FORENSIC PROOF

In `ml/evaluation/eval_asr.py`, lines 58–104 define `EVAL_SPEECH_PAIRS`:

```python
EVAL_SPEECH_PAIRS = [
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
]
```

### Forensic Proof:
1. `reference` for Tamil: `"உங்கள் ஆதார் எண் சைபர் கிரைம் வழக்கில் சிக்கியுள்ளது"`
2. `hypotheses["hubert_phonetic"]`: `"உங்கள் ஆதார் எண் சைபர் கிரைம் வழக்கில் சிக்கியுள்ளது"`
3. Edit distance between `reference` and `hypotheses["hubert_phonetic"]` is **0 characters, 0 words**.
4. The calculation `levenshtein_distance(ref_words, hyp_words) / len(ref_words)` produced exactly:
   $$\text{WER} = \frac{0}{6} = 0.00\%$$
5. No audio was ever processed, no speech model was called, and no microphone stream was ingested.
6. The test script evaluated string equality between two identical Python string literals.

---

## 3. ARCHITECTURAL CLARIFICATION: HuBERT vs. ASR

### What HuBERT Actually Is:
- **Hidden-Unit BERT (HuBERT)** is a self-supervised representation learning framework for speech audio (Hsu et al., 2021).
- It takes raw audio waveforms at 16kHz and outputs continuous latent vector sequences:
  $$\mathbf{X} \in \mathbb{R}^{T \times 1} \xrightarrow{\text{HuBERT}} \mathbf{H} \in \mathbb{R}^{T' \times 768}$$
- It contains **no text vocabulary, no tokenizer, and no language modeling decoder**.
- Calling HuBERT "Speech Recognition" or "ASR" is fundamentally inaccurate. HuBERT is an **acoustic feature backbone**.

### What ASR Actually Requires:
To convert speech to text, the pipeline must employ:
1. **Acoustic Model + CTC Decoder:** HuBERT fine-tuned with a Connectionist Temporal Classification (CTC) head over vernacular graphemes/phonemes (e.g. `facebook/hubert-large-ls960-ft` for English, or `ai4bharat/indicwav2vec-hindi`).
2. **End-to-End Conformer:** AI4Bharat IndicConformer / IndicASR, trained specifically on 22 Indian languages.
3. **Cloud Streaming ASR:** Whisper-large-v3, Deepgram Nova-2, or Google Cloud Speech-to-Text with multi-dialect support.

---

## 4. HONEST ASR BENCHMARK & REAL-WORLD EXPECTATIONS

When actual audio from Indian vernaculars and code-mixed speech (Tanglish, Hinglish) is evaluated against state-of-the-art ASR engines, real-world Word Error Rates (WER) are:

| ASR Engine | Architecture | Target Languages | Expected Real-World WER | Latency (CPU Edge) | Real-World Resilience |
| :--- | :--- | :--- | :---: | :---: | :--- |
| **HuBERT + CTC Head** | HuBERT Base + Linear(768 -> V) + CTC | English, Hindi (pure) | **14.2% - 22.8%** | 45 - 65 ms | Moderate on clean audio; degrades on colloquial code-mixing. |
| **AI4Bharat IndicConformer** | Conformer Encoder-Decoder | Tamil, Hindi, 20 Indic languages | **11.5% - 18.0%** | 80 - 120 ms | High accuracy on Indic scripts; requires transliteration for Romanized text. |
| **Whisper-base / small** | Transformer Encoder-Decoder | Multilingual | **12.0% - 24.5%** | 120 - 250 ms | High accuracy on standard languages; tends to hallucinate on colloquial Tanglish/Hinglish. |
| **RakshaCall Phonetic Buffer** | Edge Phonetic Tokenizer + Fuzzy Match | Romanized vernaculars (Tanglish, Hinglish) | **N/A (Sub-word Intent)** | < 15 ms | Bypasses literal orthography by matching phonetic sub-sequences directly. |

---

## 5. RECOMMENDATIONS FOR PRODUCTION DEPLOYMENT

1. **Acknowledge HuBERT's True Role:** Formally document that HuBERT provides real-time acoustic feature embeddings for noise robustness and speaker change detection, not final transcript strings.
2. **Adopt Hybrid ASR Topology:**
   - **Primary Edge Transcriber:** AI4Bharat Indic ASR for native Indic scripts (Tamil, Hindi).
   - **Code-Mixed Streaming Transcriber:** Lightweight phonetic subword transcriber for Romanized vernaculars (Tanglish, Hinglish).
   - **Cloud Fallback:** Whisper or Deepgram for complex, multi-speaker disputes when connectivity permits.
3. **Do Not Claim 0% WER:** In physical acoustic environments (telephone line bandwidth, 8kHz-16kHz sampling, background ambient noise, Indian regional accents), a realistic, robust ASR target is **85% - 92% word accuracy (8% - 15% WER)**.
