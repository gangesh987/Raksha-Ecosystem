"""
RakshaCall Multilingual Semantic Model V2 Inference Engine.
Loads trained weights, tokenizer, and produces:
- scam_probability: float [0.0 to 1.0]
- is_scam: bool
- tactic_probabilities: Dict[str, float]
- detected_tactics: List[str]
- representation: List[float] (768-dimensional semantic embedding)
"""

import os
import sys

def _setup_torch_dll():
    if os.name != "nt":
        return
    try:
        import importlib.util
        spec = importlib.util.find_spec("torch")
        if spec and spec.origin:
            torch_lib = os.path.join(os.path.dirname(spec.origin), "lib")
            if os.path.isdir(torch_lib):
                os.environ["PATH"] = torch_lib + os.pathsep + os.environ.get("PATH", "")
                if hasattr(os, "add_dll_directory"):
                    os.add_dll_directory(torch_lib)
    except Exception:
        pass

_setup_torch_dll()

import json
import torch
import numpy as np
from typing import Dict, List, Any

# Model architecture definition matching trained weights
import torch.nn as nn

class SemanticSubwordEncoder(nn.Module):
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

    def forward(self, input_ids):
        embeds = self.embedding(input_ids)
        out, _ = self.encoder(embeds)
        pooled = torch.mean(out, dim=1)
        return self.layer_norm(pooled)

class RakshaCallMultilingualSemanticModel(nn.Module):
    def __init__(self, rep_dim: int = 768, num_tactics: int = 9, dropout: float = 0.20):
        super().__init__()
        self.encoder = SemanticSubwordEncoder(vocab_size=8000, embed_dim=256, rep_dim=rep_dim)
        self.shared_neck = nn.Sequential(
            nn.Linear(rep_dim, 256),
            nn.BatchNorm1d(256),
            nn.GELU(),
            nn.Dropout(dropout)
        )
        self.scam_head = nn.Sequential(
            nn.Linear(256, 64),
            nn.BatchNorm1d(64),
            nn.GELU(),
            nn.Dropout(dropout),
            nn.Linear(64, 1)
        )
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

class ProductionMultilingualClassifier:
    def __init__(self, model_dir: str = None):
        if model_dir is None:
            model_dir = os.path.dirname(__file__)
        self.model_dir = model_dir
        self.is_loaded = False
        self.config = {}
        self.tactics = []
        self.vocab = {}
        self.model = None

        self._load()

    def _load(self):
        config_path = os.path.join(self.model_dir, "model_config.json")
        weights_path = os.path.join(self.model_dir, "model_weights.pt")
        tok_path = os.path.join(self.model_dir, "tokenizer.json")

        if not os.path.exists(config_path) or not os.path.exists(weights_path) or not os.path.exists(tok_path):
            return

        with open(config_path, "r", encoding="utf-8") as f:
            self.config = json.load(f)
        self.tactics = self.config.get("tactics", [])

        with open(tok_path, "r", encoding="utf-8") as f:
            self.vocab = json.load(f)

        self.model = RakshaCallMultilingualSemanticModel(rep_dim=768, num_tactics=len(self.tactics))
        self.model.load_state_dict(torch.load(weights_path, map_location=torch.device("cpu")))
        self.model.eval()
        self.is_loaded = True

    def _tokenize(self, text: str, max_len: int = 64) -> torch.Tensor:
        words = text.lower().split()
        tokens = []
        for w in words:
            if w in self.vocab:
                tokens.append(self.vocab[w])
            else:
                added = False
                for n in (4, 3):
                    if len(w) >= n:
                        for i in range(len(w) - n + 1):
                            sub = w[i:i+n]
                            if sub in self.vocab:
                                tokens.append(self.vocab[sub])
                                added = True
                if not added:
                    tokens.append(1)
        if len(tokens) > max_len:
            tokens = tokens[:max_len]
        else:
            tokens = tokens + [0] * (max_len - len(tokens))
        return torch.tensor([tokens], dtype=torch.long)

    def predict(self, text: str) -> Dict[str, Any]:
        if not self.is_loaded:
            return {"error": "Model not loaded"}

        input_ids = self._tokenize(text)
        with torch.no_grad():
            scam_log, tactic_log, shared_rep = self.model(input_ids)
            scam_prob = float(torch.sigmoid(scam_log).item())
            tactic_probs_tensor = torch.sigmoid(tactic_log)[0].numpy()

        tactic_probs = {}
        detected = []
        for idx, t in enumerate(self.tactics):
            p = round(float(tactic_probs_tensor[idx]), 3)
            tactic_probs[t] = p
            if p >= 0.40:
                detected.append(t)

        return {
            "is_scam": scam_prob >= 0.50,
            "scam_probability": round(scam_prob, 4),
            "tactic_probabilities": tactic_probs,
            "detected_tactics": detected,
            "representation_dim": shared_rep.shape[1]
        }
