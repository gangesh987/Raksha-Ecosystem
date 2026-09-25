"""
Test suite for RakshaCall Tamper-Evident Evidence Vault.
Verifies SHA-256 cryptographic chaining, genesis anchoring, and tamper detection.
"""

import json
import pytest

from app.ai.evidence_vault import TamperEvidentEvidenceLedger, GENESIS_HASH


def test_evidence_ledger_chain_creation_and_integrity():
    """Verify blocks append correctly with valid cryptographic chain."""
    ledger = TamperEvidentEvidenceLedger(session_id="session-test-vault-01")
    assert ledger.head_hash == GENESIS_HASH

    # Block 1
    b1 = ledger.append_event(
        event_id="evt-1",
        event_type="STAGE_CHANGE",
        payload={"stage": "AUTHORITY", "tactic": "AUTHORITY"}
    )
    assert b1.previous_hash == GENESIS_HASH
    assert len(b1.current_hash) == 64
    assert ledger.head_hash == b1.current_hash

    # Block 2
    b2 = ledger.append_event(
        event_id="evt-2",
        event_type="RISK_SPIKE",
        payload={"score": 75, "level": "HIGH"}
    )
    assert b2.previous_hash == b1.current_hash
    assert len(b2.current_hash) == 64
    assert ledger.head_hash == b2.current_hash

    # Block 3
    b3 = ledger.append_event(
        event_id="evt-3",
        event_type="SAFETY_BRAKE_TRIGGERED",
        payload={"action": "PAUSE", "reason": "Payment demand under pressure"}
    )
    assert b3.previous_hash == b2.current_hash

    # Verify integrity passes
    is_valid, broken_idx, msg = ledger.verify_integrity()
    assert is_valid is True
    assert broken_idx is None
    assert "CHAIN INTEGRITY VERIFIED" in msg


def test_evidence_ledger_tamper_detection_on_payload_edit():
    """Modifying a single character in a past event payload MUST invalidate the chain."""
    ledger = TamperEvidentEvidenceLedger(session_id="session-test-tamper-02")
    ledger.append_event("evt-1", "STAGE", {"s": 1})
    ledger.append_event("evt-2", "STAGE", {"s": 2})
    ledger.append_event("evt-3", "STAGE", {"s": 3})

    # Tamper with block 1 payload
    tampered_payload = json.dumps({"s": 999}, sort_keys=True)
    ledger.blocks[1].payload_json = tampered_payload

    is_valid, broken_idx, msg = ledger.verify_integrity()
    assert is_valid is False
    assert broken_idx == 1
    assert "Tamper detected at block 1" in msg


def test_evidence_ledger_tamper_detection_on_block_deletion():
    """Deleting or dropping an intermediate block MUST break the chain."""
    ledger = TamperEvidentEvidenceLedger(session_id="session-test-drop-03")
    ledger.append_event("evt-1", "EVENT", {"data": "A"})
    ledger.append_event("evt-2", "EVENT", {"data": "B"})
    ledger.append_event("evt-3", "EVENT", {"data": "C"})

    # Drop intermediate block 1
    del ledger.blocks[1]

    is_valid, broken_idx, msg = ledger.verify_integrity()
    assert is_valid is False
    assert broken_idx == 1
    assert "Hash break at block 1" in msg
