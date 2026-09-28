"""
RakshaCall Trained Model Inference Runtime.
Loads the trained MultilingualTacticModel weights and vectorizer vocabulary.
Executes genuine model forward passes to generate probabilities for:
- scam_probability (binary head)
- 9 tactic_probabilities (multi-label heads)
"""

import os
import sys
import json
import pickle
import numpy as np
import torch
from typing import Dict, Any, List

# Ensure project root is on sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(__file__))))))

from ml.training.train_multilingual_tactic import MultilingualTacticModel
from ml.preprocessing.label_mapper import RAKSHACALL_TACTICS

MODEL_DIR = os.path.dirname(__file__)

class TrainedScamClassifier:
    """Production inference runtime for the trained multilingual scam & tactic model."""

    def __init__(self):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.vectorizer_path = os.path.join(MODEL_DIR, "vectorizer.pkl")
        self.weights_path = os.path.join(MODEL_DIR, "model_weights.pt")
        self.config_path = os.path.join(MODEL_DIR, "model_config.json")

        self.model = None
        self.vectorizer = None
        self.config = {}
        self.is_loaded = False
        self._load()

    def _load(self):
        if not (os.path.exists(self.vectorizer_path) and os.path.exists(self.weights_path)):
            # Weights not trained yet; will be ready once training script runs
            return

        with open(self.config_path, "r", encoding="utf-8") as f:
            self.config = json.load(f)

        with open(self.vectorizer_path, "rb") as f:
            self.vectorizer = pickle.load(f)

        input_dim = self.config.get("input_dim", len(self.vectorizer.vocabulary_))
        hidden_dim = self.config.get("hidden_dim", 128)
        num_tactics = self.config.get("num_tactics", len(RAKSHACALL_TACTICS))

        self.model = MultilingualTacticModel(input_dim=input_dim, hidden_dim=hidden_dim, num_tactics=num_tactics)
        self.model.load_state_dict(torch.load(self.weights_path, map_location=self.device))
        self.model.to(self.device)
        self.model.eval()
        self.is_loaded = True

    def predict(self, text: str) -> Dict[str, Any]:
        """
        Runs neural inference on the input transcript.
        Returns:
            - is_scam (bool)
            - scam_probability (float)
            - tactic_probabilities (Dict[str, float])
            - detected_tactics (List[str])
        """
        if not self.is_loaded:
            self._load()
            if not self.is_loaded:
                raise RuntimeError("Trained model weights not found. Please run training pipeline first.")

        # Vectorize
        x_np = self.vectorizer.transform([text]).toarray()
        x_tensor = torch.tensor(x_np, dtype=torch.float32).to(self.device)

        with torch.no_grad():
            scam_logits, tactic_logits = self.model(x_tensor)
            scam_prob = float(torch.sigmoid(scam_logits).cpu().item())
            tactic_probs = torch.sigmoid(tactic_logits).cpu().numpy().flatten()

        tactic_map = {}
        detected = []
        for i, tactic_name in enumerate(RAKSHACALL_TACTICS):
            prob = float(tactic_probs[i])
            tactic_map[tactic_name] = round(prob, 4)
            if prob >= 0.40:
                detected.append(tactic_name)

        return {
            "is_scam": scam_prob >= 0.50,
            "scam_probability": round(scam_prob, 4),
            "tactic_probabilities": tactic_map,
            "detected_tactics": detected,
            "model_version": self.config.get("model_name", "RakshaCall-Multilingual-Tactic-v1")
        }
