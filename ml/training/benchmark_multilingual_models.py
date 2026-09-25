"""
Benchmark Script for Multilingual Models on RakshaCall Dataset:
Compares:
1. N-Gram + MLP Baseline
2. MuRIL (google/muril-base-cased)
3. XLM-R (xlm-roberta-base)
4. mBERT (bert-base-multilingual-cased)

Measures:
- Scam F1
- Tactic Macro F1
- Tactic Micro F1
- False Positive Rate (FPR)
- False Negative Rate (FNR)
- Inference Latency per sample (ms)
"""

import os
import sys
import time
import json
import torch
import torch.nn as nn
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import f1_score, precision_score, recall_score, confusion_matrix

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))
from ml.preprocessing.label_mapper import RAKSHACALL_TACTICS

FINAL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "datasets", "final")
REPORTS_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "reports")
os.makedirs(REPORTS_DIR, exist_ok=True)

def load_split(split_name: str, max_samples: int = None):
    path = os.path.join(FINAL_DIR, f"{split_name}_turns.jsonl")
    texts, scam_labels, tactic_labels, langs = [], [], [], []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            item = json.loads(line)
            texts.append(item["text"])
            scam_labels.append(1.0 if item["is_scam"] else 0.0)
            t_vec = [1.0 if t in item.get("tactics", []) else 0.0 for t in RAKSHACALL_TACTICS]
            tactic_labels.append(t_vec)
            langs.append(item.get("language", "en"))
            if max_samples and len(texts) >= max_samples:
                break
    return texts, np.array(scam_labels), np.array(tactic_labels), langs

print("Data loader helper ready.")
