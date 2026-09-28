# RAKSHA ECOSYSTEM — JURY & LIVE DEMONSTRATION RUNBOOK
**Version:** 1.0.0 Final Release  
**Audience:** Competition Jury, Technical Evaluators, Live Demonstration Team  
**Estimated Demonstration Duration:** 8–10 Minutes  

---

## 1. System Setup & Pre-Flight Checklist

1. **Backend Service:** Ensure the backend is running and warmed up:
   ```bash
   curl -s -H "Bypass-Tunnel-Reminder: true" https://poor-keys-like.loca.lt/api/health
   ```
2. **Android Device:**
   - Both APKs installed (`RakshaVideo-debug.apk` and `RakshaCall-debug.apk`).
   - Confirm both launcher icons are visible on the home screen:
     - 📹 **Raksha Video** (Blue video icon)
     - 🛡️ **RakshaCall** (Gold/Teal shield icon)
   - Cellular data active (or connected to presentation Wi-Fi).

---

## 2. Demonstration Flow: Step-by-Step

### Phase 1: Two Independent Applications (Architecture Separation)
1. **Show Device Home Screen to Evaluators:**
   - Point out that **Raksha Video** and **RakshaCall** are two distinct, installable apps.
   - Explain the architectural boundary:
     - **Raksha Video:** Real-time peer-to-peer WebRTC video calling and user communication plane.
     - **RakshaCall:** Real-time fraud defense, manipulation velocity tracking, and Safety Brake interlock plane.

### Phase 2: Live Video Call (`Raksha Video`)
1. Open **Raksha Video**.
2. Tap **"Create Call"** (generates Call ID `RCV-XXXXXX`).
3. Have a second device or browser join the call using the Call ID.
4. Show active video/audio transmission over WebRTC with camera controls.
5. Tap **"Protection Active"** to observe live in-call safety status.

### Phase 3: Ambient Protection (`RakshaCall`)
1. Switch to **RakshaCall** (or trigger handoff via app deep-link).
2. Tap **"Start Protection"** (foreground service starts, microphone monitoring active).
3. The real-time safety HUD indicates `Risk: SAFE` (Green).

### Phase 4: Multi-Turn Scam Attack Progression
Simulate a "Digital Arrest" extortion sequence:
- **Turn 1 (Authority):**
  > *"Hello, this is Inspector Rajesh Kumar from the Central Cyber Crime Bureau."*
  - **HUD Updates:** Tactic identified as `Authority Impersonation`, Stage moves to `AUTHORITY`.
- **Turn 2 (Fear & Allegation):**
  > *"Your bank account has been used in a money laundering syndicate. An arrest warrant is issued."*
  - **HUD Updates:** Stage moves to `FEAR`, Velocity spikes to 0.45.
- **Turn 3 (Isolation):**
  > *"This is a confidential national security case. Do not inform your family or friends."*
  - **HUD Updates:** Stage moves to `ISOLATION`, Velocity reaches 0.72, Risk: **HIGH** (Orange).
- **Turn 4 (Demand & Payment):**
  > *"You must immediately transfer Rs 50,000 to the court verification account."*
  - **HUD Updates:** Stage moves to `DEMAND`.
- **Turn 5 (Credential / Safety Brake Trigger):**
  > *"Read out the OTP you received immediately to confirm payment."*
  - **CRITICAL SAFETY BRAKE ACTIVATES:**
    - Full-screen high-contrast visual interlock.
    - Localized voice prompt (*"STOP! DO NOT SHARE OTP OR SEND MONEY"*).
    - 7-Step Verification Coach guides the user to safety.
    - Trusted family contact SOS prepared.
    - SHA-256 cryptographic evidence block recorded.

### Phase 5: Negative Control (Zero False Positives)
Demonstrate resilience against false alarms using benign banking advice:
> *"Hello sir, calling from your bank branch. Never share your OTP or PIN with anyone. We refunded your charges."*
- **Result:**
  - Negation intent recognized.
  - Stage remains `CONTACT`, Risk remains `0 / SAFE`.
  - Safety Brake remains dormant.
