"""
RakshaCall Multi-Vector Risk Fusion & Decision Engine.
Combines:
1. Semantic intent classification
2. Multi-tactic severity weighting
3. Cumulative session history & topic tracking
4. Manipulation velocity acceleration
5. 12-stage trajectory rank
6. Visual threat signals (badges, forged docs, deepfakes)

Outputs:
- riskScore: 0 to 100
- riskLevel: SAFE (0-19), LOW (20-39), MEDIUM (40-59), HIGH (60-79), CRITICAL (80-100)
- riskConfidence: 0.0 to 1.0
- recommendedAction: String directive for UI & user
- reasons: Numbered, transparent explainable evidence list
"""

from typing import List, Dict, Tuple, Any, Optional, Set
from dataclasses import dataclass, field

from .taxonomy import (
    CRITICAL_TACTICS, HIGH_TACTICS, MEDIUM_TACTICS, LOW_TACTICS,
    STAGE_HIERARCHY, TAXONOMY_CATEGORIES
)
from .intent_engine import DetectedTactic, IntentAnalysisResult
from .manipulation_velocity import VelocityMetric
from .context_memory import CallSessionState


@dataclass
class FusedRiskDecision:
    risk_score: int              # 0 to 100
    risk_level: str              # SAFE, LOW, MEDIUM, HIGH, CRITICAL
    risk_confidence: float       # 0.0 to 1.0
    recommended_action: str      # User-facing safety advisory
    reasons: List[str]           # Numbered, transparent explanation
    safety_brake_triggered: bool # True if CRITICAL threshold exceeded
    stage: str
    stage_confidence: float
    manipulation_velocity: float
    model_mode: str = "HYBRID"


