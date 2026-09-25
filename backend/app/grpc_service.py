"""
RakshaCall Real-Time gRPC Protection Service Implementation.
Provides bidirectional streaming communication over HTTP/2 with Protocol Buffers.
Integrates Multilingual ASR, JEV Semantic Intelligence, Scam Stage Machine,
Manipulation Velocity, YOLO11 Contextual Vision, and SHA-256 Evidence Vault.
"""

from __future__ import annotations
import asyncio
import time
import grpc
from typing import AsyncIterator

from .grpc_gen import pb2, pb2_grpc
from .ai.multilingual_asr import asr_engine
from .ai.jev_provider import JEVProviderFactory
from .ai.stage_machine import ScamStageMachine, STAGE_RANKS
from .ai.manipulation_velocity import ManipulationVelocityEngine
from .ai.yolo_vision import vision_engine, VisualContextSignal
from .ai.multimodal_fusion import fusion_engine, TACTIC_LABELS
from .ai.safety_brake import safety_brake_engine
from .ai.evidence_vault import get_session_ledger
from .ai.conversation_context import SessionRegistry


class ProtectionServiceImpl(pb2_grpc.ProtectionServiceServicer):
    """Production gRPC ProtectionService implementation."""

    def __init__(self):
        self.jev_provider = JEVProviderFactory.get_provider()
        self.stage_machines: dict[str, ScamStageMachine] = {}
        self.velocity_engines: dict[str, ManipulationVelocityEngine] = {}

    def _get_stage_machine(self, session_id: str) -> ScamStageMachine:
        if session_id not in self.stage_machines:
            self.stage_machines[session_id] = ScamStageMachine()
        return self.stage_machines[session_id]

    def _get_velocity_engine(self, session_id: str) -> ManipulationVelocityEngine:
        if session_id not in self.velocity_engines:
            self.velocity_engines[session_id] = ManipulationVelocityEngine()
        return self.velocity_engines[session_id]

    async def StreamProtection(
        self,
        request_iterator: AsyncIterator[pb2.ClientFrame],
        context: grpc.aio.ServicerContext
    ) -> AsyncIterator[pb2.RiskUpdate]:
        """
        Bidirectional stream: processes client media frames and yields risk updates.
        """
        async for frame in request_iterator:
            now_ms = int(time.time() * 1000)
            now_sec = time.time()
            session_id = frame.session_id or "default-stream-session"

            # 1. Enforce Explicit User Consent Invariant
            if not frame.user_consent_active:
                update = pb2.RiskUpdate(
                    session_id=session_id,
                    ack_sequence_number=frame.sequence_number,
                    server_timestamp_ms=now_ms,
                    risk_score=0,
                    risk_level=pb2.RISK_LEVEL_LOW,
                    scam_stage=pb2.STAGE_CONTACT,
                    manipulation_velocity=pb2.VELOCITY_LOW,
                    primary_explanation="Processing paused: explicit user consent is required.",
                    current_block_hash="0" * 64
                )
                yield update
                continue

            session_ctx = SessionRegistry.get_or_create(session_id)
            stage_machine = self._get_stage_machine(session_id)
            velocity_engine = self._get_velocity_engine(session_id)
            ledger = get_session_ledger(session_id)

            # 2. Ingest Media Payload
            payload_type = frame.WhichOneof("media_payload")
            transcript_text = ""
            lang_code = frame.detected_language or "en-IN"
            visual_signal: Optional[VisualContextSignal] = None

            if payload_type == "audio_pcm16":
                # Raw audio chunk received: decode with Multilingual Indic ASR
                asr_result = asr_engine.transcribe_audio(frame.audio_pcm16, language_hint=lang_code)
                transcript_text = asr_result.normalized_text
                lang_code = asr_result.detected_language
                session_ctx.total_audio_duration_seconds += asr_result.duration_seconds
            elif payload_type == "transcript_snippet":
                transcript_text = frame.transcript_snippet.strip()
            elif payload_type == "video_frame_jpeg":
                # Supporting video frame received: analyze with YOLO11
                visual_signal = vision_engine.analyze_frame(frame.video_frame_jpeg)

            if transcript_text:
                session_ctx.add_turn(
                    speaker="CALLER",
                    text=transcript_text,
                    language=lang_code
                )

            # 3. Sliding Window Semantic Analysis via JEV Engine
            window_context = session_ctx.get_sliding_window(k=5)
            jev_result = self.jev_provider.analyze_window(
                current_utterance=transcript_text,
                conversation_context=window_context[:-1] if window_context else [],
                detected_language=lang_code
            )

            # Record tactics in session
            session_ctx.record_tactics(jev_result.tactic_probabilities)

            # 4. Scam Stage Machine Evaluation
            current_stage, stage_conf = stage_machine.update_stage(
                tactic_probs=jev_result.tactic_probabilities,
                timestamp=now_sec,
                is_irreversible=jev_result.is_irreversible_action
            )
            stage_rank = STAGE_RANKS.get(current_stage, 0)

            # 5. Manipulation Velocity Evaluation
            velocity_calc = velocity_engine.record_event(
                timestamp=now_sec,
                tactic_probs=jev_result.tactic_probabilities,
                current_stage_rank=stage_rank
            )

            # 6. Multimodal Risk Fusion
            fused_decision = fusion_engine.fuse(
                tactic_probs=jev_result.tactic_probabilities,
                scam_stage=current_stage,
                velocity=velocity_calc,
                semantic_confidence=jev_result.confidence,
                visual_signal=visual_signal,
                is_speakerphone_active=(frame.device_context.audio_route == pb2.DeviceContext.SPEAKERPHONE)
            )

            # Track peaks in session
            if fused_decision.risk_score > session_ctx.peak_risk_score:
                session_ctx.peak_risk_score = fused_decision.risk_score
            if fused_decision.safety_brake_triggered:
                session_ctx.safety_brake_triggered = True

            # 7. Append Event to Tamper-Evident Ledger
            ledger_block = ledger.append_event(
                event_id=f"evt-{frame.sequence_number}-{now_ms}",
                event_type="RISK_EVALUATION",
                payload={
                    "seq": frame.sequence_number,
                    "score": fused_decision.risk_score,
                    "level": fused_decision.risk_level,
                    "stage": current_stage,
                    "velocity": velocity_calc.level,
                    "tactics": jev_result.tactic_probabilities,
                    "brake": fused_decision.safety_brake_triggered,
                    "transcript": transcript_text,
                    "model_version": jev_result.semantic_model.get("model_version", "RakshaCall-v2"),
                    "neural_scam_prob": jev_result.scam_probability,
                    "top_tactic": jev_result.top_tactic,
                    "top_tactic_prob": jev_result.top_tactic_probability,
                    "rule_floor_triggered": jev_result.rule_floor.get("triggered", False)
                }
            )

            # 8. Map to Protobuf Protocol Structures
            risk_level_proto = getattr(pb2, f"RISK_LEVEL_{fused_decision.risk_level}", pb2.RISK_LEVEL_LOW)
            stage_proto = getattr(pb2, f"STAGE_{current_stage}", pb2.STAGE_CONTACT)
            velocity_proto = getattr(pb2, f"VELOCITY_{velocity_calc.level}", pb2.VELOCITY_LOW)

            tactic_protos = []
            for tid, prob in jev_result.tactic_probabilities.items():
                if prob >= 0.30:
                    tactic_protos.append(pb2.TacticEvidence(
                        tactic_id=tid,
                        display_name=TACTIC_LABELS.get(tid, tid),
                        probability=prob,
                        confidence=jev_result.confidence,
                        evidence_quote=jev_result.supporting_evidence.get(tid, transcript_text),
                        timestamp_ms=now_ms,
                        language=lang_code,
                        source="SPEECH",
                        is_irreversible=(tid in ("PAYMENT", "CREDENTIAL", "REMOTE_ACCESS")),
                        explanation=f"{tid} detected via semantic intent ({prob:.2f})"
                    ))

            # Select localized intervention
            audio_alert = safety_brake_engine.select_audio_alert(jev_result.tactic_probabilities, lang_code)
            intervention_proto = pb2.InterventionPlan(
                primary_action=(
                    pb2.InterventionPlan.ENGAGE_SAFETY_BRAKE if fused_decision.safety_brake_triggered
                    else pb2.InterventionPlan.SHOW_WARNING if fused_decision.risk_level in ("HIGH", "MEDIUM")
                    else pb2.InterventionPlan.NONE
                ),
                localized_audio_alert_key=audio_alert.audio_key,
                display_headline=audio_alert.phonetic_romanized if fused_decision.safety_brake_triggered else "Live Call Protection Active",
                display_subtext=audio_alert.english_translation,
                guidance_steps=[s.instruction_english for s in safety_brake_engine.VERIFICATION_STEPS[:4]]
            )

            visual_proto = pb2.VisualPerceptionContext(
                person_count=visual_signal.person_count if visual_signal else 1,
                secondary_device_present=visual_signal.secondary_phone_detected if visual_signal else False,
                document_or_screen_present=visual_signal.screen_or_laptop_detected if visual_signal else False,
                visual_confidence=visual_signal.visual_confidence if visual_signal else 0.85,
                contextual_note=visual_signal.contextual_note if visual_signal else "Standard 1-on-1 video interaction"
            )

            # 9. Yield Real-Time Response Update
            yield pb2.RiskUpdate(
                session_id=session_id,
                ack_sequence_number=frame.sequence_number,
                server_timestamp_ms=now_ms,
                risk_score=fused_decision.risk_score,
                risk_level=risk_level_proto,
                scam_stage=stage_proto,
                manipulation_velocity=velocity_proto,
                detected_tactics=tactic_protos,
                safety_brake_triggered=fused_decision.safety_brake_triggered,
                intervention=intervention_proto,
                primary_explanation=fused_decision.primary_explanation,
                contributing_factors=fused_decision.contributing_factors,
                visual_context=visual_proto,
                model_disagreement=fused_decision.model_disagreement,
                disagreement_reason=fused_decision.disagreement_explanation or "",
                current_block_hash=ledger_block.current_hash
            )

    async def CheckLiveness(
        self,
        request: pb2.LivenessRequest,
        context: grpc.aio.ServicerContext
    ) -> pb2.LivenessResponse:
        return pb2.LivenessResponse(
            is_ready=True,
            active_model_mode="grpc.aio+multilingual-indic-asr+hubert+jev+yolo11+sha256-vault",
            supported_languages=["ta-IN", "en-IN", "hi-IN", "ta-Latn", "hi-Latn"],
            server_time_ms=int(time.time() * 1000)
        )

    async def EndSession(
        self,
        request: pb2.EndSessionRequest,
        context: grpc.aio.ServicerContext
    ) -> pb2.EndSessionResponse:
        session_id = request.session_id
        session_ctx = SessionRegistry.get_or_create(session_id)
        ledger = get_session_ledger(session_id)

        peak = session_ctx.peak_risk_score
        final_level = "CRITICAL" if peak >= 81 else "HIGH" if peak >= 61 else "MEDIUM" if peak >= 31 else "LOW"
        total_tactics = len(session_ctx.cumulative_tactics)

        return pb2.EndSessionResponse(
            session_id=session_id,
            peak_risk_score=peak,
            final_risk_level=final_level,
            total_tactics_detected=total_tactics,
            head_evidence_hash=ledger.head_hash,
            duration_seconds=int(session_ctx.elapsed_seconds())
        )


async def serve_grpc(port: int = 50051) -> grpc.aio.Server:
    """Start asynchronous gRPC Protection server."""
    server = grpc.aio.server()
    pb2_grpc.add_ProtectionServiceServicer_to_server(ProtectionServiceImpl(), server)
    server.add_insecure_port(f"0.0.0.0:{port}")
    await server.start()
    return server
