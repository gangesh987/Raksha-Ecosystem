# Intelligence Testing

## Backend checks

Run:

```bash
python -m py_compile server/app/main.py server/app/intelligence/models.py server/app/intelligence/conversation/signals.py server/app/intelligence/conversation/analyzer.py server/app/intelligence/visual/signals.py server/app/intelligence/visual/analyzer.py server/app/intelligence/fusion/analyzer.py
```

## Transcript integration example

With a valid protection session and `audio_analysis=true`, POST transcript text to the transcript endpoint. The response contains detected signals and a fused risk update.

## Visual integration example

With `visual_analysis=true`, POST frame metadata or a visual signal to the visual endpoint. The response contains the accepted visual signal and the current fused risk.

## Physical device acceptance

Use two Android devices, connect both to the same reachable backend, make a real call, enable protection, and verify the protection WebSocket receives risk events while the WebRTC call continues.
