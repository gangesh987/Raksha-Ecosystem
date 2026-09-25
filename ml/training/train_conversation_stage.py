"""
RakshaCall Multi-Turn Conversation Stage Model.
Trains a temporal sequence model (Bidirectional GRU) to predict:
1. Current Scam Stage: CONTACT -> AUTHORITY -> FEAR -> ISOLATION -> DEMAND -> PAYMENT_CREDENTIAL -> CRITICAL_BRAKE
2. Stage Transition Confidence & Risk Progression Context
"""

import os
import sys
import json
import logging
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
import numpy as np

# Ensure project root is on sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("StageTrainer")

FINAL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "datasets", "final")
STAGE_MODEL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "models", "stage_model", "v1")

SCAM_STAGES = [
    "CONTACT",
    "AUTHORITY",
    "FEAR",
    "ISOLATION",
    "DEMAND",
    "PAYMENT_CREDENTIAL",
    "CRITICAL_BRAKE"
]
STAGE_TO_IDX = {stage: i for i, stage in enumerate(SCAM_STAGES)}

class ConversationStageDataset(Dataset):
    """Encodes multi-turn conversations into sequence tensors."""
    def __init__(self, conversations, max_turns: int = 8, feature_dim: int = 12):
        self.sequences = []
        self.targets = []
        
        for conv in conversations:
            turns = conv.get("turns", [])
            seq_feats = []
            
            for t_idx, turn in enumerate(turns):
                # 9 tactic features + speaker (1) + turn_id normalized (1) + is_scam (1) = 12 dims
                t_vec = [1.0 if t in turn.get("tactics", []) else 0.0 for t in range(9)]
                speaker_val = 1.0 if turn.get("speaker") == "caller" else 0.0
                turn_norm = min(turn.get("turn_id", t_idx + 1) / 10.0, 1.0)
                scam_val = 1.0 if turn.get("is_scam", False) else 0.0
                
                feat = t_vec + [speaker_val, turn_norm, scam_val]
                seq_feats.append(feat)
                
                # Target stage
                stage_name = turn.get("stage", "CONTACT")
                target_idx = STAGE_TO_IDX.get(stage_name, 0)
                
                # Add sequence up to this turn
                padded = seq_feats.copy()
                if len(padded) < max_turns:
                    padded = padded + [[0.0] * feature_dim] * (max_turns - len(padded))
                else:
                    padded = padded[-max_turns:]
                    
                self.sequences.append(padded)
                self.targets.append(target_idx)

        self.x = torch.tensor(self.sequences, dtype=torch.float32)
        self.y = torch.tensor(self.targets, dtype=torch.long)

    def __len__(self):
        return len(self.sequences)

    def __getitem__(self, idx):
        return self.x[idx], self.y[idx]

class TemporalConversationStageModel(nn.Module):
    """Bidirectional GRU for temporal conversation dynamics."""
    def __init__(self, input_dim: int = 12, hidden_dim: int = 64, num_stages: int = 7):
        super().__init__()
        self.gru = nn.GRU(
            input_size=input_dim,
            hidden_size=hidden_dim,
            num_layers=2,
            batch_first=True,
            bidirectional=True,
            dropout=0.15
        )
        self.classifier = nn.Sequential(
            nn.Linear(hidden_dim * 2, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim, num_stages)
        )

    def forward(self, x):
        out, _ = self.gru(x)
        # Pool across sequence / use final step
        pooled = out[:, -1, :]
        logits = self.classifier(pooled)
        return logits

def train_stage_model(epochs: int = 25, lr: float = 0.003, batch_size: int = 8):
    os.makedirs(STAGE_MODEL_DIR, exist_ok=True)
    
    # Load conversation records
    train_convs_path = os.path.join(FINAL_DIR, "train_conversations.jsonl")
    val_convs_path = os.path.join(FINAL_DIR, "val_conversations.jsonl")
    
    train_convs, val_convs = [], []
    with open(train_convs_path, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                train_convs.append(json.loads(line))
                
    with open(val_convs_path, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                val_convs.append(json.loads(line))

    train_ds = ConversationStageDataset(train_convs)
    val_ds = ConversationStageDataset(val_convs)

    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True)
    val_loader = DataLoader(val_ds, batch_size=batch_size, shuffle=False)

    model = TemporalConversationStageModel(input_dim=12, hidden_dim=64, num_stages=len(SCAM_STAGES))
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=lr, weight_decay=1e-4)

    logger.info("Training Temporal Conversation Stage Model...")
    best_acc = 0.0

    for epoch in range(1, epochs + 1):
        model.train()
        total_loss = 0.0
        for x, y in train_loader:
            optimizer.zero_grad()
            logits = model(x)
            loss = criterion(logits, y)
            loss.backward()
            optimizer.step()
            total_loss += loss.item()

        # Eval
        model.eval()
        correct, total = 0, 0
        with torch.no_grad():
            for x, y in val_loader:
                logits = model(x)
                preds = torch.argmax(logits, dim=1)
                correct += (preds == y).sum().item()
                total += y.size(0)

        val_acc = correct / max(total, 1)
        if val_acc > best_acc or epoch == epochs:
            best_acc = val_acc
            torch.save(model.state_dict(), os.path.join(STAGE_MODEL_DIR, "stage_model_weights.pt"))

        if epoch % 5 == 0 or epoch == epochs:
            logger.info(f"Epoch {epoch:02d}/{epochs:02d} | Train Loss: {total_loss/len(train_loader):.4f} | Val Accuracy: {val_acc:.3f}")

    # Export metadata
    stage_config = {
        "model_name": "RakshaCall-Conversation-Stage-GRU-v1",
        "architecture": "Bidirectional-GRU",
        "input_dim": 12,
        "hidden_dim": 64,
        "stages": SCAM_STAGES,
        "num_stages": len(SCAM_STAGES),
        "validation_accuracy": best_acc
    }
    with open(os.path.join(STAGE_MODEL_DIR, "stage_config.json"), "w", encoding="utf-8") as f:
        json.dump(stage_config, f, indent=2)

    logger.info(f"Stage Model exported to {STAGE_MODEL_DIR}")
    return stage_config

if __name__ == "__main__":
    train_stage_model()