class RiskFusionEngine:
    """
    Synthesizes multi-vector threat signals into deterministic,
    evidence-backed risk scores and explainable reasons.
    """

    def fuse(
        self,
        intent_result: IntentAnalysisResult,
        session_state: CallSessionState,
        velocity_metric: VelocityMetric,
        current_stage: str,
        stage_confidence: float,
        visual_score: float = 0.0,
        model_mode: str = "HYBRID"
    ) -> FusedRiskDecision:
        """
        Evaluate full threat envelope and compute integrated risk.
        """
        # 1. Hard-negative fast path: Guaranteed SAFE
        if intent_result.is_hard_negative:
            return FusedRiskDecision(
                risk_score=0,
                risk_level="SAFE",
                risk_confidence=0.98,
                recommended_action="Normal conversation. Informational / defensive advisory identified.",
                reasons=["Conversation contains legitimate protective warning or awareness advice."],
                safety_brake_triggered=False,
                stage="NORMAL",
                stage_confidence=0.95,
                manipulation_velocity=0.0,
                model_mode=model_mode
            )

        # 2. Gather all active tactics (current utterance + active session memory)
        current_tactics = intent_result.tactics
        cumulative_tactics = session_state.cumulative_tactics
        all_active_tactic_types = set(t.type for t in current_tactics).union(cumulative_tactics)

        if not all_active_tactic_types:
            return FusedRiskDecision(
                risk_score=5,
                risk_level="SAFE",
                risk_confidence=0.90,
                recommended_action="Normal conversation. No social engineering indicators detected.",
                reasons=["No manipulative coercion or credential extraction tactics observed."],
                safety_brake_triggered=False,
                stage="NORMAL",
                stage_confidence=stage_confidence,
                manipulation_velocity=velocity_metric.velocity_score,
                model_mode=model_mode
            )

        # 3. Base Score Calculation from Tactic Weights
        base_score = 0.0
        reasons_list: List[str] = []

        # Tactic severity contributions
        for tactic in all_active_tactic_types:
            if tactic in CRITICAL_TACTICS:
                base_score += 35.0
            elif tactic in HIGH_TACTICS:
                base_score += 22.0
            elif tactic in MEDIUM_TACTICS:
                base_score += 15.0
            elif tactic in LOW_TACTICS:
                base_score += 6.0

        # Current utterance immediate intensity
        for dt in current_tactics:
            if dt.type in CRITICAL_TACTICS:
                base_score += 15.0 * dt.confidence
            elif dt.type in HIGH_TACTICS:
                base_score += 10.0 * dt.confidence
            elif dt.type in MEDIUM_TACTICS:
                base_score += 7.0 * dt.confidence

        # 4. Multi-Tactic Coercive Combinations (Synergy Boosters)
        combos = [
            ({"AUTHORITY_IMPERSONATION", "URGENCY"}, 12.0, "Authority claim combined with artificial time pressure."),
            ({"POLICE_IMPERSONATION", "LEGAL_THREAT"}, 15.0, "Police impersonation coupled with threats of prosecution."),
            ({"BANK_IMPERSONATION", "OTP_REQUEST"}, 20.0, "Financial institution impersonation paired with code extraction."),
            ({"ISOLATION", "BANK_TRANSFER_REQUEST"}, 20.0, "Social isolation enforced while demanding money transfer."),
            ({"ISOLATION", "CONTROL"}, 10.0, "Active control demands prohibiting call termination or family contact."),
            ({"ACCOUNT_BLOCK_THREAT", "URGENCY"}, 12.0, "Immediate threat of account freezing used to induce panic."),
            ({"DIGITAL_ARREST", "CONTROL"}, 18.0, "Fabricated digital arrest keeping victim under persistent surveillance."),
            ({"REMOTE_ACCESS", "BANKING_ACTION"}, 18.0, "Attempting remote device access while targeting banking applications.")
        ]

        for req_set, boost, desc in combos:
            if req_set.issubset(all_active_tactic_types):
                base_score += boost
                reasons_list.append(desc)

        # 5. Manipulation Velocity Contribution
        velocity_score = velocity_metric.velocity_score
        if velocity_score >= 0.70:
            base_score += 20.0
            reasons_list.append(f"Rapid manipulation velocity: {velocity_metric.description}")
        elif velocity_score >= 0.40:
            base_score += 10.0
            reasons_list.append("Accelerating tactic escalation within a narrow time window.")

        # 6. Stage Progression Rank Contribution
        stage_rank = STAGE_HIERARCHY.get(current_stage, 0)
        if stage_rank >= STAGE_HIERARCHY.get("CRITICAL_INTERVENTION", 11):
            base_score += 25.0
        elif stage_rank >= STAGE_HIERARCHY.get("THREAT_ESCALATION", 10):
            base_score += 18.0
        elif stage_rank >= STAGE_HIERARCHY.get("PAYMENT_REQUEST", 8):
            base_score += 14.0
        elif stage_rank >= STAGE_HIERARCHY.get("ISOLATION", 6):
            base_score += 10.0

        # 7. Visual Signals Contribution
        if visual_score > 0.60:
            base_score += 15.0 * visual_score
            reasons_list.append("Visual threat indicator detected: suspicious uniform, forged document, or webcam overlay.")

        # 8. Clamp & Discretize Risk Level
        final_score = int(min(max(base_score, 0.0), 100.0))

        if final_score >= 80 or any(t.type in CRITICAL_TACTICS for t in current_tactics):
            # Guarantee CRITICAL if explicit credential extraction or transfer coercion is active
            final_score = max(final_score, 82)
            risk_level = "CRITICAL"
            action = "🛑 CRITICAL DANGER: DO NOT SHARE CODES OR TRANSFER FUNDS. Verify caller independently."
            brake_triggered = True
        elif final_score >= 60:
            risk_level = "HIGH"
            action = "⚠️ HIGH RISK: Coercive psychological tactics detected. Pause and verify with family."
            brake_triggered = False
        elif final_score >= 40:
            risk_level = "MEDIUM"
            action = "⚡ CAUTION: Institutional claims or urgency detected. Stay alert."
            brake_triggered = False
        elif final_score >= 20:
            risk_level = "LOW"
            action = "Normal conversation with minor identity or formal references."
            brake_triggered = False
        else:
            risk_level = "SAFE"
            action = "Normal conversation."
            brake_triggered = False

        # 9. Format Numbered, Transparent Reasons (Section 28)
        formatted_reasons: List[str] = []
        # Add primary tactic descriptions
        for t in sorted(list(all_active_tactic_types)):
            desc = TAXONOMY_CATEGORIES.get(t, t.replace("_", " ").title())
            formatted_reasons.append(desc)

        # Add combination & velocity observations
        formatted_reasons.extend(reasons_list)

        # Deduplicate while preserving order
        deduped = list(dict.fromkeys(formatted_reasons))
        numbered_reasons = [f"{i+1}. {r}" for i, r in enumerate(deduped[:6])]

        # Overall confidence calculation
        confidence = min(0.70 + 0.05 * len(all_active_tactic_types) + 0.10 * stage_confidence, 0.99)

        return FusedRiskDecision(
            risk_score=final_score,
            risk_level=risk_level,
            risk_confidence=round(confidence, 2),
            recommended_action=action,
            reasons=numbered_reasons,
            safety_brake_triggered=brake_triggered,
            stage=current_stage,
            stage_confidence=round(stage_confidence, 2),
            manipulation_velocity=round(velocity_score, 2),
            model_mode=model_mode
        )


# Global risk fusion engine
risk_fusion_engine = RiskFusionEngine()
