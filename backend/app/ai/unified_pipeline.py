"""
RakshaCall Unified AI Analysis Pipeline.
Consolidates all inference paths into a single pipeline:

    Neural Model (PRIMARY)
    → Safety Floor (GUARDRAIL)
    → Cloud LLM Enhancement (OPTIONAL)
    → Stage Machine
    → Manipulation Velocity
    → Risk Fusion
    → Safety Brake Check
    → Evidence Recording

Used by BOTH REST API and gRPC paths. Eliminates duplicate/inconsistent
analysis engines.
"""

from __future__ import annotations
import time
import logging
from dataclasses import dataclass, field, asdict
from typing import Dict, List, Optional, Any, Tuple

from .jev_provider import LocalSemanticJEVProvider, JEVAnalysisResult, JEVProviderFactory
from .stage_machine import ScamStageMachine, STAGE_RANKS, STAGES
from .manipulation_velocity import ManipulationVelocityEngine, VelocityCalculation
from .multimodal_fusion import MultimodalRiskFusionEngine, FusedRiskDecision, TACTIC_LABELS
from .safety_brake import safety_brake_engine
from .evidence_vault import get_session_ledger
from .conversation_context import SessionConversationContext, SessionRegistry

logger = logging.getLogger("rakshacall.pipeline")


@dataclass
class PipelineResult:
    """Complete analysis result from the unified pipeline."""
    # Core risk
    risk_score: int                     # 0-100
    risk_level: str                     # LOW, MEDIUM, HIGH, CRITICAL
    fused_score: float                  # 0.0-1.0 (legacy compat)

    # Conversation analysis
    conversation_score: float           # 0.0-1.0
    visual_score: float
    liveness_score: float

    # Scam stage
    stage: str                          # CONTACT...CRITICAL_BRAKE
    stage_confidence: float

    # Manipulation velocity
    velocity_level: str                 # LOW, MODERATE, HIGH
    velocity_score: float

    # Tactics
    tactics: List[str]                  # Active tactic IDs
    tactic_probabilities: Dict[str, float]
    reasons: List[str]

    # Safety brake
    safety_brake_triggered: bool
    is_irreversible_action: bool
    intervention_headline: str
    intervention_subtext: str
    verification_steps: List[str]
    localized_audio_alert_key: str

    # Explanation
    primary_explanation: str
    contributing_factors: List[str]

    # Model info
    model_mode: str
    model_provider: str
    scam_probability: float
    latency_ms: float

    # Model disagreement
    model_disagreement: bool
    disagreement_explanation: Optional[str]

    # Evidence
    evidence_hash: str

    def to_api_dict(self) -> Dict[str, Any]:
        """Convert to REST API response format (backward compatible)."""
        return {
            "risk_score": self.risk_score,
            "risk_level": self.risk_level,
            "fused_score": self.fused_score,
            "conversation_score": self.conversation_score,
            "visual_score": self.visual_score,
            "liveness_score": self.liveness_score,
            "stage": self.stage,
            "stage_confidence": self.stage_confidence,
            "velocity_level": self.velocity_level,
            "velocity_score": self.velocity_score,
            "tactics": self.tactics,
            "tactic_probabilities": self.tactic_probabilities,
            "reasons": self.reasons,
            "safety_brake_triggered": self.safety_brake_triggered,
            "irreversible_action": self.is_irreversible_action,
            "intervention": {
                "headline": self.intervention_headline,
                "subtext": self.intervention_subtext,
                "verification_steps": self.verification_steps,
                "localized_audio_alert_key": self.localized_audio_alert_key,
            },
            "primary_explanation": self.primary_explanation,
            "contributing_factors": self.contributing_factors,
            "model_mode": self.model_mode,
            "model_provider": self.model_provider,
            "scam_probability": self.scam_probability,
            "latency_ms": self.latency_ms,
            "model_disagreement": self.model_disagreement,
            "disagreement_explanation": self.disagreement_explanation,
            "evidence_hash": self.evidence_hash,
        }


