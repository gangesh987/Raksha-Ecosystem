# Conversation Intelligence

Phase 4 treats conversation analysis as **signal detection**, not a definitive scam verdict.

Supported signal classes include authority claims, urgency, threats, isolation, financial requests, OTP/password/credential requests, remote-access requests, secrecy, fear language, and impersonation cues.

The backend combines signals over time. A single occurrence of a word such as `OTP` does not automatically become a high-risk verdict.

## Transcript ingestion

`POST /api/protection/sessions/{session_id}/transcript`

Requires `audio_analysis` consent. The endpoint accepts a transcript produced by a real STT provider/client. It does not pretend to perform speech recognition itself.

## Privacy

Raw audio is not stored by this implementation.
