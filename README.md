# RAKSHACALL

**A Privacy-Conscious, On-Device Real-Time AI Safety Layer Against Digital-Arrest & Coercive Scams**

---

## 1. Executive Summary & Problem Statement

Digital-arrest scams and coercive extortion calls have emerged as one of the fastest-growing cybersecurity threats globally, particularly across India and Southeast Asia. Fraudsters impersonate senior law enforcement officials (CBI, State Police, Cyber Crime Units, Supreme Court, TRAI, Customs, Enforcement Directorate) over voice and video calls. They allege that the victim's identity or Aadhaar has been implicated in contraband seizures, money laundering, or illegal parcels, and intimidate victims into 24/7 "digital custody" in isolated rooms before extorting lakhs of rupees under the guise of "RBI verification deposits" or "clearing penalty fees."

**RakshaCall** is a production-grade prototype native Android application and distributed conversational safety backend designed to serve as an on-device, real-time safety layer (full production deployment requires external cloud SMS gateway provisioning, dedicated streaming ASR decoders, and cloud container orchestration). It continuously analyzes conversational tactics, calculates manipulation velocity, tracks scam stage progression, maintains an append-only cryptographic SHA-256 evidence chain, and immediately intervenes with a **Safety Brake** before victims can execute irreversible financial transfers or disclose credentials.

---

## 2. Zero Mock Data Architecture

RakshaCall is built as a **REAL WORKING PROTOTYPE, NOT A STATIC UI MOCKUP**.
- **No Mock Data Policy**: On first installation, the Room database and DataStore preferences are strictly empty.
- Every displayed value originates from actual user input, real Android system permission status, Room database queries, or active analysis engines.
- If no previous sessions exist, the dashboard displays honest empty states (*"No protection sessions yet"*, *"No incidents recorded"*).
- All charts, trajectory curves, and evidence timelines are plotted exclusively from real stored events.
- No simulated external SMS delivery: prototype authentication and trusted contact handoffs clearly and honestly disclose local execution.

---

## 3. Technology Stack

- **Platform**: Native Android (API 24 to 36)
- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: MVVM / Clean Architecture with Repository Pattern
- **Concurrency & Reactivity**: Kotlin Coroutines & StateFlow
- **Local Persistence**:
  - **SQLite / Room-Compatible Database**: Entities for `users`, `protection_sessions`, `transcript_events`, `risk_events`, `scam_stage_events`, `trusted_contacts`, `evidence_events`, `alert_events`, `consent_records`, and `risk_points`.
  - **DataStore Preferences**: Session authentication, configurable risk thresholds, privacy settings, and sensor flags.
- **Audio Intelligence**: Android `SpeechRecognizer` with streaming chunk callback and ephemeral memory handling.
- **Visual Intelligence**: CameraX `ImageAnalysis` measuring frame luminance, contrast variance, and face stability proxy.
- **Background Processing**: Android Foreground Service with persistent notification (`FOREGROUND_SERVICE_MICROPHONE`).
- **Cryptography**: SHA-256 tamper-evident hash chaining.

---

## 4. Platform Limitations & Honest Privacy Model

> [!IMPORTANT]
> **Android Platform Restrictions on Third-Party Call Interception**
> - Modern Android operating systems sandbox applications and strictly forbid third-party apps from silently intercepting or recording encrypted third-party VoIP calls (such as WhatsApp, Telegram, or Skype) or native cellular phone calls without user awareness.
> - **RakshaCall NEVER claims to secretly intercept encrypted third-party calls.**
> - Instead, RakshaCall implements legitimate Android input flows:
>   1. **Permitted Microphone Capture**: Active during an explicit protection session with user consent, using Android `SpeechRecognizer` to transcribe audio spoken aloud or on speakerphone.
>   2. **Live Input Lab & Transcript Entry**: Allows users to paste or speak chunks directly for real-time safety analysis.
>   3. **Screen / Audio Capture Intent Flow**: Initiated only when the user explicitly approves Android's system capture dialog.
> - **Zero Raw Audio Storage**: Spoken audio is transcribed ephemerally on-device and immediately discarded. Raw audio and video are never stored on disk.

---

