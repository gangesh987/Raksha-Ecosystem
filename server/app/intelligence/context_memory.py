"""
RakshaCall Streaming Conversation Context & Session Memory.
Maintains short-lived, privacy-compliant per-call semantic memory.
Tracks:
- currentSegment & previousSegments (rolling window)
- conversationSummary
- detectedTactics history
- riskHistory, stageHistory, languageHistory
- partial vs final transcript boundary handling
"""

import time
from typing import Dict, List, Optional, Any, Set, Tuple
from dataclasses import dataclass, field

Tuple_Score = Tuple[int, int]   # (timestamp, risk_score)
Tuple_Stage = Tuple[int, str]   # (timestamp, stage_name)


@dataclass
class ConversationSegment:
    turn_id: int
    timestamp: int
    text: str
    is_final: bool
    language: str
    tactics: List[str]
    risk_score: int
    stage: str


@dataclass
class CallSessionState:
    session_id: str
    call_id: str
    created_at: int
    current_segment: Optional[ConversationSegment] = None
    previous_segments: List[ConversationSegment] = field(default_factory=list)
    cumulative_tactics: Set[str] = field(default_factory=set)
    tactic_counts: Dict[str, int] = field(default_factory=dict)
    risk_history: List[Tuple_Score] = field(default_factory=list)
    stage_history: List[Tuple_Stage] = field(default_factory=list)
    language_history: List[str] = field(default_factory=list)
    active_threat_topics: Set[str] = field(default_factory=set)
    is_ended: bool = False

    def get_recent_transcripts(self, n: int = 5) -> List[str]:
        """Returns the text of the last n finalized turns."""
        return [s.text for s in self.previous_segments[-n:]]

    def build_summary(self) -> str:
        """Constructs an explainable summary of the conversation trajectory."""
        if not self.cumulative_tactics:
            return "Call initialized. Neutral conversational exchanges."
        tactics_list = sorted(list(self.cumulative_tactics))
        return f"Coercive pattern observed across {len(self.previous_segments)} turns. Active vectors: {', '.join(tactics_list)}."




class ConversationMemoryManager:
    """
    Manages active session memories across concurrent WebRTC calls.
    Ensures zero permanent storage beyond the active call lifecycle.
    """

    def __init__(self, max_history_turns: int = 20):
        self.max_history_turns = max_history_turns
        self._sessions: Dict[str, CallSessionState] = {}

    def get_or_create_session(self, session_id: str, call_id: str = "") -> CallSessionState:
        """Retrieve existing session memory or initialize a fresh state."""
        if session_id not in self._sessions:
            self._sessions[session_id] = CallSessionState(
                session_id=session_id,
                call_id=call_id or f"CALL-{session_id}",
                created_at=int(time.time() * 1000)
            )
        return self._sessions[session_id]

    def record_turn(
        self,
        session_id: str,
        text: str,
        timestamp: int,
        is_final: bool,
        language: str,
        tactics: List[str],
        risk_score: int,
        stage: str
    ) -> CallSessionState:
        """
        Record a speech segment into the session's rolling memory.
        If is_final is True, commits segment to previous_segments and updates
        cumulative threat metrics.
        """
        session = self.get_or_create_session(session_id)
        turn_id = len(session.previous_segments) + 1

        seg = ConversationSegment(
            turn_id=turn_id,
            timestamp=timestamp,
            text=text,
            is_final=is_final,
            language=language,
            tactics=tactics,
            risk_score=risk_score,
            stage=stage
        )

        session.current_segment = seg

        if is_final:
            session.previous_segments.append(seg)
            if len(session.previous_segments) > self.max_history_turns:
                session.previous_segments.pop(0)

            # Update cumulative tactics
            for t in tactics:
                session.cumulative_tactics.add(t)
                session.tactic_counts[t] = session.tactic_counts.get(t, 0) + 1

            # Update history
            session.risk_history.append((timestamp, risk_score))
            session.stage_history.append((timestamp, stage))
            if language not in session.language_history:
                session.language_history.append(language)

            # Track thematic topics
            lower_text = text.lower()
            if any(w in lower_text for w in ["bank", "rbi", "account", "manager"]):
                session.active_threat_topics.add("BANK")
            if any(w in lower_text for w in ["police", "cbi", "crime branch", "arrest"]):
                session.active_threat_topics.add("AUTHORITY")
            if any(w in lower_text for w in ["otp", "code", "digits", "number"]):
                session.active_threat_topics.add("CREDENTIAL")
            if any(w in lower_text for w in ["transfer", "money", "funds", "pay"]):
                session.active_threat_topics.add("FINANCIAL")

        return session

    def clear_session(self, session_id: str) -> None:
        """Purge all ephemeral memory upon call termination (Privacy Rule)."""
        if session_id in self._sessions:
            self._sessions[session_id].is_ended = True
            del self._sessions[session_id]


# Global memory manager instance
session_memory_manager = ConversationMemoryManager()
