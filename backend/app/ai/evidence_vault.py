"""
RakshaCall Tamper-Evident Evidence Vault.
Maintains an append-only cryptographic SHA-256 ledger anchored at 64-zero genesis.
Enables instant verification of event history without storing raw audio.
"""

from __future__ import annotations
import hashlib
import json
import time
from dataclasses import dataclass, field
from typing import List, Dict, Tuple, Optional, Any


GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"


@dataclass
class EvidenceBlock:
    block_index: int
    event_id: str
    session_id: str
    timestamp_ms: int
    event_type: str
    payload_json: str
    previous_hash: str
    current_hash: str

    @staticmethod
    def compute_hash(previous_hash: str, event_id: str, timestamp_ms: int, event_type: str, payload_json: str) -> str:
        material = f"{previous_hash}:{event_id}:{timestamp_ms}:{event_type}:{payload_json}"
        return hashlib.sha256(material.encode("utf-8")).hexdigest()


class TamperEvidentEvidenceLedger:
    """
    Cryptographic SHA-256 Ledger for Protected Session Safety Events.
    Tampering, bit-flipping, deletion, or reordering immediately invalidates the chain.
    """
    def __init__(self, session_id: str):
        self.session_id = session_id
        self.blocks: List[EvidenceBlock] = []
        self.head_hash: str = GENESIS_HASH

    def append_event(self, event_id: str, event_type: str, payload: Dict[str, Any]) -> EvidenceBlock:
        payload_str = json.dumps(payload, sort_keys=True, separators=(",", ":"))
        timestamp = int(time.time() * 1000)
        curr_hash = EvidenceBlock.compute_hash(
            previous_hash=self.head_hash,
            event_id=event_id,
            timestamp_ms=timestamp,
            event_type=event_type,
            payload_json=payload_str
        )
        block = EvidenceBlock(
            block_index=len(self.blocks),
            event_id=event_id,
            session_id=self.session_id,
            timestamp_ms=timestamp,
            event_type=event_type,
            payload_json=payload_str,
            previous_hash=self.head_hash,
            current_hash=curr_hash
        )
        self.blocks.append(block)
        self.head_hash = curr_hash
        return block

    def verify_integrity(self) -> Tuple[bool, Optional[int], str]:
        """
        Verify the complete cryptographic chain from genesis to head.
        Returns: (is_valid, broken_block_index_or_none, status_message)
        """
        if not self.blocks:
            return True, None, "Ledger empty: anchored at genesis."

        expected_prev = GENESIS_HASH
        for idx, block in enumerate(self.blocks):
            if block.previous_hash != expected_prev:
                return False, idx, f"Hash break at block {idx}: previous_hash mismatch."

            recomputed = EvidenceBlock.compute_hash(
                previous_hash=block.previous_hash,
                event_id=block.event_id,
                timestamp_ms=block.timestamp_ms,
                event_type=block.event_type,
                payload_json=block.payload_json
            )
            if recomputed != block.current_hash:
                return False, idx, f"Tamper detected at block {idx}: recomputed hash mismatch."

            expected_prev = block.current_hash

        return True, None, f"CHAIN INTEGRITY VERIFIED: All {len(self.blocks)} blocks match Genesis 000...000 hash chain."


# Session ledger repository
_session_ledgers: Dict[str, TamperEvidentEvidenceLedger] = {}

def get_session_ledger(session_id: str) -> TamperEvidentEvidenceLedger:
    if session_id not in _session_ledgers:
        _session_ledgers[session_id] = TamperEvidentEvidenceLedger(session_id)
    return _session_ledgers[session_id]
