"""
RakshaCall Protection API Routes.
Handles sessions, analysis, trusted contacts, evidence, and real-time WebSocket.
All analysis routes use the unified pipeline (neural model + safety floor).
"""

import json
import asyncio
import hashlib
import os
import logging
import base64
from fastapi import APIRouter, Depends, HTTPException, WebSocket, WebSocketDisconnect
from sqlalchemy.orm import Session as DBSession
from ..auth import db, current_user
from ..models import User, Session, RiskEvent, TrustedContact, EvidenceReport, AuditEvent
from ..schemas import SessionCreate, Analyze, ContactCreate
from ..ai.unified_pipeline import get_pipeline
from ..ai.gemini_live import connect as gemini_connect, GeminiLiveUnavailable, parse_json, MODEL

logger = logging.getLogger("rakshacall.api.sessions")
router = APIRouter(prefix="/api", tags=["protection"])
rooms: dict[int, set] = {}


def audit_log(d: DBSession, user_id: int, action: str, meta: dict = None):
    """Record an audit event with structured metadata."""
    d.add(AuditEvent(user_id=user_id, action=action, metadata_json=meta or {}))
    d.commit()


# ─── Session CRUD ────────────────────────────────────────────────

@router.post("/sessions")
def create_session(p: SessionCreate, u=Depends(current_user), d: DBSession = Depends(db)):
    s = Session(user_id=u.id, source=p.source)
    d.add(s)
    d.commit()
    d.refresh(s)
    audit_log(d, u.id, "session.created", {"session_id": s.id})
    return {
        "id": s.id,
        "risk_level": s.risk_level,
        "risk_score": s.risk_score,
        "status": s.status,
    }


@router.get("/sessions")
def list_sessions(u=Depends(current_user), d: DBSession = Depends(db)):
    xs = d.query(Session).filter_by(user_id=u.id).order_by(Session.id.desc()).limit(50).all()
    return [
        {
            "id": x.id,
            "risk_level": x.risk_level,
            "risk_score": x.risk_score,
            "status": x.status,
            "created_at": x.created_at.isoformat() if x.created_at else "",
        }
        for x in xs
    ]


@router.get("/sessions/{sid}")
def session_detail(sid: int, u=Depends(current_user), d: DBSession = Depends(db)):
    s = d.get(Session, sid)
    if not s or s.user_id != u.id:
        raise HTTPException(404, "Session not found")
    return {
        "id": s.id,
        "status": s.status,
        "risk_level": s.risk_level,
        "risk_score": s.risk_score,
        "events": [
            {
                "id": e.id,
                "transcript": e.transcript,
                "risk_level": e.risk_level,
                "fused_score": e.fused_score,
                "reasons": e.reasons,
                "created_at": e.created_at.isoformat() if e.created_at else "",
            }
            for e in s.events
        ],
    }


# ─── Analysis (Unified Pipeline) ────────────────────────────────

@router.post("/sessions/{sid}/analyze")
async def analyze(sid: int, p: Analyze, u=Depends(current_user), d: DBSession = Depends(db)):
    """Analyze a transcript using the unified neural+safety-floor pipeline."""
    s = d.get(Session, sid)
    if not s or s.user_id != u.id:
        raise HTTPException(404, "Session not found")

    # Run unified pipeline (neural model → safety floor → stage → velocity → fusion → brake)
    pipeline = get_pipeline()
    result = pipeline.analyze(
        transcript=p.transcript,
        session_id=str(sid),
        visual_score=p.visual_score,
        liveness_score=p.liveness_score,
    )

    api_result = result.to_api_dict()

    # Persist risk event
    e = RiskEvent(
        session_id=sid,
        transcript=p.transcript,
        conversation_score=result.conversation_score,
        visual_score=result.visual_score,
        liveness_score=result.liveness_score,
        fused_score=result.fused_score,
        risk_level=result.risk_level,
        reasons=result.reasons,
    )
    d.add(e)
    s.risk_level = result.risk_level
    s.risk_score = result.fused_score
    d.commit()
    d.refresh(e)

    # Broadcast to connected WebSocket clients
    await broadcast(sid, {"type": "risk_update", "event_id": e.id, **api_result})
    audit_log(d, u.id, "risk.analyzed", {"session_id": sid, "risk": result.risk_level, "score": result.risk_score})

    return {**api_result, "event_id": e.id}


