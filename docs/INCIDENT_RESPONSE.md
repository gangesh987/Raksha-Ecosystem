# RakshaCall Incident Response Playbook

## 1. User Incident Protocol (The 5-Step Protocol)

When RakshaCall activates the **Safety Brake** or the user recognizes a coercive scam call:

```
┌─────────────────────────────────────────────────────────────┐
│ 1. DISCONNECT IMMEDIATELY                                   │
│    Hang up the voice or video call. Law enforcement will    │
│    never conduct official interrogations over video calls.  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. DO NOT COMPLY WITH RE-DIALS                              │
│    Block incoming calls from the caller number.             │
│    Ignore threats of immediate police arrival.              │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. DIAL 1930 & REPORT ONLINE                                │
│    Call the National Cyber Crime Helpline (1930) or report  │
│    via https://cybercrime.gov.in.                           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. CONSULT TRUSTED FAMILY / CONTACTS                        │
│    Break the psychological isolation immediately by speaking│
│    in person or on a known family contact number.           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. EXPORT VERIFIED EVIDENCE REPORT                          │
│    Open RakshaCall Evidence Vault, tap 'Share Incident',    │
│    and hand the cryptographically verified log to police.   │
└─────────────────────────────────────────────────────────────┘
```

## 2. Technical Incident Handling
If an evidence chain reports `IntegrityResult.Failed`:
1. The incident event ID is logged internally via `SecurityLogger`.
2. The UI explicitly alerts the user that the file has been modified since generation.
3. The report retains all pre-compromise and post-compromise records clearly marked for digital forensics investigators.
