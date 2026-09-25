import json, asyncio, hashlib, os
from fastapi import APIRouter, Depends, HTTPException, WebSocket, WebSocketDisconnect
from sqlalchemy.orm import Session as DBSession
from ..auth import db,current_user
from ..models import User,Session,RiskEvent,TrustedContact,EvidenceReport,AuditEvent
from ..schemas import SessionCreate,Analyze,ContactCreate
from ..ai.provider import provider
from ..ai.gemini_live import connect as gemini_connect, GeminiLiveUnavailable, parse_json, MODEL

router=APIRouter(prefix="/api",tags=["protection"])
rooms={}

def log(d,user_id,action,meta={}):
    d.add(AuditEvent(user_id=user_id,action=action,metadata_json=meta));d.commit()

@router.post("/sessions")
def create(p:SessionCreate,u=Depends(current_user),d:DBSession=Depends(db)):
    s=Session(user_id=u.id,source=p.source);d.add(s);d.commit();d.refresh(s);log(d,u.id,"session.created",{"session_id":s.id})
    return {"id":s.id,"risk_level":s.risk_level,"risk_score":s.risk_score,"status":s.status}

@router.get("/sessions")
def list_sessions(u=Depends(current_user),d:DBSession=Depends(db)):
    xs=d.query(Session).filter_by(user_id=u.id).order_by(Session.id.desc()).limit(50).all()
    return [{"id":x.id,"risk_level":x.risk_level,"risk_score":x.risk_score,"status":x.status,"created_at":x.created_at.isoformat()} for x in xs]

@router.post("/sessions/{sid}/analyze")
async def analyze(sid:int,p:Analyze,u=Depends(current_user),d:DBSession=Depends(db)):
    s=d.get(Session,sid)
    if not s or s.user_id!=u.id: raise HTTPException(404,"Session not found")
    result=provider.score(p.transcript,p.visual_score,p.liveness_score)
    e=RiskEvent(session_id=sid,transcript=p.transcript,**{k:result[k] for k in ["conversation_score","visual_score","liveness_score","fused_score","risk_level"]},reasons=result["reasons"])
    d.add(e);s.risk_level=result["risk_level"];s.risk_score=result["fused_score"];d.commit();d.refresh(e)
    await broadcast(sid,{"type":"risk_update","event_id":e.id,**result})
    log(d,u.id,"risk.analyzed",{"session_id":sid,"risk":result["risk_level"]})
    return result|{"event_id":e.id}

@router.get("/sessions/{sid}")
def detail(sid:int,u=Depends(current_user),d:DBSession=Depends(db)):
    s=d.get(Session,sid)
    if not s or s.user_id!=u.id: raise HTTPException(404,"Session not found")
    return {"id":s.id,"status":s.status,"risk_level":s.risk_level,"risk_score":s.risk_score,
            "events":[{"id":e.id,"transcript":e.transcript,"risk_level":e.risk_level,"fused_score":e.fused_score,"reasons":e.reasons,"created_at":e.created_at.isoformat()} for e in s.events]}

@router.post("/sessions/{sid}/warning")
def warning(sid:int,u=Depends(current_user),d:DBSession=Depends(db)):
    s=d.get(Session,sid)
    if not s or s.user_id!=u.id: raise HTTPException(404,"Session not found")
    log(d,u.id,"warning.triggered",{"session_id":sid})
    return {"ok":True,"message":"Pause and verify the caller independently. Never transfer money under pressure.","risk_level":s.risk_level}

@router.get("/contacts")
def contacts(u=Depends(current_user),d:DBSession=Depends(db)):
    return [{"id":c.id,"name":c.name,"phone":c.phone,"consent_enabled":c.consent_enabled} for c in d.query(TrustedContact).filter_by(user_id=u.id).all()]

