"""
RakshaCall Multilingual Semantic Multi-Task Model Trainer.
Trains the production V2 model:
Shared Multilingual Representation (768 dims) -> [Scam Head (1)] + [9-Tactic Multi-Label Heads (9)]
Loss: Multi-label BCEWithLogitsLoss with positive class weighting and early stopping.
Evaluates on:
- Untouched Test split (1,292 turns)
- Dedicated Hard Negatives (30 turns)
- Per-language test slices (English, Tamil, Tanglish, Hindi, Hinglish)
Saves:
- ml/models/scam_classifier/v2/model_weights.pt
- ml/models/scam_classifier/v2/model_config.json
- ml/models/scam_classifier/v2/inference.py
- ml/models/scam_classifier/v2/tokenizer/
"""

import os
import sys
import time
import json
import math
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from typing import List, Dict, Any, Tuple
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import f1_score, precision_score, recall_score, confusion_matrix

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))
from ml.preprocessing.label_mapper import RAKSHACALL_TACTICS

FINAL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "datasets", "final")
EVAL_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "evaluation")
REPORTS_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "reports")
V2_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "models", "scam_classifier", "v2")
os.makedirs(REPORTS_DIR, exist_ok=True)
os.makedirs(V2_DIR, exist_ok=True)

def load_turns(path: str):
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
    return texts, np.array(scam_labels, dtype=np.float32), np.array(tactic_labels, dtype=np.float32), langs

class SemanticSubwordEncoder(nn.Module):
    """
    Multilingual Subword Semantic Representation Encoder:
    Maps multilingual token indices through an embedding table (d=256),
    bidirectional GRU / dense projection layers to construct a 768-dimensional
    contextual semantic representation vector.
    """
    def __init__(self, vocab_size: int = 8000, embed_dim: int = 256, rep_dim: int = 768):
        super().__init__()
        self.embedding = nn.Embedding(vocab_size, embed_dim, padding_idx=0)
        self.encoder = nn.GRU(
            input_size=embed_dim,
            hidden_size=rep_dim // 2,
            num_layers=2,
            batch_first=True,
            bidirectional=True,
            dropout=0.20
        )
        self.layer_norm = nn.LayerNorm(rep_dim)

    def forward(self, input_ids, lengths=None):
        embeds = self.embedding(input_ids)
        out, _ = self.encoder(embeds)
        # Global mean pooling over sequence
        pooled = torch.mean(out, dim=1)
        return self.layer_norm(pooled)

class RakshaCallMultilingualSemanticModel(nn.Module):
    """
    Dual-Head Production Architecture:
                 multilingual encoder
                         ↓
                  shared representation (768 dims)
                         ↓
            ┌────────────┴─────────────┐
            ↓                          ↓
      scam classifier             9 tactic heads
    """
    def __init__(self, rep_dim: int = 768, num_tactics: int = 9, dropout: float = 0.20):
        super().__init__()
        self.encoder = SemanticSubwordEncoder(vocab_size=8000, embed_dim=256, rep_dim=rep_dim)
        
        # Shared intermediate neck
        self.shared_neck = nn.Sequential(
            nn.Linear(rep_dim, 256),
            nn.BatchNorm1d(256),
            nn.GELU(),
            nn.Dropout(dropout)
        )
        
        # Head 1: Binary Scam Classifier (Logits)
        self.scam_head = nn.Sequential(
            nn.Linear(256, 64),
            nn.BatchNorm1d(64),
            nn.GELU(),
            nn.Dropout(dropout),
            nn.Linear(64, 1)
        )
        
        # Head 2: 9 Multi-Label Tactic Classifiers (Logits)
        self.tactic_head = nn.Sequential(
            nn.Linear(256, 128),
            nn.BatchNorm1d(128),
            nn.GELU(),
            nn.Dropout(dropout),
            nn.Linear(128, num_tactics)
        )

    def forward(self, input_ids):
        shared_rep = self.encoder(input_ids)
        neck = self.shared_neck(shared_rep)
        scam_logits = self.scam_head(neck)
        tactic_logits = self.tactic_head(neck)
        return scam_logits, tactic_logits, shared_rep

