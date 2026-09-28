# RAKSHACALL — FINAL ARTIFACT MANIFEST

**Generated:** 2026-09-27T18:27:00+05:30  
**Verified by:** Master Orchestrator (Independent SHA-256 Computation)  
**Includes:** Public Cloud Base URL (`https://poor-keys-like.loca.lt`), WebSocket Real-Time Stream (`wss://poor-keys-like.loca.lt/api/ws/sessions/{sid}`), LAN IP Fallback (`http://172.17.35.95:8000`), Emulator Fallback (`http://10.0.2.2:8000`), Dual Authentication (📱 Phone Number on-device OTP & ✉️ Email/Password), hardware-backed Keystore AES-256-GCM token persistence, Google Credential Manager integration, and JVM-safe telemetry logging.

---

## Build Artifacts

| Artifact | Path | Size (bytes) | SHA-256 Hash |
| :--- | :--- | ---: | :--- |
| **Debug APK** | `app/build/outputs/apk/debug/app-debug.apk` | 34,182,664 | `0501C1A3099C8BC3FD0167FF1103613B70A6322BF3C6109FC1A1E483F599638E` |
| **Release APK** | `app/build/outputs/apk/release/app-release.apk` | 20,983,029 | `EA2A666907AB27859F1621CF31011E18549720AB795AD36DFCAC4F7B14061754` |
| **Root Debug APK** | `RakshaCall-Debug.apk` | 34,182,664 | `0501C1A3099C8BC3FD0167FF1103613B70A6322BF3C6109FC1A1E483F599638E` |
| **Root Release APK** | `RakshaCall-Release.apk` | 20,983,029 | `EA2A666907AB27859F1621CF31011E18549720AB795AD36DFCAC4F7B14061754` |
| **Full Project ZIP** | `RakshaCall_FINAL_FULL_PROJECT.zip` | 127,647,269 | `24BC7BE2554AE684B5397EA6CAE57CE58DBB2F7E9C7FFDC6B558B01A7ED2AA26` |

## Integrity & Quality Checklist

- **Public Cloud Base URL:** Configured to `https://poor-keys-like.loca.lt` with zero `localhost`/`127.0.0.1` hardcoded across production networking.
- **LAN / Emulator Presets:** Instantly switchable via Settings and Onboarding UI buttons.
- **Dual Authentication Modes:** Phone Number (100% offline, on-device OTP generation and verification, Room DB persistence) + Email/Password (REST API with customizable server host).
- **ZIP Structure:** 479 files, 153 directories, all package hierarchies (`app/`, `backend/`, `ml/`, `docs/`, `proto/`, `tests/`) verified intact.
- **Android Unit Tests:** 119/119 PASSED across 19 suites (`AuthenticationPersistenceTest.kt` included).
- **Backend Tests:** 51/51 PASSED (`test_google_auth_endpoint` included).
- **Total Automated Tests:** 170/170 PASSED.
- **Keystore Security:** JWT access token encrypted via AES-256-GCM hardware Keystore (`KeystoreManager.kt`).
- **Telemetry:** Instrumented with JVM-safe `RakshaLogger` logging `T0_START_PROTECTION`, `T1_WS_CONNECTED`, `T2_INPUT_SENT`, `T4_RISK_RECEIVED`, `T5_UI_UPDATED`.