@router.post("/contacts")
def add_contact(p:ContactCreate,u=Depends(current_user),d:DBSession=Depends(db)):
    c=TrustedContact(user_id=u.id,**p.model_dump());d.add(c);d.commit();d.refresh(c);return {"id":c.id,"name":c.name,"phone":c.phone,"consent_enabled":c.consent_enabled}

@router.post("/sessions/{sid}/trusted-alert")
def alert(sid:int,u=Depends(current_user),d:DBSession=Depends(db)):
    s=d.get(Session,sid)
    if not s or s.user_id!=u.id: raise HTTPException(404,"Session not found")
    c=d.query(TrustedContact).filter_by(user_id=u.id,consent_enabled=True).first()
    if not c: raise HTTPException(400,"No consent-enabled trusted contact")
    msg=f"RakshaCall Alert: I am currently in a call flagged as high-risk coercive scam (session {sid}). Please call or message me."
    sid_env=os.getenv("TWILIO_ACCOUNT_SID"); token=os.getenv("TWILIO_AUTH_TOKEN"); sender=os.getenv("TWILIO_FROM_NUMBER")
    if not (sid_env and token and sender):
        return {"ok":False,"status":"provider_not_configured","contact":c.name,"destination":c.phone,
                "message":"No external message was sent. Configure Twilio to enable real SMS delivery."}
    import urllib.parse, urllib.request
    endpoint=f"https://api.twilio.com/2010-04-01/Accounts/{sid_env}/Messages.json"
    data=urllib.parse.urlencode({"From":sender,"To":c.phone,"Body":msg}).encode()
    req=urllib.request.Request(endpoint,data=data)
    import base64
    req.add_header("Authorization","Basic "+base64.b64encode(f"{sid_env}:{token}".encode()).decode())
    try:
        with urllib.request.urlopen(req,timeout=10) as resp:
            body=json.loads(resp.read().decode())
        log(d,u.id,"trusted_contact.alert_sent",{"session_id":sid,"contact_id":c.id,"provider":"twilio","message_sid":body.get("sid")})
        return {"ok":True,"status":"sent","provider":"twilio","contact":c.name,"destination":c.phone,"message_sid":body.get("sid")}
    except Exception as exc:
        log(d,u.id,"trusted_contact.alert_failed",{"session_id":sid,"contact_id":c.id,"provider":"twilio","error":str(exc)[:300]})
        return {"ok":False,"status":"delivery_failed","provider":"twilio","error":"SMS provider rejected or could not deliver the message."}

@router.post("/sessions/{sid}/evidence")
def evidence(sid:int,u=Depends(current_user),d:DBSession=Depends(db)):
    s=d.get(Session,sid)
    if not s or s.user_id!=u.id: raise HTTPException(404,"Session not found")
    events=[]; previous="0"*64
    for e in s.events:
        body={"timestamp":e.created_at.isoformat(),"transcript":e.transcript,"risk_level":e.risk_level,"score":e.fused_score,"reasons":e.reasons}
        material=previous+json.dumps(body,sort_keys=True,separators=(",",":"))
        current=hashlib.sha256(material.encode()).hexdigest()
        events.append(body|{"previous_hash":previous,"hash":current})
        previous=current
    payload={"session_id":sid,"risk_level":s.risk_level,"risk_score":s.risk_score,
             "generated_at":s.updated_at.isoformat(),"integrity":"VALID","head_hash":previous,
             "events":events,"note":"Generated from consented session events. Human verification is required."}
    r=EvidenceReport(session_id=sid,payload=payload);d.add(r);d.commit();d.refresh(r);log(d,u.id,"evidence.generated",{"session_id":sid,"report_id":r.id,"head_hash":previous})
    return {"id":r.id,"report":payload}

@router.get("/stats")
def stats(u=Depends(current_user),d:DBSession=Depends(db)):
    return {"sessions":d.query(Session).filter_by(user_id=u.id).count(),
            "events":d.query(RiskEvent).join(Session).filter(Session.user_id==u.id).count(),
            "contacts":d.query(TrustedContact).filter_by(user_id=u.id).count(),
            "reports":d.query(EvidenceReport).join(Session).filter(Session.user_id==u.id).count()}

