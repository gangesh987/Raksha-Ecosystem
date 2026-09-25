"""
RakshaCall Multilingual Multi-Task Tactic Classifier.
Trains a neural network to simultaneously predict:
1. Binary Scam Probability (is_scam)
2. 9 Multi-Label Tactic Probabilities
Uses shared multilingual representation, produces genuine probabilities from model inference.
"""

import os
import sys
import json
import logging
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from sklearn.feature_extraction.text import TfidfVectorizer
import numpy as np

# Ensure project root is on sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

from ml.preprocessing.label_mapper import RAKSHACALL_TACTICS

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("TacticTrainer")

FINAL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "datasets", "final")
MODEL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "models", "scam_classifier", "v1")

class TextTurnDataset(Dataset):
    def __init__(self, texts, is_scam_labels, tactic_labels, vectorizer=None, is_train=True):
        self.texts = texts
        self.is_scam = torch.tensor(is_scam_labels, dtype=torch.float32).unsqueeze(1)
        self.tactics = torch.tensor(tactic_labels, dtype=torch.float32)
        
        if is_train:
            self.vectorizer = TfidfVectorizer(max_features=2500, ngram_range=(1, 3), token_pattern=r"(?u)\b\w+\b")
            self.features = torch.tensor(self.vectorizer.fit_transform(texts).toarray(), dtype=torch.float32)
        else:
            self.vectorizer = vectorizer
            self.features = torch.tensor(self.vectorizer.transform(texts).toarray(), dtype=torch.float32)

    def __len__(self):
        return len(self.texts)

    def __getitem__(self, idx):
        return self.features[idx], self.is_scam[idx], self.tactics[idx]