## 5. Intelligence Engines & Algorithmic Design

### A. Deterministic Local NLP Risk Engine (`RiskEngine.kt`)
Analyzes real-time transcript events against 9 coercive scam tactics:
1. **Authority Impersonation** (+15): CBI, Police, Cyber Crime Cell, Supreme Court, TRAI, Customs, Inspector.
2. **Criminal Allegation / Fear** (+15): Money laundering, Aadhaar involvement, drugs found, parcel seized, arrest warrant, FIR.
3. **Urgency** (+10): "Immediately", "right now", "within 15 minutes", "final warning".
4. **Isolation** (+15): "Do not disconnect", "stay in a quiet room", "do not tell family", "digital arrest".
5. **Payment Demand** (+20, Irreversible): "Transfer ₹50,000", "security deposit", "escrow account", "penalty fee".
6. **Credential / OTP Pressure** (+20, Irreversible): "Give me your OTP", "UPI PIN", "CVV", "netbanking password".
7. **Remote Access Pressure** (+15, Irreversible): "Install AnyDesk", "TeamViewer", "QuickSupport", "screen share".
8. **Suspicious Link** (+10): Malicious verification forms or APK downloads.
9. **Coercive Escalation** (+10): Threats of physical arrest teams, home raids, property seizure.

**Scoring Nuances**:
- **Duplicate Suppression**: Repeated tactics within 45 seconds have their weights dampened (0.3x) to prevent score gaming.
- **Escalation Multiplier**: Detecting 2 or more distinct tactic categories multiplies contributions by 1.15x; 4 or more distinct tactics applies 1.35x.
- **Temporal Decay**: 2 points decay for every 45 seconds of clean conversation (down to highest detected stage floor).
- **Maximum Score**: Capped strictly at 100.
- **Risk Levels**: Low (0–29), Medium (30–59), High (60–79), Critical (80–100).

### B. Scam Stage State Machine (`ScamStageMachine.kt`)
Models the digital-arrest psychological funnel:
`CONTACT` ➔ `AUTHORITY` ➔ `FEAR` ➔ `ISOLATION` ➔ `DEMAND` ➔ `PAYMENT_CREDENTIAL` ➔ `ESCALATION`
Transitions are strictly monotonic (forward-only) and require verified detected signals.

### C. Manipulation Velocity Engine (`ManipulationVelocityEngine.kt`)
Measures coercive density over actual event timestamps within a 90-second sliding window:
$$\text{Velocity} = \frac{\sum \text{Weights in 90s}}{\Delta t}$$
Categorized into `LOW`, `MODERATE`, or `HIGH`, explaining: *"3 coercive tactics detected within 20-second intervals (rapid coercion spike)."*

### D. Explainable Risk Fusion & Disagreement Guard (`RiskFusionEngine.kt`)
- **Conversation is PRIMARY**: Verbal threats and demands carry overriding priority.
- **Visual Signals are SUPPORTING**: CameraX metrics (face presence, lighting, stability) provide context. Weak visual signals never override strong conversational coercion.
- **Model Disagreement Guard**: Flags discrepancies explicitly:
  *"Conversation evidence indicates significant coercive behavior. Supporting visual signal appears clear. Conversational coercion remains PRIMARY."*

---

## 6. Interventions & Cryptographic Evidence Vault

### A. Safety Brake Intervention
When risk score crosses `HIGH` (≥60) AND an irreversible action is detected (Payment, OTP, Credential, Remote Access):
- Full-screen high-priority protective warning modal activates.
- Direct guidance: STOP, do not transfer funds, do not share OTP, do not install software.
- Actions:
  - **PAUSE & VERIFY**: Opens the Independent Verification Coach.
  - **CONTACT TRUSTED PERSON**: Triggers Android SMS / Share intent with an emergency alert message.
  - **VIEW WHY**: Displays transparent scoring breakdown.

### B. Independent Verification Coach
5-step interactive protocol:
1. Pause or disconnect the call.
2. Do not call back on numbers provided by the caller.
3. Find official government / department contacts independently (e.g. cybercrime.gov.in, Dial 1930).
4. Contact through independent channels or visit nearest police station.
5. Verify with a trusted family member.
- Allows user to update status: `NOT STARTED`, `IN PROGRESS`, `VERIFIED`, `UNABLE TO VERIFY` (persisted to Room).

