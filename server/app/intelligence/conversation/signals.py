import re
from ..models import Signal

PATTERNS = {
    "AUTHORITY_CLAIM": [r"police", r"bank manager", r"income tax", r"government", r"cyber crime", r"officer", r"official"],
    "URGENCY": [r"immediately", r"right now", r"urgent", r"within \d+ minutes", r"don't wait", r"act now"],
    "THREAT": [r"arrest", r"jail", r"legal action", r"account.*block", r"case.*file", r"consequences"],
    "ISOLATION": [r"don't tell", r"keep.*secret", r"don't.*call anyone", r"stay on the line"],
    "FINANCIAL_REQUEST": [r"transfer", r"send.*money", r"pay.*fee", r"refund", r"upi", r"bank account", r"card details"],
    "OTP_REQUEST": [r"otp", r"one[- ]time password", r"verification code"],
    "PASSWORD_REQUEST": [r"password", r"passcode", r"pin"],
    "CREDENTIAL_REQUEST": [r"login", r"username", r"credentials", r"cvv"],
    "REMOTE_ACCESS_REQUEST": [r"anydesk", r"teamviewer", r"remote access", r"screen share", r"install.*app"],
    "SECRECY_REQUEST": [r"secret", r"confidential", r"don't tell anyone"],
    "FEAR_LANGUAGE": [r"scared", r"danger", r"risk", r"you are in trouble", r"problem"],
    "IMPERSONATION_SIGNAL": [r"calling from", r"this is .* department", r"support team", r"customer care"],
}

def detect_signals(text: str, timestamp: int, source: str = "transcript") -> list[Signal]:
    text_l = text.lower()
    out: list[Signal] = []
    for signal_type, patterns in PATTERNS.items():
        hits = [p for p in patterns if re.search(p, text_l)]
        if hits:
            confidence = min(0.55 + 0.1 * len(hits), 0.95)
            out.append(Signal(type=signal_type, confidence=confidence, timestamp=timestamp, source=source,
                              evidence_ref=f"transcript:{timestamp}", metadata={"pattern_hits": len(hits)}))
    return out
