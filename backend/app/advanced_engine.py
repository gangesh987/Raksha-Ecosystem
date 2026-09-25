
from __future__ import annotations
from dataclasses import dataclass, field
from hashlib import sha256
from time import time
from typing import Any

@dataclass
class Signal:
    name: str
    score: float
    confidence: float = 1.0
    quality: float = 1.0
    reason: str = ""

@dataclass
class Decision:
    level: str
    score: float
    trajectory: float
    acceleration: float
    reasons: list[str]
    counterfactuals: list[str]
    trace: list[dict[str, Any]] = field(default_factory=list)

TACTIC_WEIGHTS = {
    "authority_impersonation": 0.20,
    "urgency": 0.16,
    "isolation": 0.14,
    "payment_pressure": 0.22,
    "credential_request": 0.24,
    "threat": 0.18,
    "suspicious_entity": 0.10,
    "repetition_escalation": 0.12,
}

def detect_tactics(text: str) -> dict[str, float]:
    t = (text or "").lower()
    groups = {
        "authority_impersonation": ["police", "court", "customs", "cbi", "investigation", "officer"],
        "urgency": ["now", "immediately", "urgent", "within", "don't delay", "do not delay"],
        "isolation": ["don't tell", "do not tell", "stay on the call", "disconnect", "keep this secret"],
        "payment_pressure": ["transfer", "pay", "upi", "bank account", "money", "deposit"],
        "credential_request": ["otp", "pin", "password", "passcode", "cvv", "verification code"],
        "threat": ["arrest", "warrant", "freeze", "jail", "criminal case", "you are under investigation"],
        "suspicious_entity": ["link", "apk", "remote access", "screen share"],
    }
    out={}
    for k, words in groups.items():
        hits=sum(1 for w in words if w in t)
        if hits: out[k]=min(1.0, 0.35 + 0.18*hits)
    return out

def fuse(signals: list[Signal], previous_score: float = 0.0) -> Decision:
    usable=[s for s in signals if s.quality > 0 and s.confidence > 0]
    if not usable:
        return Decision("LOW", 0.0, 0.0, 0.0, [], ["Collect clearer conversation evidence."])
    total=sum(s.score*s.confidence*s.quality for s in usable)
    denom=sum(s.confidence*s.quality for s in usable)
    score=max(0.0, min(1.0, total/denom if denom else 0.0))
    trajectory=score-previous_score
    acceleration=max(0.0, trajectory)
    if score >= .78: level="CRITICAL"
    elif score >= .58: level="HIGH"
    elif score >= .32: level="MEDIUM"
    else: level="LOW"
    reasons=[f"{s.name}: {s.reason}" for s in sorted(usable,key=lambda x:x.score,reverse=True) if s.score >= .35][:5]
    counter=[]
    if score >= .58:
        counter.append("End the call or independently verify the caller through an official channel.")
        if any(s.name=="conversation" and s.score>=.5 for s in usable):
            counter.append("Do not share OTPs, PINs, passwords, or transfer money under pressure.")
    return Decision(level, score, trajectory, acceleration, reasons, counter,
                    [{"signal":s.name,"score":s.score,"confidence":s.confidence,"quality":s.quality} for s in usable])

def evidence_hash(previous_hash: str, event: dict[str, Any]) -> str:
    payload=(previous_hash+"|"+repr(sorted(event.items()))).encode()
    return sha256(payload).hexdigest()
