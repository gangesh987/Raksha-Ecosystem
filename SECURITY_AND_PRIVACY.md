# RakshaCall Security, Cryptography & Privacy Architecture
**Document Version:** 1.0.0  
**Classification:** Production Engineering Specification  
**System Scope:** End-to-End Client (Android Native / Flutter), gRPC Data-Plane, REST Control-Plane, Cryptographic Evidence Vault  

---

## 1. Executive Summary & Privacy Principles

RakshaCall is an ambient, real-time multimodal conversational safety platform designed to protect citizens from high-velocity fraud, digital arrest scams, and social engineering. Because conversational safety engines process sensitive acoustic, textual, and visual data, **privacy-by-design and defense-in-depth are not optional overlays—they are fundamental architectural invariants.**

### Core Tenets:
1. **Explicit User Consent First**: Zero audio, visual, or textual ingestion occurs without an explicit, verifiable consent session initiated by the user.
2. **Zero Client Secrets**: No third-party API keys, LLM credentials, cloud tokens, or signing master keys are ever packaged within the Android APK or Flutter bundle.
3. **Ephemeral Media Ingestion**: Live audio frames are processed in-memory in rolling 300ms chunks and immediately discarded following feature extraction and ASR transcription. Unconsented raw audio is never written to persistent disk storage.
4. **Hardware-Backed Device Isolation**: All device-side tokens, session identifiers, and emergency contacts are encrypted using Android Keystore hardware-backed keys (`MasterKey` with AES-256-GCM).
5. **Tamper-Evident Evidence Chaining**: Incidents produce SHA-256 hash-chained forensic audit logs. This provides verifiable data integrity (detecting any post-hoc modification) without overclaiming statutory legal certification.

---

## 2. Threat Model & Security Boundaries

RakshaCall operates across three distinct security domains:
```
┌─────────────────────────────────┐       mTLS / TLS 1.3      ┌─────────────────────────────────┐
│     CLIENT TRUST DOMAIN         │  Bidirectional Streaming  │     BACKEND CLOUD DOMAIN        │
│  - Android SE-Linux Sandbox     │ ────────────────────────> │  - Isolated Container Network   │
│  - Hardware Keystore (TEE/StrongBox)                        │  - Secure Secrets Manager       │
│  - Explicit Mic / Camera Perms  │ <──────────────────────── │  - RBAC Control Plane (FastAPI) │
│  - Local Safety Fallback        │     Protobuf Payloads     │  - gRPC High-Throughput Worker  │
└─────────────────────────────────┘                           └─────────────────────────────────┘
                 │                                                             │
                 ▼                                                             ▼
     [Unprivileged Local App]                                      [Protected Cloud Services]
     - Cannot eavesdrop other apps                                 - Anthropic / OpenAI / Deepgram
     - Cannot bypass SE-Linux sandbox                              - Hash-Chained Evidence Vault
```

### Threat Vectors & Mitigation Strategies

| Threat Vector | Severity | Attack Scenario | RakshaCall Architectural Mitigation |
| :--- | :--- | :--- | :--- |
| **Reverse Engineering APK** | High | Attacker decompiles APK to extract LLM/ASR API keys or spoof safety alerts. | **Zero Client Secrets.** All AI models and external API keys reside strictly in backend environment variables/Vault. Client receives signed JWTs for session authorization only. |
| **Man-in-the-Middle (MitM)** | Critical | Rogue Wi-Fi router intercepts live audio stream or alters risk scores to suppress safety brake. | **TLS 1.3 + gRPC Channel Encryption.** Optional gRPC Certificate Pinning ensures the client only streams frames to verified RakshaCall safety clusters. |
| **Tampered Evidence Logs** | High | Compromised device or bad actor edits scam transcript to frame or exonerate someone. | **Cryptographic SHA-256 Chaining.** Every event incorporates `previous_hash`, `timestamp`, `session_id`, and `payload_hash`. Modifying a single character invalidates the genesis chain. |
| **VoIP Eavesdropping Claim** | Critical | False claim that app silently taps WhatsApp/Telegram calls, violating privacy laws. | **Platform Honesty Boundary.** Android SE-Linux prevents third-party VoIP tapping. RakshaCall operates only via user-consented channels (Acoustic Speaker Capture, MediaProjection, or In-App WebRTC). |
| **Unauthorized Data Leaks** | High | Leaking raw user audio recordings to cloud storage or third-party training pipelines. | **Ephemeral In-Memory Buffers.** Audio frames are normalized to 16kHz mono PCM in RAM, processed by HuBERT/ASR, and immediately discarded. Raw audio is never persisted. |
| **Malicious Session Injection** | Medium | Attacker spoofs `session_id` to pollute another user's conversation state. | **Cryptographically Random UUIDv4 + Session Token.** Backend validates session ownership on every gRPC frame. |

