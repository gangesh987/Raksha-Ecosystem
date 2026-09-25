"""
Test suite for Scam Stage Machine and Manipulation Velocity Engine.
Verifies temporal progression, damping hysteresis, and velocity acceleration under attacks.
"""

import time
import pytest

from app.ai.stage_machine import ScamStageMachine, STAGES, STAGE_RANKS
from app.ai.manipulation_velocity import ManipulationVelocityEngine


def test_scam_stage_progression_and_damping():
    """Verify stage transitions advance through the hierarchy with damping."""
    sm = ScamStageMachine()
    assert sm.state.current_stage == "CONTACT"

    now = time.time()

    # Authority tactic should advance to AUTHORITY
    stage, conf = sm.update_stage({"AUTHORITY": 0.85}, now)
    assert stage == "AUTHORITY"
    assert sm.state.highest_stage == "AUTHORITY"

    # Fear tactic should advance to FEAR
    stage, conf = sm.update_stage({"FEAR": 0.80}, now + 5)
    assert stage == "FEAR"

    # Isolation tactic should advance to ISOLATION
    stage, conf = sm.update_stage({"ISOLATION": 0.75}, now + 10)
    assert stage == "ISOLATION"

    # Payment tactic with irreversible flag must advance to PAYMENT_CREDENTIAL
    stage, conf = sm.update_stage({"PAYMENT": 0.90}, now + 15, is_irreversible=True)
    assert stage == "PAYMENT_CREDENTIAL"


def test_stage_damping_prevents_wild_jumps():
    """An isolated weak signal cannot jump from CONTACT straight to CRITICAL_BRAKE."""
    sm = ScamStageMachine()
    now = time.time()

    # Single moderate escalation signal
    stage, conf = sm.update_stage({"ESCALATION": 0.55}, now)
    # Must be damped, not allowed to jump all 6 ranks to CRITICAL_BRAKE on 0.55 probability
    assert stage != "CRITICAL_BRAKE"
    assert STAGE_RANKS[stage] < STAGE_RANKS["CRITICAL_BRAKE"]


def test_manipulation_velocity_acceleration():
    """
    Rapid multi-tactic barrage within 20s must produce HIGH velocity,
    whereas same tactics spread over long periods produce LOW velocity.
    """
    engine_rapid = ManipulationVelocityEngine(window_seconds=90.0)
    t0 = 1000.0

    # Rapid cascade: Authority -> Fear -> Urgency -> Payment within 25 seconds
    engine_rapid.record_event(t0, {"AUTHORITY": 0.9}, current_stage_rank=1)
    engine_rapid.record_event(t0 + 5, {"FEAR": 0.85}, current_stage_rank=2)
    engine_rapid.record_event(t0 + 15, {"URGENCY": 0.80}, current_stage_rank=3)
    rapid_res = engine_rapid.record_event(t0 + 25, {"PAYMENT": 0.95}, current_stage_rank=5)

    assert rapid_res.level == "HIGH"
    assert rapid_res.velocity_score >= 25.0
    assert rapid_res.stage_delta >= 3

    # Slow, deliberate conversation: same tactics spread across 30 minutes
    engine_slow = ManipulationVelocityEngine(window_seconds=90.0)
    engine_slow.record_event(t0, {"AUTHORITY": 0.9}, current_stage_rank=1)
    slow_res = engine_slow.record_event(t0 + 80, {"FEAR": 0.85}, current_stage_rank=2)
    assert slow_res.level in ("LOW", "MODERATE")
    assert slow_res.velocity_score < rapid_res.velocity_score
