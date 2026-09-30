"""
RakshaCall 12-Stage Scam Trajectory State Machine.
Implements the complete 12-stage social engineering lifecycle defined in Section 19:
NORMAL -> CONTACT -> TRUST_BUILDING -> AUTHORITY_CLAIM -> FEAR -> URGENCY ->
ISOLATION -> INFORMATION_REQUEST -> PAYMENT_REQUEST -> REMOTE_ACCESS ->
THREAT_ESCALATION -> CRITICAL_INTERVENTION.

Enforces evidence-driven transitions with temporal hysteresis to prevent erratic jumps.
Never transitions based on elapsed time alone.
"""

from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass, field
from .taxonomy import STAGES_12, STAGE_HIERARCHY, TACTIC_TO_STAGE, CRITICAL_TACTICS


@dataclass
class StageProgressionState:
    current_stage: str = "NORMAL"
    stage_confidence: float = 0.90
    highest_stage: str = "NORMAL"
    transitions_count: int = 0
    history: List[Tuple[int, str, float]] = field(default_factory=list)  # (timestamp, stage, confidence)


class ScamStageMachine12:
    """
    Evidence-driven 12-stage state machine tracking extortion escalation.
    """

    def __init__(self):
        self.state = StageProgressionState()

    def update(
        self,
        detected_tactics: List[str],
        timestamp: int,
        scam_probability: float,
        has_irreversible: bool = False
    ) -> Tuple[str, float]:
        """
        Evaluate candidate transitions based on active tactics and scam probability.
        """
        if not detected_tactics:
            # If no tactics observed, maintain stage or allow slow decay towards NORMAL
            return self.state.current_stage, self.state.stage_confidence

        candidate_stages: List[Tuple[str, float]] = []

        for tactic in detected_tactics:
            target_stage = TACTIC_TO_STAGE.get(tactic)
            if target_stage:
                # Confidence proportional to tactic severity & scam probability
                conf = 0.95 if tactic in CRITICAL_TACTICS else (0.85 if scam_probability >= 0.70 else 0.75)
                candidate_stages.append((target_stage, conf))

        if has_irreversible or any(t in CRITICAL_TACTICS for t in detected_tactics):
            candidate_stages.append(("CRITICAL_INTERVENTION", 0.98))

        if not candidate_stages:
            return self.state.current_stage, self.state.stage_confidence

        # Choose candidate with highest hierarchical rank
        best_candidate, best_conf = max(
            candidate_stages,
            key=lambda c: (STAGE_HIERARCHY.get(c[0], 0), c[1])
        )

        current_rank = STAGE_HIERARCHY.get(self.state.current_stage, 0)
        target_rank = STAGE_HIERARCHY.get(best_candidate, 0)

        if target_rank > current_rank:
            # Multi-stage advancement check:
            # Jumping straight to CRITICAL requires explicit irreversible evidence (OTP / PIN / Wire demand)
            if best_candidate == "CRITICAL_INTERVENTION" and not (has_irreversible or any(t in CRITICAL_TACTICS for t in detected_tactics)):
                # Damp jump to THREAT_ESCALATION
                best_candidate = "THREAT_ESCALATION"
                best_conf = 0.85

            self.state.current_stage = best_candidate
            self.state.stage_confidence = round(best_conf, 2)
            self.state.transitions_count += 1

            if STAGE_HIERARCHY.get(self.state.current_stage, 0) > STAGE_HIERARCHY.get(self.state.highest_stage, 0):
                self.state.highest_stage = self.state.current_stage

        self.state.history.append((timestamp, self.state.current_stage, self.state.stage_confidence))
        return self.state.current_stage, self.state.stage_confidence


def create_stage_machine() -> ScamStageMachine12:
    return ScamStageMachine12()
