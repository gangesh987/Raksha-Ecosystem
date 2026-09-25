# Android Platform Security Boundaries & Permitted Input Flows

## 1. Operating System Sandboxing & Call Interception Restrictions

Modern Android operating systems (API 29+) enforce strict application sandboxing and permission isolation:
1. **Third-Party Encrypted Calls**: Android strictly prohibits third-party applications from silently intercepting, recording, or tapping audio from encrypted VoIP calls (such as WhatsApp, Telegram, Signal, or Skype).
2. **Cellular Call Audio**: Direct recording of the remote cellular party's audio stream is blocked by the Android telecom subsystem for non-system dialers.
3. **Hidden / Private APIs**: Bypassing these restrictions requires root access, device compromise, or private system APIs—none of which are viable or legal for a consumer security product.

## 2. Platform Honesty Commitment

**RakshaCall NEVER claims to secretly tap or silently intercept encrypted third-party calls.**

Instead, RakshaCall operates through legitimate, transparent, and permitted on-device channels:

1. **Permitted Microphone Capture**:
   - Active during an explicit user-initiated protection session.
   - Leverages Android `SpeechRecognizer` to transcribe audio spoken aloud or played through device speakerphone.
   - Accompanied by a persistent Foreground Service notification (`FOREGROUND_SERVICE_MICROPHONE`).
2. **Interactive Live Input Lab**:
   - A dedicated verification environment allowing users, presenters, and security evaluators to speak live phrases or test coercive patterns dynamically.
   - Proves engine responsiveness in real time without simulating fake call intercepts.
3. **Screen / Audio Capture Intent Flow**:
   - Uses standard Android `MediaProjection` APIs requiring explicit, interactive user consent before any frame or audio stream is processed.
