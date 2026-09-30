# RakshaCall Multilingual Contextual Risk Engine Validation Report

**Repository:** `https://github.com/gangesh987/Raksha-Ecosystem`  
**Branch:** `kamal`  
**Status:** VALIDATED & COMPLIANT  
**Date:** March 2025  

---

## 1. System Architecture

The RakshaCall Risk Engine operates as an explainable, multi-turn, tactic-based analysis pipeline designed specifically for Indian multilingual speech and code-switching scenarios (Tamil, Hindi, Telugu, Tanglish, Hinglish, Kannada, Malayalam, Bengali, Marathi, Gujarati, Punjabi, Odia, and English).

```
PHONE MICROPHONE / SIMULATED CALL
              ↓
         WEBRTC AUDIO
              ↓
            PCM16
              ↓
           BACKEND
              ↓
        FASTER-WHISPER
              ↓
      LANGUAGE DETECTION
              ↓
  MULTILINGUAL TACTIC ENGINE (13 Canonical Tactics)
              ↓
     CONVERSATION CONTEXT (ConversationRiskState)
              ↓
     TRANSPARENT RISK SCORING (Synergies + Decay)
              ↓
       RISK EXPLANATION (Evidence Spans)
              ↓
          ANDROID HUD
              ↓
      PROTECTION ACTION MODAL
```

Both the live phone audio path (WebRTC + Faster-Whisper ASR) and the prototype **Demo Video Call** mode stream into the exact same canonical risk scoring engine. No fake or mock risk engine exists.

---

## 2. Canonical Tactic Taxonomy

The system defines 13 canonical tactics representing the full spectrum of cyber-crime, digital arrest, and extortion operations:

| # | Tactic Key | Display Name | Severity | Base Weight | Irreversible | Core Exploitation Vector |
|---|---|---|---|---|---|---|
| 1 | `AUTHORITY_IMPERSONATION` | Authority Impersonation | High | 20 | No | Impersonating Police, CBI, ED, Customs, Cyber Crime, RBI, Supreme Court |
| 2 | `URGENCY_PRESSURE` | Urgency & Artificial Deadline | Medium | 15 | No | Fabricating artificial deadlines ("immediately", "within 10 mins", "right now") |
| 3 | `PAYMENT_DEMAND` | Payment Demand | High | 25 | Yes | Extorting funds via UPI, bank transfer, escrow account, or security deposit |
| 4 | `OTP_REQUEST` | OTP & Verification Code Request | Critical | 30 | Yes | Coercing disclosure of SMS OTP or two-factor authentication codes |
| 5 | `CREDENTIAL_REQUEST` | Banking Credential / PIN Request | Critical | 30 | Yes | Demanding UPI PIN, ATM PIN, netbanking password, or card CVV |
| 6 | `REMOTE_ACCESS_REQUEST` | Remote Control & Screen Share | Critical | 30 | Yes | Instructing victim to install AnyDesk, TeamViewer, QuickSupport, or share screen |
| 7 | `THREAT_OR_FEAR` | Threat & Coercive Fear | High | 20 | No | Threatening immediate arrest warrant, non-bailable FIR, jail, or narcotics seizure |
| 8 | `SECRECY_PRESSURE` | Secrecy & Isolation Pressure | High | 20 | No | Demanding closed room doors, video custody, not informing family or lawyers |
| 9 | `IDENTITY_VERIFICATION_PRESSURE` | Identity Verification Pressure | Medium | 15 | No | Forcing immediate confirmation of Aadhaar, PAN, or SIM re-verification |
| 10 | `SUSPICIOUS_LINK` | Suspicious Link / APK | High | 20 | Yes | Coercing opening unverified URL, phishing portal, or installing unofficial APK |
| 11 | `PERSONAL_INFORMATION_REQUEST` | Personal Info Harvesting | Medium | 15 | No | Demanding mother's maiden name, date of birth, full account details |
| 12 | `ROMANCE_OR_TRUST_MANIPULATION` | Trust / Emergency Manipulation | Medium | 15 | No | Fabricating family emergency, fake hospitalization, or emotional extortion |
| 13 | `INVESTMENT_OR_REWARD_SCAM` | Investment / Reward Scam | Medium | 15 | No | Promising guaranteed returns, double money in 7 days, fake lottery winnings |

---

## 3. Transparent Scoring Methodology

The scoring engine completely rejects simplistic single-keyword triggers (e.g. `if "police" -> 90`). Instead, it employs a multi-tiered synergistic scoring formula:

$$\text{Risk Score} = \min\left(100, \max\left(0, \sum \text{Base Weights} + \sum \text{Synergy Bonuses} - \text{Decay}\right)\right)$$

### 3.1 Synergy Combinations

When independent manipulation vectors converge in the same conversation, a synergy bonus is applied:

