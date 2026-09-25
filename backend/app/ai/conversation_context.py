"""
RakshaCall Multi-Turn Conversation Context Management.
Maintains session state, dialogue history, sliding conversation windows,
and chronological attack progression.
"""

from __future__ import annotations
import time
from collections import deque
from dataclasses import dataclass, field
from typing import List, Dict, Optional, Any


@dataclass
class ConversationTurn:
    """Individual turn in a protected conversation."""
    turn_id: int
    speaker: str           # "CALLER", "USER", "SYSTEM"
    text: str
    timestamp: float = field(default_factory=time.time)
    language: str = "en-IN"
    tactic_hits: List[str] = field(default_factory=list)
    confidence: float = 1.0


@dataclass
class SessionConversationContext:
    """Multi-turn conversation session state maintained across gRPC stream."""
    session_id: str
    start_time: float = field(default_factory=time.time)
    turns: deque = field(default_factory=lambda: deque(maxlen=40))
    cumulative_tactics: Dict[str, float] = field(default_factory=dict) # tactic -> peak probability
    highest_stage: str = "CONTACT"
    peak_risk_score: int = 0
    safety_brake_triggered: bool = False
    total_audio_duration_seconds: float = 0.0

    def add_turn(
        self,
        speaker: str,
        text: str,
        language: str = "en-IN",
        tactic_hits: Optional[List[str]] = None,
        confidence: float = 1.0
    ) -> ConversationTurn:
        turn = ConversationTurn(
            turn_id=len(self.turns) + 1,
            speaker=speaker,
            text=text.strip(),
            timestamp=time.time(),
            language=language,
            tactic_hits=tactic_hits or [],
            confidence=confidence
        )
        self.turns.append(turn)
        return turn

    def get_sliding_window(self, k: int = 5) -> List[str]:
        """Return the texts of the last k dialogue turns for contextual windowing."""
        recent = list(self.turns)[-k:]
        return [t.text for t in recent if t.text]

    def elapsed_seconds(self) -> float:
        return max(1.0, time.time() - self.start_time)

    def record_tactics(self, tactic_probs: Dict[str, float]) -> None:
        """Update cumulative peak probabilities for detected tactics."""
        for tactic, prob in tactic_probs.items():
            current_peak = self.cumulative_tactics.get(tactic, 0.0)
            if prob > current_peak:
                self.cumulative_tactics[tactic] = prob


class SessionRegistry:
    """Thread-safe session context storage for real-time streams."""
    _instances: Dict[str, SessionConversationContext] = {}

    @classmethod
    def get_or_create(cls, session_id: str) -> SessionConversationContext:
        if session_id not in cls._instances:
            cls._instances[session_id] = SessionConversationContext(session_id=session_id)
        return cls._instances[session_id]

    @classmethod
    def remove(cls, session_id: str) -> Optional[SessionConversationContext]:
        return cls._instances.pop(session_id, None)
