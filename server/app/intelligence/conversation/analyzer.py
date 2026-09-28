from collections import deque
from .signals import detect_signals
from ..models import Signal

class ConversationAnalyzer:
    def __init__(self) -> None:
        self.recent: deque[Signal] = deque(maxlen=64)

    def analyze(self, text: str, timestamp: int, source: str = "transcript") -> list[Signal]:
        signals = detect_signals(text, timestamp, source)
        self.recent.extend(signals)
        return signals

    def stage(self) -> str:
        types = {s.type for s in self.recent}
        if "CREDENTIAL_REQUEST" in types or "OTP_REQUEST" in types:
            return "CREDENTIAL_REQUEST"
        if "FINANCIAL_REQUEST" in types:
            return "FINANCIAL_REQUEST"
        if "ISOLATION" in types:
            return "ISOLATION"
        if "THREAT" in types or "FEAR_LANGUAGE" in types:
            return "FEAR"
        if "URGENCY" in types:
            return "URGENCY"
        if "AUTHORITY_CLAIM" in types or "IMPERSONATION_SIGNAL" in types:
            return "AUTHORITY"
        return "NEUTRAL"
