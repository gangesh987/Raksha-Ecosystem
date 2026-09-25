# RakshaCall Production Audit Report

**Date**: September 2026  
**Auditor**: Antigravity Principal Security & Android Architect  
**Baseline Version**: 1.0.0-PROTOTYPE (Clean Architecture, Kotlin, Jetpack Compose, Room/SQLite, DataStore, CameraX, Android SpeechRecognizer)  
**Verification Result**: `./gradlew testDebugUnitTest assembleDebug` ➔ `BUILD SUCCESSFUL` (100% test pass rate, 0 compiler warnings)

---

## 1. Existing Architecture Analysis

### Current Structure:
- **Presentation Layer**: 
  - Jetpack Compose with Material 3.
  - Screens directly access `ServiceLocator` repositories or engines in several Composable functions (`HomeScreen`, `LiveInputLabScreen`, `ProtectionScreen`) rather than delegating strictly through ViewModels and UseCases.
- **Domain Layer**:
  - Contains core business models: `ScamTactic` (9 distinct tactics), `ScamStage` (7 forward stages), `RiskLevel`, `RiskSignal`, `TranscriptEvent`, `ProtectionSession`, `EvidenceEvent`, `TrustedContact`, `VisualSignal`, `FusedRiskAssessment`.
  - Engines: `RiskEngine` (deterministic regex/NLP), `ScamStageMachine` (forward monotonic state transitions), `ManipulationVelocityEngine` (sliding window tactic density), `RiskFusionEngine` (speech primary, visual supporting), `VisualAnalysisEngine` & `LocalLivenessEngine` (luminance, contrast, face stability).
  - Repositories: Defined interfaces in `domain.repository.RepositoryInterfaces.kt`.
- **Data Layer**:
  - `RakshaDatabase`: Custom SQLite OpenHelper implementation mimicking Room with reactive `StateFlow` streams.
  - `RakshaPreferences`: Jetpack DataStore Preferences for settings, thresholds, and local user ID.
  - `LocalRepositories`: Mapping between entities and domain models.
- **Dependency Injection**:
  - Manual singleton container (`ServiceLocator`) holding repositories and engines.
- **Hardware & System Integration**:
  - Android `SpeechRecognizer` in continuous chunk mode (`SpeechRecognitionManager.kt`).
  - CameraX image analysis measuring frame luminance, contrast variance, and face stability (`CameraManager.kt`).
  - Foreground Service with microphone type and notification controls (`ProtectionForegroundService.kt`).
  - Cryptography: SHA-256 tamper-evident hash chaining in `EvidenceHasher.kt`.

---

## 2. Current Capabilities (Verified Working)

1. **Zero-Mock Verification**: Database begins in a clean state with zero fake calls, mock statistics, or simulated records. All UI updates originate from real sensors or user interactions.
2. **Real Speech Pipeline**: Microphone stream passes directly into on-device `SpeechRecognizer`, transcribing spoken chunks with zero permanent raw audio storage.
3. **9 Coercive Scam Tactics Detection**: Local deterministic NLP engine accurately identifies:
   1. *Authority Impersonation* (+15)
   2. *Criminal Allegation / Fear* (+15)
   3. *Urgency* (+10)
   4. *Isolation* (+15)
   5. *Payment Demand* (+20, irreversible action)
   6. *Credential / OTP Pressure* (+20, irreversible action)
   7. *Remote Access Pressure* (+15, irreversible action)
   8. *Suspicious Links* (+10)
   9. *Coercive Escalation* (+10)
4. **Algorithmic Nuance**: Implements temporal decay, 45-second duplicate tactic suppression dampening, and multi-tactic escalation multipliers.
5. **Manipulation Velocity Engine**: Tracks tactic density within a 90-second sliding window (`LOW`, `MODERATE`, `HIGH`).
6. **Scam Stage Machine**: Monotonic state machine tracking progression across 7 stages (`CONTACT` ➔ `ESCALATION`).
7. **Safety Brake & Anti-Pressure UX**: Intervenes when Risk $\ge 60$ and irreversible action is detected, presenting a full-screen warning modal with independent verification actions.
8. **Tamper-Evident SHA-256 Evidence Chain**: Computes cumulative hash `SHA256(prevHash:eventId:timestamp:type:sessionId:payload)` for every evidence event, with automated integrity verification.
9. **Live Input Lab**: Interactive playground for speech and test utterances that dynamically drives the full detection-to-evidence pipeline.

---

## 3. Technical Debt Identified

1. **UI Direct Coupling to ServiceLocator**:
   - Composable functions (`HomeScreen`, `ProtectionScreen`, `LiveInputLabScreen`) directly call `ServiceLocator.repository` or `ServiceLocator.engine`.
   - *Fix*: Introduce ViewModels for each feature and wrap business logic in formal UseCases (`StartProtectionSessionUseCase`, `ProcessTranscriptUseCase`, etc.).
2. **Room vs SQLiteOpenHelper**:
   - `RakshaDatabase` is currently written using `SQLiteOpenHelper` with manual cursors and `MutableStateFlow` invalidation, rather than official Room annotations (`@Database`, `@Dao`, `@Entity`). While functional, it increases boilerplate and lacks compile-time query verification.
   - *Fix*: Upgrade to standard Room DAOs and TypeConverters with explicit Room Migrations while preserving existing SQLite table schemas and data.