# ─── Risk & Timeline ────────────────────────────────────────────

@router.get("/sessions/{sid}/risk")
def session_risk(sid: int, u=Depends(current_user), d: DBSession = Depends(db)):
    """Get the current risk state for a session."""
    s = d.get(Session, sid)
    if not s or s.user_id != u.id:
        raise HTTPException(404, "Session not found")
    return {
        "session_id": s.id,
        "risk_level": s.risk_level,
        "risk_score": s.risk_score,
        "status": s.status,
        "event_count": len(s.events),
    }


@router.get("/sessions/{sid}/timeline")
def session_timeline(sid: int, u=Depends(current_user), d: DBSession = Depends(db)):
    """Get the chronological event timeline for a session."""
    s = d.get(Session, sid)
    if not s or s.user_id != u.id:
        raise HTTPException(404, "Session not found")
    return {
        "session_id": s.id,
        "events": [
            {
                "id": e.id,
                "timestamp": e.created_at.isoformat() if e.created_at else "",
                "risk_level": e.risk_level,
                "fused_score": e.fused_score,
                "transcript_preview": e.transcript[:100] if e.transcript else "",
                "reasons": e.reasons,
            }
            for e in s.events
        ],
    }


# ─── Safety Warning ─────────────────────────────────────────────

@router.post("/sessions/{sid}/warning")
def trigger_warning(sid: int, u=Depends(current_user), d: DBSession = Depends(db)):
    s = d.get(Session, sid)
    if not s or s.user_id != u.id:
        raise HTTPException(404, "Session not found")
    audit_log(d, u.id, "warning.triggered", {"session_id": sid})
    return {
        "ok": True,
        "message": "Pause and verify the caller independently. Never transfer money under pressure.",
        "risk_level": s.risk_level,
    }


# ─── Verification Coach ─────────────────────────────────────────

@router.post("/sessions/{sid}/verification/start")
def start_verification(sid: int, u=Depends(current_user), d: DBSession = Depends(db)):
    """Start the guided verification flow for a session."""
    s = d.get(Session, sid)
    if not s or s.user_id != u.id:
        raise HTTPException(404, "Session not found")

    from ..ai.safety_brake import safety_brake_engine
    steps = [
        {
            "step": step.step_number,
            "title": step.title_english,
            "title_vernacular": step.title_vernacular,
            "instruction": step.instruction_english,
            "instruction_vernacular": step.instruction_vernacular,
        }
        for step in safety_brake_engine.VERIFICATION_STEPS
    ]
    audit_log(d, u.id, "verification.started", {"session_id": sid})
    return {"ok": True, "session_id": sid, "steps": steps}


# ─── Trusted Contacts ───────────────────────────────────────────

@router.get("/contacts")
def list_contacts(u=Depends(current_user), d: DBSession = Depends(db)):
    return [
        {"id": c.id, "name": c.name, "phone": c.phone, "consent_enabled": c.consent_enabled}
        for c in d.query(TrustedContact).filter_by(user_id=u.id).all()
    ]


@router.post("/contacts")
def add_contact(p: ContactCreate, u=Depends(current_user), d: DBSession = Depends(db)):
    c = TrustedContact(user_id=u.id, **p.model_dump())
    d.add(c)
    d.commit()
    d.refresh(c)
    return {"id": c.id, "name": c.name, "phone": c.phone, "consent_enabled": c.consent_enabled}


