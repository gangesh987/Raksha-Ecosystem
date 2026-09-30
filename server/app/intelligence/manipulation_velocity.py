"""
RakshaCall Manipulation Velocity Engine.
Calculates psychological coercion acceleration and tactic escalation speed
over dynamic sliding windows.
Features:
- threats per minute
- urgent commands per minute
- payment / credential requests per minute
- repeated coercive demands
- tactic density & rapid stage transitions
Exposes:
- manipulationVelocityScore (0.0 to 1.0)
- velocityConfidence (0.0 to 1.0)
"""

import time
from typing import List, Dict, Tuple, Any
from dataclasses import dataclass
from .taxonomy import CRITICAL_TACTICS, HIGH_TACTICS, MEDIUM_TACTICS


@dataclass
class VelocityMetric:
    velocity_score: float        # 0.0 to 1.0
    velocity_level: str          # LOW, MODERATE, HIGH, EXTREME
    velocity_confidence: float   # 0.0 to 1.0
    events_per_minute: float
    threats_per_minute: float
    coercion_acceleration: float
    description: str


class ManipulationVelocityCalculator:
    """
    Computes time-domain derivative of psychological coercion:
    V = d(Tactics) / dt, weighted by tactic severity and window density.
    """

    def __init__(self, window_seconds: float = 120.0):
        self.window_seconds = window_seconds

    def calculate(
        self,
        event_history: List[Tuple[int, List[str]]],
        stage_transitions: int = 0
    ) -> VelocityMetric:
        """
        Evaluate recent turns within sliding window.
        event_history is a list of (timestamp_ms, [tactic_types]).
        """
        if not event_history or len(event_history) < 2:
            return VelocityMetric(
                velocity_score=0.0,
                velocity_level="LOW",
                velocity_confidence=0.50,
                events_per_minute=0.0,
                threats_per_minute=0.0,
                coercion_acceleration=0.0,
                description="Insufficient history for velocity measurement."
            )

        now_ms = event_history[-1][0]
        window_ms = self.window_seconds * 1000.0
        earliest_ms = max(event_history[0][0], now_ms - window_ms)
        elapsed_seconds = max((now_ms - earliest_ms) / 1000.0, 5.0)

        # Filter events inside the sliding window
        window_events = [ev for ev in event_history if ev[0] >= earliest_ms]
        if not window_events:
            window_events = event_history[-2:]

        threat_count = 0
        urgency_count = 0
        payment_cred_count = 0
        total_tactics = 0

        for ts, tactics in window_events:
            for t in tactics:
                total_tactics += 1
                if t in CRITICAL_TACTICS:
                    payment_cred_count += 1
                    threat_count += 1
                elif t in HIGH_TACTICS:
                    threat_count += 1
                elif t in MEDIUM_TACTICS:
                    if "URGENCY" in t or "PRESSURE" in t:
                        urgency_count += 1

        minutes = elapsed_seconds / 60.0
        events_per_min = len(window_events) / minutes
        threats_per_min = threat_count / minutes
        urgent_per_min = urgency_count / minutes
        cred_per_min = payment_cred_count / minutes

        # Core escalation formula
        # Baseline density + weighted threat intensity + rapid stage acceleration
        raw_velocity = (
            (threats_per_min * 0.25) +
            (cred_per_min * 0.40) +
            (urgent_per_min * 0.15) +
            (min(stage_transitions, 5) * 0.10)
        )

        # Normalize score into [0.0, 1.0]
        velocity_score = min(max(raw_velocity / 4.0, 0.0), 1.0)

        # Confidence correlates with window depth and sample size
        confidence = min(0.60 + 0.08 * len(window_events), 0.98)

        # Categorize velocity level
        if velocity_score >= 0.75:
            level = "EXTREME"
            desc = "Critical coercion spike: rapid demands for funds or credentials within minutes."
        elif velocity_score >= 0.50:
            level = "HIGH"
            desc = "Accelerating manipulation: active escalation across authority, threats, and urgency."
        elif velocity_score >= 0.25:
            level = "MODERATE"
            desc = "Moderate conversational pace with emerging pressure elements."
        else:
            level = "LOW"
            desc = "Normal pacing with no rapid coercion acceleration."

        return VelocityMetric(
            velocity_score=round(velocity_score, 2),
            velocity_level=level,
            velocity_confidence=round(confidence, 2),
            events_per_minute=round(events_per_min, 1),
            threats_per_minute=round(threats_per_min, 1),
            coercion_acceleration=round(raw_velocity, 2),
            description=desc
        )


# Global velocity calculator
velocity_engine = ManipulationVelocityCalculator()
