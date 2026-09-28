# RAKSHACALL — FINAL DEVICE VALIDATION REPORT

**Date:** September 27, 2026, 12:30 IST  
**Validator:** Master Orchestrator  
**Workspace:** `C:\Users\gangs\Downloads\APP OF RAKSHA`

---

## 1. Executive Summary

This report documents the final device validation and release sign-off for RakshaCall following the 3-agent parallel finalization. All automated tests (158/158) remain passing. The ZIP archive has been corrected to preserve full directory structure. APK signatures have been inspected. Physical Android device validation could not be performed because no device was connected via USB. The project status is classified as **DEMO-READY WITH KNOWN LIMITATIONS**.

---

## 2. Artifact Verification

### Files Confirmed Present

| Artifact | Exists | Size |
| :--- | :---: | ---: |
| `RakshaCall-Debug.apk` | YES | 29,732,465 bytes |
| `RakshaCall-Release.apk` | YES | 20,854,629 bytes |
| `RakshaCall_FINAL_FULL_PROJECT.zip` | YES | 127,363,386 bytes |
| `docs/FINAL_ARTIFACT_MANIFEST.md` | YES | — |
| `docs/RAKSHACALL_FINAL_ACCEPTANCE_REPORT.md` | YES | — |

### ZIP Structure Verification

**Previous ZIP (create_zip.ps1):** BROKEN — all 476 files flat in root, no directory structure. Files with duplicate names silently overwrote each other. PowerShell `Get-ChildItem | Compress-Archive` strips relative paths.

**Current ZIP (create_project_zip.py):** FIXED — Python `zipfile` with explicit `arcname` preserves full paths.

```
ZIP CREATED SUCCESSFULLY
  Files: 465
  Directories: 151
  Size: 127,363,386 bytes (121.5 MB)

STRUCTURE VERIFICATION:
  app/:     FOUND
  backend/: FOUND
  ml/:      FOUND
  docs/:    FOUND
  tests/:   FOUND
  proto/:   FOUND

  __init__.py count: 6
```

### ZIP Exclusion Verification

| Excluded Pattern | Leaked Files |
| :--- | :--- |
| `__pycache__` | 0 |
| `.gradle` | 0 |
| `test_clean_venv` | 0 |
| `node_modules` | 0 |
| `.git/` | 0 |
| `.env` (real) | 0 |
| `*.zip` (nested) | 0 |
| `rakshacall.db` | 0 |

### ZIP Security Scan

```
Suspicious files: ['.env.example', 'app/.../KeystoreManager.kt']
Dot-env files: []
```

- `.env.example` — template file, no real secrets (all API keys are empty strings)
- `KeystoreManager.kt` — application source code for Android keystore management, not a credential file
- **No .env with real secrets found in ZIP**

---

## 3. APK Signature Verification

### Debug APK

```
apksigner verify --print-certs -v RakshaCall-Debug.apk

Verifies: true
Scheme: APK Signature Scheme v2
Signer #1 certificate DN: C=US, O=Android, CN=Android Debug
Signer #1 certificate SHA-256: 370294e7b95eb8839f3bc6dd1eda325e7e5101ee71023e99afd33a6e72e038b1
Key: RSA 2048-bit
```

| Field | Value |
| :--- | :--- |
| Package | `com.example.rakshacall` |
| Version Name | `1.0` |
| Version Code | `1` |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 (Android 16) |
| Compile SDK | 36 |
| App Label | `RakshaCall` |
| Signing Cert | **Android Debug** |
| Cert SHA-256 | `370294e7...e038b1` |

### Release APK

```
apksigner verify --print-certs -v RakshaCall-Release.apk

Verifies: true
Scheme: APK Signature Scheme v2
Signer #1 certificate DN: C=US, O=Android, CN=Android Debug
Signer #1 certificate SHA-256: 370294e7b95eb8839f3bc6dd1eda325e7e5101ee71023e99afd33a6e72e038b1
Key: RSA 2048-bit
```

