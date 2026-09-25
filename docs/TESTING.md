# RakshaCall Automated Testing & Red-Team Verification

## 1. Automated Test Suite Overview

All unit and integration tests run on Gradle via standard Android test runners:

```bash
./gradlew testDebugUnitTest
```

### Core Engine Test Coverage:
1. `RiskEngineTest`:
   - Validates detection of all 9 coercive tactics independently.
   - Verifies duplicate tactic suppression dampening within the 45-second window.
   - Verifies multi-tactic escalation multipliers.
   - Verifies temporal decay toward stage baselines.
   - Verifies deterministic Safety Brake triggering on high-risk + irreversible actions.
2. `ScamStageMachineTest`:
   - Validates monotonic forward transitions (`CONTACT` ➔ `AUTHORITY` ➔ `FEAR` ➔ `ISOLATION` ➔ `DEMAND` ➔ `PAYMENT_CREDENTIAL` ➔ `ESCALATION`).
   - Verifies that subsequent lower-stage tactics do not regress the established stage.
3. `ManipulationVelocityTest`:
   - Verifies sliding 90-second timestamp window density calculations (`LOW`, `MODERATE`, `HIGH`).
4. `EvidenceIntegrityTest`:
   - Verifies valid SHA-256 sequential hash chaining.
   - Verifies that tampering with a single byte in payload, timestamp, or event ID immediately results in `IntegrityResult.Failed`.
5. `RiskFusionEngineTest`:
   - Verifies conversation-first priority.
   - Verifies explicit model disagreement detection when conversational coercion is high but visual signals are normal.

## 2. Adversarial Red-Team Scenarios

- **Scenario 1: Legitimate Police Contact**: User discussing filing a police report about lost keys.
  - Result: Authority keyword matched, but absence of criminal allegation, urgency, isolation, or payment demand keeps risk at **LOW (15/100)**. Safety Brake is NOT triggered.
- **Scenario 2: Routine Bank Transfer**: User asking family member to split dinner bill via UPI.
  - Result: No authority impersonation or arrest allegations; risk score remains **LOW**.
- **Scenario 3: Full Digital Arrest Progression**:
  - Live Speech: Police Impersonation ➔ Narcotics Allegation ➔ Isolation in Closed Room ➔ Urgent Escrow Transfer.
  - Result: Triggers all 4 stages, hits **CRITICAL (85+/100)**, activates Safety Brake modal within <100ms.
