"""
RakshaCall Manipulation Velocity Engine.
Measures the temporal acceleration of psychological coercion:
Number of tactics + Severity weighting + Stage transition delta / Elapsed time.
"""

from __future__ import annotations
import math
import time
from collections import deque
from dataclasses import dataclass, field
from typing import List, Tuple, Dict


TACTIC_SEVERITY = {
    "AUTHORITY": 15,
    "FEAR": 15,
    "URGENCY": 10,
    "ISOLATION": 15,
    "PAYMENT": 25,
    "CREDENTIAL": 25,
    "REMOTE_ACCESS": 20,
    "SUSPICIOUS_LINK": 10,
    "ESCALATION": 20
}


@dataclass
class VelocityCalculation:
    velocity_score: float  # Points per minute
    level: str             # "LOW", "MODERATE", "HIGH"
    tactic_rate_per_min: float
    stage_delta: int
    explanation: str


class ManipulationVelocityEngine:
    """
    Real-time sliding window temporal velocity engine.
    Detects rapid escalation cascades characteristic of digital arrest scams.
    """
    def __init__(self, window_seconds: float = 90.0):
        self.window_seconds = window_seconds
        # Queue of (timestamp, tactic_id, severity, stage_rank)
        self.events: deque = deque()

    def record_event(
        self,
        timestamp: float,
        tactic_probs: Dict[str, float],
        current_stage_rank: int
    ) -> VelocityCalculation:
        """
        Record newly detected tactics and compute instant manipulation velocity.
        """
        # Append active tactics
        for tactic, prob in tactic_probs.items():
            if prob >= 0.40:
                severity = TACTIC_SEVERITY.get(tactic, 10) * prob
                self.events.append((timestamp, tactic, severity, current_stage_rank))

        # Prune events outside sliding window
        cutoff = timestamp - self.window_seconds
        while self.events and self.events[0][0] < cutoff:
            self.events.popleft()

        if len(self.events) < 2:
            return VelocityCalculation(
                velocity_score=0.0,
                level="LOW",
                tactic_rate_per_min=0.0,
                stage_delta=0,
                explanation="Coercive signals are sparse or spaced over benign intervals."
            )

        # Elapsed time in window (clamped between 10s and window_seconds)
        dt_seconds = max(10.0, min(self.window_seconds, timestamp - self.events[0][0]))
        dt_minutes = dt_seconds / 60.0

        # Weighted severity sum with exponential decay (half-life = 45s)
        weighted_severity = 0.0
        for ev_time, _, sev, _ in self.events:
            age = max(0.0, timestamp - ev_time)
            decay = math.pow(0.5, age / 45.0)
            weighted_severity += sev * decay

        # Stage transition delta in window
        initial_stage = self.events[0][3]
        final_stage = self.events[-1][3]
        stage_delta = max(0, final_stage - initial_stage)

        # Velocity formulation: (weighted severity + stage jump penalty) / time
        stage_penalty = stage_delta * 12.0
        raw_velocity = (weighted_severity + stage_penalty) / dt_minutes
        velocity_score = round(raw_velocity, 2)

        tactic_rate = round(len(self.events) / dt_minutes, 1)

        if velocity_score >= 25.0:
            level = "HIGH"
            explanation = (
                f"HIGH VELOCITY ({velocity_score} pts/min): Rapid multi-tactic escalation "
                f"({tactic_rate} tactics/min, stage jump +{stage_delta}) within {int(dt_seconds)}s."
            )
        elif velocity_score >= 10.0:
            level = "MODERATE"
            explanation = (
                f"MODERATE VELOCITY ({velocity_score} pts/min): Sustained pressure "
                f"accumulating across dialogue turns ({tactic_rate} tactics/min)."
            )
        else:
            level = "LOW"
            explanation = f"LOW VELOCITY ({velocity_score} pts/min): Normal deliberate dialogue cadence."

        return VelocityCalculation(
            velocity_score=velocity_score,
            level=level,
            tactic_rate_per_min=tactic_rate,
            stage_delta=stage_delta,
            explanation=explanation
        )
