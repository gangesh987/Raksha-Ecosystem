# Android Sentinel — implementation target

A real Android deployment should use:
- explicit microphone permission
- explicit camera permission
- Android screen capture/MediaProjection consent when screen capture is needed
- foreground service with visible notification while protection is active
- on-device ASR where possible
- local NLP/risk fusion for low latency
- optional server-side enrichment only with consent
- visible STOP PROTECTION control
- no root, no accessibility abuse, no silent call interception

The app should expose connector status and capture provenance in the UI.
