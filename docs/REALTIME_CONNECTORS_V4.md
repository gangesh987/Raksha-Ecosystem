# RakshaCall Sentinel V4 — Real-Time Call Intelligence

## Important platform boundary

RakshaCall must not silently intercept encrypted WhatsApp traffic or bypass application security.

The supported architecture is **user-consented media capture**:

### WhatsApp / WhatsApp Web / Desktop
`User starts protection → explicit OS/browser capture permission → audio/video capture adapter → frame/audio pipeline → on-device ASR/CV → risk fusion`

This observes the media that the user explicitly shares with RakshaCall. It does not decrypt WhatsApp transport traffic.

### Google Meet
Google Meet has an official Meet Media API that can provide real-time media to an authorized third-party media application. The participant/meeting consent and Google Workspace permissions are required. RakshaCall should use this connector where eligible.

## Connector states

Each connector reports:
- unavailable
- permission_required
- connecting
- connected
- degraded
- stopped
- error

The UI must always display the active capture source and permission state.

## Real-time pipeline

Audio/video chunks
→ jitter buffer
→ VAD
→ streaming ASR
→ phrase/tactic detector
→ entity extractor
→ speaker-turn tracker
→ visual consistency/liveness supporting signals
→ signal quality estimator
→ adaptive risk fusion
→ intervention policy
→ warning / trusted contact / evidence

## Novel safety features

### 1. Scam Stage Machine
Tracks progression:
`CONTACT → AUTHORITY → FEAR → ISOLATION → DEMAND → PAYMENT/CREDENTIAL → ESCALATION`

### 2. Manipulation Velocity
Measures how quickly coercive tactics accumulate. A rapid escalation can raise urgency even before the absolute score becomes very high.

### 3. Safety Brake
When HIGH/CRITICAL risk is detected, the product can present:
- end call
- verify independently
- call trusted contact
- lock/hold evidence capture
- show anti-scam checklist

### 4. Independent Verification Coach
Instead of simply saying "scam", RakshaCall asks the user to verify through an independently obtained official number/site.

### 5. Evidence Integrity Graph
Events are chained with hashes and grouped by incident. Export includes a manifest and model/provider status.

### 6. Privacy Budget
A configurable retention budget controls raw audio/video retention. Derived risk events can be retained longer than raw media.

### 7. Model Disagreement Guard
If ASR, NLP, visual and liveness models disagree strongly, the system lowers confidence and explains the uncertainty rather than manufacturing certainty.

### 8. Attack Replay Lab
A synthetic replay mode allows developers to test scam scenarios without capturing real private calls.

## Platform roadmap

- Android: foreground service + explicit microphone/camera/screen capture permissions.
- Desktop: explicit OS screen/audio capture.
- Chrome/Meet: browser extension or Meet media connector where permitted.
- WhatsApp Web: browser/tab capture only with explicit user permission.
- WhatsApp desktop: OS-level user-consented capture, not protocol interception.

## Production rule

Do not label a connector "LIVE" unless its provider/session is actually connected. Demo connectors must be visibly marked SIMULATED.
