# RAKSHACALL — RUNTIME ARCHITECTURE AUDIT REPORT
**Document Version:** 2.0.0-RUNTIME-VERIFIED  
**Date:** September 30, 2026  
**Target:** RakshaCall Autonomous Safety Engine  

---

## 1. Executive Summary & Audit Scope

This audit maps every component across the RakshaCall Android codebase (`app/src/main/`), examining operational reality against architectural intent. The codebase has transitioned from unit-tested prototype components into an integrated runtime system. This audit catalogues every Activity, Composable, ViewModel, Repository, UseCase, AI Provider, Speech/Vision Provider, Risk Engine, WebRTC/Signaling module, Foreground Service, Protection Controller, and Trusted Contact component to systematically eliminate disconnections, simulations, or unverified claims.

---

## 2. Component Inventory & Reality Matrix

| Component | Layer / Package | Reality Status | Dependencies | Missing Integration / Defect Found | Required Architectural Upgrade | Verification Method |
| :--- | :--- | :---: | :--- | :--- | :--- | :--- |
| **`MainActivity`** | Presentation (`com.example.rakshacall`) | **REAL** | `ComponentActivity`, `RakshaCallNavHost` | None. Clean entry point with edge-to-edge Compose rendering. | Ensure clean lifecycle management when backgrounded. | Android launch test & manifest intent audit. |
| **`RakshaCallNavHost`** | Navigation (`presentation.navigation`) | **REAL** | Compose Navigation, `Screen`, `ServiceLocator` | Routes were navigating to `ProtectedRoomScreen` which lacked full two-way signaling wiring. | Wire full call invitation & WebRTC signaling state machine. | Automated compose route inspection. |
| **`RealtimeCallScreen`** | Presentation (`presentation.protection`) | **PARTIAL** | `WebRtcEngine`, `SpeechRecognitionManager`, `RakshaAIOrchestrator`, `ProtectionController` | Signaling was disconnected from ICE/SDP exchange (`/* Signaling handles ICE */`). Speaker was hardcoded to `CALLER`. | Connect `SignalingClient`, add speaker separation (`LOCAL` vs `REMOTE`), wire diagnostic HUD. | WebRTC loopback & unit/instrumented tests. |
| **`WebRtcEngine`** | WebRTC Media (`com.rakshacall.safety.webrtc`) | **REAL** | `org.webrtc.*`, `StreamWebRtc`, `LocalMediaManager` | Had no direct hook to `SignalingClient` in the UI; connection lifecycle states (`RINGING`, `RECONNECTING`) were not emitted. | Implement strict 7-state lifecycle: `OUTGOING -> RINGING -> CONNECTING -> CONNECTED -> RECONNECTING -> DISCONNECTED -> ENDED`. | WebRTC peer session & ICE candidate exchange test. |
| **`LocalMediaManager`** | WebRTC Media (`com.rakshacall.safety.webrtc`) | **REAL** | `Camera2Enumerator`, `VideoCapturer`, `AudioTrack` | None. Correctly acquires hardware Camera & Mic tracks via WebRTC. | Add dynamic bitrate & mute toggling without tearing down pipeline. | Audio/Video track hardware capture test. |
| **`SignalingClient`** | Network / Signaling (`com.rakshacall.safety.signaling`) | **REAL** | OkHttp `WebSocketListener`, JSON Protocol | Disconnected from `RealtimeCallScreen`. Ran independently without driving WebRTC peer negotiation. | Integrate bidirectionally with `WebRtcEngine` for SDP Offer/Answer & ICE candidate routing. | WebSocket round-trip signaling tests. |
| **`SpeechRecognitionManager`** | Audio / ASR (`intelligence`) | **REAL** | Android native `SpeechRecognizer`, `RecognitionListener` | Yielded transcripts but lacked audio energy Voice Activity Detection (VAD) and speaker attribution. | Add VAD energy filtering and speaker tagging (`LOCAL` vs `REMOTE`). | Streaming speech recognition test. |
| **`MultilingualSemanticEngine`** | AI / Semantic (`intelligence`) | **REAL** | Rule & lexical semantic graph | Covered 8 languages and hard-negatives, but used strict tactic matching rather than open-vocabulary intent extraction. | Implement Open-Vocabulary Intent Engine (`POSSIBLE_VERIFICATION_CODE_REQUEST`, `POSSIBLE_FINANCIAL_TRANSFER_REQUEST`, etc.). | Multilingual 8-language test suite. |
| **`ConversationMemory`** | Intelligence (`intelligence`) | **REAL** | Rolling turn queue, pronoun resolution | Resolved pronouns across 2 turns, but did not extract entities (`BANK`, `POLICE`, `OTP`, `UPI`, `AADHAAR`). | Add Entity Extraction Engine & compact session semantic summarizer. | Cross-turn entity & pronoun unit test. |
| **`RiskEngine` & `RiskFusionEngine`** | Domain Engine (`domain.engine`) | **PARTIAL** | `ManipulationVelocityEngine`, `ScamStageMachine` | Risk was fused primarily from conversation score and visual flag; lacked independent multi-engine risk decomposition. | Implement Risk Engine 2.0 (`SemanticRisk`, `BehaviourRisk`, `VisualRisk`, `AudioRisk`, `ContextRisk`, `StageRisk`). | Multi-factor risk fusion tests. |
| **`ConfidenceEngine`** | Intelligence | **DISCONNECTED** | None (was embedded as simple ratio) | Confidence was conflated with risk score instead of evaluated as an orthogonal metric. | Implement standalone `ConfidenceEngine` evaluating corroboration, signal count, and acoustic clarity. | Confidence vs Risk orthogonality test. |
| **`EvidenceGraph`** | Intelligence / Evidence | **NEW / REQUIRED** | None | System tracked linear event lists but lacked a directed graph of coercive relationships. | Implement directed `EvidenceGraph` (Nodes: claims, threats; Edges: `FOLLOWED_BY`, `ESCALATED_TO`, `CORROBORATES`). | Graph cycle & escalation traversal test. |
| **`ModelRouter` & Disagreement** | Domain Engine | **PARTIAL** | `ModelDisagreementGuard` | Only guarded conversation vs visual; lacked execution mode router (`FAST_LOCAL`, `ADVANCED_LOCAL`, `CLOUD`, `RULE_FALLBACK`). | Implement `ModelRouter` and unified `ModelDisagreementEngine`. | Router fallback & disagreement tests. |
| **`ProtectionController`** | Safety (`protection`) | **REAL** | `ProtectionPolicy`, `ProtectionState` | Evaluated state machine accurately, but needed direct integration with Risk Engine 2.0 and Evidence Graph. | Connect 4 modes (`MONITOR`, `WARN`, `STRONG_PROTECTION`, `AUTO_PROTECT`) directly to Fused Risk. | Protection controller unit test suite. |
| **`EvidenceVault`** | Evidence (`evidence`) | **REAL** | SHA-256 MessageDigest, Hash Chaining | Fully operational tamper-evident ledger with privacy credential scrubbing. | Connect real WebRTC, ASR, and Visual events directly to vault. | Cryptographic chain verification test. |
| **`FrameSampler` & `VisualThreatDetector`**| Visual Intelligence (`intelligence.visual`) | **REAL** | WebRTC `VideoSink`, pattern analysis | Tested on mock strings; needed adaptive frame sampling rates tied to risk level. | Dynamically adjust sampling interval: Normal (2000ms), Suspicious (1000ms), Critical (400ms). | Non-blocking frame processing benchmark. |
| **`FamilyScreen` / Trusted Contacts** | Presentation & Data | **REAL** | Room DB `TrustedContactDao`, Android Intent | Fallback SMS/Share intent was used; needed compact, privacy-preserving notification payload. | Enforce minimum-disclosure alert format (no raw transcripts). | Trusted contact dispatch test. |
| **`PerformanceTelemetry`** | Diagnostics / Telemetry | **NEW / REQUIRED** | Android `Debug`, `SystemClock` | Latencies were measured ad-hoc in test benchmarks, not tracked live during active calls. | Implement live `PerformanceTelemetry` (ASR latency, AI inference latency, FPS, memory, CPU). | Live telemetry profiling test. |