3. **Global Mutable State in Singletons**:
   - `ServiceLocator` holds mutable singleton engine instances, which can cause state bleeding across tests if not properly reset.
   - *Fix*: Transition to modular constructor-injected components and scoped UseCases.
4. **Demo Data Contamination Risk**:
   - Test utterances triggered in `LiveInputLabScreen` write to the same session store without marking `sessionType = DEMO`, potentially polluting historical analytics.
   - *Fix*: Mark demo sessions with `inputSource = "LIVE_LAB"` or `sessionType = "DEMO"` and exclude them from personal security analytics by default.

---

## 4. Security & Cryptographic Weaknesses

1. **Evidence At Rest**:
   - Evidence records in `evidence_events` are hashed with SHA-256, but the payload strings themselves are stored in plain text SQLite. If a device is rooted or physically extracted, incident payloads are unencrypted.
   - *Fix*: Implement local encryption at rest using AES-256-GCM via the Android Keystore (`EncryptedSharedPreferences` / Keystore-derived cipher).
2. **Hardcoded Secrets & Token Lifecycles**:
   - No token refresh mechanism or session expiry handling exists.
   - *Fix*: Implement secure token storage in Android Keystore with automatic token expiration, refresh headers, and clear logout/account-wipe workflows.
3. **Logging Discipline**:
   - Release builds must strictly strip debug logs and prevent raw transcripts or phone numbers from ever appearing in logcat or analytics.
   - *Fix*: Implement `SecurityLogger` that hashes session IDs, strips PII, and only logs operational codes.

---

## 5. Missing Production Features

1. **Clean Architecture UseCases**: Missing dedicated domain interactors orchestrating complex multi-engine transactions.
2. **Production Network & Sync Layer**: No Retrofit/OkHttp client, no offline-first `SyncManager` queue, no sync states (`LOCAL_ONLY`, `PENDING`, `SYNCING`, `SYNCED`, `FAILED`, `CONFLICT`).
3. **Firebase Cloud Ready Connectors**: Absence of optional `FirebaseAuthRepository`, `FirestoreSessionRepository`, and FCM push notification handlers.
4. **Formal Consent Management**: Missing `ConsentManager` tracking granular user grants (`MICROPHONE`, `CAMERA`, `CLOUD_SYNC`, `EVIDENCE_STORAGE`) with policy versions.
5. **Real-time Dynamic Permission Lifecycle**: Need a centralized `PermissionStateManager` that handles `DENIED_PERMANENTLY`, `REVOKED`, and settings-return reconciliation.
6. **Multi-language Architecture**: Hindi and regional language support placeholders with `SpeechLanguageManager` and language-aware tactic patterns.
7. **Incident Report V2**: PDF generation alongside JSON and formatted text, with legal disclaimers.

---

## 6. Android Platform Boundaries & Honesty

- **Third-Party Encrypted Call Interception**:
  - Android's security architecture strictly sandboxes applications. A third-party app cannot silently attach an audio tap or record calls from WhatsApp, Telegram, Signal, Skype, or cellular lines.
  - **Production Commitment**: RakshaCall will never advertise or claim silent background interception of encrypted VoIP apps. Supported capture paths are:
    1. Legitimate user microphone capture during an active protection session.
    2. Interactive Live Input Lab & manual text chunk analysis.
    3. User-consented screen / audio capture intent (via MediaProjection).
- **Foreground Service Transparency**:
  - Background audio capture requires a visible Foreground Service with `foregroundServiceType="microphone"` and a persistent non-dismissible notification.

---

## 7. Migration Risks & Data Integrity

- **Risk of Data Loss During Schema Upgrade**:
  - Modifying table schemas (e.g. adding sync columns: `localId`, `serverId`, `syncState`, `version`) could wipe existing user sessions if destructive migration is used.
  - *Mitigation*: Write explicit `Migration(1, 2)` scripts using `ALTER TABLE ADD COLUMN` for all 10 tables, maintaining backwards compatibility with any existing records.
- **SpeechRecognizer Hardware Variance**:
  - Some Android devices lack offline speech recognition packs or kill continuous recognition after silent intervals.
  - *Mitigation*: Implement robust error recovery (`ERROR_NO_MATCH`, `ERROR_SPEECH_TIMEOUT`), auto-restart listening loops with backoff, and graceful fallback to manual input.

---

## 8. Recommended Upgrade Order (Phase 2 Roadmap)

```
Phase 2.1: Domain UseCases & Production Clean Architecture
    ↓
Phase 2.2: Security & Local Keystore Encryption
    ↓
Phase 2.3: Room Schema Migration & Syncable Entities
    ↓
Phase 2.4: Offline-First Sync Engine & Network Layer
    ↓
Phase 2.5: Optional Firebase Cloud Integration
    ↓
Phase 2.6: ConsentManager & Permission Lifecycle
    ↓
Phase 2.7: Robust Speech Pipeline & Multilingual Architecture
    ↓
Phase 2.8: UI ViewModels & Production Polish
    ↓
Phase 2.9: Production Documentation & Verification
```