async def broadcast(sid,payload):
    for ws in list(rooms.get(sid,set())):
        try: await ws.send_json(payload)
        except: rooms[sid].discard(ws)

@router.websocket("/ws/sessions/{sid}")
async def ws(websocket:WebSocket,sid:int):
    await websocket.accept()
    # The browser/mobile client owns consent. This socket only processes data explicitly sent by it.
    rooms.setdefault(sid,set()).add(websocket)
    client=None
    live_session=None
    last_transcript=""
    try:
        try:
            client, live_session = await gemini_connect()
            await websocket.send_json({"type":"ai_status","status":"connected","model":MODEL})
        except GeminiLiveUnavailable as exc:
            await websocket.send_json({"type":"ai_status","status":"unavailable","reason":str(exc)})

        async def receiver():
            nonlocal live_session
            while True:
                raw=await websocket.receive_json()
                typ=raw.get("type")
                if typ=="text":
                    text=str(raw.get("text","")).strip()
                    if text and live_session:
                        await live_session.send_realtime_input(text=text)
                elif typ=="audio_pcm16":
                    import base64
                    data=base64.b64decode(raw.get("data",""))
                    if data and live_session:
                        from google.genai import types
                        await live_session.send_realtime_input(audio=types.Blob(data=data,mime_type="audio/pcm;rate=16000"))
                elif typ=="video_jpeg":
                    import base64
                    data=base64.b64decode(raw.get("data",""))
                    if data and live_session:
                        from google.genai import types
                        await live_session.send_realtime_input(video=types.Blob(data=data,mime_type="image/jpeg"))
                elif typ=="ping":
                    await websocket.send_json({"type":"pong"})

        async def sender():
            if not live_session: return
            async for response in live_session.receive():
                content=getattr(response,"server_content",None)
                if not content: continue
                inp=getattr(content,"input_transcription",None)
                if inp and getattr(inp,"text",None):
                    await websocket.send_json({"type":"transcript","text":inp.text})
                    # Persist/score every final transcript fragment through the same deterministic safety layer.
                    text=inp.text.strip()
                    last_transcript=text
                    if text:
                        result=provider.score(text,0.15,0.85)
                        await websocket.send_json({"type":"risk_update","event_id":None,**result})
                turn=getattr(content,"model_turn",None)
                if turn:
                    for part in getattr(turn,"parts",[]) or []:
                        txt=getattr(part,"text",None)
                        if not txt: continue
                        parsed=parse_json(txt)
                        if parsed:
                            result=provider.score(last_transcript,float(parsed.get("visual_score",0.15) or 0.15),float(parsed.get("liveness_confidence",0.85) or 0.85),ai=parsed)
                            s=d.get(Session,sid)
                            if s:
                                e=RiskEvent(session_id=sid,transcript=last_transcript,**{k:result[k] for k in ["conversation_score","visual_score","liveness_score","fused_score","risk_level"]},reasons=result["reasons"])
                                d.add(e); s.risk_level=result["risk_level"]; s.risk_score=result["fused_score"]; d.commit(); d.refresh(e)
                                result["event_id"]=e.id
                            await websocket.send_json({"type":"ai_analysis","analysis":parsed,"risk_update":result})

        import asyncio
        recv_task=asyncio.create_task(receiver())
        send_task=asyncio.create_task(sender())
        done,pending=await asyncio.wait({recv_task,send_task},return_when=asyncio.FIRST_COMPLETED)
        for task in pending: task.cancel()
    except WebSocketDisconnect:
        pass
    finally:
        rooms.get(sid,set()).discard(websocket)
        if client:
            try: await client.aio.aclose()
            except Exception: pass