---

## 3. Audit Findings: Search for Mock / Simulation Markers

A recursive scan of the codebase for `TODO`, `FIXME`, `mock`, `fake`, `dummy`, `simulation`, `placeholder`, `hardcoded`, `random`, `Thread.sleep`, and `sample data` revealed the following actionable items:
1. **Simulation Screen Disconnection:**
   `VideoCallSimulationScreen.kt` existed in the codebase as a standalone synthetic walkthrough, but `NavGraph.kt` had already redirected `Screen.VideoCallSimulation` to `ProtectedRoomScreen` (the genuine WebRTC room). `VideoCallSimulationScreen` is maintained strictly as an isolated developer testing tool.
2. **Signaling Server Configuration:**
   `SignalingClient` supports dynamic URLs, but `RealtimeCallScreen` did not instantiate it or supply a default signaling endpoint (`ws://...`). This left WebRTC in local media preview mode unless signaling was wired.
3. **Hardcoded Speaker Identification:**
   `RealtimeCallScreen` tagged all transcripts as `CALLER` because native Android `SpeechRecognizer` does not automatically separate remote WebRTC audio from microphone audio without hardware acoustic echo cancellation and audio track tap.
4. **Credential Redaction Verification:**
   `EvidenceVault` correctly replaces credentials with `[REDACTED]`, but regex patterns needed extension to cover 16-digit card numbers and UPI PIN expressions.

