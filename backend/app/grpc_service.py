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
from .ai.yolo_vision import vision_engine, VisualContextSignal
from .ai.multimodal_fusion import TACTIC_LABELS
from .ai.safety_brake import safety_brake_engine
from .ai.evidence_vault import get_session_ledger
from .ai.conversation_context import SessionRegistry
from .ai.unified_pipeline import UnifiedAnalysisPipeline, get_pipeline


class ProtectionServiceImpl(pb2_grpc.ProtectionServiceServicer):
    """
    Production gRPC ProtectionService implementation.
    Acts as a streaming Protobuf transport adapter to the canonical UnifiedAnalysisPipeline.
    Ensures UnifiedAnalysisPipeline is the single source of truth across REST, WS, and gRPC.
    """

    def __init__(self, pipeline: Optional[UnifiedAnalysisPipeline] = None):
        self.pipeline = pipeline or get_pipeline()

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

            # 2. Ingest Media Payload
            payload_type = frame.WhichOneof("media_payload")
            transcript_text = ""
            lang_code = frame.detected_language or "en-IN"
            visual_signal: Optional[VisualContextSignal] = None

            if payload_type == "audio_pcm16":
                # Raw audio chunk received: decode with Multilingual Indic ASR
                asr_result = asr_engine.transcribe_audio(
                    frame.audio_pcm16,
                    language_hint=lang_code,
                    session_id=session_id
                )
                transcript_text = asr_result.normalized_text
                lang_code = asr_result.detected_language
                session_ctx.total_audio_duration_seconds += asr_result.duration_seconds
                if asr_result.status in ("ASR_ERROR", "ASR_UNAVAILABLE"):
                    logger.warning(
                        f"Session {session_id} ASR status: {asr_result.status} - "
                        f"{asr_result.error_message or 'Speech decoding unavailable'}"
                    )
            elif payload_type == "transcript_snippet":
                transcript_text = frame.transcript_snippet.strip()
            elif payload_type == "video_frame_jpeg":
                # Supporting video frame received: analyze with YOLO11
                visual_signal = vision_engine.analyze_frame(frame.video_frame_jpeg)

            # 3. Run canonical UnifiedAnalysisPipeline
            result = self.pipeline.analyze(
                transcript=transcript_text,
                session_id=session_id,
                detected_language=lang_code,
                visual_signal=visual_signal,
            )

            # 4. Map to Protobuf Protocol Structures
            risk_level_proto = getattr(pb2, f"RISK_LEVEL_{result.risk_level}", pb2.RISK_LEVEL_LOW)
            stage_proto = getattr(pb2, f"STAGE_{result.stage}", pb2.STAGE_CONTACT)
            velocity_proto = getattr(pb2, f"VELOCITY_{result.velocity_level}", pb2.VELOCITY_LOW)

            tactic_protos = []
            for tid, prob in result.tactic_probabilities.items():
                if prob >= 0.30:
                    tactic_protos.append(pb2.TacticEvidence(
                        tactic_id=tid,
                        display_name=TACTIC_LABELS.get(tid, tid),
                        probability=prob,
                        confidence=prob,
                        evidence_quote=transcript_text,
                        timestamp_ms=now_ms,
                        language=lang_code,
                        source="SPEECH",
                        is_irreversible=(tid in ("PAYMENT", "CREDENTIAL", "REMOTE_ACCESS")),
                        explanation=f"{tid} detected via semantic intent ({prob:.2f})"
                    ))

            # Select localized intervention
            intervention_proto = pb2.InterventionPlan(
                primary_action=(
                    pb2.InterventionPlan.ENGAGE_SAFETY_BRAKE if result.safety_brake_triggered
                    else pb2.InterventionPlan.SHOW_WARNING if result.risk_level in ("HIGH", "MEDIUM")
                    else pb2.InterventionPlan.NONE
                ),
                localized_audio_alert_key=result.localized_audio_alert_key,
                display_headline=result.intervention_headline if result.safety_brake_triggered else "Live Call Protection Active",
                display_subtext=result.intervention_subtext,
                guidance_steps=result.verification_steps[:4] if result.verification_steps else [s.instruction_english for s in safety_brake_engine.VERIFICATION_STEPS[:4]]
            )

            visual_proto = pb2.VisualPerceptionContext(
                person_count=visual_signal.person_count if visual_signal else 1,
                secondary_device_present=visual_signal.secondary_phone_detected if visual_signal else False,
                document_or_screen_present=visual_signal.screen_or_laptop_detected if visual_signal else False,
                visual_confidence=visual_signal.visual_confidence if visual_signal else 0.85,
                contextual_note=visual_signal.contextual_note if visual_signal else "Standard 1-on-1 video interaction"
            )

            # 5. Yield Real-Time Response Update
            yield pb2.RiskUpdate(
                session_id=session_id,
                ack_sequence_number=frame.sequence_number,
                server_timestamp_ms=now_ms,
                risk_score=result.risk_score,
                risk_level=risk_level_proto,
                scam_stage=stage_proto,
                manipulation_velocity=velocity_proto,
                detected_tactics=tactic_protos,
                safety_brake_triggered=result.safety_brake_triggered,
                intervention=intervention_proto,
                primary_explanation=result.primary_explanation,
                contributing_factors=result.contributing_factors,
                visual_context=visual_proto,
                model_disagreement=result.model_disagreement,
                disagreement_reason=result.disagreement_explanation or "",
                current_block_hash=result.evidence_hash
            )

    async def CheckLiveness(
        self,
        request: pb2.LivenessRequest,
        context: grpc.aio.ServicerContext
    ) -> pb2.LivenessResponse:
        return pb2.LivenessResponse(
            is_ready=True,
            active_model_mode="grpc.aio+canonical-unified-pipeline+multilingual-indic-asr+hubert+jev+yolo11+sha256-vault",
            supported_languages=["ta-IN", "en-IN", "hi-IN", "ta-Latn", "hi-Latn"],
            server_time_ms=int(time.time() * 1000)
        )

    async def EndSession(
        self,
        request: pb2.EndSessionRequest,
        context: grpc.aio.ServicerContext
    ) -> pb2.EndSessionResponse:
        session_id = request.session_id
        summary = self.pipeline.end_session(session_id)

        return pb2.EndSessionResponse(
            session_id=session_id,
            peak_risk_score=summary["peak_risk_score"],
            final_risk_level=summary["final_risk_level"],
            total_tactics_detected=summary["total_tactics_detected"],
            head_evidence_hash=summary["head_evidence_hash"],
            duration_seconds=summary["duration_seconds"]
        )


async def serve_grpc(port: int = 50051) -> grpc.aio.Server:
    """Start asynchronous gRPC Protection server."""
    server = grpc.aio.server()
    pb2_grpc.add_ProtectionServiceServicer_to_server(ProtectionServiceImpl(), server)
    server.add_insecure_port(f"0.0.0.0:{port}")
    await server.start()
    return server
