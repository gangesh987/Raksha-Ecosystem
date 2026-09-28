from .signals import normalize_visual_signal
from ..models import Signal

class VisualAnalyzer:
    def analyze(self, payload: dict, timestamp: int) -> list[Signal]:
        return normalize_visual_signal(payload, timestamp)
