# Risk Event Schema

```json
{
  "version": 1,
  "type": "risk_update",
  "event_id": "uuid",
  "session_id": "protect_x",
  "call_id": "RCV-xxxxxx",
  "timestamp": 0,
  "payload": {
    "level": "HIGH",
    "score": 82,
    "reasons": ["urgency", "authority_pressure"]
  }
}
```

`score` is optional. The client never invents a score.
