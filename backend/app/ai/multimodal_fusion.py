"""
RakshaCall Multimodal Risk Fusion Engine.
Synthesizes Conversation Evidence, Scam Stage Progression, Manipulation Velocity,
Semantic Model Confidence, and Supporting YOLO11 Visual Signals into an explainable 0-100 score.
"""

from __future__ import annotations
import math
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple, Any

from .stage_machine import STAGE_RANKS
from .manipulation_velocity import VelocityCalculation
from .yolo_vision import VisualContextSignal


TACTIC_WEIGHTS = {
    "AUTHORITY": 15,
    "FEAR": 15,
    "URGENCY": 10,
    "ISOLATION": 15,
    "PAYMENT": 20,
    "CREDENTIAL": 20,
    "REMOTE_ACCESS": 15,
    "SUSPICIOUS_LINK": 10,
    "ESCALATION": 10
}

TACTIC_LABELS = {
    "AUTHORITY": "Authority Impersonation (Police/CBI/Customs/TRAI)",
    "FEAR": "Criminal Allegation / Fear (Drugs/Arrest/Warrant)",
    "URGENCY": "Extreme Temporal Urgency ('Within 10 minutes')",
    "ISOLATION": "Coercive Isolation ('Do not tell family / closed room')",
    "PAYMENT": "Payment Demand (Security deposit / clearance transfer)",
    "CREDENTIAL": "Credential / OTP Pressure (Read out 6-digit code)",
    "REMOTE_ACCESS": "Remote Access Tool Pressure (AnyDesk/TeamViewer)",
    "SUSPICIOUS_LINK": "Suspicious Link / Verification APK Pressure",
    "ESCALATION": "Escalation Threat (Home raid / immediate physical arrest)"
}

STAGE_POINTS = {
    "CONTACT": 0,
    "AUTHORITY": 10,
    "FEAR": 25,
    "ISOLATION": 45,
    "DEMAND": 65,
    "PAYMENT_CREDENTIAL": 85,
    "CRITICAL_BRAKE": 95
}


@dataclass
class FusedRiskDecision:
    """Rich explainable output from the Multimodal Risk Engine."""
    risk_score: int             # 0 to 100
    risk_level: str             # "LOW", "MEDIUM", "HIGH", "CRITICAL"
    scam_stage: str             # Current scam stage
    manipulation_velocity: str  # "LOW", "MODERATE", "HIGH"
    velocity_score: float
    contributing_factors: List[str]
    primary_explanation: str
    is_irreversible_action: bool
    safety_brake_triggered: bool
    model_disagreement: bool
    disagreement_explanation: Optional[str]
    tactic_breakdown: Dict[str, float]
    visual_summary: str