@router.delete("/contacts/{contact_id}")
def delete_contact(contact_id: int, u=Depends(current_user), d: DBSession = Depends(db)):
    c = d.get(TrustedContact, contact_id)
    if not c or c.user_id != u.id:
        raise HTTPException(404, "Contact not found")
    d.delete(c)
    d.commit()
    audit_log(d, u.id, "contact.deleted", {"contact_id": contact_id})
    return {"ok": True}


@router.post("/sessions/{sid}/trusted-alert")
def send_trusted_alert(sid: int, u=Depends(current_user), d: DBSession = Depends(db)):
    """Send alert to trusted contact. Honestly reports delivery status."""
    s = d.get(Session, sid)
    if not s or s.user_id != u.id:
        raise HTTPException(404, "Session not found")

    c = d.query(TrustedContact).filter_by(user_id=u.id, consent_enabled=True).first()
    if not c:
        raise HTTPException(400, "No consent-enabled trusted contact")

    msg = (
        f"RakshaCall Alert: I am currently in a call flagged as high-risk "
        f"coercive scam (session {sid}). Please call or message me."
    )

    sid_env = os.getenv("TWILIO_ACCOUNT_SID")
    token = os.getenv("TWILIO_AUTH_TOKEN")
    sender = os.getenv("TWILIO_FROM_NUMBER")

    if not (sid_env and token and sender):
        audit_log(d, u.id, "trusted_contact.alert_not_configured", {
            "session_id": sid, "contact_id": c.id
        })
        return {
            "ok": False,
            "status": "provider_not_configured",
            "contact": c.name,
            "destination": c.phone,
            "message": "No external message was sent. Configure Twilio to enable real SMS delivery.",
        }

    import urllib.parse
    import urllib.request

    endpoint = f"https://api.twilio.com/2010-04-01/Accounts/{sid_env}/Messages.json"
    data = urllib.parse.urlencode({"From": sender, "To": c.phone, "Body": msg}).encode()
    req = urllib.request.Request(endpoint, data=data)
    req.add_header(
        "Authorization",
        "Basic " + base64.b64encode(f"{sid_env}:{token}".encode()).decode(),
    )

    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            body = json.loads(resp.read().decode())
        audit_log(d, u.id, "trusted_contact.alert_sent", {
            "session_id": sid, "contact_id": c.id,
            "provider": "twilio", "message_sid": body.get("sid"),
        })
        return {
            "ok": True,
            "status": "sent",
            "provider": "twilio",
            "contact": c.name,
            "destination": c.phone,
            "message_sid": body.get("sid"),
        }
    except Exception as exc:
        audit_log(d, u.id, "trusted_contact.alert_failed", {
            "session_id": sid, "contact_id": c.id,
            "provider": "twilio", "error": str(exc)[:300],
        })
        return {
            "ok": False,
            "status": "delivery_failed",
            "provider": "twilio",
            "error": "SMS provider rejected or could not deliver the message.",
        }


# ─── Evidence ────────────────────────────────────────────────────

@router.post("/sessions/{sid}/evidence")
def generate_evidence(sid: int, u=Depends(current_user), d: DBSession = Depends(db)):
    """Generate tamper-evident evidence report with SHA-256 hash chain."""
    s = d.get(Session, sid)
    if not s or s.user_id != u.id:
        raise HTTPException(404, "Session not found")

    events = []
    previous = "0" * 64  # Genesis hash

    for e in s.events:
        body = {
            "timestamp": e.created_at.isoformat() if e.created_at else "",
            "transcript": e.transcript,
            "risk_level": e.risk_level,
            "score": e.fused_score,
            "reasons": e.reasons,
        }
        # Hash: SHA256(previous_hash + ":" + event_data)
        material = previous + ":" + json.dumps(body, sort_keys=True, separators=(",", ":"))
        current = hashlib.sha256(material.encode()).hexdigest()
        events.append({**body, "previous_hash": previous, "hash": current})
        previous = current

    payload = {
        "session_id": sid,
        "risk_level": s.risk_level,
        "risk_score": s.risk_score,
        "generated_at": s.updated_at.isoformat() if s.updated_at else s.created_at.isoformat() if s.created_at else "",
        "integrity": "VALID",
        "head_hash": previous,
        "events": events,
        "note": "Generated from consented session events. Human verification is required.",
    }

    r = EvidenceReport(session_id=sid, payload=payload)
    d.add(r)
    d.commit()
    d.refresh(r)
    audit_log(d, u.id, "evidence.generated", {
        "session_id": sid, "report_id": r.id, "head_hash": previous,
    })
    return {"id": r.id, "report": payload}


