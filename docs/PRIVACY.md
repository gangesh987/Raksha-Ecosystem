# RakshaCall Privacy By Design

## 1. Zero Raw Audio & Video Storage
- **Ephemeral Processing**: Spoken voice chunks captured by Android `SpeechRecognizer` exist only in volatile device RAM long enough to extract transcript text.
- **Immediate Discard**: Raw audio buffers and camera frames are immediately released and garbage collected. No `.wav`, `.mp3`, or `.mp4` recordings are ever written to the internal filesystem or external storage.

## 2. Granular Consent Management (`ConsentManager.kt`)
Every user grant is tracked in an immutable local audit log with policy versioning:
1. `MICROPHONE`: Required for real-time speech-to-text safety analysis.
2. `CAMERA`: Optional supporting signal for frame quality and luminance analysis.
3. `SCREEN_CAPTURE`: User-consented capture flows for video calls.
4. `CLOUD_SYNC`: Optional remote synchronization of incident metadata.
5. `TRANSCRIPT_STORAGE`: Local persistence of transcript events.
6. `EVIDENCE_STORAGE`: Cryptographic hash chain maintenance.
7. `TRUSTED_CONTACT_ALERTS`: Handoff of emergency notices to family.

## 3. Right to Erasure & Account Deletion
- **One-Tap Data Wipe**: The Privacy Center exposes a complete data erasure workflow (`DeleteUserDataUseCase`).
- Wiping data executes a SQL delete on all 10 local tables, clears DataStore preferences, resets all consent grants, and issues cloud deletion requests if sync is configured.