---

## 3. Secret Management & Credential Isolation

### 3.1 Strict Separation of Concerns
```
+-----------------------------------------------------------------------+
| NO SECRETS IN CLIENT BINARIES                                         |
|                                                                       |
| ❌ NO Anthropic API Keys in assets/ or strings.xml                    |
| ❌ NO Google Cloud / HuggingFace tokens in BuildConfig                |
| ❌ NO Master Encryption Keys in SharedPreferences                     |
| ❌ NO Hardcoded backend passwords in Dart/Kotlin                      |
+-----------------------------------------------------------------------+
```

### 3.2 Backend Secrets Management
- **Environment Isolation**: Production credentials (`ANTHROPIC_API_KEY`, `DEEPGRAM_API_KEY`, `RAKSHA_VAULT_KEY`) are injected at container runtime via secure secrets managers (AWS Secrets Manager / GCP Secret Manager / HashiCorp Vault) or strictly controlled `.env` files with 600 file permissions.
- **Provider Redundancy**: If external cloud providers are unavailable or fail, the `JEVProviderFactory` seamlessly degrades to `LocalSemanticJEVProvider` without crashing or exposing raw stack traces to the network.

---

## 4. Client-Side Security: Android Keystore & Room Encryption

On Android devices, local safety policies, cached emergency contacts, and active session tokens must remain secure even if the device is rooted or malware attempts to inspect app storage.

### 4.1 Android Keystore MasterKey Implementation
RakshaCall utilizes the Android Jetpack Security library (`EncryptedSharedPreferences` and `EncryptedFile`) backed by the hardware **Trusted Execution Environment (TEE)** or **StrongBox Keymaster**:

```kotlin
// Android Client Architecture (Production Implementation Pattern)
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

fun getSecureStorage(context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setUserAuthenticationRequired(false) // Accessible during active protection service
        .build()

    return EncryptedSharedPreferences.create(
        context,
        "rakshacall_secure_vault",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
```

### 4.2 Local Room Database Protection
- Metadata, stage history, and emergency contact phone numbers are stored in a local SQLite Room database encrypted using **SQLCipher** with a key derived from the Keystore-backed MasterKey.
- If biometric or PIN lock is configured, exporting or viewing raw incident logs requires local biometric authentication (`BiometricPrompt`).

---

## 5. Network Security: gRPC over HTTP/2 & mTLS

### 5.1 Protocol Buffers Transport Security
All real-time communications use **gRPC over HTTP/2** with the following transport security guarantees:
- **ALPN Negotiation**: Enforces `h2` protocol negotiation over TLS 1.3.
- **Cipher Suites**: Restricted to forward-secret AEAD ciphers:
  - `TLS_AES_256_GCM_SHA384`
  - `TLS_CHACHA20_POLY1305_SHA256`
  - `TLS_AES_128_GCM_SHA256`
- **Certificate Pinning**: The client application can pin the safety cluster's leaf certificate public key (`SPKI` pin), mitigating compromised root Certificate Authorities (CAs).

### 5.2 Stream Authentication & Anti-Replay
1. **Initial Handshake**: The client establishes a gRPC stream by sending an initial metadata frame with a signed bearer JWT acquired during user sign-in.
2. **Sequence Numbering**: Every `ClientFrame` carries a monotonically increasing `sequence_number`. The backend drops frames with non-monotonic sequences to prevent replay attacks.
3. **Heartbeat / Keepalive**: HTTP/2 `PING` frames are transmitted every 30 seconds to detect half-open sockets and prevent silent disconnection during live scam calls.

---

## 6. Cryptographic Evidence Vault (SHA-256 Chaining)

The RakshaCall Evidence Vault preserves an append-only, tamper-evident audit record of detected scam tactics, risk spikes, and Safety Brake triggers.

### 6.1 Cryptographic Chain Specification
Each evidence event is cryptographically bound to its parent via a linked blockchain-style hash chain:

$$H_0 = \text{SHA-256}(\text{"GENESIS"} \parallel \text{session\_id})$$
$$H_i = \text{SHA-256}(H_{i-1} \parallel \text{event\_id} \parallel \text{timestamp} \parallel \text{event\_type} \parallel \text{canonical\_payload})$$

```
+------------------------------------+       +------------------------------------+
| BLOCK 0: GENESIS                   |       | BLOCK 1: TACTIC_DETECTED           |
| prev_hash: "0000000000000000..."   | ----> | prev_hash: H_0                     |
| event_type: "SESSION_START"        |       | event_type: "TACTIC_DETECTED"      |
| hash: H_0                          |       | hash: H_1 = SHA256(H_0 + payload1) |
+------------------------------------+       +------------------------------------+
                                                                |
                                                                v
+------------------------------------+       +------------------------------------+
| BLOCK 3: SAFETY_BRAKE_TRIGGERED    |       | BLOCK 2: STAGE_TRANSITION          |
| prev_hash: H_2                     | <---- | prev_hash: H_1                     |
| event_type: "SAFETY_BRAKE"         |       | event_type: "STAGE_TRANSITION"     |
| hash: H_3 = SHA256(H_2 + payload3) |       | hash: H_2 = SHA256(H_1 + payload2) |
+------------------------------------+       +------------------------------------+
```

