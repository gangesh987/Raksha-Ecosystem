from ..models import Signal

def normalize_visual_signal(payload: dict, timestamp: int) -> list[Signal]:
    signal_type = str(payload.get("signal_type", payload.get("type", "VISUAL_FRAME")))
    confidence = float(payload.get("confidence", 0.0) or 0.0)
    return [Signal(type=signal_type, confidence=max(0.0, min(confidence, 1.0)), timestamp=timestamp,
                   source=str(payload.get("source", "camera")), evidence_ref=payload.get("evidence_ref"),
                   metadata={k: v for k, v in payload.items() if k not in {"signal_type", "type", "confidence", "source", "evidence_ref"}})]
