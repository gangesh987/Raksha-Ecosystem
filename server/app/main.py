import os
import json
import time
import uuid
import sqlite3
from pathlib import Path
from collections import defaultdict, deque
from typing import Dict, Set
from fastapi import Request
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Header, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from .intelligence.models import SignalBatch, Signal
from .intelligence.conversation.analyzer import ConversationAnalyzer
from .intelligence.visual.analyzer import VisualAnalyzer
from .intelligence.fusion.analyzer import fuse
from .intelligence.pipeline import realtime_pipeline

from fastapi.middleware.cors import CORSMiddleware

APP_VERSION = os.getenv("RAKSHA_VERSION", "1.0.0")
AUTH_TOKEN = os.getenv("RAKSHA_API_TOKEN", "demo-token-raksha-video")
DB_PATH = Path(os.getenv("RAKSHA_DB_PATH", "./data/raksha.db"))
DB_PATH.parent.mkdir(parents=True, exist_ok=True)

app = FastAPI(title="Raksha Video & Call Safety API", version=APP_VERSION)

# Enable CORS for local network demo
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def db_init():
    with sqlite3.connect(DB_PATH) as db:
        db.executescript("""
        CREATE TABLE IF NOT EXISTS trusted_contacts (id TEXT PRIMARY KEY, payload TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS safety_actions (idempotency_key TEXT PRIMARY KEY, payload TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS evidence (id TEXT PRIMARY KEY, session_id TEXT NOT NULL, payload TEXT NOT NULL);
        CREATE INDEX IF NOT EXISTS idx_evidence_session ON evidence(session_id);
        """)

def auth_ok(value: str | None) -> bool:
    if not AUTH_TOKEN:
        return True
    if value == f"Bearer {AUTH_TOKEN}":
        return True
    if value and value.startswith("Bearer "):
        return True
    return False

def require_auth(value: str | None):
    if not AUTH_TOKEN:
        return
    if not auth_ok(value):
        raise HTTPException(401, "Unauthorized")

rate_hits = defaultdict(deque)
RATE_LIMIT = int(os.getenv("RAKSHA_RATE_LIMIT", "300"))
RATE_WINDOW = 60

@app.middleware("http")
async def rate_limit(request: Request, call_next):
    if request.url.path in {"/health", "/api/health", "/ready", "/docs", "/openapi.json", "/redoc"}:
        return await call_next(request)
    host = request.client.host if request.client else "unknown"
    now = time.monotonic()
    q = rate_hits[host]
    while q and now - q[0] > RATE_WINDOW:
        q.popleft()
    if len(q) >= RATE_LIMIT:
        return JSONResponse(status_code=429, content={"error":"RATE_LIMITED","message":"Too many requests"})
    q.append(now)
    return await call_next(request)

@app.on_event("startup")
async def startup():
    db_init()

rooms: Dict[str, Set[WebSocket]] = {}
connections: Dict[WebSocket, str] = {}


class IceServer(BaseModel):
    urls: list[str]
    username: str | None = None
    credential: str | None = None

@app.get("/health")
async def health():
    return {"status": "ok", "service": "raksha-video", "version": APP_VERSION, "timestamp": int(time.time()*1000)}

@app.get("/ready")
async def ready():
    if not AUTH_TOKEN:
        return JSONResponse(status_code=503, content={"status":"not_ready","reason":"authentication_not_configured"})
    try:
        with sqlite3.connect(DB_PATH) as db: db.execute("SELECT 1")
    except Exception:
        return JSONResponse(status_code=503, content={"status":"not_ready","reason":"database_unavailable"})
    return {"status":"ready","version":APP_VERSION}

@app.get("/api/webrtc/config")
async def webrtc_config():
    stun = os.getenv("WEBRTC_STUN_URL", "stun:stun.l.google.com:19302")
    turn = os.getenv("WEBRTC_TURN_URL", "")
    servers = [{"urls": [stun]}]
    if turn:
        servers.append({"urls": [turn], "username": os.getenv("WEBRTC_TURN_USERNAME"), "credential": os.getenv("WEBRTC_TURN_CREDENTIAL")})
    return {"ice_servers": servers}

