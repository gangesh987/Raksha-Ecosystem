
from app.advanced_engine import detect_tactics, fuse, Signal, evidence_hash

def test_tactics():
    x=detect_tactics("This is police. You are under investigation. Transfer money now. Do not tell anyone. Give OTP.")
    assert x["authority_impersonation"] > 0
    assert x["payment_pressure"] > 0
    assert x["credential_request"] > 0

def test_fusion_escalates():
    d=fuse([Signal("conversation", .9, reason="payment + threat"), Signal("visual", .4, .7)])
    assert d.level in {"HIGH","CRITICAL"}
    assert d.trajectory >= 0

def test_hash_chain_changes():
    a=evidence_hash("0", {"event":"a"})
    b=evidence_hash(a, {"event":"b"})
    assert a != b