### 6.2 Data Integrity vs. Legal Certification Distinction
> [!IMPORTANT]
> **Legal Admissibility Clarification:**
> The RakshaCall Evidence Vault provides **mathematical tamper-evidence** (guaranteeing that the stored timeline has not been altered or rearranged since creation). It **does not claim statutory forensic certification** under Section 65B of the Indian Evidence Act unless accompanied by an authorized custodian affidavit and chain-of-custody verification.

### 6.3 Local Verification Utility
The client application provides a user-facing **"VERIFY EVIDENCE INTEGRITY"** button that runs in $O(N)$ time on the client or backend, validating the entire hash sequence. Any modified byte breaks all subsequent hashes, immediately flagging tampering.

---

## 7. Data Retention & Privacy Lifecycle

### 7.1 Retention Policy by Data Tier

| Data Category | Storage Location | Retention Lifetime | Justification |
| :--- | :--- | :--- | :--- |
| **Raw Ingested Audio** | Client RAM only | **0 seconds (Ephemeral)** | Discarded immediately after 16kHz mono normalization and acoustic feature extraction. |
| **Raw Camera Frames** | Client RAM only | **0 seconds (Ephemeral)** | Discarded immediately after YOLO11 bounding box inference. |
| **Normalized Transcripts** | In-Memory Sliding Window | Session duration + 5 min | Required for multi-turn conversational context ($k=5$). Purged upon session close. |
| **Tactic & Risk Events** | Encrypted SQLite (Local) | 30 days (User configurable) | Allows the user to review scam incidents or share evidence with local law enforcement. |
| **Evidence Chain** | Evidence Vault (Backend/Local) | 90 days (Configurable) | Preserves cryptographic timeline for dispute resolution and family alerts. |
| **Trusted Contact Info** | Android Keystore Encrypted | Indefinite (User controlled) | Needed for one-tap emergency SOS and automated Safety Brake notifications. |

### 7.2 Right to Erasure ("One-Tap Purge")
The user can tap **"Purge Protection History"** in the settings menu at any time. This executes:
1. SQLCipher `DELETE FROM incidents WHERE 1;` followed by `VACUUM;`
2. Backend session cache invalidation.
3. Overwrite of memory buffers with zeroed byte arrays.

---

## 8. Role-Based Access Control (RBAC) & Multi-Tenancy

For administrative, law enforcement, or enterprise deployments, RakshaCall enforces strict RBAC across API endpoints:

```
                  ┌──────────────────────┐
                  │    Authenticated     │
                  │      Principal       │
                  └──────────┬───────────┘
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
   [Role: End Citizen]               [Role: Safety Admin / LEA]
   - Start/End own session           - Cannot view live unconsented audio
   - Stream own frames               - Can verify evidence chain hash
   - Read own incident vault         - Can query aggregated scam telemetry
   - Manage trusted contacts         - Read-only access to audit logs
```

---

## 9. Compliance & Regulatory Alignment

RakshaCall is designed to comply with international and Indian statutory data protection frameworks:
- **Digital Personal Data Protection Act (DPDPA 2023 - India)**:
  - *Notice and Consent*: Transparent notice displayed in Tamil, Hindi, and English before initiating protection.
  - *Purpose Limitation*: Audio and video are strictly utilized for real-time safety classification, never for profiling or behavioral ad targeting.
  - *Data Minimization*: Only extracted semantic features and tactic probabilities are logged; raw recordings are discarded.
- **Telecom Commercial Communications Customer Preference Regulations (TCCCPR)**:
  - Supports reporting fraudulent sender IDs, spoofed numbers, and malicious UPI handles directly to citizen safety databases (e.g., Chakshu portal / 1930 Cyber Helpline).

---

## 10. Security Audit & Verification Checklist

- [x] Zero API keys or secrets in Android source code or `strings.xml`.
- [x] Zero hardcoded cloud tokens in client bundles.
- [x] gRPC channel operates over TLS 1.3 with HTTP/2 transport.
- [x] Audio streaming requires explicit user consent flag (`consent_granted == true`).
- [x] In-memory audio buffers purged after feature extraction.
- [x] Android Keystore hardware-backed encryption used for local tokens.
- [x] SHA-256 evidence chain verified by automated regression test suite (`test_evidence_vault.py`).
- [x] Platform limitations honestly disclosed (no silent third-party VoIP eavesdropping).