### C. Append-Only SHA-256 Tamper-Evident Evidence Vault
Every session event is cryptographically linked to its predecessor:
$$\text{currentHash} = \text{SHA-256}(\text{previousHash} : \text{eventId} : \text{timestamp} : \text{eventType} : \text{sessionId} : \text{payloadJson})$$
- Verification verifies every block sequentially from genesis (`0000...0000`).
- If any record or payload is mutated, `EvidenceHasher.verifyChain()` returns `IntegrityResult.Failed` identifying the compromised event.
- Export options: Formatted plain-text log, JSON, and Android Share Intent.

---

## 7. Phase 2 Production Architecture

RakshaCall enforces strict **Clean Architecture** where the UI layer communicates exclusively through ViewModels and Domain UseCases down to provider-neutral repository interfaces:

```
                    UI (Jetpack Compose)
                             │
                     StateFlow / Events
                             │
                         ViewModels
                             │
                      Domain Use Cases
        (StartProtectionSession, ProcessTranscript,
         AnalyzeRisk, UpdateScamStage, SafetyBrake,
         VerifyEvidence, AlertTrustedContact, Sync)
                             │
                    Repository Interfaces
            (User, Session, Risk, Evidence,
             TrustedContact, Consent, Sync)
                             │
             ┌───────────────┼──────────────┐
             ▼               ▼              ▼
        Local Room      Firebase SDK    REST API
      (SQLite + AES)   (Auth/Firestore) (Retrofit/OkHttp)
```

### Core Phase 2 Components
1. **Domain Use Cases** (`com.rakshacall.safety.domain.usecase`):
   - `StartProtectionSessionUseCase`: Initializes session and generates genesis SHA-256 evidence block.
   - `ProcessTranscriptUseCase`: Ingests speech, detects tactics, and persists transcript and risk signals.
   - `AnalyzeRiskUseCase`: Computes explainable risk scoring, temporal decay, and duplicate suppression.
   - `UpdateScamStageUseCase`: Enforces monotonic progression across the 7-stage scam funnel.
   - `CalculateManipulationVelocityUseCase`: Measures tactic density across sliding time windows.
   - `TriggerSafetyBrakeUseCase`: Deterministically fires protective intervention on high score + irreversible action.
   - `VerifyEvidenceIntegrityUseCase`: Performs full-chain SHA-256 verification and pinpoints tampered blocks.
   - `FuseRiskSignalsUseCase`: Fuses speech (primary) and visual (supporting) signals with disagreement guards.
   - `AlertTrustedContactUseCase`: Manages emergency contact dispatch with honest delivery status tracking.
   - `DeleteUserDataUseCase`: Fully purges local databases, preferences, and issues cloud deletion requests.

2. **Hardware-Backed Keystore Security** (`KeystoreManager.kt`):
   - AES-256-GCM authenticated encryption using keys safely generated in the hardware AndroidKeyStore.
   - Encrypts sensitive evidence payloads at rest before writing to Room database.

3. **Offline-First Synchronization Engine** (`SyncManager.kt`):
   - Local Room remains the single source of truth for offline safety.
   - Tracks sync states (`LOCAL_ONLY`, `PENDING`, `SYNCING`, `SYNCED`, `FAILED`, `CONFLICT`).
   - If network connectivity is lost, protection and evidence generation continue uninterrupted; changes resume sync automatically upon reconnection.

4. **Production Networking Layer** (`ApiService.kt`, `NetworkClient.kt`):
   - Retrofit + OkHttp 4.12.0 client with exponential backoff, request timeouts, and connection monitoring.
   - Defines standard REST endpoints for sessions, risk analysis, evidence verification, and trusted contact alerts.

5. **Provider-Neutral Firebase Integration**:
   - `FirebaseAuthRepository`: Bridges phone and Google authentication with fallback to local security.
   - `FirestoreSessionRepository`: Mirrors local sessions to Cloud Firestore when cloud sync is granted.
   - `FirebaseNotificationHandler`: FCM push notification handler strictly redacting PII / OTP data on lock screens.

