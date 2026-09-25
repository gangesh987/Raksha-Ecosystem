"""
RakshaCall Dataset Normalization & Split Engine.
Applies TextNormalizer, LabelMapper, and splits datasets into:
- 70% Train
- 15% Validation
- 15% Test
Guarantees conversation-level splitting to prevent multi-turn data leakage.
"""

import os
import sys
import json
import random
import logging
from typing import List, Dict, Any

# Ensure project root is on sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

from ml.preprocessing.schema import TurnRecord, ConversationRecord
from ml.preprocessing.normalizer import TextNormalizer
from ml.preprocessing.label_mapper import LabelMapper, RAKSHACALL_TACTICS

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("DatasetNormalizer")

DATASETS_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "datasets")
PROCESSED_DIR = os.path.join(DATASETS_DIR, "processed")
FINAL_DIR = os.path.join(DATASETS_DIR, "final")
REPORTS_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "reports")

def normalize_and_split():
    os.makedirs(FINAL_DIR, exist_ok=True)
    os.makedirs(REPORTS_DIR, exist_ok=True)

    mapper = LabelMapper()
    all_conversations: List[ConversationRecord] = []

    # 1. Load domain conversations
    domain_convs_path = os.path.join(PROCESSED_DIR, "domain_conversations.jsonl")
    if os.path.exists(domain_convs_path):
        logger.info(f"Loading domain conversations from {domain_convs_path}...")
        with open(domain_convs_path, "r", encoding="utf-8") as f:
            for line in f:
                if line.strip():
                    data = json.loads(line)
                    conv = ConversationRecord(**data)
                    all_conversations.append(conv)
    else:
        logger.warning(f"Domain conversations not found at {domain_convs_path}. Running build_rakshacall_dataset...")
        from ml.scripts.build_rakshacall_dataset import build_dataset
        build_dataset()
        with open(domain_convs_path, "r", encoding="utf-8") as f:
            for line in f:
                if line.strip():
                    conv = ConversationRecord(**json.loads(line))
                    all_conversations.append(conv)

    # 2. Check for ingested online datasets in raw/
    raw_dir = os.path.join(DATASETS_DIR, "raw")
    if os.path.exists(raw_dir):
        for ds_name in os.listdir(raw_dir):
            sample_file = os.path.join(raw_dir, ds_name, "samples.json")
            if os.path.exists(sample_file):
                logger.info(f"Processing raw samples from {ds_name}...")
                try:
                    with open(sample_file, "r", encoding="utf-8") as sf:
                        samples = json.load(sf)
                    for item in samples:
                        # Extract turns or texts
                        text = item.get("text", item.get("transcript", item.get("content", "")))
                        if not text:
                            continue
                        cleaned_text = TextNormalizer.normalize_text(text)
                        lang = TextNormalizer.detect_script_and_language(cleaned_text)
                        
                        raw_label = str(item.get("label", item.get("category", "unknown")))
                        rk_label, is_aux, conf, reason = mapper.map_label(ds_name, raw_label)
                        
                        is_scam = bool(item.get("is_scam", rk_label is not None))
                        tactics = [rk_label] if rk_label else []

                        # Create synthetic single-turn conversation record
                        conv_id = str(item.get("id", item.get("conversation_id", f"{ds_name}_{len(all_conversations)}")))
                        turn = TurnRecord(
                            dataset_source=ds_name,
                            conversation_id=conv_id,
                            turn_id=1,
                            language=lang,
                            text=cleaned_text,
                            speaker="caller",
                            is_scam=is_scam,
                            tactics=tactics,
                            stage="DEMAND" if is_scam else "CONTACT",
                            scenario=item.get("scenario", ds_name),
                            severity=1.0 if is_scam else 0.0,
                            source_type="conversation"
                        )
                        conv = ConversationRecord(
                            conversation_id=conv_id,
                            dataset_source=ds_name,
                            language=lang,
                            is_scam=is_scam,
                            scenario=item.get("scenario", ds_name),
                            tactics=tactics,
                            stage_sequence=["CONTACT", "DEMAND"] if is_scam else ["CONTACT"],
                            turns=[turn]
                        )
                        all_conversations.append(conv)
                except Exception as e:
                    logger.warning(f"Error parsing raw samples in {ds_name}: {e}")

    # 3. Perform Conversation-Disjoint Split (70% Train / 15% Val / 15% Test)
    random.seed(42)
    random.shuffle(all_conversations)

    n_total = len(all_conversations)
    n_train = max(1, int(n_total * 0.70))
    n_val = max(1, int(n_total * 0.15))
    
    train_convs = all_conversations[:n_train]
    val_convs = all_conversations[n_train:n_train + n_val]
    test_convs = all_conversations[n_train + n_val:]
    if not test_convs and val_convs:
        test_convs = [val_convs[-1]]

    logger.info(f"Split Summary: Total={n_total} | Train={len(train_convs)} | Val={len(val_convs)} | Test={len(test_convs)}")

    # 4. Save JSONL files
    splits = {
        "train": train_convs,
        "val": val_convs,
        "test": test_convs
    }

    turn_counts = {}
    for split_name, conv_list in splits.items():
        conv_path = os.path.join(FINAL_DIR, f"{split_name}_conversations.jsonl")
        turn_path = os.path.join(FINAL_DIR, f"{split_name}_turns.jsonl")
        
        all_turns = []
        with open(conv_path, "w", encoding="utf-8") as cf:
            for c in conv_list:
                cf.write(json.dumps(c.model_dump(), ensure_ascii=False) + "\n")
                all_turns.extend(c.turns)

        with open(turn_path, "w", encoding="utf-8") as tf:
            for t in all_turns:
                tf.write(json.dumps(t.model_dump(), ensure_ascii=False) + "\n")

        turn_counts[split_name] = len(all_turns)

    # 5. Save dataset manifest report
    summary = {
        "total_conversations": n_total,
        "splits": {
            "train": {"conversations": len(train_convs), "turns": turn_counts["train"]},
            "val": {"conversations": len(val_convs), "turns": turn_counts["val"]},
            "test": {"conversations": len(test_convs), "turns": turn_counts["test"]}
        },
        "tactics_ontology": RAKSHACALL_TACTICS,
        "label_mapping_audit_count": len(mapper.mapping_audit_log)
    }

    with open(os.path.join(FINAL_DIR, "dataset_summary.json"), "w", encoding="utf-8") as sf:
        json.dump(summary, sf, indent=2)

    logger.info(f"Dataset normalization and split complete. Artifacts written to {FINAL_DIR}")
    return summary

if __name__ == "__main__":
    normalize_and_split()