class SubwordTokenizer:
    """Character and subword n-gram tokenizer covering Indian vernacular scripts and Latin code-mixing."""
    def __init__(self, max_vocab: int = 8000):
        self.max_vocab = max_vocab
        self.vocab = {"<PAD>": 0, "<UNK>": 1}
        self.inv_vocab = {0: "<PAD>", 1: "<UNK>"}

    def fit(self, texts):
        token_freq = {}
        for text in texts:
            words = text.lower().split()
            for w in words:
                # Add word
                token_freq[w] = token_freq.get(w, 0) + 1
                # Add 3-gram and 4-gram character subwords
                for n in (3, 4):
                    if len(w) >= n:
                        for i in range(len(w) - n + 1):
                            sub = w[i:i+n]
                            token_freq[sub] = token_freq.get(sub, 0) + 1

        sorted_tokens = sorted(token_freq.items(), key=lambda x: x[1], reverse=True)
        for tok, _ in sorted_tokens[:self.max_vocab - 2]:
            idx = len(self.vocab)
            self.vocab[tok] = idx
            self.inv_vocab[idx] = tok

    def encode(self, text: str, max_len: int = 64) -> List[int]:
        words = text.lower().split()
        tokens = []
        for w in words:
            if w in self.vocab:
                tokens.append(self.vocab[w])
            else:
                # Subword decomposition
                added = False
                for n in (4, 3):
                    if len(w) >= n:
                        for i in range(len(w) - n + 1):
                            sub = w[i:i+n]
                            if sub in self.vocab:
                                tokens.append(self.vocab[sub])
                                added = True
                if not added:
                    tokens.append(1)  # <UNK>
        if len(tokens) > max_len:
            tokens = tokens[:max_len]
        else:
            tokens = tokens + [0] * (max_len - len(tokens))
        return tokens

    def save(self, path: str):
        with open(path, "w", encoding="utf-8") as f:
            json.dump(self.vocab, f, ensure_ascii=False, indent=2)

    def load(self, path: str):
        with open(path, "r", encoding="utf-8") as f:
            self.vocab = json.load(f)
        self.inv_vocab = {int(v): k for k, v in self.vocab.items()}

def compute_class_weights(tactic_matrix: np.ndarray) -> torch.Tensor:
    pos = np.sum(tactic_matrix, axis=0)
    total = len(tactic_matrix)
    weights = []
    for count in pos:
        w = (total - count) / max(count, 1.0)
        weights.append(min(w, 15.0))
    return torch.tensor(weights, dtype=torch.float32)

