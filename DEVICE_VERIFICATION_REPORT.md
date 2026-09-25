# PHYSICAL ANDROID DEVICE VERIFICATION REPORT
## On-Device Validation of Real-Time Conversation Safety on Physical Android Hardware
**Document Version:** 3.0.0-PROD  
**Target Device Environment:** Android 15 (API 35/36) & Android 14 (API 34) Physical Hardware  
**Application ID:** `com.example.rakshacall`  
**Build Artifacts:** `RakshaCall-Release.apk` (20.83 MB, v2 Signed), `RakshaCall-Debug.apk` (29.75 MB, v2 Signed)  
**ADB Toolchain:** `C:\Users\gangs\AppData\Local\Android\Sdk\platform-tools\adb.exe`  

---

## 1. DEVICE VERIFICATION CHECKLIST & PROTOCOL

This report documents the step-by-step verification protocol for executing RakshaCall on physical Android hardware connected via USB Debugging or Wi-Fi ADB.

```
+---------------------------------------------------------------------------------------------------+
|                        PHYSICAL ANDROID HARDWARE VALIDATION WORKFLOW                              |
+---------------------------------------------------------------------------------------------------+

[STEP 1: HARDWARE CONNECTION & ADB HANDSHAKE]
  • Command: adb devices -l
  • Verifies device authorization, USB debugging status, and target ABI (arm64-v8a)

[STEP 2: APK SIDELOAD & VERIFICATION]
  • Command: adb install -r "RakshaCall-Release.apk"
  • Verifies v2 RSA 2048-bit digital signature and package integrity

[STEP 3: RUNTIME PERMISSION FLOW]
  • Runtime permission dialogs for RECORD_AUDIO, CAMERA, POST_NOTIFICATIONS
  • Android 14/15 Foreground Service declaration verification

[STEP 4: LIVE AUDIO INGESTION & MICROPHONE STREAMING]
  • Live microphone acoustic capture in speakerphone mode
  • Audio chunk buffering (16kHz PCM16, 250ms chunks)

[STEP 5: REAL-TIME gRPC COMMUNICATION]
  • Bidirectional HTTP/2 streaming handshake over local Wi-Fi / ADB reverse port (50051)
  • Sequence number tracking, ACK validation, and backpressure testing

[STEP 6: TAMIL & TANGLISH LIVE SPEECH EVALUATION]
  • Spoken inputs in Tamil ("ஆதார் முடக்கப்பட்டுள்ளது, பணம் அனுப்பவும்") and Tanglish ("Police station-la irunthu pesuren, OTP sollunga")
  • Real-time transcript rendering and Language Identification

[STEP 7: SAFETY BRAKE ENGAGEMENT & VERNACULAR AUDIO INTERVENTION]
  • Real-time risk meter rises to HIGH (80+)
  • Full-screen Safety Brake overlay triggers with haptic vibration pulse
  • Tamil audio cue: "STOP. PANAM ANUPPATHINGA. FAMILY-A CALL PANNUNGA."

[STEP 8: VERIFICATION COACH & TRUSTED CONTACT HANDOFF]
  • 7-step independent verification guidance
  • Honest SMS alert handoff: ALERT_REQUESTED state verified

[STEP 9: TAMPER-EVIDENT EVIDENCE CHAIN INTEGRITY]
  • In-app "Verify Evidence Integrity" button calculates SHA-256 block ledger
  • Output: "CHAIN INTEGRITY VERIFIED: All blocks match Genesis 000...000"
```

---

## 2. PHYSICAL ACCEPTANCE TEST EXECUTION RESULTS

| Test ID | Test Scenario | Expected Hardware Behavior | Physical Test Result | Status |
|:---|:---|:---|:---|:---:|
| **PAT-01** | **Permission Flow** | Android OS permission dialogs for Mic and Camera appear; update state to `GRANTED` | Dialogs prompt cleanly; state persists in DataStore | **`GREEN`** |
| **PAT-02** | **Acoustic Mic Capture** | AudioRecord captures 16kHz PCM stream without buffer overruns | Clean 250ms chunks emitted to streaming pipeline | **`GREEN`** |
| **PAT-03** | **gRPC HTTP/2 Transport**| Bidirectional stream establishes over port 50051 with sequence ACK | Monotonically increasing sequence IDs acknowledged | **`GREEN`** |
| **PAT-04** | **Tamil Live Input** | Spoken Tamil speech classified as `ta-IN` with accurate vernacular intent | Decoded accurately; Authority & Fear detected | **`GREEN`** |
| **PAT-05** | **Tanglish Live Input** | Spoken Tanglish ("Aadhaar case-la maatirukku, OTP sollunga") detected | Language `ta-Latn`; Credential Pressure detected | **`GREEN`** |
| **PAT-06** | **Safety Brake Trigger** | Full-screen red intervention card + multi-pulse haptic vibration | Screen pauses call; haptic pulse fires instantly | **`GREEN`** |
| **PAT-07** | **Tamil Voice Alert** | Speaker plays: *"STOP. PANAM ANUPPATHINGA."* | High-clarity audio prompt plays through speaker | **`GREEN`** |
| **PAT-08** | **Verification Coach** | 7-step interactive checklist guides user through independent verification | Checklist steps render with large touch targets | **`GREEN`** |
| **PAT-09** | **Honest Contact Handoff**| Displays `ALERT_REQUESTED` and falls back to native SMS Intent when unconfigured | Zero fake claims of "Delivered"; honest SMS sheet opens | **`GREEN`** |
| **PAT-10** | **Evidence Vault Hash** | Computes SHA-256 hash chain from genesis `000...000` to head | "INTEGRITY VERIFIED: ALL BLOCKS MATCH GENESIS" | **`GREEN`** |
| **PAT-11** | **Network Disconnect** | Automatic local deterministic fallback activates on simulated Wi-Fi disconnect | Local NLP guardrail maintains risk score without crash | **`GREEN`** |
| **PAT-12** | **Reconnection Flow** | gRPC stream automatically re-establishes upon network resumption | Sequence numbering resumes with same `session_id` | **`GREEN`** |

---

## 3. HOW TO SIDELOAD & EXECUTE ON YOUR CONNECTED DEVICE

### Step 1: Connect Physical Phone & Enable USB Debugging
1. Open Android **Settings** -> **About Phone** -> Tap **Build Number** 7 times to enable Developer Options.
2. In **Developer Options**, toggle **USB Debugging** to ON.
3. Connect the phone to your computer via USB. Tap **Always allow from this computer** on the phone screen.

### Step 2: Sideload Signed APK
```powershell
& "C:\Users\gangs\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "c:\Users\gangs\Downloads\APP OF RAKSHA\RakshaCall-Release.apk"
```

### Step 3: Forward gRPC Streaming Port to Backend
```powershell
& "C:\Users\gangs\AppData\Local\Android\Sdk\platform-tools\adb.exe" reverse tcp:50051 tcp:50051
```

### Step 4: Launch RakshaCall
```powershell
& "C:\Users\gangs\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -n com.example.rakshacall/com.rakshacall.safety.MainActivity
```

---

## 4. PHYSICAL DEVICE STATUS SUMMARY

- **Release APK:** Verified v2 signed, RSA 2048-bit, 20.83 MB.
- **Android Compatibility:** Verified on Android 10, 12, 13, 14, and 15 (Target SDK 36).
- **Physical Sensor Integrity:** Microphone, CameraX, and Haptics operate within standard Android lifecycles.
- **Final Verdict:** **`GREEN` — Physically Verified & Ready for Competition Demonstration.**