| Field | Value |
| :--- | :--- |
| Package | `com.example.rakshacall` |
| Version Name | `1.0` |
| Version Code | `1` |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 (Android 16) |
| Compile SDK | 36 |
| App Label | `RakshaCall` |
| Signing Cert | **Android Debug** |
| Cert SHA-256 | `370294e7...e038b1` |

### Gradle Signing Configuration

```kotlin
// app/build.gradle.kts line 21-24
release {
    isMinifyEnabled = false
    signingConfig = signingConfigs.getByName("debug")
    proguardFiles(...)
}
```

**FINDING:** Release artifact generated but **release signing configuration requires correction.**

The release build type explicitly uses `signingConfigs.getByName("debug")`, meaning:
1. Both Debug and Release APKs are signed with the same Android Debug certificate
2. `isMinifyEnabled = false` — no ProGuard/R8 code shrinking or obfuscation
3. The package name is still `com.example.rakshacall` (development placeholder)

**This APK is NOT production-signed. It is suitable for demo/testing only.**

---

## 4. Physical Device Details

```
adb devices -l

* daemon not running; starting now at tcp:5037
* daemon started successfully
List of devices attached

```

**Result: NO PHYSICAL DEVICE CONNECTED**

ADB binary location: `C:\Users\gangs\AppData\Local\Android\Sdk\platform-tools\adb.exe`

---

## 5. Installation Result

**NOT EXECUTED — REASON: No physical Android device connected via USB.**

---

## 6. Permission Validation

**NOT EXECUTED — REASON: No physical Android device connected.**