@router.get("/sessions/{sid}/evidence")
def get_evidence(sid: int, u=Depends(current_user), d: DBSession = Depends(db)):
    """Retrieve latest evidence report for a session."""
    s = d.get(Session, sid)
    if not s or s.user_id != u.id:
        raise HTTPException(404, "Session not found")

    report = (
        d.query(EvidenceReport)
        .filter_by(session_id=sid)
        .order_by(EvidenceReport.id.desc())
        .first()
    )
    if not report:
        raise HTTPException(404, "No evidence report generated yet")
    return {"id": report.id, "report": report.payload}


# ─── Stats ───────────────────────────────────────────────────────

@router.get("/stats")
def stats(u=Depends(current_user), d: DBSession = Depends(db)):
    return {
        "sessions": d.query(Session).filter_by(user_id=u.id).count(),
        "events": d.query(RiskEvent).join(Session).filter(Session.user_id == u.id).count(),
        "contacts": d.query(TrustedContact).filter_by(user_id=u.id).count(),
        "reports": d.query(EvidenceReport).join(Session).filter(Session.user_id == u.id).count(),
    }


# ─── WebSocket Real-Time ─────────────────────────────────────────

async def broadcast(sid: int, payload: dict):
    """Broadcast risk update to all WebSocket clients for a session."""
    for ws in list(rooms.get(sid, set())):
        try:
            await ws.send_json(payload)
        except Exception:
            rooms[sid].discard(ws)