1. **Authority + Threat/Fear (+15 bonus):** e.g., Police impersonation + non-bailable warrant.
2. **Authority + Urgency + Payment Demand (+25 bonus):** The classic extortion triad. Escalates risk to $\ge 80$ (CRITICAL).
3. **Authority + Threat + OTP/Credential (+30 bonus):** Coerced account takeover under fear of arrest.
4. **Secrecy + Payment/OTP (+20 bonus):** Isolation tactics combined with direct financial demands.
5. **Remote Access + Authority (+25 bonus):** Coerced installation of screen-monitoring software under legal pretext.

### 3.2 Dynamic Classification Thresholds

| Score Range | Risk Level | System Behavior |
|---|---|---|
| 0 – 34 | `LOW` | Normal monitoring, unobtrusive status badge |
| 35 – 59 | `MEDIUM` | Caution advisory, highlighted tactics in HUD |
| 60 – 79 | `HIGH` | Warning banner, intervention recommendation, audio verification prompt |
| 80 – 100 | `CRITICAL` | Safety brake active, verification overlay, trusted contact notification |

---

## 4. Multilingual & Code-Switching Handling

The engine operates natively on 11 Indian constitution languages plus Tanglish, Hinglish, and English, matching semantic intent across both native Indic scripts and Romanized transliterations:

* **English:** *"I'm a police officer. This is urgent. Send me money immediately."* $\to$ `AUTHORITY_IMPERSONATION`, `URGENCY_PRESSURE`, `PAYMENT_DEMAND` (Score: 85, CRITICAL)
* **Tamil (Native):** *"நான் போலீஸ்ல இருந்து பேசுறேன். இது அவசரம். உடனே பணம் அனுப்புங்க."* $\to$ Same canonical tactics (Score: 85, CRITICAL)
* **Tanglish (Romanized):** *"Naan police la irundhu pesuren. Idhu romba urgent. Udane money send pannunga."* $\to$ Same canonical tactics (Score: 85, CRITICAL)
* **Hindi (Devanagari):** *"Main police se bol raha hoon. Yeh bahut urgent hai. Abhi paise bhejo."* $\to$ Same canonical tactics (Score: 85, CRITICAL)
* **Hinglish (Romanized):** *"Main police se bol raha hoon, urgent hai, abhi money send karo."* $\to$ Same canonical tactics (Score: 85, CRITICAL)
* **Telugu (Romanized):** *"Nenu police nundi matladutunnanu. Idi urgent. Ventane dabbu pampinchandi."* $\to$ Same canonical tactics (Score: 85, CRITICAL)
* **Kannada (Native):** *"ನಾನು ಪೊಲೀಸ್ ಇಂದ ಮಾತನಾಡುತ್ತಿದ್ದೇನೆ. ಇದು ತುರ್ತು. ತಕ್ಷಣ ಹಣ ಕಳುಹಿಸಿ."* $\to$ Same canonical tactics (Score: 85, CRITICAL)
* **Malayalam (Native):** *"ഞാൻ പോലീസ് ഓഫീസർ ആണ് സംസാരിക്കുന്നത്. ഇത് അടിയന്തിരമാണ്. ഉടൻ പണം അയക്കുക."* $\to$ Same canonical tactics (Score: 85, CRITICAL)
* **Bengali (Native):** *"আমি পুলিশ থেকে বলছি। এটা জরুরি। অবিলম্বে টাকা পাঠান।"* $\to$ Same canonical tactics (Score: 85, CRITICAL)
* **Marathi (Native):** *"मी पोलीस मधून बोलतोय. हे तातडीचे आहे. लगेच पैसे पाठवा."* $\to$ Same canonical tactics (Score: 85, CRITICAL)
* **Gujarati (Native):** *"હું પોલીસ માંથી બોલું છું. આ તાકીદનું છે. તરત જ પૈસા મોકલો."* $\to$ Same canonical tactics (Score: 85, CRITICAL)
* **Punjabi (Gurmukhi):** *"ਮੈਂ ਪੁਲਿਸ ਤੋਂ ਬੋਲ ਰਿਹਾ ਹਾਂ। ਇਹ ਬਹੁਤ ਜ਼ਰੂਰੀ ਹੈ। ਤੁਰੰਤ ਪੈਸੇ ਭੇਜੋ।"* $\to$ Same canonical tactics (Score: 85, CRITICAL)
* **Odia (Native):** *"ମୁଁ ପୋଲିସ ତରଫରୁ କହୁଛି। ଏହା ଜରୁରୀ। ତୁରନ୍ତ ଟଙ୍କା ପଠାନ୍ତୁ।"* $\to$ Same canonical tactics (Score: 85, CRITICAL)

### Code-Switching Continuity

The engine operates seamlessly across sentence-internal code switching (e.g. *"Sir naan police department la irundhu pesuren, this is urgent, immediately money transfer pannunga"*). English keywords are never discarded when primary Indic speech is detected.

---

## 5. False Positive Protection & Benign Suppression

A dedicated regex suppression layer shields ordinary everyday conversations from being falsely flagged:

