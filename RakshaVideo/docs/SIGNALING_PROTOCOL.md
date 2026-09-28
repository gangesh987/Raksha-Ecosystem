# Signaling Protocol

All messages use:

```json
{
  "version": 1,
  "type": "...",
  "request_id": "uuid",
  "timestamp": 0,
  "payload": {}
}
```

Supported messages: `join_call`, `leave_call`, `participant_joined`, `participant_left`, `offer`, `answer`, `ice_candidate`, `mute_changed`, `camera_changed`, `call_ended`, `ping`, `pong`, `error`.

The signaling server does not carry RTP media. It only relays negotiation/control messages.