class MultilingualTacticModel(nn.Module):
    """
    Multi-Task Neural Architecture:
    Shared Multilingual Representation -> [Scam Head] + [9-Tactic Multi-Label Heads]
    """
    def __init__(self, input_dim: int, hidden_dim: int = 128, num_tactics: int = 9, dropout: float = 0.2):
        super().__init__()
        self.shared_encoder = nn.Sequential(
            nn.Linear(input_dim, hidden_dim),
            nn.BatchNorm1d(hidden_dim),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Linear(hidden_dim, hidden_dim // 2),
            nn.BatchNorm1d(hidden_dim // 2),
            nn.ReLU(),
            nn.Dropout(dropout)
        )
        # Shared representation dimension
        rep_dim = hidden_dim // 2

        # Head 1: Binary Scam Classifier (Logits)
        self.scam_head = nn.Linear(rep_dim, 1)

        # Head 2: 9 Multi-Label Tactic Classifiers (Logits)
        self.tactic_head = nn.Linear(rep_dim, num_tactics)

    def forward(self, x):
        shared = self.shared_encoder(x)
        scam_logits = self.scam_head(shared)
        tactic_logits = self.tactic_head(shared)
        return scam_logits, tactic_logits

def load_data(file_name: str):
    file_path = os.path.join(FINAL_DIR, file_name)
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Dataset split not found at {file_path}")

    texts, scam_labels, tactic_labels = [], [], []
    with open(file_path, "r", encoding="utf-8") as f:
        for line in f:
            if not line.strip():
                continue
            item = json.loads(line)
            texts.append(item["text"])
            scam_labels.append(1.0 if item["is_scam"] else 0.0)

            # Build multi-hot tactic vector
            t_vec = [1.0 if t in item.get("tactics", []) else 0.0 for t in RAKSHACALL_TACTICS]
            tactic_labels.append(t_vec)

    return texts, scam_labels, tactic_labels

def train_model(epochs: int = 25, lr: float = 0.005, batch_size: int = 16):
    os.makedirs(MODEL_DIR, exist_ok=True)
    logger.info("Loading normalized train/val splits...")
    
    train_texts, train_scam, train_tactics = load_data("train_turns.jsonl")
    val_texts, val_scam, val_tactics = load_data("val_turns.jsonl")

    logger.info(f"Train samples: {len(train_texts)} | Val samples: {len(val_texts)}")

    # Vectorize and build PyTorch datasets
    train_ds = TextTurnDataset(train_texts, train_scam, train_tactics, is_train=True)
    val_ds = TextTurnDataset(val_texts, val_scam, val_tactics, vectorizer=train_ds.vectorizer, is_train=False)

    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True)
    val_loader = DataLoader(val_ds, batch_size=batch_size, shuffle=False)

    input_dim = train_ds.features.shape[1]
    model = MultilingualTacticModel(input_dim=input_dim, hidden_dim=128, num_tactics=len(RAKSHACALL_TACTICS))

    criterion_scam = nn.BCEWithLogitsLoss()
    criterion_tactics = nn.BCEWithLogitsLoss()
    optimizer = optim.AdamW(model.parameters(), lr=lr, weight_decay=1e-4)

    logger.info("Beginning multi-task training...")
    best_val_loss = float("inf")
    metrics_history = []

    for epoch in range(1, epochs + 1):
        model.train()
        total_train_loss = 0.0
        for x, y_scam, y_tactics in train_loader:
            optimizer.zero_grad()
            scam_logits, tactic_logits = model(x)
            loss_scam = criterion_scam(scam_logits, y_scam)
            loss_tactics = criterion_tactics(tactic_logits, y_tactics)
            # Weighted loss balancing scam detection and multi-label tactic classification
            loss = loss_scam + 1.5 * loss_tactics
            loss.backward()
            optimizer.step()
            total_train_loss += loss.item()

        # Validation
        model.eval()
        total_val_loss = 0.0
        scam_preds, scam_targets = [], []
        tactic_preds, tactic_targets = [], []

        with torch.no_grad():
            for x, y_scam, y_tactics in val_loader:
                scam_logits, tactic_logits = model(x)
                loss_scam = criterion_scam(scam_logits, y_scam)
                loss_tactics = criterion_tactics(tactic_logits, y_tactics)
                loss = loss_scam + 1.5 * loss_tactics
                total_val_loss += loss.item()

                probs_scam = torch.sigmoid(scam_logits).numpy()
                probs_tactics = torch.sigmoid(tactic_logits).numpy()

                scam_preds.extend((probs_scam > 0.5).astype(int).flatten())
                scam_targets.extend(y_scam.numpy().flatten())

                tactic_preds.extend((probs_tactics > 0.4).astype(int))
                tactic_targets.extend(y_tactics.numpy())

        avg_train_loss = total_train_loss / len(train_loader)
        avg_val_loss = total_val_loss / len(val_loader)
        
        # Calculate Micro/Macro F1
        from sklearn.metrics import f1_score, precision_score, recall_score
        scam_f1 = f1_score(scam_targets, scam_preds, zero_division=0)
        tactic_macro_f1 = f1_score(tactic_targets, tactic_preds, average="macro", zero_division=0)
        tactic_micro_f1 = f1_score(tactic_targets, tactic_preds, average="micro", zero_division=0)

        metrics_history.append({
            "epoch": epoch,
            "train_loss": avg_train_loss,
            "val_loss": avg_val_loss,
            "scam_f1": scam_f1,
            "tactic_macro_f1": tactic_macro_f1,
            "tactic_micro_f1": tactic_micro_f1
        })

        if epoch % 5 == 0 or epoch == epochs:
            logger.info(f"Epoch {epoch:02d}/{epochs:02d} | Train Loss: {avg_train_loss:.4f} | Val Loss: {avg_val_loss:.4f} | Scam F1: {scam_f1:.3f} | Tactic Macro F1: {tactic_macro_f1:.3f}")

        if avg_val_loss < best_val_loss:
            best_val_loss = avg_val_loss
            # Save checkpoint
            torch.save(model.state_dict(), os.path.join(MODEL_DIR, "model_weights.pt"))
            # Save vectorizer vocabulary & IDF
            import pickle
            with open(os.path.join(MODEL_DIR, "vectorizer.pkl"), "wb") as vf:
                pickle.dump(train_ds.vectorizer, vf)

    # Save model config and label map
    model_config = {
        "model_name": "RakshaCall-Multilingual-Tactic-v1",
        "architecture": "MultiTaskNeuralEncoder",
        "input_dim": input_dim,
        "hidden_dim": 128,
        "num_tactics": len(RAKSHACALL_TACTICS),
        "tactics": RAKSHACALL_TACTICS,
        "languages_supported": ["ta", "ta-Latn", "hi", "hi-Latn", "en"]
    }
    with open(os.path.join(MODEL_DIR, "model_config.json"), "w", encoding="utf-8") as cf:
        json.dump(model_config, cf, indent=2)

    with open(os.path.join(MODEL_DIR, "label_map.json"), "w", encoding="utf-8") as lf:
        json.dump({i: t for i, t in enumerate(RAKSHACALL_TACTICS)}, lf, indent=2)

    with open(os.path.join(MODEL_DIR, "metrics.json"), "w", encoding="utf-8") as mf:
        json.dump(metrics_history[-1], mf, indent=2)

    logger.info(f"Model exported successfully to {MODEL_DIR}")
    return model_config, metrics_history[-1]

if __name__ == "__main__":
    train_model()
