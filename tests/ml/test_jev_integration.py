"""
Integration tests for Hybrid JEV Semantic Intent Engine.
Verifies real neural inference + rule safety floor fusion:
1. semantic model called
2. rule floor called
3. fusion called
4. output schema
5. negation
6. multi-turn context
7. Safety Brake compatibility
"""

import pytest
import os
import sys

from app.ai.jev_provider import LocalSemanticJEVProvider, JEVAnalysisResult
from app.ai.stage_machine import ScamStageMachine, STAGE_RANKS
from app.ai.manipulation_velocity import ManipulationVelocityEngine, VelocityCalculation
from app.ai.multimodal_fusion import MultimodalRiskFusionEngine
from app.ai.safety_brake import safety_brake_engine, LocalizedAudioAlert


@pytest.fixture(scope="module")
def jev():
    provider = LocalSemanticJEVProvider()
    assert provider.neural_classifier is not None
    assert provider.neural_classifier.is_loaded is True
    return provider


def test_1_semantic_model_called(jev):
    """Test 1: Verify the real PyTorch model was called and populated neural telemetry."""
    utterance = "Your Aadhaar card was linked to illegal money laundering in Mumbai."
    res = jev.analyze_window(utterance, conversation_context=[], detected_language="en")
    assert isinstance(res, JEVAnalysisResult)
    assert res.semantic_model is not None
    assert "model_version" in res.semantic_model
    assert "scam_probability" in res.semantic_model
    assert res.semantic_model["scam_probability"] > 0.0


def test_2_rule_floor_called(jev):
    """Test 2: Verify rule safety floor executes and provides deterministic diagnostics."""
    utterance = "Transfer 50,000 rupees to RBI verification account immediately."
    res = jev.analyze_window(utterance, conversation_context=[], detected_language="en")
    assert res.rule_floor is not None
    assert "triggered" in res.rule_floor
    assert res.rule_floor["triggered"] is True
    assert "floor_matches" in res.rule_floor
    assert "PAYMENT" in res.rule_floor["floor_matches"]


def test_3_fusion_called(jev):
    """Test 3: Verify fusion combines neural probabilities with safety floor evidence."""
    utterance = "Please enter your 6 digit UPI pin or read out the OTP right now."
    res = jev.analyze_window(utterance, conversation_context=[], detected_language="en")
    assert "CREDENTIAL" in res.tactic_probabilities
    # Credential tactic should have high probability (>= 0.70)
    assert res.tactic_probabilities["CREDENTIAL"] >= 0.70
    assert "CREDENTIAL" in res.supporting_evidence
    assert res.scam_probability >= 0.70


def test_4_output_schema(jev):
    """Test 4: Verify full JEV output schema conforms to Phase 8 requirements."""
    utterance = "Install AnyDesk app and share your remote access code."
    res = jev.analyze_window(utterance, conversation_context=[], detected_language="en")
    assert hasattr(res, "primary_intent")
    assert hasattr(res, "tactic_probabilities")
    assert hasattr(res, "confidence")
    assert hasattr(res, "supporting_evidence")
    assert hasattr(res, "explanation")
    assert hasattr(res, "is_irreversible_action")
    assert hasattr(res, "model_provider")
    assert hasattr(res, "latency_ms")
    assert hasattr(res, "scam_probability")
    assert hasattr(res, "top_tactic")
    assert hasattr(res, "top_tactic_probability")
    assert hasattr(res, "semantic_model")
    assert hasattr(res, "rule_floor")

    assert res.is_irreversible_action is True
    assert res.primary_intent in ("REMOTE_ACCESS", "CREDENTIAL", "PAYMENT")


def test_5_negation_handling(jev):
    """Test 5: Educational/protective mentions are strictly suppressed and do not trigger alarms."""
    protective_examples = [
        "Please remember: bank officials will never ask for your confidential password or OTP.",
        "Never transfer money to unknown accounts or caller requests.",
        "Police will never ask for payment over video call. This is a scam warning.",
        "I am disconnecting this call, this sounds like a scam fraud."
    ]
    for text in protective_examples:
        res = jev.analyze_window(text, conversation_context=[], detected_language="en")
        assert res.primary_intent == "PROTECTIVE_ADVISORY"
        assert res.scam_probability == 0.0
        assert all(p == 0.0 for p in res.tactic_probabilities.values())
        assert res.is_irreversible_action is False


def test_6_multi_turn_context(jev):
    """Test 6: Verify multi-turn conversational context accumulates tactics across turns."""
    stage_machine = ScamStageMachine()
    velocity_engine = ManipulationVelocityEngine()

    turns = [
        ("Turn 1 (Authority)", "This is Inspector Sharma from Delhi Police Crime Branch."),
        ("Turn 2 (Fear)", "Your passport and identity were found in an illegal narcotics shipment."),
        ("Turn 3 (Isolation)", "You are under digital arrest. Stay on camera and do not tell family."),
        ("Turn 4 (Urgency)", "You must resolve this verification within 15 minutes."),
        ("Turn 5 (Payment)", "Transfer 50,000 rupees to the RBI security escrow account right now.")
    ]

    history = []
    current_time = 1000.0

    for label, text in turns:
        res = jev.analyze_window(text, conversation_context=history, detected_language="en")
        history.append(text)

        # Feed into stage machine and velocity engine
        stage_name, stage_conf = stage_machine.update_stage(
            res.tactic_probabilities,
            timestamp=current_time,
            is_irreversible=res.is_irreversible_action
        )
        current_rank = STAGE_RANKS.get(stage_name, 0)
        velo = velocity_engine.record_event(
            timestamp=current_time,
            tactic_probs=res.tactic_probabilities,
            current_stage_rank=current_rank
        )
        current_time += 15.0

    # At the end of the multi-turn scenario:
    assert stage_machine.state.current_stage in ("DEMAND", "PAYMENT_CREDENTIAL", "CRITICAL_BRAKE")
    assert len(stage_machine.state.history) >= 3


def test_7_safety_brake_compatibility(jev):
    """Test 7: Verify neural JEV output properly triggers Safety Brake on critical attack turn."""
    fusion = MultimodalRiskFusionEngine()
    velocity_calc = VelocityCalculation(
        velocity_score=85.0,
        level="HIGH",
        tactic_rate_per_min=4.0,
        stage_delta=3,
        explanation="High velocity manipulation detected"
    )

    attack_utterance = "Transfer 50,000 rupees clearance fee to RBI verification account immediately."
    res = jev.analyze_window(attack_utterance, conversation_context=[
        "This is CBI Cyber Branch.",
        "You are under digital arrest."
    ], detected_language="en")

    # Multimodal risk fusion
    decision = fusion.fuse(
        tactic_probs=res.tactic_probabilities,
        scam_stage="DEMAND",
        velocity=velocity_calc,
        semantic_confidence=res.confidence,
        visual_signal=None,
        is_speakerphone_active=True
    )

    assert decision.risk_score >= 60

    # Evaluate Safety Brake intervention
    alert = safety_brake_engine.select_audio_alert(
        detected_tactics=res.tactic_probabilities,
        language_code="ta"
    )

    assert isinstance(alert, LocalizedAudioAlert)
    assert alert.language_code == "ta"
    assert "PANAM" in alert.phonetic_romanized
