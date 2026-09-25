# RakshaCall Security Architecture

## 1. Cryptographic Model

### A. SHA-256 Tamper-Evident Evidence Vault
Each event in an active protection session is hashed into a sequential, append-only cryptographic ledger:

$$\text{currentHash} = \text{SHA-256}(\text{previousHash} \parallel \text{eventId} \parallel \text{timestamp} \parallel \text{eventType} \parallel \text{sessionId} \parallel \text{payloadJson})$$

- **Genesis Block**: Root event anchored to $\text{previousHash} = 0000000000000000000000000000000000000000000000000000000000000000$.
- **Chain Verification**: The `VerifyEvidenceIntegrityUseCase` recalculates the hash chain sequentially from the genesis block. If any single byte of an event payload or timestamp is modified, verification terminates with `IntegrityResult.Failed` identifying the exact compromised event.

### B. Hardware-Backed Encryption at Rest
- Sensitive local records and auth tokens are encrypted using **AES-256-GCM** with keys generated and secured inside the **Android Keystore** (`KeystoreManager.kt`).
- Keys are hardware-isolated from the application runtime and non-exportable.

## 2. PII Protection & Logging Discipline
- **SecurityLogger**: Strictly filters and suppresses transcripts, phone numbers, OTPs, and private credentials from system logcat.
- Session IDs are truncated to non-identifying 8-character hashes in debug traces.
- Verbose logging is disabled by default in release builds.

## 3. Network Security & Token Lifecycle
- **HTTPS Only**: All remote communications require TLS 1.3 with certificate validation.
- **Short-Lived Tokens**: Auth tokens expire periodically and require silent token refresh or user re-authentication.
- **Client Risk Zero-Trust**: When cloud risk intelligence is enabled, server-side microservices re-evaluate event structure independently and never accept client-asserted risk scores without verification.
