# Privacy Data Flow

Default design:

`WebRTC media -> call peer`

`Transcript/signals -> consent-gated intelligence endpoint -> risk event`

`Video -> real frame sampler -> metadata signal -> intelligence endpoint`

Raw audio/video/frame persistence is not implemented. Protection uses explicit consent flags for conversation analysis, visual analysis, risk detection, and trusted-contact alerts.