class UnifiedAnalysisPipeline:
    """
    Single, canonical pipeline for all RakshaCall conversation analysis.

    Pipeline stages:
    1. JEV Semantic Analysis (Neural Model + Safety Floor)
    2. Scam Stage Machine update
    3. Manipulation Velocity calculation
    4. Multimodal Risk Fusion (0-100 score)
    5. Safety Brake evaluation
    6. Evidence chain recording
    7. Explainable result assembly
    """

    def __init__(self):
        self.jev_provider = JEVProviderFactory.get_provider()
        self.fusion_engine = MultimodalRiskFusionEngine()
        # Per-session state
        self._stage_machines: Dict[str, ScamStageMachine] = {}
        self._velocity_engines: Dict[str, ManipulationVelocityEngine] = {}
        logger.info(f"[Pipeline] Initialized with provider: {self.jev_provider.provider_name}")

    def _get_stage_machine(self, session_id: str) -> ScamStageMachine:
        if session_id not in self._stage_machines:
            self._stage_machines[session_id] = ScamStageMachine()
        return self._stage_machines[session_id]

    def _get_velocity_engine(self, session_id: str) -> ManipulationVelocityEngine:
        if session_id not in self._velocity_engines:
            self._velocity_engines[session_id] = ManipulationVelocityEngine()
        return self._velocity_engines[session_id]

    def analyze(
        self,
        transcript: str,
        session_id: str = "default",
        visual_score: float = 0.15,
        liveness_score: float = 0.85,
        detected_language: str = "en-IN",
        conversation_context: Optional[List[str]] = None,
        visual_signal: Optional[Any] = None,  # VisualContextSignal
    ) -> PipelineResult:
        """
        Run the complete analysis pipeline on a transcript.

        This is the SINGLE entry point for all analysis — REST API, gRPC, WebSocket.
        """
        t0 = time.perf_counter()
        now_sec = time.time()
        now_ms = int(now_sec * 1000)

        # Get or create session context
        session_ctx = SessionRegistry.get_or_create(session_id)
        stage_machine = self._get_stage_machine(session_id)
        velocity_engine = self._get_velocity_engine(session_id)
        ledger = get_session_ledger(session_id)

        # Record turn
        if transcript.strip():
            session_ctx.add_turn(
                speaker="CALLER",
                text=transcript.strip(),
                language=detected_language,
            )

        # ─── STAGE 1: JEV Semantic Analysis ────────────────────────
        window_context = conversation_context or session_ctx.get_sliding_window(k=5)
        # Use all but the last item as context (last = current utterance)
        ctx_for_jev = window_context[:-1] if len(window_context) > 1 else []

        jev_result = self.jev_provider.analyze_window(
            current_utterance=transcript,
            conversation_context=ctx_for_jev,
            detected_language=detected_language,
        )

        # Record tactics in session
        session_ctx.record_tactics(jev_result.tactic_probabilities)

        # ─── STAGE 2: Scam Stage Machine ───────────────────────────
        current_stage, stage_conf = stage_machine.update_stage(
            tactic_probs=jev_result.tactic_probabilities,
            timestamp=now_sec,
            is_irreversible=jev_result.is_irreversible_action,
        )
        stage_rank = STAGE_RANKS.get(current_stage, 0)

        # ─── STAGE 3: Manipulation Velocity ────────────────────────
        velocity_calc = velocity_engine.record_event(
            timestamp=now_sec,
            tactic_probs=jev_result.tactic_probabilities,
            current_stage_rank=stage_rank,
        )

        # ─── STAGE 4: Multimodal Risk Fusion ──────────────────────
        fused_decision = self.fusion_engine.fuse(
            tactic_probs=jev_result.tactic_probabilities,
            scam_stage=current_stage,
            velocity=velocity_calc,
            semantic_confidence=jev_result.confidence,
            visual_signal=visual_signal,
        )

        # Track peaks in session
        if fused_decision.risk_score > session_ctx.peak_risk_score:
            session_ctx.peak_risk_score = fused_decision.risk_score
        if fused_decision.safety_brake_triggered:
            session_ctx.safety_brake_triggered = True

        # ─── STAGE 5: Safety Brake & Intervention ─────────────────
        audio_alert = safety_brake_engine.select_audio_alert(
            detected_tactics=jev_result.tactic_probabilities,
            language_code=detected_language,
        )

        if fused_decision.safety_brake_triggered:
            headline = audio_alert.phonetic_romanized
            subtext = audio_alert.english_translation
        elif fused_decision.risk_level in ("HIGH", "CRITICAL"):
            headline = "⚠️ HIGH RISK DETECTED"
            subtext = "Verify the caller independently before taking any action."
        else:
            headline = "Live Call Protection Active"
            subtext = "Monitoring conversation for safety."

        verification_steps = [
            s.instruction_english
            for s in safety_brake_engine.VERIFICATION_STEPS[:4]
        ] if fused_decision.safety_brake_triggered else []

        # ─── STAGE 6: Evidence Chain ──────────────────────────────
        event_id = f"evt-{session_id}-{now_ms}"
        active_tactics = [
            tid for tid, prob in jev_result.tactic_probabilities.items()
            if prob >= 0.30
        ]

        ledger_block = ledger.append_event(
            event_id=event_id,
            event_type="RISK_EVALUATION",
            payload={
                "score": fused_decision.risk_score,
                "level": fused_decision.risk_level,
                "stage": current_stage,
                "velocity": velocity_calc.level,
                "tactics": active_tactics,
                "brake": fused_decision.safety_brake_triggered,
                "transcript_length": len(transcript),
                "scam_probability": jev_result.scam_probability,
                "top_tactic": jev_result.top_tactic,
                "model_version": jev_result.semantic_model.get("model_version", "unknown"),
            },
        )

        # ─── STAGE 7: Assemble Result ────────────────────────────
        elapsed_ms = round((time.perf_counter() - t0) * 1000.0, 2)

        # Build reasons list from tactics
        reasons = []
        for tid in active_tactics:
            label = TACTIC_LABELS.get(tid, tid)
            prob = jev_result.tactic_probabilities.get(tid, 0.0)
            reasons.append(f"{label} (confidence: {prob:.0%})")
        if not reasons:
            reasons = ["No strong coercion pattern detected"]

        # Compute legacy fused_score (0.0-1.0 range) for backward compatibility
        fused_score_legacy = round(fused_decision.risk_score / 100.0, 3)

        result = PipelineResult(
            risk_score=fused_decision.risk_score,
            risk_level=fused_decision.risk_level,
            fused_score=fused_score_legacy,
            conversation_score=round(jev_result.scam_probability, 3),
            visual_score=round(visual_score, 3),
            liveness_score=round(liveness_score, 3),
            stage=current_stage,
            stage_confidence=round(stage_conf, 3),
            velocity_level=velocity_calc.level,
            velocity_score=velocity_calc.velocity_score,
            tactics=active_tactics,
            tactic_probabilities=jev_result.tactic_probabilities,
            reasons=reasons,
            safety_brake_triggered=fused_decision.safety_brake_triggered,
            is_irreversible_action=fused_decision.is_irreversible_action,
            intervention_headline=headline,
            intervention_subtext=subtext,
            verification_steps=verification_steps,
            localized_audio_alert_key=audio_alert.audio_key,
            primary_explanation=fused_decision.primary_explanation,
            contributing_factors=fused_decision.contributing_factors,
            model_mode=f"neural-v2+safety-floor",
            model_provider=self.jev_provider.provider_name,
            scam_probability=jev_result.scam_probability,
            latency_ms=elapsed_ms,
            model_disagreement=fused_decision.model_disagreement,
            disagreement_explanation=fused_decision.disagreement_explanation,
            evidence_hash=ledger_block.current_hash,
        )

        logger.debug(
            f"[Pipeline] session={session_id} score={result.risk_score} "
            f"level={result.risk_level} stage={result.stage} "
            f"velocity={result.velocity_level} brake={result.safety_brake_triggered} "
            f"latency={result.latency_ms}ms"
        )

        return result

    def end_session(self, session_id: str) -> Dict[str, Any]:
        """Clean up session state and return summary."""
        session_ctx = SessionRegistry.get_or_create(session_id)
        ledger = get_session_ledger(session_id)

        peak = session_ctx.peak_risk_score
        final_level = (
            "CRITICAL" if peak >= 81
            else "HIGH" if peak >= 61
            else "MEDIUM" if peak >= 31
            else "LOW"
        )

        summary = {
            "session_id": session_id,
            "peak_risk_score": peak,
            "final_risk_level": final_level,
            "total_turns": len(session_ctx.turns),
            "total_tactics_detected": len(session_ctx.cumulative_tactics),
            "safety_brake_triggered": session_ctx.safety_brake_triggered,
            "head_evidence_hash": ledger.head_hash,
            "duration_seconds": int(session_ctx.elapsed_seconds()),
            "evidence_chain_length": len(ledger.blocks),
            "evidence_chain_valid": ledger.verify_integrity()[0],
        }

        # Clean up session state
        self._stage_machines.pop(session_id, None)
        self._velocity_engines.pop(session_id, None)
        SessionRegistry.remove(session_id)

        return summary


# Global singleton
_pipeline: Optional[UnifiedAnalysisPipeline] = None

def get_pipeline() -> UnifiedAnalysisPipeline:
    """Get or create the global unified analysis pipeline singleton."""
    global _pipeline
    if _pipeline is None:
        _pipeline = UnifiedAnalysisPipeline()
    return _pipeline
