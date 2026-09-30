"""
RakshaCall Conversation Analyzer.
Bridges low-level speech transcripts into the Real-Time Multilingual
Scam Intelligence Pipeline, generating structured Signal objects
and tracking stage progression.
"""

from collections import deque
from typing import List, Dict, Any
from ..models import Signal
from ..pipeline import realtime_pipeline
from ..taxonomy import TACTIC_TO_STAGE


class ConversationAnalyzer:
    def __init__(self, session_id: str = "default") -> None:
        self.session_id = session_id
        self.recent: deque[Signal] = deque(maxlen=64)
        self._last_result: Dict[str, Any] = {}

    def analyze(self, text: str, timestamp: int, source: str = "transcript") -> List[Signal]:
        """
        Execute deep semantic intent pass via the real-time multilingual pipeline.
        Transforms detected tactics into structured Signal events.
        """
        res = realtime_pipeline.analyze(
            transcript=text,
            session_id=self.session_id,
            timestamp=timestamp,
            is_final=True,
            source=source
        )
        self._last_result = res

        signals: List[Signal] = []
        for t in res.get("tactics", []):
            sig = Signal(
                type=t["type"],
                confidence=float(t["confidence"]),
                timestamp=timestamp,
                source=source,
                evidence_ref=f"transcript:{timestamp}",
                metadata={
                    "evidence": t.get("evidence", ""),
                    "language": res.get("language", "en"),
                    "is_code_switched": res.get("isCodeSwitched", False),
                    "slang": res.get("slangTokens", []),
                    "stage": res.get("stage", "NORMAL"),
                    "manipulation_velocity": res.get("manipulationVelocity", 0.0)
                }
            )
            signals.append(sig)
            self.recent.append(sig)

        return signals

    def stage(self) -> str:
        """Returns the current 12-stage state."""
        return self._last_result.get("stage", "NORMAL")

    def get_last_result(self) -> Dict[str, Any]:
        """Returns the full Section 26 contract result from the last turn."""
        return self._last_result
