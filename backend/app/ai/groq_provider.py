import os
import json
from ..config import settings

_groq_client = None

def get_client():
    global _groq_client
    if _groq_client is None:
        from groq import Groq
        api_key = settings.groq_api_key or os.getenv("GROQ_API_KEY")
        if api_key:
            _groq_client = Groq(api_key=api_key, max_retries=0, timeout=3.0)
    return _groq_client

def enabled() -> bool:
    return bool(settings.groq_api_key or os.getenv("GROQ_API_KEY"))

def analyze(text: str):
    client = get_client()
    if not client:
        return None

    model = settings.groq_model or os.getenv("GROQ_MODEL", "qwen/qwen3.8-27b")
    system_prompt = (
        "You are RakshaCall's real-time scam-risk analyst. Analyze caller/user conversation only for coercive scam tactics. "
        "Return ONLY a compact JSON object with keys: "
        "risk_score (0.0 to 1.0), confidence (0.0 to 1.0), tactics (array of strings from: AUTHORITY, FEAR, URGENCY, ISOLATION, PAYMENT, CREDENTIAL, REMOTE_ACCESS, SUSPICIOUS_LINK, ESCALATION), "
        "stage (CONTACT, AUTHORITY, FEAR, ISOLATION, DEMAND, PAYMENT_CREDENTIAL, CRITICAL_BRAKE), "
        "manipulation_velocity (LOW, MODERATE, HIGH), irreversible_action (boolean), "
        "reasons (array of short strings), recommended_action (short guidance sentence)."
    )

    try:
        completion = client.chat.completions.create(
            model=model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": f"Analyze conversation: {text}"}
            ],
            response_format={"type": "json_object"},
            temperature=0.1,
            max_tokens=250
        )
        content = completion.choices[0].message.content
        return json.loads(content)
    except Exception as e:
        print(f"[GroqProvider] Error during analysis: {e}")
        return None