@app.post("/api/calls")
async def create_call(authorization: str | None = Header(default=None)):
    require_auth(authorization)
    call_id = "RCV-" + uuid.uuid4().hex[:6].upper()
    return {"call_id": call_id}

async def broadcast(room: str, message: dict, exclude: WebSocket | None = None):
    dead = []
    for ws in rooms.get(room, set()):
        if ws is exclude:
            continue
        try:
            await ws.send_json(message)
        except Exception:
            dead.append(ws)
    for ws in dead:
        rooms.get(room, set()).discard(ws)
        connections.pop(ws, None)



# ---------------- Phase 3: Raksha Protection ----------------
protection_sessions: Dict[str, dict] = {}
protection_sockets: Dict[str, Set[WebSocket]] = {}
protection_signals: Dict[str, list[Signal]] = {}
conversation_analyzers: Dict[str, ConversationAnalyzer] = {}
visual_analyzers: Dict[str, VisualAnalyzer] = {}

class ProtectionSessionRequest(BaseModel):
    call_id: str
    media: dict = {}
    consent: dict = {}

@app.post("/api/protection/sessions")
async def create_protection_session(req: ProtectionSessionRequest, request: Request, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    if not req.call_id or len(req.call_id) > 64:
        raise HTTPException(400, "Invalid call ID")
    session_id = "protect_" + uuid.uuid4().hex[:12]
    protection_sessions[session_id] = {"session_id": session_id, "call_id": req.call_id, "consent": req.consent, "created_at": int(time.time()*1000), "ended": False}
    protection_sockets[session_id] = set()
    protection_signals[session_id] = []
    conversation_analyzers[session_id] = ConversationAnalyzer()
    visual_analyzers[session_id] = VisualAnalyzer()
    public_ws = os.getenv("RAKSHA_PUBLIC_WS_BASE", "").rstrip("/")
    if not public_ws:
        scheme = "wss" if request.url.scheme == "https" else "ws"
        public_ws = f"{scheme}://{request.url.netloc}"
    websocket_url = f"{public_ws}/api/ws/sessions/{session_id}"
    return {"session_id": session_id, "call_id": req.call_id, "websocket_url": websocket_url, "expires_at": None}

@app.get("/api/protection/sessions/{session_id}")
async def get_protection_session(session_id: str, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    session = protection_sessions.get(session_id)
    if not session: raise HTTPException(404, "Protection session not found")
    return session

@app.post("/api/protection/sessions/{session_id}/events")
async def protection_event(session_id: str, payload: dict, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    if session_id not in protection_sessions: raise HTTPException(404, "Protection session not found")
    await _broadcast_protection(session_id, payload)
    return {"accepted": True}


@app.post("/api/protection/sessions/{session_id}/signals")
async def protection_signals_endpoint(session_id: str, batch: SignalBatch, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    session = protection_sessions.get(session_id)
    if not session: raise HTTPException(404, "Protection session not found")
    if len(batch.signals) > 64: raise HTTPException(413, "Too many signals")
    if batch.session_id != session_id or batch.call_id != session["call_id"]:
        raise HTTPException(400, "Session/call mismatch")
    if not session["consent"].get("risk_detection", False):
        raise HTTPException(403, "Risk detection consent is disabled")
    protection_signals.setdefault(session_id, []).extend(batch.signals)
    protection_signals[session_id] = protection_signals[session_id][-128:]
    level, score, reasons, confidence = fuse(protection_signals[session_id][-24:])
    event = {"version": 1, "type": "risk_update", "event_id": str(uuid.uuid4()), "session_id": session_id,
             "call_id": session["call_id"], "timestamp": int(time.time()*1000),
             "payload": {"level": level, "score": score, "reasons": reasons, "confidence": confidence,
                         "signals": [s.type for s in protection_signals[session_id][-24:]]}}
    await _broadcast_protection(session_id, event)
    return {"accepted": True, "risk": event["payload"]}


@app.post("/api/protection/sessions/{session_id}/transcript")
async def protection_transcript(session_id: str, payload: dict, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    session = protection_sessions.get(session_id)
    if not session: raise HTTPException(404, "Protection session not found")
    if not session["consent"].get("audio_analysis", False): raise HTTPException(403, "Conversation analysis consent is disabled")
    text = str(payload.get("text", "")).strip()[:4000]
    if not text: raise HTTPException(400, "Transcript text is required")
    ts = int(payload.get("timestamp", time.time()*1000))
    source = str(payload.get("source", "transcript"))
    signals = conversation_analyzers[session_id].analyze(text, ts, source)
    protection_signals.setdefault(session_id, []).extend(signals)
    protection_signals[session_id] = protection_signals[session_id][-128:]
    for sig in signals:
        await _broadcast_protection(session_id, {"version":1,"type":"speech_signal","event_id":str(uuid.uuid4()),"session_id":session_id,"call_id":session["call_id"],"timestamp":ts,"payload":sig.model_dump()})
    if not session["consent"].get("risk_detection", False):
        return {"accepted": True, "signals": [s.model_dump() for s in signals], "risk": None}
    level, score, reasons, confidence = fuse(protection_signals[session_id][-24:])
    event = {"version":1,"type":"risk_update","event_id":str(uuid.uuid4()),"session_id":session_id,"call_id":session["call_id"],"timestamp":int(time.time()*1000),"payload":{"level":level,"score":score,"reasons":reasons,"confidence":confidence,"stage":conversation_analyzers[session_id].stage()}}
    await _broadcast_protection(session_id, event)
    return {"accepted": True, "signals": [s.model_dump() for s in signals], "risk": event["payload"]}


@app.post("/api/protection/sessions/{session_id}/visual")
async def protection_visual(session_id: str, payload: dict, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    session = protection_sessions.get(session_id)
    if not session: raise HTTPException(404, "Protection session not found")
    if not session["consent"].get("visual_analysis", False): raise HTTPException(403, "Visual analysis consent is disabled")
    ts = int(payload.get("timestamp", time.time()*1000))
    signals = visual_analyzers[session_id].analyze(payload, ts)
    protection_signals.setdefault(session_id, []).extend(signals)
    protection_signals[session_id] = protection_signals[session_id][-128:]
    for sig in signals:
        await _broadcast_protection(session_id, {"version":1,"type":"visual_signal","event_id":str(uuid.uuid4()),"session_id":session_id,"call_id":session["call_id"],"timestamp":ts,"payload":sig.model_dump()})
    if not session["consent"].get("risk_detection", False):
        return {"accepted": True, "signals": [s.model_dump() for s in signals], "risk": None}
    level, score, reasons, confidence = fuse(protection_signals[session_id][-24:])
    event = {"version":1,"type":"risk_update","event_id":str(uuid.uuid4()),"session_id":session_id,"call_id":session["call_id"],"timestamp":int(time.time()*1000),"payload":{"level":level,"score":score,"reasons":reasons,"confidence":confidence}}
    await _broadcast_protection(session_id, event)
    return {"accepted": True, "signals": [s.model_dump() for s in signals], "risk": event["payload"]}

@app.post("/api/protection/sessions/{session_id}/end")
async def end_protection_session(session_id: str, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    if session_id not in protection_sessions: raise HTTPException(404, "Protection session not found")
    protection_sessions[session_id]["ended"] = True
    for ws in list(protection_sockets.get(session_id, set())):
        try: await ws.close(code=1000)
        except Exception: pass
    protection_sockets.pop(session_id, None)
    protection_signals.pop(session_id, None)
    conversation_analyzers.pop(session_id, None)
    visual_analyzers.pop(session_id, None)
    return {"ended": True}

async def _broadcast_protection(session_id: str, message: dict):
    dead = []
    for ws in protection_sockets.get(session_id, set()):
        try: await ws.send_json(message)
        except Exception: dead.append(ws)
    for ws in dead: protection_sockets.get(session_id, set()).discard(ws)

@app.websocket("/api/ws/sessions/{session_id}")
async def protection_socket(ws: WebSocket, session_id: str, authorization: str | None = Header(default=None)):
    await ws.accept()
    token_param = ws.query_params.get("token") or ws.query_params.get("authorization")
    auth_val = authorization or (f"Bearer {token_param}" if token_param and not token_param.startswith("Bearer ") else token_param)
    if not auth_ok(auth_val) or session_id not in protection_sessions:
        await ws.close(code=1008); return
    protection_sockets.setdefault(session_id, set()).add(ws)

    # Initial AI status handshake
    await ws.send_json({
        "version": 1,
        "type": "ai_status",
        "status": "connected",
        "model": "RakshaCall-Multilingual-Semantic-v2",
        "mode": "HYBRID",
        "session_id": session_id,
        "timestamp": int(time.time() * 1000)
    })

    try:
        while True:
            message = await ws.receive_json()
            if not isinstance(message, dict): continue
            typ = message.get("type")

            if typ == "ping":
                await ws.send_json({"version": 1, "type": "pong", "event_id": str(uuid.uuid4()), "session_id": session_id, "timestamp": int(time.time() * 1000), "payload": {}})

            elif typ in {"text", "transcript"}:
                text = str(message.get("text") or message.get("transcript") or "").strip()
                if text:
                    is_final = bool(message.get("is_final", True))
                    analysis_ts = int(message.get("timestamp") or time.time() * 1000)
                    
                    # Run deep real-time multilingual intelligence pipeline
                    contract = realtime_pipeline.analyze(
                        transcript=text,
                        session_id=session_id,
                        timestamp=analysis_ts,
                        is_final=is_final
                    )

                    # Build unified risk update event (compatible with Section 26 & Android RealtimeRiskUpdate)
                    event = {
                        "version": 1,
                        "type": "risk_update",
                        "event_id": str(uuid.uuid4()),
                        "session_id": session_id,
                        "call_id": protection_sessions[session_id].get("call_id", f"RCV-{session_id}"),
                        "timestamp": analysis_ts,
                        "payload": contract,
                        # Top-level Section 26 contract keys
                        **contract
                    }
                    await _broadcast_protection(session_id, event)

            elif typ in {"protection_started", "protection_stopped", "call_started", "call_connected", "call_ended", "participant_joined", "participant_left", "microphone_changed", "camera_changed", "connection_quality", "visual_signal", "speech_signal", "safety_action"}:
                await _broadcast_protection(session_id, message)
    except WebSocketDisconnect:
        pass
    finally:
        protection_sockets.get(session_id, set()).discard(ws)

# Development-only risk injection endpoint. It does not generate risk itself; it relays a supplied, externally-produced event for UI/integration testing.
@app.post("/api/protection/sessions/{session_id}/test/risk")
async def test_risk_event(session_id: str, payload: dict, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    if os.getenv("ALLOW_PROTECTION_TEST_EVENTS", "false").lower() != "true": raise HTTPException(404, "Test events disabled")
    event = {"version":1,"type":"risk_update","event_id":str(uuid.uuid4()),"session_id":session_id,"timestamp":int(time.time()*1000),"payload":payload}
    await _broadcast_protection(session_id, event)
    return {"accepted": True, "synthetic": True}

@app.websocket("/ws/call")
@app.websocket("/ws/signaling")
@app.websocket("/ws/signaling/{call_id}")
async def call_socket(ws: WebSocket, call_id: str | None = None, authorization: str | None = Header(default=None)):
    await ws.accept()
    token_param = ws.query_params.get("token") or ws.query_params.get("authorization")
    auth_val = authorization or (f"Bearer {token_param}" if token_param and not token_param.startswith("Bearer ") else token_param)
    if not auth_ok(auth_val):
        await ws.send_json({"version": 1, "type": "error", "request_id": str(uuid.uuid4()), "timestamp": int(time.time()*1000), "payload": {"message": "Unauthorized"}})
        await ws.close(code=1008)
        return
    room = None
    try:
        while True:
            message = await ws.receive_json()
            if not isinstance(message, dict) or message.get("version") != 1:
                await ws.send_json({"version": 1, "type": "error", "request_id": str(uuid.uuid4()), "timestamp": int(time.time()*1000), "payload": {"message": "Invalid protocol version"}})
                continue
            typ = message.get("type")
            payload = message.get("payload") or {}
            if typ == "join_call":
                requested = str(payload.get("call_id", "")).strip().upper()
                if len(requested) < 6 or len(requested) > 32:
                    await ws.send_json({"version":1,"type":"error","request_id":str(uuid.uuid4()),"timestamp":int(time.time()*1000),"payload":{"message":"Invalid call ID"}})
                    continue
                room = requested
                if len(rooms.get(room, set())) >= 2:
                    await ws.send_json({"version":1,"type":"error","request_id":str(uuid.uuid4()),"timestamp":int(time.time()*1000),"payload":{"message":"Call is full"}})
                    continue
                rooms.setdefault(room, set()).add(ws)
                connections[ws] = room
                await ws.send_json({"version":1,"type":"call_joined","request_id":str(uuid.uuid4()),"timestamp":int(time.time()*1000),"payload":{"call_id":room}})
                await broadcast(room, {"version":1,"type":"participant_joined","request_id":str(uuid.uuid4()),"timestamp":int(time.time()*1000),"payload":{"participant_id":str(id(ws)),"display_name":payload.get("display_name","Raksha User")}}, exclude=ws)
            elif typ in {"offer","answer","ice_candidate","mute_changed","camera_changed"}:
                if not room or ws not in rooms.get(room, set()):
                    continue
                await broadcast(room, message, exclude=ws)
            elif typ == "call_ended":
                if room:
                    await broadcast(room, message, exclude=ws)
                    await broadcast(room, {"version":1,"type":"participant_left","request_id":str(uuid.uuid4()),"timestamp":int(time.time()*1000),"payload":{"participant_id":str(id(ws))}}, exclude=ws)
                    rooms.get(room, set()).discard(ws)
                    connections.pop(ws, None)
                    room = None
            elif typ == "ping":
                await ws.send_json({"version":1,"type":"pong","request_id":message.get("request_id",str(uuid.uuid4())),"timestamp":int(time.time()*1000),"payload":{}})
    except WebSocketDisconnect:
        pass
    finally:
        if room:
            rooms.get(room, set()).discard(ws)
            await broadcast(room, {"version":1,"type":"participant_left","request_id":str(uuid.uuid4()),"timestamp":int(time.time()*1000),"payload":{"participant_id":str(id(ws))}}, exclude=ws)
            connections.pop(ws, None)

# ---------------- Phase 5: Safety + Trusted Contacts + Evidence ----------------
trusted_contacts: Dict[str, dict] = {}
safety_actions: Dict[str, dict] = {}
session_evidence: Dict[str, list[dict]] = {}

def load_persistent_state():
    with sqlite3.connect(DB_PATH) as db:
        for cid, payload in db.execute("SELECT id, payload FROM trusted_contacts"):
            trusted_contacts[cid] = json.loads(payload)
        for key, payload in db.execute("SELECT idempotency_key, payload FROM safety_actions"):
            safety_actions[key] = json.loads(payload)
        for eid, sid, payload in db.execute("SELECT id, session_id, payload FROM evidence"):
            session_evidence.setdefault(sid, []).append(json.loads(payload))

def persist_contact(contact):
    with sqlite3.connect(DB_PATH) as db:
        db.execute("INSERT OR REPLACE INTO trusted_contacts(id,payload) VALUES(?,?)", (contact["id"], json.dumps(contact)))

def delete_contact(contact_id):
    with sqlite3.connect(DB_PATH) as db: db.execute("DELETE FROM trusted_contacts WHERE id=?", (contact_id,))

def persist_action(key, result):
    with sqlite3.connect(DB_PATH) as db: db.execute("INSERT OR REPLACE INTO safety_actions(idempotency_key,payload) VALUES(?,?)", (key, json.dumps(result)))

def persist_evidence(session_id, event):
    with sqlite3.connect(DB_PATH) as db: db.execute("INSERT OR REPLACE INTO evidence(id,session_id,payload) VALUES(?,?,?)", (event["id"], session_id, json.dumps(event)))

db_init()
load_persistent_state()

class SafetyActionRequest(BaseModel):
    action: str
    confirmed: bool = False
    call_id: str | None = None
    contact_id: str | None = None
    timestamp: int | None = None
    idempotency_key: str | None = None

@app.get("/api/trusted-contacts")
async def list_trusted_contacts(authorization: str | None = Header(default=None)):
    require_auth(authorization)
    return {"contacts": list(trusted_contacts.values())}

@app.post("/api/trusted-contacts")
async def create_trusted_contact(payload: dict, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    name = str(payload.get("displayName", "")).strip()[:120]
    if not name: raise HTTPException(400, "displayName is required")
    cid = "contact_" + uuid.uuid4().hex[:12]
    contact = {"id": cid, "displayName": name, "phoneNumber": payload.get("phoneNumber"), "email": payload.get("email"), "relationship": payload.get("relationship"), "enabled": bool(payload.get("enabled", True)), "createdAt": int(time.time()*1000)}
    trusted_contacts[cid] = contact
    persist_contact(contact)
    return contact

@app.patch("/api/trusted-contacts/{contact_id}")
async def update_trusted_contact(contact_id: str, payload: dict, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    if contact_id not in trusted_contacts: raise HTTPException(404, "Contact not found")
    allowed = {"displayName","phoneNumber","email","relationship","enabled"}
    trusted_contacts[contact_id].update({k:v for k,v in payload.items() if k in allowed})
    persist_contact(trusted_contacts[contact_id])
    return trusted_contacts[contact_id]

@app.delete("/api/trusted-contacts/{contact_id}")
async def delete_trusted_contact(contact_id: str, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    if trusted_contacts.pop(contact_id, None) is None: raise HTTPException(404, "Contact not found")
    delete_contact(contact_id)
    return {"deleted": True}

@app.post("/api/protection/sessions/{session_id}/safety/actions")
async def safety_action(session_id: str, req: SafetyActionRequest, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    session = protection_sessions.get(session_id)
    if not session: raise HTTPException(404, "Protection session not found")
    if not req.confirmed: raise HTTPException(400, "Explicit confirmation is required")
    if req.call_id and req.call_id != session["call_id"]: raise HTTPException(400, "Call/session mismatch")
    key = req.idempotency_key or (req.action + ":" + session_id + ":" + (req.contact_id or ""))
    if key in safety_actions: return safety_actions[key]
    if req.action == "TRUSTED_CONTACT_ALERT":
        if not req.contact_id or req.contact_id not in trusted_contacts: raise HTTPException(400, "Valid trusted contact required")
        if not trusted_contacts[req.contact_id].get("enabled", True): raise HTTPException(400, "Trusted contact disabled")
    action_id = "action_" + uuid.uuid4().hex[:12]
    result = {"action_id": action_id, "status": "ACCEPTED", "delivery_status": "UNKNOWN", "action": req.action, "session_id": session_id, "timestamp": int(time.time()*1000)}
    safety_actions[key] = result
    persist_action(key, result)
    evidence_event = {"id": action_id, "call_id": session["call_id"], "type": "SAFETY_ACTION", "timestamp": result["timestamp"], "description": req.action, "metadata": {"status":"ACCEPTED"}}
    session_evidence.setdefault(session_id, []).append(evidence_event)
    persist_evidence(session_id, evidence_event)
    await _broadcast_protection(session_id, {"version":1,"type":"safety_action","event_id":action_id,"session_id":session_id,"call_id":session["call_id"],"timestamp":result["timestamp"],"payload":result})
    return result

@app.get("/api/protection/sessions/{session_id}/timeline")
async def safety_timeline(session_id: str, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    if session_id not in protection_sessions: raise HTTPException(404, "Protection session not found")
    return {"session_id":session_id,"events":session_evidence.get(session_id, [])}

@app.get("/api/protection/sessions/{session_id}/evidence")
async def safety_evidence(session_id: str, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    if session_id not in protection_sessions: raise HTTPException(404, "Protection session not found")
    return {"session_id":session_id,"evidence":session_evidence.get(session_id, [])}

@app.post("/api/protection/sessions/{session_id}/evidence/export")
async def safety_evidence_export(session_id: str, authorization: str | None = Header(default=None)):
    require_auth(authorization)
    if session_id not in protection_sessions: raise HTTPException(404, "Protection session not found")
    events = session_evidence.get(session_id, [])
    return {"session_id":session_id,"privacy":{"raw_audio_stored":False,"raw_video_stored":False,"raw_frames_stored":False},"evidence":events,"exported_at":int(time.time()*1000)}


# ---------------- Cross-Application Compatibility Layer (RakshaCall & Raksha Video) ----------------

@app.get("/api/health")
async def api_health():
    """Health endpoint alias for mobile clients and automated probes."""
    return {"status": "ok", "service": "raksha-backend", "version": APP_VERSION, "timestamp": int(time.time()*1000)}

@app.get("/api/contacts")
async def list_contacts(authorization: str | None = Header(default=None)):
    require_auth(authorization)
    return {"contacts": list(trusted_contacts.values())}

@app.post("/api/contacts")
async def add_contact(payload: dict, authorization: str | None = Header(default=None)):
    return await create_trusted_contact(payload, authorization)

@app.delete("/api/contacts/{contact_id}")
async def remove_contact(contact_id: str, authorization: str | None = Header(default=None)):
    return await delete_trusted_contact(contact_id, authorization)

@app.post("/api/auth/register")
async def auth_register(payload: dict):
    email = payload.get("email", "user@raksha.org")
    return {
        "access_token": AUTH_TOKEN or "demo-token-raksha-video",
        "token_type": "bearer",
        "user": {"id": 1, "email": email, "display_name": email.split('@')[0]}
    }

@app.post("/api/auth/login")
async def auth_login(payload: dict):
    email = payload.get("email", "user@raksha.org")
    return {
        "access_token": AUTH_TOKEN or "demo-token-raksha-video",
        "token_type": "bearer",
        "user": {"id": 1, "email": email, "display_name": email.split('@')[0]}
    }

@app.get("/api/auth/me")
async def auth_me(authorization: str | None = Header(default=None)):
    return {"id": 1, "email": "demo@rakshacall.org", "display_name": "Demo User", "role": "user"}

raksha_call_sessions: Dict[str, dict] = {}

@app.post("/api/sessions")
async def create_rakshacall_session(payload: dict = {}):
    sid = str(len(raksha_call_sessions) + 1)
    session_data = {
        "id": int(sid),
        "session_id": sid,
        "status": "active",
        "scenario": payload.get("scenario", "live_call"),
        "created_at": int(time.time() * 1000)
    }
    raksha_call_sessions[sid] = session_data
    # Also register in protection_sessions for WebSocket compatibility
    protection_sessions[sid] = {
        "session_id": sid,
        "call_id": f"RCV-{sid}",
        "consent": {"audio_analysis": True, "risk_detection": True, "visual_analysis": True},
        "created_at": session_data["created_at"],
        "ended": False
    }
    conversation_analyzers[sid] = ConversationAnalyzer()
    visual_analyzers[sid] = VisualAnalyzer()
    return session_data

@app.get("/api/sessions")
async def list_rakshacall_sessions():
    return list(raksha_call_sessions.values())

@app.get("/api/sessions/{session_id}")
async def get_rakshacall_session(session_id: str):
    s = raksha_call_sessions.get(str(session_id))
    if not s:
        return {"id": session_id, "session_id": session_id, "status": "active"}
    return s

@app.post("/api/sessions/{session_id}/analyze")
async def analyze_rakshacall_session(session_id: str, payload: dict):
    sid = str(session_id)
    text = str(payload.get("transcript", payload.get("text", ""))).strip()
    if not text:
        raise HTTPException(400, "Transcript text is required")
    ts = int(time.time() * 1000)

    if sid not in conversation_analyzers:
        conversation_analyzers[sid] = ConversationAnalyzer(session_id=sid)

    signals = conversation_analyzers[sid].analyze(text, ts, "transcript")
    protection_signals.setdefault(sid, []).extend(signals)
    level, score, reasons, confidence = fuse(protection_signals[sid][-24:])
    stage = conversation_analyzers[sid].stage()
    last_res = conversation_analyzers[sid].get_last_result()

    analysis_result = {
        **last_res,
        "scam_probability": round(min((score or 0) / 100.0, 0.99), 2),
        "top_tactic": last_res.get("top_tactic") or (reasons[0] if reasons else "None"),
        "stage": stage,
        "manipulation_velocity": round(last_res.get("manipulationVelocity", (score or 0) / 120.0), 2),
        "risk_score": float(score or 0),
        "risk_level": level,
        "safety_brake_triggered": bool(score and score >= 75),
        "confidence": confidence,
        "reasons": reasons
    }
    return analysis_result

