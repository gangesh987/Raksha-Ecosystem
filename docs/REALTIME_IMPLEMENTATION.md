# Real-time implementation

## Pipeline

`User consent → microphone/camera → WebSocket → Gemini Live + deterministic guardrail → explainable risk fusion → Safety Brake → trusted contact/evidence`

## Gemini Live

The backend uses `google-genai` and a stateful Live API WebSocket. Audio is raw 16-bit PCM at 16 kHz. Camera frames are JPEG and sent at no more than approximately one frame per second.

## Guardrail

Gemini is not the sole decision-maker. The deterministic classifier provides an independent conversation signal and catches explicit high-risk patterns. AI output is merged into the conversation signal; visual/liveness remain secondary.

## No-mock policy

No seeded users, fake risk events, scripted scenario buttons, or simulated delivery states are included in the final console.
