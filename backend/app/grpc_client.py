"""
RakshaCall Real-Time gRPC Client for Automated Testing & Benchmark Validation.
Simulates live mobile client streaming frames to ProtectionService.
"""

from __future__ import annotations
import asyncio
import time
import grpc
from typing import List, Dict, AsyncIterator, Any

from .grpc_gen import pb2, pb2_grpc


class ProtectionServiceClient:
    """Async gRPC Client for RakshaCall ProtectionService."""

    def __init__(self, target: str = "localhost:50051"):
        self.target = target
        self.channel = None
        self.stub = None

    async def connect(self):
        self.channel = grpc.aio.insecure_channel(self.target)
        self.stub = pb2_grpc.ProtectionServiceStub(self.channel)

    async def close(self):
        if self.channel:
            await self.channel.close()

    async def check_liveness(self) -> pb2.LivenessResponse:
        req = pb2.LivenessRequest(client_version="3.0.0-PROD")
        return await self.stub.CheckLiveness(req)

    async def stream_utterances(
        self,
        session_id: str,
        utterances: List[str],
        language_code: str = "en-IN"
    ) -> List[pb2.RiskUpdate]:
        """Stream a sequence of dialogue utterances and collect risk updates."""
        updates: List[pb2.RiskUpdate] = []

        async def frame_generator() -> AsyncIterator[pb2.ClientFrame]:
            for seq, text in enumerate(utterances, start=1):
                frame = pb2.ClientFrame(
                    session_id=session_id,
                    sequence_number=seq,
                    client_timestamp_ms=int(time.time() * 1000),
                    transcript_snippet=text,
                    detected_language=language_code,
                    user_consent_active=True,
                    device_context=pb2.DeviceContext(
                        audio_route=pb2.DeviceContext.SPEAKERPHONE,
                        is_foreground_service=True,
                        network_type="WIFI"
                    )
                )
                yield frame
                await asyncio.sleep(0.05)  # Simulate small inter-frame arrival interval

        stream = self.stub.StreamProtection(frame_generator())
        async for update in stream:
            updates.append(update)

        return updates

    async def end_session(self, session_id: str) -> pb2.EndSessionResponse:
        req = pb2.EndSessionRequest(
            session_id=session_id,
            termination_reason="USER_DISCONNECTED"
        )
        return await self.stub.EndSession(req)
