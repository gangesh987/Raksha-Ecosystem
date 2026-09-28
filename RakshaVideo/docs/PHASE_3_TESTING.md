# Phase 3 Physical Testing

Required acceptance tests:
- Phone A <-> Phone B real video/audio
- Protection OFF: call still works
- Protection ON: protection becomes ACTIVE/MONITORING
- Backend disconnect: call continues, protection unavailable
- Backend reconnect: protection reconnects
- Real backend risk event: warning appears
- End call: protection session ends and WebRTC resources release

This environment cannot perform physical-device testing. Do not report these tests as passed until performed.
