# RakshaCall API Specification (v4.0.0-PROD)

This document provides complete technical documentation for the RakshaCall backend REST and WebSocket APIs.

## 1. Overview & Architecture
The RakshaCall backend is built with **FastAPI** and provides:
- Session lifecycle and real-time state synchronization
- High-throughput WebSocket bi-directional streaming for audio PCM16 and transcript tokens
- Gemini Live / Gemini 1.5 Flash AI Provider integration
- Twilio emergency SMS alerts for trusted contacts with delivery receipts
- SHA-256 tamper-evident evidence export and verification

Base URL: `http://localhost:8000` or `https://api.rakshacall.internal`

---

## 2. Authentication & Headers
- `Authorization: Bearer <JWT_TOKEN>` (For authenticated user requests)
- `X-RakshaCall-ID: RC-XXXXXX` (Unique device identity header)
- `Content-Type: application/json`

---

## 3. Endpoints

### 3.1 Health & Readiness
- **GET** `/health`
  - Returns service status, database connectivity, and active AI engine readiness.
  - Response:
    ```json
    {
      "status": "healthy",
      "timestamp": 1726228800000,
      "ai_provider": "gemini-1.5-flash",
      "version": "4.0.0"
    }
    ```

### 3.2 Session Management
- **POST** `/api/sessions`
  - Starts a new protection session.
  - Body:
    ```json
    {
      "raksha_call_id": "RC-839102",
      "platform": "ANDROID",
      "consent_types": ["MICROPHONE", "CAMERA", "TRANSCRIPT_STORAGE"]
    }
    ```
  - Response:
    ```json
    {
      "session_id": "sess_904812f8",
      "status": "ACTIVE",
      "created_at": 1726228810000
    }
    ```

- **GET** `/api/sessions/{session_id}`
  - Retrieves session metadata, accumulated risk score, and current scam stage.

- **POST** `/api/sessions/{session_id}/terminate`
  - Ends session, finalizes evidence chain, and seals SHA-256 hash tree.

### 3.3 Transcripts & Coercion Intelligence
- **POST** `/api/sessions/{session_id}/transcript`
  - Receives streaming transcript chunks from Android SpeechRecognizer.
  - Body:
    ```json
    {
      "timestamp": 1726228825000,
      "speaker": "CALLER",
      "text": "This is Mumbai Police Cyber Crime. Your Aadhaar is implicated in money laundering."
    }
    ```
  - Response:
    ```json
    {
      "tactics_detected": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION"],
      "risk_contribution": 30,
      "current_stage": "FEAR",
      "manipulation_velocity": "HIGH",
      "safety_brake": false
    }
    ```

### 3.4 Trusted Contacts & Emergency Alerts
- **POST** `/api/sessions/{session_id}/alert`
  - Triggers an emergency SMS alert to a verified trusted contact.
  - **Honest Delivery Policy**: Only returns `DELIVERED` or `SENT` if Twilio credentials (`TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`) are valid and Twilio API returns a message SID. If unconfigured, returns `SMS_PROVIDER_NOT_CONFIGURED` with recommendation to use native Android Intent handoff.
  - Body:
    ```json
    {
      "contact_id": "cnt_1029",
      "phone_number": "+919876543210",
      "raksha_call_id": "RC-839102",
      "risk_score": 87,
      "detected_tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION", "ISOLATION", "PAYMENT_DEMAND"]
    }
    ```

### 3.5 Evidence Vault & Verification
- **GET** `/api/sessions/{session_id}/evidence`
  - Returns the cryptographic append-only timeline of events with SHA-256 hashes.
- **GET** `/api/sessions/{session_id}/evidence/verify`
  - Verifies the integrity of the hash chain from genesis block (`00000000...0000`).
  - Response:
    ```json
    {
      "session_id": "sess_904812f8",
      "total_events": 14,
      "chain_valid": true,
      "root_hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "status": "TAMPER_FREE"
    }
    ```

- **GET** `/api/sessions/{session_id}/report?format=json|txt`
  - Generates an official informational Incident Report for cybersecurity authorities / 1930 reporting.

---

## 4. WebSocket Real-Time Streaming
- **Path**: `ws://localhost:8000/api/ws/sessions/{session_id}`
- **Protocol**:
  - Client sends JSON frames:
    - Audio Chunk: `{"type": "AUDIO_CHUNK", "payload": "<base64_pcm16>"}`
    - Transcript: `{"type": "TRANSCRIPT", "speaker": "CALLER", "text": "..."}`
    - Heartbeat Ping: `{"type": "PING", "timestamp": 1726228830000}`
  - Server broadcasts:
    - `{"type": "PONG"}`
    - `{"type": "RISK_UPDATE", "score": 87, "level": "CRITICAL", "stage": "PAYMENT_CREDENTIAL", "safety_brake": true}`
    - `{"type": "DISAGREEMENT_GUARD", "disagreement": true, "explanation": "Conversational coercion remains PRIMARY."}`
