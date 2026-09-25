"""
Trains and evaluates the N-Gram + MLP Baseline on the rectified 4,541 train / 1,292 test split.
Outputs honest baseline metrics for MODEL_COMPARISON.md.
"""

import os
import sys
import time
import json
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import f1_score, precision_score, recall_score, confusion_matrix
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))
from ml.preprocessing.label_mapper import RAKSHACALL_TACTICS

FINAL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "datasets", "final")
REPORTS_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "reports")

def load_data(split_name: str):
    path = os.path.join(FINAL_DIR, f"{split_name}_turns.jsonl")
    texts, scam_labels, tactic_labels = [], [], []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            item = json.loads(line)
            texts.append(item["text"])
            scam_labels.append(1.0 if item["is_scam"] else 0.0)
            t_vec = [1.0 if t in item.get("tactics", []) else 0.0 for t in RAKSHACALL_TACTICS]
            tactic_labels.append(t_vec)
    return texts, np.array(scam_labels, dtype=np.float32), np.array(tactic_labels, dtype=np.float32)

class BaselineModel(nn.Module):
    def __init__(self, input_dim: int, hidden_dim: int = 128, num_tactics: int = 9, dropout: float = 0.2):
        super().__init__()
        self.shared = nn.Sequential(
            nn.Linear(input_dim, hidden_dim),
            nn.BatchNorm1d(hidden_dim),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Linear(hidden_dim, hidden_dim // 2),
            nn.BatchNorm1d(hidden_dim // 2),
            nn.ReLU(),
            nn.Dropout(dropout)
        )
        rep_dim = hidden_dim // 2
        self.scam_head = nn.Linear(rep_dim, 1)
        self.tactic_head = nn.Linear(rep_dim, num_tactics)

    def forward(self, x):
        h = self.shared(x)
        return self.scam_head(h), self.tactic_head(h)

def train_and_eval_baseline():
    train_texts, train_scam, train_tactics = load_data("train")
    val_texts, val_scam, val_tactics = load_data("val")
    test_texts, test_scam, test_tactics = load_data("test")

    print(f"Loaded: Train={len(train_texts)}, Val={len(val_texts)}, Test={len(test_texts)}")

    # Fit TF-IDF Vectorizer
    vectorizer = TfidfVectorizer(max_features=2500, ngram_range=(1, 3), token_pattern=r"(?u)\b\w+\b")
    X_train = torch.tensor(vectorizer.fit_transform(train_texts).toarray(), dtype=torch.float32)
    X_val = torch.tensor(vectorizer.transform(val_texts).toarray(), dtype=torch.float32)
    X_test = torch.tensor(vectorizer.transform(test_texts).toarray(), dtype=torch.float32)

    y_scam_train = torch.tensor(train_scam).unsqueeze(1)
    y_tactics_train = torch.tensor(train_tactics)
    y_scam_val = torch.tensor(val_scam).unsqueeze(1)
    y_tactics_val = torch.tensor(val_tactics)

    input_dim = X_train.shape[1]
    model = BaselineModel(input_dim=input_dim)
    optimizer = optim.AdamW(model.parameters(), lr=0.003, weight_decay=1e-4)
    scam_loss_fn = nn.BCEWithLogitsLoss()
    tactic_loss_fn = nn.BCEWithLogitsLoss()

    dataset = torch.utils.data.TensorDataset(X_train, y_scam_train, y_tactics_train)
    loader = DataLoader(dataset, batch_size=32, shuffle=True)

    best_val_f1 = 0.0
    for epoch in range(1, 16):
        model.train()
        total_loss = 0.0
        for bx, by_scam, by_tactic in loader:
            optimizer.zero_grad()
            scam_logits, tactic_logits = model(bx)
            loss = scam_loss_fn(scam_logits, by_scam) + 1.2 * tactic_loss_fn(tactic_logits, by_tactic)
            loss.backward()
            optimizer.step()
            total_loss += loss.item()

        # Validate
        model.eval()
        with torch.no_grad():
            v_scam_log, v_tac_log = model(X_val)
            v_scam_preds = (torch.sigmoid(v_scam_log) >= 0.5).cpu().numpy().astype(int)
            val_f1 = f1_score(val_scam, v_scam_preds, zero_division=0)
            if val_f1 > best_val_f1:
                best_val_f1 = val_f1

        if epoch % 5 == 0 or epoch == 1:
            print(f"Epoch {epoch:02d} | Train Loss: {total_loss/len(loader):.4f} | Val Scam F1: {val_f1:.4f}")

    # Evaluate on Test Split
    model.eval()
    t0 = time.perf_counter()
    with torch.no_grad():
        test_scam_log, test_tac_log = model(X_test)
        latency_ms = (time.perf_counter() - t0) * 1000.0 / len(test_texts)
        test_scam_preds = (torch.sigmoid(test_scam_log) >= 0.5).cpu().numpy().astype(int)
        test_tac_preds = (torch.sigmoid(test_tac_log) >= 0.4).cpu().numpy().astype(int)

    scam_prec = precision_score(test_scam, test_scam_preds, zero_division=0)
    scam_rec = recall_score(test_scam, test_scam_preds, zero_division=0)
    scam_f1 = f1_score(test_scam, test_scam_preds, zero_division=0)
    tn, fp, fn, tp = confusion_matrix(test_scam, test_scam_preds, labels=[0, 1]).ravel()
    fpr = fp / max(fp + tn, 1)
    fnr = fn / max(fn + tp, 1)

    macro_f1 = f1_score(test_tactics, test_tac_preds, average="macro", zero_division=0)
    micro_f1 = f1_score(test_tactics, test_tac_preds, average="micro", zero_division=0)

    total_params = sum(p.numel() for p in model.parameters())

    results = {
        "model": "N-gram + MLP Baseline",
        "type": "Bag-of-Words / TF-IDF N-Gram MLP",
        "parameters": total_params,
        "train_samples": len(train_texts),
        "test_samples": len(test_texts),
        "scam_precision": round(float(scam_prec), 4),
        "scam_recall": round(float(scam_rec), 4),
        "scam_f1": round(float(scam_f1), 4),
        "tactic_macro_f1": round(float(macro_f1), 4),
        "tactic_micro_f1": round(float(micro_f1), 4),
        "fpr": round(float(fpr) * 100, 2),
        "fnr": round(float(fnr) * 100, 2),
        "latency_ms": round(float(latency_ms), 3)
    }

    print("\n--- BASELINE RESULTS ---")
    print(json.dumps(results, indent=2))
    with open(os.path.join(REPORTS_DIR, "baseline_benchmark.json"), "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)
    return results

if __name__ == "__main__":
    train_and_eval_baseline()