6. **Granular Consent & Lifecycle Permission Management**:
   - `ConsentManager`: Manages explicit user consent records for Microphone, Camera, Cloud Sync, and Evidence Storage with strict policy versioning (`2.0.0`).
   - `PermissionStateManager`: Reconciles actual Android runtime permissions dynamically when returning from system settings.

7. **Multilingual Architecture for Indian Languages**:
   - `SpeechLanguageManager`: Exposes active supported locales (`en-IN`, `hi-IN`, `ta-IN`, `te-IN`, `mr-IN`, etc.).
   - `LanguageAwareTacticEngine`: Detects Hindi and Hinglish coercion vocabulary (*"क्राइम ब्रांच"*, *"अरेस्ट वारंट"*, *"पैसे ट्रांसफर"*, *"कमरे में बंद रहो"*) seamlessly.

8. **Demo Data Isolation**:
   - `LiveInputLab` test sessions are flagged with `isDemoSession = true`.
   - Personal security analytics in `HomeDashboard` and `IntelligenceScreen` strictly isolate and exclude test sessions.

---

## 8. Automated Test Suite (28 Tests Passing)

All 28 automated unit tests pass 100% via Gradle:

```bash
./gradlew testDebugUnitTest
```

```text
BUILD SUCCESSFUL in 29s
24 actionable tasks: 24 up-to-date
```

### Test Suite Breakdown:
- `Phase2ProductionArchitectureTest` (8 tests):
  - `testStartProtectionSessionUseCaseCreatesSessionAndGenesisEvidence` — PASSED
  - `testProcessTranscriptUseCaseDetectsTacticsAndPersists` — PASSED
  - `testAnalyzeRiskUseCaseProducesExplainableDecision` — PASSED
  - `testScamStageProgressionUseCaseMonotonicTransitions` — PASSED
  - `testSafetyBrakeUseCaseTrigger` — PASSED
  - `testVerifyEvidenceIntegrityUseCaseDetectsTampering` — PASSED
  - `testLanguageAwareTacticEngineDetectsHindiCoercion` — PASSED
  - `testKeystoreEncryptionAndDecryptionRoundtrip` — PASSED
- `RiskEngineTest` (8 tests):
  - `detects authority impersonation from live speech text` — PASSED
  - `detects criminal allegation and fear tactics` — PASSED
  - `detects isolation tactic` — PASSED
  - `detects payment demand and irreversible action flag` — PASSED
  - `detects credential and OTP pressure` — PASSED
  - `applies duplicate suppression dampening within 45 seconds` — PASSED
  - `calculates cumulative score and escalation multiplier with multiple distinct tactics` — PASSED
  - `triggers Safety Brake when score is HIGH and context includes payment pressure` — PASSED
- `ScamStageMachineTest` (3 tests):
  - `initial stage starts at CONTACT` — PASSED
  - `progresses sequentially through AUTHORITY and FEAR upon verified tactics` — PASSED
  - `strictly preserves monotonic progression and does not regress to earlier stage` — PASSED
- `ManipulationVelocityTest` (2 tests):
  - `rapid barrage of coercive tactics within 30 seconds triggers HIGH velocity` — PASSED
  - `sparse tactics across wide time produce LOW velocity` — PASSED
- `EvidenceIntegrityTest` (3 tests):
  - `valid evidence chain verifies as IntegrityResult Valid` — PASSED
  - `tampered payload in middle of chain immediately causes IntegrityResult Failed` — PASSED
  - `broken previousHash link immediately fails validation` — PASSED
- `RiskFusionEngineTest` (2 tests):
  - `fuses signals with conversation as primary and includes explicit reasons` — PASSED
  - `detects model disagreement when conversation is high but visual is clear` — PASSED
- `MainScreenViewModelTest` (2 tests):
  - `initial state has empty inputs and idle analysis` — PASSED
  - `runAnalysis updates state with real engine results` — PASSED

---

## 9. Comprehensive Engineering Documentation (`docs/`)

The repository includes a complete 15-document technical architecture and governance suite:

