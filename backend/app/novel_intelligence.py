from dataclasses import dataclass, field
from collections import deque
from time import time
from typing import Iterable

STAGES=("CONTACT","AUTHORITY","FEAR","ISOLATION","DEMAND","PAYMENT_CREDENTIAL","ESCALATION")

@dataclass
class ScamState:
    stage:str="CONTACT"
    tactic_count:int=0
    manipulation_velocity:float=0.0
    history:deque=field(default_factory=lambda: deque(maxlen=30))

def update_scam_state(state:ScamState, tactics:Iterable[str], timestamp:float|None=None)->ScamState:
    now=timestamp or time()
    tactics=list(tactics)
    state.history.append((now, list(tactics)))
    state.tactic_count += len(tactics)
    mapping={
        "authority_impersonation":"AUTHORITY",
        "threat":"FEAR",
        "isolation":"ISOLATION",
        "urgency":"DEMAND",
        "payment_pressure":"PAYMENT_CREDENTIAL",
        "credential_request":"PAYMENT_CREDENTIAL",
        "repetition_escalation":"ESCALATION",
    }
    ranks={s:i for i,s in enumerate(STAGES)}
    candidates=[mapping[t] for t in tactics if t in mapping]
    if candidates:
        best=max(candidates,key=lambda s:ranks[s])
        if ranks[best]>ranks[state.stage]: state.stage=best
    recent=list(state.history)[-5:]
    if len(recent)>=2:
        dt=max(1.0,recent[-1][0]-recent[0][0])
        state.manipulation_velocity=sum(len(x[1]) for x in recent)/dt*60
    return state

def counterfactuals(stage:str):
    if stage in {"PAYMENT_CREDENTIAL","ESCALATION"}:
        return [
            "Do not share OTP/PIN/passwords or transfer funds.",
            "End the call and independently verify the claimed organization.",
            "Contact a trusted person before taking any financial action."
        ]
    if stage in {"AUTHORITY","FEAR","ISOLATION","DEMAND"}:
        return [
            "Slow the conversation down.",
            "Do not let the caller prevent independent verification.",
            "Use an official contact method you found yourself."
        ]
    return ["Continue observing; do not share sensitive information."]
