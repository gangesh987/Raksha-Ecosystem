# RakshaCall Deterministic Risk Engine V2

## 1. The 9 Coercive Scam Tactics

| # | Scam Tactic | Base Weight | Irreversible Action? | Example Detection Triggers |
| :---: | :--- | :---: | :---: | :--- |
| **1** | **Authority Impersonation** | +15 | No | CBI, Police, Cyber Cell, Supreme Court, TRAI, Customs, ED, Inspector |
| **2** | **Criminal Allegation / Fear** | +15 | No | Contraband found, drugs in parcel, Aadhaar illegal misuse, money laundering, FIR, arrest warrant |
| **3** | **Urgency** | +10 | No | "Immediately", "within 15 minutes", "right now", "do not delay", "final warning" |
| **4** | **Isolation** | +15 | No | "Do not hang up", "stay in closed room alone", "keep camera active", "do not tell family", "digital custody" |
| **5** | **Payment Demand** | +20 | **YES** | Transfer ₹50,000, security deposit, escrow verification account, penalty clearance |
| **6** | **Credential / OTP Pressure** | +20 | **YES** | "Share your OTP", UPI PIN, netbanking password, card CVV, Aadhaar OTP |
| **7** | **Remote Access Pressure** | +15 | **YES** | "Download AnyDesk", TeamViewer, RustDesk, QuickSupport, screen sharing |
| **8** | **Suspicious Links** | +10 | No | External verification link, APK download file |
| **9** | **Coercive Escalation** | +10 | No | "Sending police patrol team to your home", asset freezing, non-bailable arrest |

## 2. Mathematical Scoring Model

The risk engine computes a raw cumulative score from detected signals $S = \{s_1, s_2, \dots, s_n\}$:

$$\text{RawScore} = \sum_{i=1}^n w(s_i) \cdot \delta(s_i)$$

Where:
- $w(s_i)$ is the tactic weight.
- $\delta(s_i)$ is duplicate dampening ($0.5$ if the same tactic repeated within 45 seconds).

### Escalation Multiplier:
- If distinct tactics count $k \ge 4$, apply multiplier $M = 1.30$.
- If distinct tactics count $k \ge 3$, apply multiplier $M = 1.15$.
- Else $M = 1.0$.

$$\text{AdjustedScore} = \min(100, \text{RawScore} \times M)$$

### Temporal Decay:
Over extended call durations where no new tactics emerge, the score decays gradually toward the established stage baseline, preventing stale anxiety while retaining memory of critical allegations.

## 3. Scam Stage Progression (7 Monotonic Stages)

```
CONTACT (0) ➔ AUTHORITY (1) ➔ FEAR (2) ➔ ISOLATION (3) ➔ DEMAND (4) ➔ PAYMENT_CREDENTIAL (5) ➔ ESCALATION (6)
```

Transitions are strictly forward-progressing; once a scammer has claimed statutory police authority and alleged criminal money laundering, the session context retains that classification.
