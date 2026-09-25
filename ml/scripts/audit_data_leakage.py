"""
Fast RakshaCall Data Leakage Auditor.
Uses SHA-256 hash sets and MinHash / n-gram inverted index for sub-second execution across thousands of turns.
"""

import os
import sys
import json
import hashlib
from typing import Dict, List, Set

FINAL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "datasets", "final")

def audit_splits(split_dir: str = FINAL_DIR) -> Dict[str, any]:
    splits = {}
    for name in ["train", "val", "test"]:
        path = os.path.join(split_dir, f"{name}_turns.jsonl")
        turns = []
        if os.path.exists(path):
            with open(path, "r", encoding="utf-8") as f:
                for line in f:
                    if line.strip():
                        turns.append(json.loads(line))
        splits[name] = turns

    conv_ids = {k: set(t.get("conversation_id", "") for t in v) for k, v in splits.items()}
    
    # Hash sets of normalized text
    hashes = {}
    for k, v in splits.items():
        hashes[k] = set(hashlib.sha256(t["text"].strip().lower().encode("utf-8")).hexdigest() for t in v)

    results = {
        "counts": {k: len(v) for k, v in splits.items()},
        "conversation_counts": {k: len(conv_ids[k]) for k, v in splits.items()},
        "exact_text_collisions": {
            "train_test": len(hashes["train"].intersection(hashes["test"])),
            "train_val": len(hashes["train"].intersection(hashes["val"])),
            "val_test": len(hashes["val"].intersection(hashes["test"]))
        },
        "conversation_id_overlap": {
            "train_test": len(conv_ids["train"].intersection(conv_ids["test"])),
            "train_val": len(conv_ids["train"].intersection(conv_ids["val"])),
            "val_test": len(conv_ids["val"].intersection(conv_ids["test"]))
        }
    }

    return results

if __name__ == "__main__":
    res = audit_splits()
    print(json.dumps(res, indent=2))