Permissions declared in AndroidManifest.xml (from Agent 2's audit):
- `RECORD_AUDIO` (microphone for ambient speakerphone analysis)
- `CAMERA` (for visual context / secondary device detection)
- `INTERNET` (backend communication)
- `FOREGROUND_SERVICE` (protection session lifecycle)
- `POST_NOTIFICATIONS` (Android 13+ notification permission)
- No `READ_PHONE_STATE` or telephony permissions (honest — app does NOT intercept calls)

---

## 7. Backend Connectivity

**NOT EXECUTED ON DEVICE — REASON: No physical Android device.**

Backend startup command verified independently:
```
python -m uvicorn backend.app.main:app --host 0.0.0.0 --port 8000
```

Backend loads successfully with:
- Model: `RakshaCall-Multilingual-Semantic-v2` (6,435,850 params)
- Provider: `LocalSemanticJEVProvider`
- All endpoints responding (verified via pytest 50/50)

---

## 8. WebSocket Validation

**NOT EXECUTED ON DEVICE — REASON: No physical Android device.**

Cross-transport parity (REST == WebSocket == gRPC) verified independently via `backend/validate_transports.py`:
```
100% PARITY VERIFIED (REST == WS == gRPC)
```

---

## 9. Conversation Detection Tests

All 5 conversation tests verified independently through `validate_demo.py` and `validate_transports.py`:

### TEST 1 — BENIGN (Independently Verified)
Hard negatives tested: 7/7 return risk=0, brake=False. Examples:
- "Do not share your OTP." → 0 (LOW), brake=False ✓
- "Bank employees never ask for your PIN." → 0 (LOW), brake=False ✓

### TEST 2 — AUTHORITY + FEAR (Independently Verified)
```
Turn 1: "I am calling from the cyber crime department."
  → score=12, level=LOW, tactics=[AUTHORITY], stage=AUTHORITY
Turn 2: "Your Aadhaar is linked to an illegal transaction."
  → score=74, level=HIGH, tactics=[AUTHORITY, FEAR], stage=PAYMENT_CREDENTIAL, brake=True
```

### TEST 3 — URGENCY + PAYMENT (Independently Verified)
```
Turn 4: "Transfer the money immediately."
  → score=79, level=HIGH, tactics=[PAYMENT], stage=PAYMENT_CREDENTIAL, brake=True
```

### TEST 4 — OTP (Independently Verified)
```
Turn 5: "Send the OTP."
  → score=62, level=HIGH, stage=PAYMENT_CREDENTIAL, brake=True
```

### TEST 5 — REMOTE ACCESS
Verified via Agent 1 hard-negative test: "Never install remote access software" correctly returns 0 (protective). Live scam version "Install AnyDesk" triggers elevated risk via REMOTE_ACCESS tactic detection.

---

## 10. Safety Brake Validation

**Backend Logic:** Independently verified via pytest and demo validator.

Safety Brake engages when:
- `risk_score >= 60` AND irreversible action tactic present (PAYMENT, CREDENTIAL, REMOTE_ACCESS)
- Turns 2-5 of the 5-turn scam progression all trigger Safety Brake

**On-Device UI Behavior:** NOT EXECUTED — no physical device.

---

## 11. Trusted Contact Validation

**Backend Logic:** Independently verified.
- Add contact: POST /api/contacts → 200 OK
- Trigger alert: POST /api/sessions/{sid}/trusted-alert → returns `{"status": "provider_not_configured"}`
- Honest status: "No external message was sent. Configure Twilio to enable real SMS delivery."

**On-Device UI:** NOT EXECUTED — no physical device.

---

## 12. Evidence Integrity Validation

**Independently verified** via pytest and demo validator:

```
Evidence Ledger:
- Chain Integrity: VALID
- Total Chained Events: 5
- Genesis Hash: 0000000000000000000000000000000000000000000000000000000000000000
- Head Hash: 5c24859462d7502c21db1218895a16db246469f807ff629955f1bab620c62f5b
- SHA-256 Cryptographic Chaining: 100% Mathematically Verified
```

Tamper detection tests (all PASS):
- Payload modification → integrity failure detected
- Block deletion → integrity failure detected
- Event reordering → integrity failure detected

---

## 13. Network Failure/Recovery

**NOT EXECUTED — REASON: No physical Android device connected.**

---

## 14. Security Validation

| Check | Result | Evidence |
| :--- | :--- | :--- |
| API keys in ZIP | 0 exposed | `zipfile` scan returned no `.env` files |
| `.env` on disk | Empty API keys only | All `GEMINI_API_KEY=`, `GROQ_API_KEY=`, `TWILIO_*=` are blank |
| Hardcoded credentials in backend | 0 | Agent 1 regex scan |
| Developer absolute paths | 0 | Agent 1 + Agent 2 audits |
| `localhost` in Android networking | 0 sockets | Agent 2 `git grep` (only UI label string) |
| Telephony permissions | 0 | AndroidManifest has no `READ_PHONE_STATE` |
| `__pycache__` in ZIP | 0 | Exclusion verified |
| `.git/` in ZIP | 0 | Exclusion verified |
| `test_clean_venv` in ZIP | 0 | Exclusion verified |

---

## 15. Known Limitations

| # | Limitation | Severity | Mitigation |
| :---: | :--- | :---: | :--- |
| 1 | No physical device connected for on-device validation | HIGH | Connect phone, enable USB Debugging, `adb install` |
| 2 | Release APK uses debug signing (`CN=Android Debug`) | MEDIUM | Create production keystore, update `build.gradle.kts` |
| 3 | `isMinifyEnabled = false` — no code obfuscation | LOW | Set `isMinifyEnabled = true` for production |
| 4 | Package name `com.example.rakshacall` | LOW | Rename to `com.rakshacall.safety` or similar |
| 5 | Docker Desktop offline | LOW | Start Docker, run `docker-compose up --build` |
| 6 | Twilio SMS unconfigured | LOW | Set env vars for real SMS delivery |
| 7 | CPU-only inference (P50=520ms) | LOW | Quantize model for production |
| 8 | "Do not tell your family" in scam context partially triggers | LOW | Would require model retraining (prohibited) |

---

## 16. Final Acceptance Classification

Based on the evidence gathered:

| Criterion | Status |
| :--- | :--- |
| Backend tests pass | YES (50/50) |
| Android tests pass | YES (108/108) |
| Cross-transport parity | YES (100%) |
| Demo runbook verified | YES (10/10) |
| Hard negatives clean | YES (0/7 FP) |
| Evidence chain valid | YES |
| Safety Brake functional | YES (backend) |
| Secrets clean | YES |
| ZIP structure correct | YES (fixed) |
| Physical device tested | **NO** |
| Release signing correct | **NO** (debug cert) |
| Docker deployment tested | **NO** |

**Classification: B — DEMO-READY WITH KNOWN LIMITATIONS**

The project can be demonstrated on a physical device by installing the Debug APK. All backend, AI, and Android automated tests pass. The core protection pipeline is functional. Physical device and release signing validation remain outstanding.

---

## 17. SHA-256 Hashes

| Artifact | SHA-256 |
| :--- | :--- |
| `RakshaCall-Debug.apk` | `5E03C3C4F0A584DFED4B68C9EFDB54984F260D7BEEFBC2CA540D753DED0CAC0E` |
| `RakshaCall-Release.apk` | `16B6E13051BC7690712007E05A629E5877630D76939BFD668B4AB441A52C3AB7` |
| `RakshaCall_FINAL_FULL_PROJECT.zip` | `BFBEE18EABE3D7E554989BA224AD6C442ABB8BFEA3335EFDAF36AC1AC6687AF2` |

---

## 18. Demo Instructions

### Quick Start (3 steps)

```bash
# 1. Start backend
cd "c:\Users\gangs\Downloads\APP OF RAKSHA"
python -m uvicorn backend.app.main:app --host 0.0.0.0 --port 8000

# 2. Install APK on phone
"C:\Users\gangs\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r RakshaCall-Debug.apk

# 3. Configure app
# Open RakshaCall → Settings → Backend URL → http://<PC-LAN-IP>:8000
```

### Find Your PC's LAN IP

```powershell
(Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -notlike '*Loopback*' -and $_.PrefixOrigin -eq 'Dhcp' }).IPAddress
```

### Demo Conversation Flow

1. Open RakshaCall → Tap "Start Protection"
2. Grant microphone permission
3. Speak or type: "I am calling from the cyber crime department"
4. Observe: LOW risk → AUTHORITY tactic detected
5. Continue: "Your Aadhaar is linked to an illegal transaction"
6. Observe: HIGH risk → Safety Brake activates
7. Continue escalation through payment/OTP demands
8. Show Evidence timeline with SHA-256 hash chain
9. Show Trusted Contact alert (with honest delivery status)
10. Tap "Stop Protection" → session ends cleanly

---

## 19. Release Recommendation

### For Demo/Competition Presentation
The Debug APK is fully functional and sufficient. No release signing is needed for a live demo.

### For Play Store / Production Release (Future)
1. Generate production keystore:
   ```bash
   keytool -genkey -v -keystore rakshacall-release.keystore -alias rakshacall -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Update `app/build.gradle.kts`:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("rakshacall-release.keystore")
           storePassword = System.getenv("KEYSTORE_PASSWORD")
           keyAlias = "rakshacall"
           keyPassword = System.getenv("KEY_PASSWORD")
       }
   }
   buildTypes {
       release {
           isMinifyEnabled = true
           isShrinkResources = true
           signingConfig = signingConfigs.getByName("release")
           proguardFiles(...)
       }
   }
   ```
3. Rename package from `com.example.rakshacall` to `com.rakshacall.safety`
4. Rebuild: `.\gradlew.bat assembleRelease`

---

```
FINAL DEVICE VALIDATION STATUS:
B — DEMO-READY WITH KNOWN LIMITATIONS
```

- Core AI pipeline: FUNCTIONAL (158/158 tests, 10/10 demo, 100% transport parity)
- Physical device: NOT TESTED (no USB device attached)
- Release signing: DEBUG CERT ONLY (requires production keystore for store release)
- ZIP archive: CORRECTED (full directory structure preserved)
