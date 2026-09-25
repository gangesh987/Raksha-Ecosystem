# RakshaCall Safety Intervention Flow
**Document Version:** 1.0.0  
**Scope:** Safety Brake Activation, Verification Coach Workflow, and Escalation Guardrails  

---

## 1. Mermaid Safety Intervention Sequence

```mermaid
stateDiagram-v2
    [*] --> MonitoringState: Consented Session Active

    state MonitoringState {
        direction TB
        IngestMedia --> ExtractFeatures
        ExtractFeatures --> EvaluateTactics
        EvaluateTactics --> UpdateStageAndVelocity
        UpdateStageAndVelocity --> CalculateRiskScore
    }

    CalculateRiskScore --> MonitoringState: Risk < 80 OR No Irreversible Action
    CalculateRiskScore --> SafetyBrakeTriggered: Risk >= 80 AND Irreversible Action Detected

    state SafetyBrakeTriggered {
        direction TB
        
        state "PHASE 1: PAUSE (Aural & Visual Interlock)" as Phase1 {
            direction LR
            VisualLock: Full-Screen Red Modal\n(High Contrast, Large Typography)
            AuralCue: Tamil/English Voice Alert\n("STOP! PANAM ANUPPATHINGA!")
            HapticFeedback: Triple Emergency Pulse
        }

        state "PHASE 2: VERIFY (Verification Coach)" as Phase2 {
            direction TB
            Step1: "1. Take a breath and pause."
            Step2: "2. Disconnect the call immediately."
            Step3: "3. Do not dial numbers provided by the caller."
            Step4: "4. Search for the institution's official helpline independently."
            Step5: "5. Confirm whether an actual investigation exists."
            Step6: "6. Consult your family or trusted contact."
            Step7: "7. Resume financial actions only after verification."
            Step1 --> Step2 --> Step3 --> Step4 --> Step5 --> Step6 --> Step7
        }

        state "PHASE 3: ESCALATE & PRESERVE" as Phase3 {
            direction LR
            AlertFamily: One-Tap Family SOS Dispatch\n(Status: ALERT_REQUESTED)
            HashVault: Append SHA-256 Tamper-Evident Evidence Block
        }

        Phase1 --> Phase2
        Phase2 --> Phase3
    }

    SafetyBrakeTriggered --> CitizenDecides: User Reviews Guidance

    state CitizenDecides <<choice>>
    CitizenDecides --> SessionSafelyEnded: User Disconnects & Verifies
    CitizenDecides --> SafeOverride: User Explicitly Overrides After Cooldown
```

---

## 2. Low-Literacy Voice Prompt Matrix

For citizens in rural and semi-urban communities where complex written warnings are ineffective, the Safety Brake dispatches natural, culturally familiar voice prompts:

| Irreversible Action Detected | Tamil Voice Prompt (Audio + Text) | English Voice Prompt (Audio + Text) | UI Display Banner |
| :--- | :--- | :--- | :--- |
| **Payment / Bank Transfer Demand** | *"STOP! PANAM ANUPPATHINGA! Idhu oru scam aaga irukkalaam. Call-a cut pannunga!"* | *"STOP! DO NOT SEND MONEY! This may be a scam. Hang up immediately!"* | 🛑 **STOP PAYMENT** |
| **OTP / PIN / Password Request** | *"STOP! OTP YAARUKKUM SOLLAATHINGA! Bank eppovum OTP kekka maattanga!"* | *"STOP! NEVER SHARE YOUR OTP! Banks will never ask for your OTP!"* | 🔐 **PROTECT OTP** |
| **Remote Access App (AnyDesk/TeamViewer)** | *"STOP! INTHA APP-A DOWNLOAD PANNAATHINGA! Ungal phone control poividum!"* | *"STOP! DO NOT INSTALL THIS APP! It will take control of your device!"* | 📱 **BLOCK APP INSTALL** |
| **Arrest / Police Legal Coercion** | *"BAYAPPADAATHINGA! Police phone-la panam kekka maattanga. Family-a call pannunga!"* | *"DO NOT PANIC! Police never demand money over phone. Call your family!"* | ⚖️ **VERIFY POLICE CLAIM** |

---

## 3. Trusted Contact Dispatch Honesty Matrix

To maintain strict truth-in-advertising and prevent false security assumptions:

```
[ SAFETY BRAKE TRIGGERS SOS DISPATCH ]
                   │
                   ▼
       Is SMS / WhatsApp Permission
          Explicitly Granted by OS?
         /                         \
       YES                         NO
        │                           │
        ▼                           ▼
[ Open System SMS Intent ]   [ Prompt User to Call Contact ]
        │                           │
        ▼                           ▼
UI displays:                 UI displays:
"ALERT_REQUESTED"            "ALERT_REQUESTED"
(Never displays "SENT"       (Guides user to dial
 until carrier delivers)      primary caregiver)
```

---

## 4. 7-Step Verification Coach Protocol

When the citizen taps **"WHAT SHOULD I DO?"**, the Verification Coach displays concise, actionable guidance:

1. **Pause**: Stop taking immediate action. Scammers rely on artificial adrenaline and urgency.
2. **Disconnect**: Hang up the call. Legitimate authorities will never arrest you for disconnecting a phone call.
3. **Reject Provided Contacts**: Do not call back on numbers or links sent via WhatsApp, SMS, or caller ID.
4. **Locate Official Helplines**: Look up official numbers from your passbook, credit card back, or verified state portal.
5. **Verify the Claim**: Contact the official institution and ask if any warrant or hold exists on your account.
6. **Consult Trusted Contacts**: Speak with a spouse, child, parent, or trusted neighbor before transferring any funds.
7. **Resume Safely**: Complete transactions only after independent verification has confirmed legitimacy.
