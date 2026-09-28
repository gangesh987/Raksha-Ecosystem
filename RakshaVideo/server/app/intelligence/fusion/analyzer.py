from ..models import Signal

HIGH_IMPACT = {"OTP_REQUEST", "PASSWORD_REQUEST", "CREDENTIAL_REQUEST", "REMOTE_ACCESS_REQUEST"}
MEDIUM_IMPACT = {"FINANCIAL_REQUEST", "THREAT", "ISOLATION", "SECRECY_REQUEST", "AUTHORITY_CLAIM", "URGENCY"}

def fuse(signals: list[Signal]) -> tuple[str, int, list[str], float]:
    if not signals:
        return "UNKNOWN", None, [], 0.0
    types = {s.type for s in signals}
    score = 0
    reasons: list[str] = []
    for s in signals:
        if s.type in HIGH_IMPACT:
            score += round(28 * s.confidence)
        elif s.type in MEDIUM_IMPACT:
            score += round(14 * s.confidence)
    combos = [
        ({"AUTHORITY_CLAIM", "URGENCY"}, "Authority pressure combined with urgency"),
        ({"URGENCY", "FINANCIAL_REQUEST"}, "Urgency combined with a financial request"),
        ({"FINANCIAL_REQUEST", "OTP_REQUEST"}, "Financial request combined with an OTP request"),
        ({"ISOLATION", "FINANCIAL_REQUEST"}, "Isolation language combined with a financial request"),
    ]
    for combo, reason in combos:
        if combo.issubset(types):
            score += 15
            reasons.append(reason)
    reasons.extend(sorted(t.replace("_", " ").title() for t in types if t in HIGH_IMPACT or t in MEDIUM_IMPACT))
    score = min(score, 100)
    if score >= 75:
        level = "CRITICAL"
    elif score >= 55:
        level = "HIGH"
    elif score >= 25:
        level = "MEDIUM"
    else:
        level = "LOW"
    confidence = min(sum(s.confidence for s in signals) / max(len(signals), 1) + (0.08 if len(types) >= 2 else 0), 0.99)
    return level, score, list(dict.fromkeys(reasons)), confidence
