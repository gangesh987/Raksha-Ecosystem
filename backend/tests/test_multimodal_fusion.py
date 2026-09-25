"""
Test suite for Multimodal Risk Fusion Engine.
Verifies primary vs supporting signal weighting, Safety Brake trigger invariants,
and model disagreement resolution.
"""

import pytest

from app.ai.multimodal_fusion import MultimodalRiskFusionEngine
from app.ai.manipulation_velocity import VelocityCalculation
from app.ai.yolo_vision import VisualContextSignal


def test_safety_brake_trigger_invariant():
    """
    Safety Brake MUST engage when:
    Risk >= 60 AND Irreversible Action Tactic (Payment / OTP / Remote Access) is active.
    """
    fusion = MultimodalRiskFusionEngine()
    velocity = VelocityCalculation(
        velocity_score=30.0,
        level="HIGH",
        tactic_rate_per_min=3.5,
        stage_delta=3,
        explanation="Rapid escalation"
    )

    tactics = {
        "AUTHORITY": 0.85,
        "FEAR": 0.80,
        "ISOLATION": 0.75,
        "PAYMENT": 0.90
    }

    decision = fusion.fuse(
        tactic_probs=tactics,
        scam_stage="PAYMENT_CREDENTIAL",
        velocity=velocity,
        semantic_confidence=0.92
    )

    assert decision.risk_score >= 60
    assert decision.risk_level in ("HIGH", "CRITICAL")
    assert decision.is_irreversible_action is True
    assert decision.safety_brake_triggered is True
    assert len(decision.contributing_factors) > 0


def test_visual_perception_is_supporting_only():
    """
    YOLO11 visual signals must NEVER trigger high risk on benign speech alone.
    """
    fusion = MultimodalRiskFusionEngine()
    velocity = VelocityCalculation(
        velocity_score=0.0,
        level="LOW",
        tactic_rate_per_min=0.0,
        stage_delta=0,
        explanation="Benign"
    )

    # Benign conversation, but visual shows secondary phone and document
    visual = VisualContextSignal(
        person_count=2,
        secondary_phone_detected=True,
        screen_or_laptop_detected=True,
        document_detected=True,
        contextual_note="User holding phone and paper"
    )

    decision = fusion.fuse(
        tactic_probs={}, # Zero coercive speech tactics
        scam_stage="CONTACT",
        velocity=velocity,
        semantic_confidence=0.85,
        visual_signal=visual
    )

    # Without verbal coercion, risk must remain LOW
    assert decision.risk_score < 30
    assert decision.risk_level == "LOW"
    assert decision.safety_brake_triggered is False


def test_model_disagreement_resolution():
    """
    When speech is highly coercive but visual looks completely normal,
    model disagreement must be flagged and explain that speech remains primary.
    """
    fusion = MultimodalRiskFusionEngine()
    velocity = VelocityCalculation(
        velocity_score=28.0,
        level="HIGH",
        tactic_rate_per_min=3.0,
        stage_delta=2,
        explanation="Rapid escalation"
    )

    tactics = {
        "AUTHORITY": 0.90,
        "FEAR": 0.85,
        "CREDENTIAL": 0.95
    }

    # Visual stream appears completely calm / normal
    visual_normal = VisualContextSignal(
        person_count=1,
        secondary_phone_detected=False,
        screen_or_laptop_detected=False,
        document_detected=False,
        contextual_note="Single user, normal lighting"
    )

    decision = fusion.fuse(
        tactic_probs=tactics,
        scam_stage="PAYMENT_CREDENTIAL",
        velocity=velocity,
        semantic_confidence=0.94,
        visual_signal=visual_normal
    )

    assert decision.model_disagreement is True
    assert decision.disagreement_explanation is not None
    assert "safety invariant" in decision.disagreement_explanation.lower()
    assert decision.safety_brake_triggered is True