1. [`docs/PRODUCTION_AUDIT.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/PRODUCTION_AUDIT.md) — Comprehensive pre-upgrade audit of all components, permissions, and migration paths.
2. [`docs/ARCHITECTURE.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/ARCHITECTURE.md) — Clean Architecture layers, data flow, and dependency injection specifications.
3. [`docs/SECURITY.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/SECURITY.md) — Keystore encryption, zero-trust cloud ingestion, and PII protection policies.
4. [`docs/PRIVACY.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/PRIVACY.md) — Privacy-by-design, ephemeral audio discarding, and retention limits.
5. [`docs/DATA_MODEL.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/DATA_MODEL.md) — Room schemas, version 2 migrations, and Firestore collections.
6. [`docs/SYNC.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/SYNC.md) — Offline-first sync lifecycle, conflict resolution, and acknowledgement queue.
7. [`docs/AI_PIPELINE.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/AI_PIPELINE.md) — Layered conversation intelligence, multilingual engines, and future ONNX runtime integration.
8. [`docs/RISK_ENGINE.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/RISK_ENGINE.md) — 9 scam tactics, duplicate dampening, temporal decay, and calibration parameters.
9. [`docs/EVIDENCE.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/EVIDENCE.md) — Append-only SHA-256 cryptographic chain, payload hashing, and tamper verification.
10. [`docs/ANDROID_LIMITATIONS.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/ANDROID_LIMITATIONS.md) — Security sandboxing, third-party encrypted call boundaries, and permitted capture paths.
11. [`docs/FIREBASE_MIGRATION.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/FIREBASE_MIGRATION.md) — Step-by-step guide for configuring Google Services, Phone Auth, and Firestore rules.
12. [`docs/DEPLOYMENT.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/DEPLOYMENT.md) — Gradle build commands, ProGuard/R8 minification, and release artifact generation.
13. [`docs/TESTING.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/TESTING.md) — Test strategy covering unit tests, adversarial red-team tests, and CI/CD validation.
14. [`docs/THREAT_MODEL.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/THREAT_MODEL.md) — Formal STRIDE threat assessment with mitigation and residual risk analysis.
15. [`docs/INCIDENT_RESPONSE.md`](file:///c:/Users/gangs/Downloads/New%20folder%20(4)/docs/INCIDENT_RESPONSE.md) — Cryptographic key compromise recovery, compromised client triage, and incident response playbooks.

---

## 10. How to Build & Run

### Prerequisites
- JDK 17+ or JDK 23
- Android SDK with Platform 35 or 36 and Build-Tools (`ANDROID_HOME` configured)

### Build Debug APK
```bash
./gradlew assembleDebug
```
Output artifact: `app/build/outputs/apk/debug/app-debug.apk` (Size: ~22.7 MB).

### Execute Complete Unit Test Suite
```bash
./gradlew testDebugUnitTest
```

### Install on Connected Device / Emulator
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 11. Live Interactive Demonstration Flow

1. **Launch RakshaCall**: Notice the clean zero-mock home state with zero sessions.
2. Tap **Live Lab** from the Home dashboard.
3. Tap **Speak** (Android SpeechRecognizer) or select interactive test phrases:
   - *"I am calling from Mumbai Police Cyber Crime Cell."* ➔ Detects **Authority Impersonation** (+15), stage moves to `AUTHORITY`.
   - *"Your Aadhaar is involved in illegal money laundering."* ➔ Detects **Criminal Allegation** (+15), escalation multiplier triggers, stage moves to `FEAR`.
   - *"Do not disconnect the call. Stay in a quiet closed room."* ➔ Detects **Isolation** (+15), manipulation velocity increases, stage moves to `ISOLATION`.
   - *"Transfer ₹50,000 immediately to RBI verification account."* ➔ Detects **Payment Demand** (+20, Irreversible).
4. **Safety Brake Triggers**: Full-screen high-priority alert pauses the session.
5. Tap **Pause & Verify**: Walk through the 5-step Independent Verification Coach.
6. Open **Evidence Vault**: Real-time timeline displays all events with cryptographic SHA-256 verification (`Integrity: VALID`). Tap **Export** to generate an incident report.
7. Return to **Home**: Observe real session count and risk points updated in Room without artificial data. Test sessions remain isolated from personal analytics.