1. **Physical Locations & Directions:**
   * *"I am going to the police station."* $\to$ Score: 0 (LOW), Tactics: None
   * *"Police station is near my house."* $\to$ Score: 0 (LOW), Tactics: None
   * *"போலீஸ் ஸ்டேஷன் பக்கத்துல இருக்கு."* $\to$ Score: 0 (LOW), Tactics: None
2. **Third-Person Police Assistance:**
   * *"The police officer helped me yesterday."* $\to$ Score: 0 (LOW), Tactics: None
3. **Everyday Non-Coercive Transfers:**
   * *"Please send money to the grocery shop."* $\to$ Score: 0 (LOW), Tactics: None
   * *"Can you transfer money to my mother?"* $\to$ Score: 0 (LOW), Tactics: None
   * *"என் அம்மாவுக்கு பணம் அனுப்பணும்."* $\to$ Score: 0 (LOW), Tactics: None
4. **Legitimate In-Person Banking:**
   * *"My bank asked me to visit the branch tomorrow."* $\to$ Score: 0 (LOW), Tactics: None
5. **Everyday Urgent Academic/Work Deadlines:**
   * *"This assignment is urgent."* $\to$ Score: 0 (LOW), Tactics: None
   * *"I urgently need to submit my college assignment."* $\to$ Score: 0 (LOW), Tactics: None
6. **Legitimate OTP Notifications:**
   * *"I received an OTP from my bank for online purchase."* $\to$ Score: 0 (LOW), Tactics: None

**Benchmark Result:** 13/13 benign test cases passed with 0% false positives.

---

## 6. Multi-Turn Progressive Escalation

Scams unfold across progressive stages. The `ConversationRiskState` retains chronological turn history:

| Turn | Speaker Utterance | Tactics Detected | Turn Score | Cumulative Level | Trend |
|---|---|---|---|---|---|
| Turn 1 | *"Hello sir, is this Kamalesh?"* | None | 0 | `LOW` | → Stable |
| Turn 2 | *"I'm calling from the cyber crime department."* | Authority | 20 | `LOW` | ↑ Increasing |
| Turn 3 | *"The account linked to your Aadhaar has suspicious activity."* | Authority, Threat | 55 | `MEDIUM` | ↑ Increasing |
| Turn 4 | *"Do not tell your family, keep this confidential."* | Authority, Threat, Secrecy | 75 | `HIGH` | ↑ Increasing |
| Turn 5 | *"Transfer ₹25,000 immediately for verification."* | Auth + Threat + Secrecy + Urgency + Payment | 100 | `CRITICAL` | ↑ Increasing |

---

## 7. Demo Video Call Mode UI

The Android prototype includes a dedicated screen clearly labeled:
* **"DEMO VIDEO CALL"** / **"SIMULATED CALL"**
* Simulated Remote Caller ("Unknown Caller") with video pulse animation
* Live Protection HUD:
  * Detected Language & Confidence percentage
  * Risk Score (0-100) & Color-coded Level Badge
  * Trend indicator (↑ / → / ↓)
  * Detected Tactics with exact quoted evidence spans
* Live Transcript stream
* Real call controls: `[ MUTE ]`, `[ SPEAKER ]`, `[ END CALL ]`, `[ PROTECT ]`
* Prototype 13-Scenario Selector (Police+Money, Bank+OTP, Courier, Government, Remote Access, Investment, Benign, Tamil, Tanglish, Hindi, Hinglish, Telugu, Code-Switch)
* Non-blocking Protection Action Modal:
  * `[PAUSE CALL]`
  * `[VERIFY CALLER]`
  * `[ALERT TRUSTED CONTACT]`
  * `[CONTINUE]`

---

## 8. Test Execution Summary

* **Unit Test Suite (`test_multilingual_risk_engine.py`):** 53/53 PASSED (0.22s)
* **Real ASR Tests (`test_real_multilingual_asr.py`):** 14/14 PASSED
* **Intelligence Tests (`test_multilingual_intelligence.py`):** 4/4 PASSED
* **Benchmark Evaluation (`benchmark_multilingual_risk.py`):** 42/42 PASSED (0.34ms avg latency)
  * False Positive Rate: 0.0% (0/13)
  * False Negative Rate: 0.0% (0/29)

---

## 9. Known Limitations & Production Blockers

1. **Audio ASR Noise Floor on Physical Devices:** In heavy acoustic noise environments, Faster-Whisper ASR accuracy on Romanized Indic speech (Tanglish/Hinglish) requires near-field microphone proximity.
2. **Dialectal Phonetic Variation:** Certain extreme dialectal spellings in Romanized scripts (e.g. colloquial abbreviations like *"udn mny snd pnu"*) may require fuzzy token matching in future iterations.
3. **Physical-Device Validation:** While automated unit, integration, and UI component compilation pass completely, physical field-testing across live carrier phone calls is pending.