class MultimodalRiskFusionEngine:
    """
    Multimodal Risk Fusion Engine.
    Strictly adheres to:
    Conversation + Stage + Velocity = PRIMARY
    YOLO11 Vision = SUPPORTING
    Model Disagreement = SAFETY SIGNAL
    """

    def fuse(
        self,
        tactic_probs: Dict[str, float],
        scam_stage: str,
        velocity: VelocityCalculation,
        semantic_confidence: float,
        visual_signal: Optional[VisualContextSignal] = None,
        is_speakerphone_active: bool = True
    ) -> FusedRiskDecision:
        contributing_factors: List[str] = []
        
        # 1. PRIMARY: Conversation Tactic Scoring
        raw_conv_score = 0.0
        for tactic, prob in tactic_probs.items():
            if prob >= 0.35 and tactic in TACTIC_WEIGHTS:
                weight = TACTIC_WEIGHTS[tactic]
                contribution = weight * prob
                raw_conv_score += contribution
                contributing_factors.append(
                    f"+{int(contribution)} {TACTIC_LABELS.get(tactic, tactic)} (prob: {prob:.2f})"
                )

        # Multi-tactic synergistic amplification (if 3+ coercive tactics co-occur, or 2 tactics with irreversible action)
        coercive_count = sum(1 for p in tactic_probs.values() if p >= 0.40)
        irreversible = any(
            t in tactic_probs and tactic_probs[t] >= 0.45
            for t in ("PAYMENT", "CREDENTIAL", "REMOTE_ACCESS")
        )
        if coercive_count >= 3 or (irreversible and coercive_count >= 2):
            raw_conv_score = max(raw_conv_score * 1.25, 42.0)
            contributing_factors.append("Multi-tactic coercion cluster detected (+synergy)")

        conv_score = min(100.0, raw_conv_score)

        # 2. PRIMARY: Stage Progression Component
        stage_pts = STAGE_POINTS.get(scam_stage, 0)
        if stage_pts >= 25:
            contributing_factors.append(f"Scam Stage '{scam_stage}' progression (+{int(stage_pts * 0.15)} pts)")

        # 3. PRIMARY: Manipulation Velocity Component
        velo_pts = min(20.0, velocity.velocity_score * 0.6)
        if velocity.level == "HIGH":
            contributing_factors.append(f"Rapid escalation velocity: {velocity.velocity_score} pts/min (+{int(velo_pts)} pts)")
        elif velocity.level == "MODERATE":
            contributing_factors.append(f"Noticeable escalation cadence (+{int(velo_pts)} pts)")

        # 4. SUPPORTING: YOLO11 Visual Context Component
        visual_pts = 0.0
        visual_summary = "Visual stream inactive or normal."
        if visual_signal is not None:
            visual_summary = visual_signal.contextual_note
            if visual_signal.secondary_phone_detected:
                visual_pts += 5.0
                contributing_factors.append("Supporting Visual: Secondary mobile phone detected in user hands (+5 pts)")
            if visual_signal.screen_or_laptop_detected:
                visual_pts += 5.0
                contributing_factors.append("Supporting Visual: Remote display/monitor context detected (+5 pts)")

        # 5. Multimodal Context Fusion Formula
        # Primary conversation + stage + velocity dynamically scaled
        fused = (
            conv_score * 0.70 +
            (stage_pts * 0.20) +
            (velo_pts * 0.80) +
            visual_pts
        )
        final_score = int(min(100, max(0, round(fused))))

        # 6. Risk Level Categorization
        if final_score >= 81:
            risk_level = "CRITICAL"
        elif final_score >= 61:
            risk_level = "HIGH"
        elif final_score >= 31:
            risk_level = "MEDIUM"
        else:
            risk_level = "LOW"

        # 7. Irreversible Action Determination
        irreversible = any(
            t in tactic_probs and tactic_probs[t] >= 0.45
            for t in ("PAYMENT", "CREDENTIAL", "REMOTE_ACCESS")
        )

        # 8. Safety Brake Trigger Invariant:
        # HIGH/CRITICAL Risk (>= 60) AND Irreversible Action Tactic Present
        safety_brake = (final_score >= 60 and irreversible) or (final_score >= 85)

        # 9. Model Disagreement Detection
        disagreement = False
        disagreement_exp = None
        if final_score >= 60 and (visual_signal is not None and visual_signal.person_count == 1 and not visual_signal.secondary_phone_detected):
            disagreement = True
            disagreement_exp = (
                "Conversational evidence indicates severe coercion while visual environment appears normal. "
                "Per safety invariant, speech coercion takes primary precedence."
            )
        elif final_score < 30 and (visual_signal is not None and visual_signal.secondary_phone_detected):
            disagreement = True
            disagreement_exp = "Visual anomalies observed without verbal coercion; monitoring continues."

        # 10. Primary Explanation Formulation
        if safety_brake:
            primary_exp = (
                f"SAFETY BRAKE ENGAGED: Risk {final_score}/100 with imminent irreversible harm "
                f"({', '.join(t for t in ('PAYMENT', 'CREDENTIAL', 'REMOTE_ACCESS') if t in tactic_probs)})."
            )
        elif final_score >= 61:
            primary_exp = f"HIGH RISK ({final_score}/100): Coercive entrapment detected in conversation."
        elif final_score >= 31:
            primary_exp = f"MEDIUM RISK ({final_score}/100): Suspicious authority claims or urgency observed."
        else:
            primary_exp = "LOW RISK: Dialogue is currently benign or standard customer communication."

        return FusedRiskDecision(
            risk_score=final_score,
            risk_level=risk_level,
            scam_stage=scam_stage,
            manipulation_velocity=velocity.level,
            velocity_score=velocity.velocity_score,
            contributing_factors=contributing_factors,
            primary_explanation=primary_exp,
            is_irreversible_action=irreversible,
            safety_brake_triggered=safety_brake,
            model_disagreement=disagreement,
            disagreement_explanation=disagreement_exp,
            tactic_breakdown=tactic_probs,
            visual_summary=visual_summary
        )


# Global singleton
fusion_engine = MultimodalRiskFusionEngine()
