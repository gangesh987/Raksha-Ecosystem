import os, json, re, base64
from google import genai
from google.genai import types

MODEL = os.getenv('GEMINI_LIVE_MODEL', 'gemini-3.1-flash-live-preview')
SYSTEM = '''You are RakshaCall's real-time scam-risk analyst. Analyze the latest caller/user conversation content only for safety signals. This is a safety classifier, not a legal authority. Conversation/coercion is primary. Return ONLY one compact JSON object, no markdown, with keys: risk_score (0..1), confidence (0..1), tactics (array of strings from AUTHORITY, FEAR, URGENCY, ISOLATION, PAYMENT, CREDENTIAL, REMOTE_ACCESS, SUSPICIOUS_LINK, ESCALATION), stage (CONTACT, AUTHORITY, FEAR, ISOLATION, DEMAND, PAYMENT_CREDENTIAL, ESCALATION), manipulation_velocity (LOW, MODERATE, HIGH), irreversible_action (boolean), reasons (array of short strings), visual_score (0..1, supporting only), liveness_confidence (0..1 or null; never claim biometric certainty), recommended_action (one short sentence). Be conservative: ordinary mentions of banks/police without coercion are not automatically scams. Never claim certainty or identify a person as a criminal.'''

class GeminiLiveUnavailable(RuntimeError):
    pass

def enabled():
    return bool(os.getenv('GEMINI_API_KEY'))

async def connect():
    if not enabled():
        raise GeminiLiveUnavailable('GEMINI_API_KEY is not configured')
    client = genai.Client(api_key=os.environ['GEMINI_API_KEY'])
    config = types.LiveConnectConfig(
        response_modalities=['TEXT'],
        system_instruction=SYSTEM,
        input_audio_transcription=types.AudioTranscriptionConfig(),
    )
    return client, await client.aio.live.connect(model=MODEL, config=config)

def parse_json(text: str):
    if not text:
        return None
    text=text.strip()
    try:
        return json.loads(text)
    except Exception:
        m=re.search(r'\{.*\}', text, re.S)
        if not m: return None
        try: return json.loads(m.group(0))
        except Exception: return None

async def handle_live_websocket(websocket, on_text=None):
    client=None
    try:
        client, session = await connect()
        await websocket.send_json({'type':'ai_status','status':'connected','model':MODEL})
        async for response in session.receive():
            content=getattr(response,'server_content',None)
            if not content: continue
            inp=getattr(content,'input_transcription',None)
            if inp and getattr(inp,'text',None):
                await websocket.send_json({'type':'transcript','text':inp.text})
            turn=getattr(content,'model_turn',None)
            if turn:
                for part in getattr(turn,'parts',[]) or []:
                    txt=getattr(part,'text',None)
                    if not txt: continue
                    await websocket.send_json({'type':'ai_analysis_raw','text':txt})
                    parsed=parse_json(txt)
                    if parsed: await websocket.send_json({'type':'ai_analysis','analysis':parsed})
    finally:
        if client:
            try: await client.aio.aclose()
            except Exception: pass
