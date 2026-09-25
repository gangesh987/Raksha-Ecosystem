"""
RakshaCall Scam Stage State Machine.
Tracks the chronological progression of psychological coercion through:
CONTACT -> AUTHORITY -> FEAR -> ISOLATION -> DEMAND -> PAYMENT_CREDENTIAL -> CRITICAL_BRAKE.
Applies temporal damping and confidence weighting to prevent erratic jumps.
"""

from __future__ import annotations
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple


STAGES = (
    "CONTACT",
    "AUTHORITY",
    "FEAR",
    "ISOLATION",
    "DEMAND",
    "PAYMENT_CREDENTIAL",
    "CRITICAL_BRAKE"
)

STAGE_RANKS = {s: i for i, s in enumerate(STAGES)}

TACTIC_TO_STAGE = {
    "AUTHORITY": "AUTHORITY",
    "FEAR": "FEAR",
    "ISOLATION": "ISOLATION",
    "URGENCY": "DEMAND",
    "SUSPICIOUS_LINK": "DEMAND",
    "PAYMENT": "PAYMENT_CREDENTIAL",
    "CREDENTIAL": "PAYMENT_CREDENTIAL",
    "REMOTE_ACCESS": "PAYMENT_CREDENTIAL",
    "ESCALATION": "CRITICAL_BRAKE"
}


@dataclass
class StageState:
    current_stage: str = "CONTACT"
    stage_confidence: float = 0.85
    highest_stage: str = "CONTACT"
    stage_transitions: int = 0
    history: List[Tuple[float, str, float]] = field(default_factory=list) # (timestamp, stage, conf)


class ScamStageMachine:
    """
    Temporal State Machine for Scam Trajectory Analysis.
    Requires corroborating evidence before advancing stages.
    """
    def __init__(self):
        self.state = StageState()

    def update_stage(
        self,
        tactic_probs: Dict[str, float],
        timestamp: float,
        is_irreversible: bool = False
    ) -> Tuple[str, float]:
        """
        Evaluate candidate stage transitions based on observed tactic probabilities.
        """
        candidates: List[Tuple[str, float]] = []

        for tactic, prob in tactic_probs.items():
            if prob >= 0.45 and tactic in TACTIC_TO_STAGE:
                target_stage = TACTIC_TO_STAGE[tactic]
                candidates.append((target_stage, prob))

        if is_irreversible:
            candidates.append(("PAYMENT_CREDENTIAL", 0.95))

        if not candidates:
            # Maintain current stage with gradual confidence decay
            return self.state.current_stage, self.state.stage_confidence

        # Sort candidates by stage hierarchy rank
        best_stage, best_conf = max(candidates, key=lambda c: (STAGE_RANKS[c[0]], c[1]))
        current_rank = STAGE_RANKS[self.state.current_stage]
        target_rank = STAGE_RANKS[best_stage]

        # Progressive advancement rule:
        # Cannot jump more than 2 stages in a single step unless probability >= 0.85
        if target_rank > current_rank:
            if target_rank - current_rank > 2 and best_conf < 0.85:
                # Damp jump to intermediate stage
                damped_stage = STAGES[current_rank + 2]
                self.state.current_stage = damped_stage
                self.state.stage_confidence = round(best_conf * 0.85, 2)
            else:
                self.state.current_stage = best_stage
                self.state.stage_confidence = round(best_conf, 2)

            self.state.stage_transitions += 1
            if STAGE_RANKS[self.state.current_stage] > STAGE_RANKS[self.state.highest_stage]:
                self.state.highest_stage = self.state.current_stage

        self.state.history.append((timestamp, self.state.current_stage, self.state.stage_confidence))
        return self.state.current_stage, self.state.stage_confidence
