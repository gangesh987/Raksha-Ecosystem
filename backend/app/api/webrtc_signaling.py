"""
WebRTC Signaling and Cross-App Protection Routing for Raksha Video integration.
Supports STUN/TURN negotiation, peer-to-peer room signaling, and signal telemetry
into the canonical UnifiedAnalysisPipeline.
"""

import os
import uuid
import time
import logging
from typing import Dict, Set
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Request, HTTPException
from pydantic import BaseModel

router = APIRouter(tags=["webrtc_signaling"])
logger = logging.getLogger("rakshacall.webrtc")

rooms: Dict[str, Set[WebSocket]] = {}
protection_sessions: Dict[str, dict] = {}
protection_signals: Dict[str, list] = {}


class ProtectionSessionRequest(BaseModel):
    call_id: str
    media: dict = {}
    consent: dict = {}


@router.get("/api/webrtc/config")
async def webrtc_config():
    """Returns ICE server configuration for WebRTC peer-to-peer calls."""
    stun = os.getenv("WEBRTC_STUN_URL", "stun:stun.l.google.com:19302")
    turn = os.getenv("WEBRTC_TURN_URL", "")
    servers = [{"urls": [stun]}]
    if turn:
        servers.append({
            "urls": [turn],
            "username": os.getenv("WEBRTC_TURN_USERNAME", ""),
            "credential": os.getenv("WEBRTC_TURN_CREDENTIAL", "")
        })
    return {"ice_servers": servers}


@router.post("/api/calls")
async def create_call():
    """Allocates a unique Call ID for Raksha Video calling."""
    call_id = "RCV-" + uuid.uuid4().hex[:6].upper()
    return {"call_id": call_id}


@router.websocket("/ws/signaling/{call_id}")
async def websocket_signaling(websocket: WebSocket, call_id: str):
    """Bidirectional WebRTC signaling endpoint (SDP Offer/Answer & ICE Candidates)."""
    await websocket.accept()
    rooms.setdefault(call_id, set()).add(websocket)
    logger.info(f"[Signaling] Client connected to call room {call_id} (peers: {len(rooms[call_id])})")
    try:
        while True:
            data = await websocket.receive_json()
            dead = []
            for peer in rooms.get(call_id, set()):
                if peer != websocket:
                    try:
                        await peer.send_json(data)
                    except Exception:
                        dead.append(peer)
            for p in dead:
                rooms.get(call_id, set()).discard(p)
    except (WebSocketDisconnect, Exception):
        rooms.get(call_id, set()).discard(websocket)
        logger.info(f"[Signaling] Client disconnected from call room {call_id}")


@router.post("/api/protection/sessions")
async def create_protection_session(req: ProtectionSessionRequest, request: Request):
    """Creates a protection session linked to an active Raksha Video call."""
    if not req.call_id:
        raise HTTPException(400, "call_id is required")
    session_id = "protect_" + uuid.uuid4().hex[:12]
    protection_sessions[session_id] = {
        "session_id": session_id,
        "call_id": req.call_id,
        "consent": req.consent,
        "created_at": int(time.time() * 1000)
    }
    protection_signals[session_id] = []

    scheme = "wss" if request.url.scheme == "https" else "ws"
    websocket_url = f"{scheme}://{request.url.netloc}/api/ws/sessions/{session_id}"
    return {
        "session_id": session_id,
        "call_id": req.call_id,
        "websocket_url": websocket_url
    }


@router.post("/api/protection/sessions/{session_id}/signals")
async def handle_protection_signals(session_id: str, payload: dict):
    """Receives multi-modal signals from Raksha Video and evaluates risk via UnifiedAnalysisPipeline."""
    if session_id not in protection_sessions:
        raise HTTPException(404, "Session not found")

    signals = payload.get("signals", [])
    protection_signals.setdefault(session_id, []).extend(signals)

    risk_score = 0.0
    risk_level = "SAFE"
    top_tactic = "None"
    brake_triggered = False

    text_content = ""
    for s in signals:
        if s.get("source") == "speech" or s.get("type") == "tactic":
            text_content += " " + str(s.get("name", "")) + " " + str(s.get("content", ""))

    if text_content.strip():
        try:
            from ..ai.unified_pipeline import get_pipeline
            pipeline = get_pipeline()
            analysis = pipeline.analyze_turn(text_content.strip())
            risk_score = analysis.risk_score
            risk_level = analysis.risk_level
            top_tactic = analysis.top_tactic
            brake_triggered = analysis.safety_brake_triggered
        except Exception as e:
            logger.warning(f"Error analyzing signals: {e}")

    return {
        "accepted": True,
        "risk": {
            "score": risk_score,
            "level": risk_level,
            "top_tactic": top_tactic,
            "safety_brake": brake_triggered
        }
    }