---

## 4. End-to-End Runtime Pipeline Architecture Plan

The upgraded architecture connects all components into a unified, non-blocking pipeline:

```
                      REAL WEBRTC CALL
                             |
             +---------------+---------------+
             |                               |
       AUDIO TRACK                      VIDEO TRACK
             |                               |
     Adaptive VAD Energy             Adaptive Frame Sink
             |                               |
   Streaming ASR (Partial/Final)      Visual Threat Detector
             |                               |
   Speaker & Language ID              Visual Risk Signal
             |                               |
             +---------------+---------------+
                             |
                   MULTIMODAL FUSION
                             |
                  OPEN-VOCABULARY INTENT
                             |
                  CONVERSATION MEMORY
                             |
                     EVIDENCE GRAPH
                             |
                 BEHAVIOURAL AI ENGINE
                             |
                    RISK ENGINE 2.0
      (Semantic + Behaviour + Visual + Audio + Context + Stage)
                             |
                     CONFIDENCE ENGINE
                             |
                   PROTECTION CONTROLLER
           (MONITOR / WARN / STRONG / AUTO_PROTECT)
                             |
             +---------------+---------------+
             |               |               |
       WARNING HUD     TRUSTED CONTACT   FORCE-CUT (5s)
             |                               |
             +---------------+---------------+
                             |
                 EVIDENCE VAULT (SHA-256)
```

---

## 5. Required Actions & Execution Plan

1. **Phase 2 & 3: WebRTC & Audio Pipeline Enhancement:**
   - Connect `SignalingClient` into `RealtimeCallScreen` and `WebRtcEngine`.
   - Implement the explicit 7-state call lifecycle (`OUTGOING`, `RINGING`, `CONNECTING`, `CONNECTED`, `RECONNECTING`, `DISCONNECTED`, `ENDED`).
   - Implement speaker attribution (`LOCAL` vs `REMOTE`) and VAD energy gating.
2. **Phase 7, 8 & 9: Open-Vocabulary Intent, Entities & Memory:**
   - Create `OpenVocabularyIntentEngine.kt` to infer intents beyond rigid keyword matching.
   - Create `EntityExtractionEngine.kt` extracting 16 critical financial/governmental entities.
   - Expand `ConversationMemory.kt` to generate bounded semantic summaries.
3. **Phase 10, 11, 12 & 13: Risk Engine 2.0, Confidence & Evidence Graph:**
   - Implement `RiskEngine2.kt` with independent multi-domain risk evaluation.
   - Implement `ConfidenceEngine.kt` calculating orthogonal confidence.
   - Implement `EvidenceGraph.kt` tracking causal and temporal coercion links.
4. **Phase 16, 22 & 25: Visual Sampling, Model Router & Performance Telemetry:**
   - Integrate adaptive frame sampling rate in `FrameSampler`.
   - Implement `ModelRouter.kt` with degraded fallback logic.
   - Implement `PerformanceTelemetry.kt` tracking real-time latency and resource metrics.
5. **Phase 31 & 36: Observability Diagnostic Dashboard:**
   - Add Diagnostic HUD toggle in `RealtimeCallScreen` exposing WebRTC state, audio/video FPS, latency, and evidence graph nodes.