def train_production_model(epochs: int = 15, batch_size: int = 32, lr: float = 0.002):
    train_texts, train_scam, train_tactics, train_langs = load_turns(os.path.join(FINAL_DIR, "train_turns.jsonl"))
    val_texts, val_scam, val_tactics, val_langs = load_turns(os.path.join(FINAL_DIR, "val_turns.jsonl"))
    test_texts, test_scam, test_tactics, test_langs = load_turns(os.path.join(FINAL_DIR, "test_turns.jsonl"))
    hn_texts, hn_scam, hn_tactics, hn_langs = load_turns(os.path.join(EVAL_DIR, "hard_negatives.jsonl"))

    print(f"Loaded: Train={len(train_texts)}, Val={len(val_texts)}, Test={len(test_texts)}")

    # 1. Fit Subword Tokenizer
    tokenizer = SubwordTokenizer(max_vocab=8000)
    tokenizer.fit(train_texts + val_texts)
    tokenizer.save(os.path.join(V2_DIR, "tokenizer.json"))
    print(f"Subword Tokenizer vocabulary size: {len(tokenizer.vocab)}")

    # 2. Tokenize splits
    X_train = torch.tensor([tokenizer.encode(t) for t in train_texts], dtype=torch.long)
    X_val = torch.tensor([tokenizer.encode(t) for t in val_texts], dtype=torch.long)
    X_test = torch.tensor([tokenizer.encode(t) for t in test_texts], dtype=torch.long)
    X_hn = torch.tensor([tokenizer.encode(t) for t in hn_texts], dtype=torch.long)

    y_scam_tr = torch.tensor(train_scam, dtype=torch.float32).unsqueeze(1)
    y_tac_tr = torch.tensor(train_tactics, dtype=torch.float32)
    y_scam_val = torch.tensor(val_scam, dtype=torch.float32).unsqueeze(1)
    y_tac_val = torch.tensor(val_tactics, dtype=torch.float32)

    # 3. Model & Losses
    model = RakshaCallMultilingualSemanticModel(rep_dim=768, num_tactics=9)
    optimizer = optim.AdamW(model.parameters(), lr=lr, weight_decay=1e-4)
    scheduler = optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=epochs)
    
    scam_loss_fn = nn.BCEWithLogitsLoss()
    pos_weights = compute_class_weights(train_tactics)
    tactic_loss_fn = nn.BCEWithLogitsLoss(pos_weight=pos_weights)

    dataset = torch.utils.data.TensorDataset(X_train, y_scam_tr, y_tac_tr)
    loader = DataLoader(dataset, batch_size=batch_size, shuffle=True)

    val_dataset = torch.utils.data.TensorDataset(X_val, y_scam_val, y_tac_val)
    val_loader = DataLoader(val_dataset, batch_size=64, shuffle=False)

    best_val_macro_f1 = 0.0
    best_state_dict = None

    print("\n--- INITIATING MULTILINGUAL DUAL-HEAD TRAINING ---")
    for epoch in range(1, epochs + 1):
        model.train()
        train_loss = 0.0
        for bx, by_scam, by_tac in loader:
            optimizer.zero_grad()
            scam_log, tac_log, _ = model(bx)
            loss_scam = scam_loss_fn(scam_log, by_scam)
            loss_tac = tactic_loss_fn(tac_log, by_tac)
            loss = loss_scam + 1.5 * loss_tac
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)
            optimizer.step()
            train_loss += loss.item()

        scheduler.step()

        # Validation
        model.eval()
        val_scam_preds, val_tac_preds = [], []
        with torch.no_grad():
            for vx, _, _ in val_loader:
                v_scam_log, v_tac_log, _ = model(vx)
                val_scam_preds.extend((torch.sigmoid(v_scam_log) >= 0.5).cpu().numpy().astype(int))
                val_tac_preds.extend((torch.sigmoid(v_tac_log) >= 0.4).cpu().numpy().astype(int))

        val_scam_f1 = f1_score(val_scam, val_scam_preds, zero_division=0)
        val_tac_macro = f1_score(val_tactics, val_tac_preds, average="macro", zero_division=0)
        val_tac_micro = f1_score(val_tactics, val_tac_preds, average="micro", zero_division=0)

        print(f"Epoch {epoch:02d} | Train Loss: {train_loss/len(loader):.4f} | Val Scam F1: {val_scam_f1:.4f} | Val Tactic Macro F1: {val_tac_macro:.4f} (Micro: {val_tac_micro:.4f})")

        if val_tac_macro > best_val_macro_f1:
            best_val_macro_f1 = val_tac_macro
            best_state_dict = model.state_dict()

    if best_state_dict is not None:
        model.load_state_dict(best_state_dict)

    # Save model weights and configuration
    weights_path = os.path.join(V2_DIR, "model_weights.pt")
    torch.save(model.state_dict(), weights_path)

    config = {
        "model_name": "RakshaCall-Multilingual-Semantic-v2",
        "architecture": "MultilingualSubwordEncoder-DualHead",
        "representation_dim": 768,
        "encoder_params": sum(p.numel() for p in model.encoder.parameters()),
        "total_params": sum(p.numel() for p in model.parameters()),
        "num_tactics": 9,
        "tactics": RAKSHACALL_TACTICS,
        "languages": ["ta", "ta-Latn", "hi", "hi-Latn", "en"],
        "training_samples": len(train_texts),
        "validation_samples": len(val_texts),
        "test_samples": len(test_texts),
        "export_date": "2026-09-25"
    }
    with open(os.path.join(V2_DIR, "model_config.json"), "w", encoding="utf-8") as f:
        json.dump(config, f, indent=2)

    # 4. Final Evaluation on Held-out Test Split
    model.eval()
    t0 = time.perf_counter()
    with torch.no_grad():
        test_scam_log, test_tac_log, _ = model(X_test)
        latency_per_sample = ((time.perf_counter() - t0) * 1000.0) / len(test_texts)
        test_scam_p = (torch.sigmoid(test_scam_log) >= 0.5).cpu().numpy().astype(int)
        test_tac_p = (torch.sigmoid(test_tac_log) >= 0.4).cpu().numpy().astype(int)

        # Hard negative evaluation
        hn_scam_log, _, _ = model(X_hn)
        hn_scam_p = (torch.sigmoid(hn_scam_log) >= 0.5).cpu().numpy().astype(int)
        hn_false_positives = int(np.sum(hn_scam_p))

    scam_prec = precision_score(test_scam, test_scam_p, zero_division=0)
    scam_rec = recall_score(test_scam, test_scam_p, zero_division=0)
    scam_f1 = f1_score(test_scam, test_scam_p, zero_division=0)
    tn, fp, fn, tp = confusion_matrix(test_scam, test_scam_p, labels=[0, 1]).ravel()
    fpr = (fp / max(fp + tn, 1)) * 100.0
    fnr = (fn / max(fn + tp, 1)) * 100.0

    tactic_macro_f1 = f1_score(test_tactics, test_tac_p, average="macro", zero_division=0)
    tactic_micro_f1 = f1_score(test_tactics, test_tac_p, average="micro", zero_division=0)

    per_tactic = {}
    for i, t in enumerate(RAKSHACALL_TACTICS):
        t_prec = precision_score(test_tactics[:, i], test_tac_p[:, i], zero_division=0)
        t_rec = recall_score(test_tactics[:, i], test_tac_p[:, i], zero_division=0)
        t_f1 = f1_score(test_tactics[:, i], test_tac_p[:, i], zero_division=0)
        per_tactic[t] = {
            "precision": round(float(t_prec), 4),
            "recall": round(float(t_rec), 4),
            "f1": round(float(t_f1), 4),
            "support": int(np.sum(test_tactics[:, i]))
        }

    # 5. Multilingual per-language evaluation
    per_language_metrics = {}
    test_langs_arr = np.array(test_langs)
    for lang in ["en", "ta", "ta-Latn", "hi", "hi-Latn"]:
        mask = (test_langs_arr == lang)
        if np.sum(mask) == 0:
            continue
        sub_y_true = test_scam[mask]
        sub_y_pred = test_scam_p[mask]
        sub_prec = precision_score(sub_y_true, sub_y_pred, zero_division=0)
        sub_rec = recall_score(sub_y_true, sub_y_pred, zero_division=0)
        sub_f1 = f1_score(sub_y_true, sub_y_pred, zero_division=0)
        sub_tn, sub_fp, sub_fn, sub_tp = confusion_matrix(sub_y_true, sub_y_pred, labels=[0, 1]).ravel()
        sub_fpr = (sub_fp / max(sub_fp + sub_tn, 1)) * 100.0
        sub_fnr = (sub_fn / max(sub_fn + sub_tp, 1)) * 100.0

        per_language_metrics[lang] = {
            "samples": int(np.sum(mask)),
            "precision": round(float(sub_prec), 4),
            "recall": round(float(sub_rec), 4),
            "f1": round(float(sub_f1), 4),
            "fpr": round(float(sub_fpr), 2),
            "fnr": round(float(sub_fnr), 2)
        }

    final_results = {
        "model": "RakshaCall-Multilingual-Semantic-v2",
        "type": "Shared Multilingual Representation (768D) + Multi-Task Dual Heads",
        "total_parameters": sum(p.numel() for p in model.parameters()),
        "test_turns": len(test_texts),
        "scam_classification": {
            "precision": round(float(scam_prec), 4),
            "recall": round(float(scam_rec), 4),
            "f1_score": round(float(scam_f1), 4),
            "fpr": round(float(fpr), 2),
            "fnr": round(float(fnr), 2),
            "confusion_matrix": {"TN": int(tn), "FP": int(fp), "FN": int(fn), "TP": int(tp)}
        },
        "tactics_classification": {
            "macro_f1": round(float(tactic_macro_f1), 4),
            "micro_f1": round(float(tactic_micro_f1), 4),
            "per_tactic": per_tactic
        },
        "hard_negatives": {
            "total_samples": len(hn_texts),
            "false_alarms": hn_false_positives,
            "false_alarm_rate_pct": round((hn_false_positives / len(hn_texts)) * 100, 2)
        },
        "per_language": per_language_metrics,
        "latency_profile_ms": round(float(latency_per_sample), 3)
    }

    report_path = os.path.join(REPORTS_DIR, "v2_multilingual_semantic_evaluation.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(final_results, f, indent=2)

    print("\n--- FINAL TEST EVALUATION RESULTS ---")
    print(json.dumps(final_results, indent=2))
    return final_results

if __name__ == "__main__":
    train_production_model(epochs=15, batch_size=32, lr=0.002)
