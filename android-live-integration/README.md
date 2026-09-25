# Android live integration

Drop these classes into the existing Kotlin/Compose RakshaCall app. The app keeps Android microphone permission and foreground-service lifecycle local; the class below only transports consented transcript/audio events to the RakshaCall backend.

Endpoint: `wss://YOUR_HOST/api/ws/sessions/{sessionId}`

For Android, the safest first integration is the existing `SpeechRecognizer` path with `createOnDeviceSpeechRecognizer()` when available, then send only transcript text over the WebSocket. This avoids silently shipping raw audio to a server. A future AudioRecord/PCM path can use the same `audio_pcm16` message contract.
