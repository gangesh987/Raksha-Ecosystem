# RakshaCall Threat Model (STRIDE Methodology)

| Threat Category | Specific Threat | Impact | Likelihood | Mitigation Strategy | Residual Risk |
| :--- | :--- | :---: | :---: | :--- | :---: |
| **Spoofing** | Fraudster claims to be CBI/Police officer via voice/video. | HIGH | HIGH | Real-time on-device NLP detects authority claims + allegation + isolation; activates Independent Verification Coach (1930). | Low (User guided to disconnect). |
| **Tampering** | Malware modifies local evidence SQLite database to hide coercive events. | HIGH | LOW | Append-only SHA-256 hash chaining links each event to predecessor. Tampering is flagged as `Integrity: FAILED`. | Very Low. |
| **Repudiation** | Scammer denies making coercive payment or arrest threats. | HIGH | MED | Cryptographically hashed chronological event timeline with exact millisecond timestamps exported for police FIR. | Very Low. |
| **Information Disclosure** | Stolen device or malicious app attempts to read sensitive transcripts/PII. | HIGH | LOW | Hardware-backed AES-256-GCM Keystore encryption at rest. Raw audio is never stored on disk. | Very Low. |
| **Denial of Service** | Scammer floods call with rapid noise to overwhelm NLP engine. | MED | LOW | Ephemeral chunking, duplicate dampening, and bounded sliding windows prevent memory leaks or CPU exhaustion. | Low. |
| **Elevation of Privilege** | Malicious app attempts to execute arbitrary code via RakshaCall intents. | HIGH | VERY LOW | All BroadcastReceivers and Foreground Services explicitly marked `android:exported="false"`. | Negligible. |
