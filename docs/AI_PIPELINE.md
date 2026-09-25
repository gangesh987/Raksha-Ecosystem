# RakshaCall AI & Audio Intelligence Pipeline

## 1. Multi-Tier Conversation Intelligence

The conversation intelligence pipeline processes speech through six distinct analytical layers:

```
┌────────────────────────────────────────────────────────┐
│ 1. Continuous SpeechRecognizer Streaming Callback      │
└──────────────────────────┬─────────────────────────────┘
                           │ Ephemeral Transcript Chunk
                           ▼
┌────────────────────────────────────────────────────────┐
│ 2. Text Normalization & PII Sanitization               │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│ 3. 9-Tactic Pattern Matching (Regex NLP Engine)        │
│    English + Hindi / Hinglish Regional Variants        │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│ 4. Contextual Nuance & Negative Filter Guard           │
│    (Suppresses false alarms like routine discussions)  │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│ 5. Temporal Decay & Duplicate Dampening (<45s window)  │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│ 6. Multi-Tactic Escalation Multipliers                 │
│    (Accumulating distinct tactics increases multiplier)│
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
                 Risk Decision (0 - 100)
```

## 2. Audio Ephemerality & On-Device Transcription
- Android `SpeechRecognizer` runs continuously during an active protection session.
- Spoken utterances are converted to text chunks in real time.
- Memory containing raw PCM audio is immediately released to ensure absolute privacy by design.

## 3. Multilingual Support
- `SpeechLanguageManager.kt` manages language availability (English `en-IN`, Hindi `hi-IN` active; regional languages in staging).
- `LanguageAwareTacticEngine.kt` recognizes Hindi / Hinglish phrases ("पुलिस", "सीबीआई", "डिजिटल अरेस्ट", "पैसे भेजो", "ओटीपी") and maps them to the canonical 9 `ScamTactic` classifications.