@router.websocket("/ws/sessions/{sid}")
@router.websocket("/sessions/{sid}/ws")
async def ws_session(websocket: WebSocket, sid: int):
    """
    WebSocket endpoint for real-time session monitoring.
    Integrates with Gemini Live when available, falls back to
    local unified pipeline for text-only analysis.
    """
    await websocket.accept()
    rooms.setdefault(sid, set()).add(websocket)
    client = None
    live_session = None
    last_transcript = ""
    pipeline = get_pipeline()

    try:
        # Try connecting Gemini Live
        try:
            client, live_session = await gemini_connect()
            await websocket.send_json({"type": "ai_status", "status": "connected", "model": MODEL})
        except GeminiLiveUnavailable as exc:
            await websocket.send_json({
                "type": "ai_status",
                "status": "unavailable",
                "reason": str(exc),
                "fallback": "local-neural-pipeline",
            })

        async def receiver():
            nonlocal last_transcript
            while True:
                raw = await websocket.receive_json()
                typ = raw.get("type")

                if typ == "text":
                    text = str(raw.get("text", "")).strip()
                    if not text:
                        continue
                    last_transcript = text

                    if live_session:
                        await live_session.send_realtime_input(text=text)
                    else:
                        # No Gemini Live — use unified pipeline directly
                        result = pipeline.analyze(
                            transcript=text,
                            session_id=str(sid),
                        )
                        api_result = result.to_api_dict()
                        await websocket.send_json({"type": "risk_update", "event_id": None, **api_result})

                        # Persist to DB with own session
                        from ..db import SessionLocal
                        with SessionLocal() as db_sess:
                            s = db_sess.get(Session, sid)
                            if s:
                                e = RiskEvent(
                                    session_id=sid,
                                    transcript=text,
                                    conversation_score=result.conversation_score,
                                    visual_score=result.visual_score,
                                    liveness_score=result.liveness_score,
                                    fused_score=result.fused_score,
                                    risk_level=result.risk_level,
                                    reasons=result.reasons,
                                )
                                db_sess.add(e)
                                s.risk_level = result.risk_level
                                s.risk_score = result.fused_score
                                db_sess.commit()

                elif typ == "audio_pcm16":
                    data = base64.b64decode(raw.get("data", ""))
                    if data and live_session:
                        from google.genai import types
                        await live_session.send_realtime_input(
                            audio=types.Blob(data=data, mime_type="audio/pcm;rate=16000")
                        )

                elif typ == "video_jpeg":
                    data = base64.b64decode(raw.get("data", ""))
                    if data and live_session:
                        from google.genai import types
                        await live_session.send_realtime_input(
                            video=types.Blob(data=data, mime_type="image/jpeg")
                        )

                elif typ == "ping":
                    await websocket.send_json({"type": "pong"})

        async def sender():
            if not live_session:
                # Block forever (receiver handles everything for local mode)
                await asyncio.Event().wait()
                return

            async for response in live_session.receive():
                content = getattr(response, "server_content", None)
                if not content:
                    continue

                inp = getattr(content, "input_transcription", None)
                if inp and getattr(inp, "text", None):
                    text = inp.text.strip()
                    await websocket.send_json({"type": "transcript", "text": text})

                    if text:
                        nonlocal last_transcript
                        last_transcript = text
                        result = pipeline.analyze(
                            transcript=text,
                            session_id=str(sid),
                        )
                        api_result = result.to_api_dict()
                        await websocket.send_json({"type": "risk_update", "event_id": None, **api_result})

                turn = getattr(content, "model_turn", None)
                if turn:
                    for part in getattr(turn, "parts", []) or []:
                        txt = getattr(part, "text", None)
                        if not txt:
                            continue
                        parsed = parse_json(txt)
                        if parsed:
                            # Gemini returned structured analysis — run through pipeline too
                            result = pipeline.analyze(
                                transcript=last_transcript,
                                session_id=str(sid),
                                visual_score=float(parsed.get("visual_score", 0.15) or 0.15),
                                liveness_score=float(parsed.get("liveness_confidence", 0.85) or 0.85),
                            )
                            api_result = result.to_api_dict()

                            # Persist with own DB session (fixes lifecycle bug)
                            from ..db import SessionLocal
                            with SessionLocal() as db_sess:
                                s = db_sess.get(Session, sid)
                                if s:
                                    e = RiskEvent(
                                        session_id=sid,
                                        transcript=last_transcript,
                                        conversation_score=result.conversation_score,
                                        visual_score=result.visual_score,
                                        liveness_score=result.liveness_score,
                                        fused_score=result.fused_score,
                                        risk_level=result.risk_level,
                                        reasons=result.reasons,
                                    )
                                    db_sess.add(e)
                                    s.risk_level = result.risk_level
                                    s.risk_score = result.fused_score
                                    db_sess.commit()
                                    api_result["event_id"] = e.id

                            await websocket.send_json({
                                "type": "ai_analysis",
                                "analysis": parsed,
                                "risk_update": api_result,
                            })

        recv_task = asyncio.create_task(receiver())
        send_task = asyncio.create_task(sender())
        done, pending = await asyncio.wait(
            {recv_task, send_task}, return_when=asyncio.FIRST_COMPLETED
        )
        for task in pending:
            task.cancel()

    except WebSocketDisconnect:
        pass
    except Exception as exc:
        logger.error(f"WebSocket error for session {sid}: {exc}")
    finally:
        rooms.get(sid, set()).discard(websocket)
        if client:
            try:
                await client.aio.aclose()
            except Exception:
                pass
