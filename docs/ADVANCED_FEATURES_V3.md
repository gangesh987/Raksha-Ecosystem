# RakshaCall Sentinel — Advanced Feature Blueprint v3

This release adds production-oriented architecture around the core safety loop.

## Core safety loop
CAPTURE WITH CONSENT → TRANSCRIBE → DETECT TACTICS → FUSE SIGNALS → EXPLAIN → WARN → ESCALATE → PRESERVE EVIDENCE

## Primary intelligence
Conversation/coercion remains the strongest durable signal. The engine tracks:
- authority impersonation
- urgency/time pressure
- isolation/secrecy
- payment/transfer pressure
- OTP/PIN/password requests
- threats/investigation language
- repetition and escalation
- suspicious links/contact/payment entities
- multi-stage scam-script progression

## Advanced risk science
- Risk trajectory and acceleration
- Time-decayed evidence
- Signal-quality scoring
- Signal disagreement detection
- Scenario-aware thresholds
- Calibrated confidence boundary
- Counterfactual explanations
- Human-readable decision trace

## Incident response
A graduated response ladder prevents overreaction:
LOW → observe
MEDIUM → caution
HIGH → interrupt + offer trusted-contact escalation
CRITICAL → emergency workflow boundary (explicit user confirmation where required)

Anti-spam cooldowns and an incident state machine prevent notification storms.

## Evidence integrity
Every event can be chained with a cryptographic hash reference. The exported manifest records:
- event timestamp
- event type
- transcript segment hash
- risk decision
- contributing signals
- model/version metadata
- consent state
- previous-event hash

This is tamper-evident provenance, not a claim of legal admissibility.

## Privacy
The product must never silently intercept encrypted communications. Capture must be explicit and revocable. Sensitive raw media should be minimized, redacted where possible, and retained only according to user policy.

## ML boundary
ASR, NLP, CV and liveness are adapters. The application can run with deterministic demo adapters today and real ONNX/Hugging Face/local models later without rewriting the UI or risk engine.
