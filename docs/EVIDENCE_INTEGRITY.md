# Evidence Integrity & Cryptographic Chain

## 1. Overview
In digital-arrest and coercion scams, victims are frequently threatened, gaslit, or forced to destroy evidence. The RakshaCall **Evidence Vault** implements an append-only, tamper-evident cryptographic chain that guarantees chronological integrity for incident reporting, legal defense, and cybercrime (1930) investigations.

---

## 2. Cryptographic Architecture

### Genesis Hash
The chain initializes with a fixed 64-character zero hex string:
```text
0000000000000000000000000000000000000000000000000000000000000000
```

### Event Hash Formulation
Every state transition (transcript chunk, tactic trigger, risk acceleration, scam stage increment, Safety Brake firing, verification action, or contact dispatch) generates an immutable `EvidenceEvent`:

```text
Hash_n = SHA-256( previousHash + ":" + eventId + ":" + timestamp + ":" + eventType + ":" + payloadJson )
```

Where:
- `previousHash`: The SHA-256 digest of the immediately preceding event in this session (or genesis for event 0).
- `eventId`: UUID v4 uniquely identifying the event.
- `timestamp`: Epoch milliseconds recorded when the event occurred.
- `eventType`: Standardized identifier (`TRANSCRIPT`, `TACTIC_DETECTED`, `RISK_CHANGE`, `STAGE_TRANSITION`, `VELOCITY_ALERT`, `SAFETY_BRAKE_TRIGGERED`, `VERIFICATION_STEP`, `ALERT_DISPATCHED`).
- `payloadJson`: Canonical JSON representation of event attributes.

---

## 3. Verification Protocol
At any point—including during offline export—the app or an external auditor can verify the entire session chain:

```kotlin
fun verifyChain(events: List<EvidenceEvent>): ChainVerificationResult {
    var expectedPreviousHash = "0000000000000000000000000000000000000000000000000000000000000000"
    for (event in events) {
        if (event.previousHash != expectedPreviousHash) {
            return ChainVerificationResult.Tampered(event.eventId, "Broken link in chain")
        }
        val computedHash = sha256("${event.previousHash}:${event.eventId}:${event.timestamp}:${event.eventType}:${event.payloadJson}")
        if (computedHash != event.currentHash) {
            return ChainVerificationResult.Tampered(event.eventId, "Hash mismatch: payload altered")
        }
        expectedPreviousHash = event.currentHash
    }
    return ChainVerificationResult.Valid(events.size)
}
```

### Tamper Sensitivity
If even a single character in the transcript, a timestamp, or a tactic category is modified in the local database or export file:
1. The computed hash for that record immediately differs from `currentHash`.
2. All subsequent events in the session fail validation because `previousHash` no longer aligns.
3. The UI flags the evidence as `FAILED / INTEGRITY_COMPROMISED`.

---

## 4. Hardware Keystore Signing Boundary
In production environments with Android hardware security modules (TEE / StrongBox):
- The final session root hash is signed using an asymmetric key pair generated in `AndroidKeyStore`.
- The public key is exported in the incident manifest.
- This proves to law enforcement that the record was generated on that specific device and has not been altered after recording.

---

## 5. Export Format
Evidence is exportable via Android Share Sheet in two standardized formats:
1. **Machine-Readable JSON**: Complete array of typed events, hash chain links, and metadata.
2. **Human-Readable TXT**: Structured timeline detailing timestamped occurrences, coercion stages, and safety interventions, suitable for filing with `cybercrime.gov.in` and national helpline `1930`.
