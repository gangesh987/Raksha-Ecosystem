"""
RakshaCall Model Provider Interfaces & Hybrid Integration.
Connects the trained data-driven PyTorch models to the real-time gRPC backend.
Preserves the deterministic guardrail strictly as a SAFETY FLOOR,
while ML Semantic Detection provides genuine probabilistic predictions.
"""

from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional
import os
import sys

# Ensure project root is on sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(__file__)))))

from ml.preprocessing.label_mapper import RAKSHACALL_TACTICS

class ScamClassifierProvider(ABC):
    """Abstract interface for scam and tactic classification providers."""
    
    @abstractmethod
    def predict(self, text: str) -> Dict[str, Any]:
        """
        Predicts scam and tactic probabilities.
        Returns dict containing:
        - is_scam (bool)
        - scam_probability (float)
        - tactic_probabilities (Dict[str, float])
        - detected_tactics (List[str])
        - source (str): Provider identity
        """
        pass

class TrainedMLScamClassifierProvider(ScamClassifierProvider):
    """
    ML Provider that executes genuine neural inference
    using the trained PyTorch Multilingual Tactic Model.
    """
    def __init__(self):
        from ml.models.scam_classifier.v1.inference import TrainedScamClassifier
        self.classifier = TrainedScamClassifier()

    def predict(self, text: str) -> Dict[str, Any]:
        if not self.classifier.is_loaded:
            self.classifier._load()
            
        if self.classifier.is_loaded:
            res = self.classifier.predict(text)
            res["source"] = "TRAINED_ML_NEURAL_ENCODER"
            return res
        else:
            # Fallback if weights not yet generated
            return {
                "is_scam": False,
                "scam_probability": 0.0,
                "tactic_probabilities": {t: 0.0 for t in RAKSHACALL_TACTICS},
                "detected_tactics": [],
                "source": "ML_MODEL_UNAVAILABLE"
            }

class DeterministicSafetyFloorGuardrail:
    """
    DETERMINISTIC SAFETY GUARDRAIL (Safety Floor).
    Only intervenes for explicit, unambiguous, irreversible harm signals
    (e.g., direct OTP solicitation or remote control app installation).
    Distinguished from ML semantic detection.
    """
    def check_safety_floor(self, text: str) -> Optional[Dict[str, Any]]:
        lower = text.lower()
        
        # Explicit OTP theft (English, Tanglish, Hinglish, Tamil, Hindi)
        otp_patterns = [
            "tell me otp", "share your otp", "otp sollunga", "otp batao", "otp send",
            "ஓடிபி", "ஓடிபி எண்ணை", "சொல்லுங்கள்", "ओटीपी", "ओटीपी बताइए"
        ]
        if any(p in lower or p in text for p in otp_patterns):
            return {
                "tactic": "CREDENTIAL_OTP_PRESSURE",
                "probability": 0.99,
                "confidence": 0.99,
                "reason": "Direct credential / OTP extraction detected by safety floor",
                "source": "DETERMINISTIC_SAFETY_GUARDRAIL"
            }

        # Explicit payment demand
        payment_patterns = [
            "transfer money immediately", "panam anuppunga", "panatha transfer", "paise transfer",
            "பணத்தை மாற்ற", "பணத்தை உடனடியாக", "पैसे ट्रांसफर", "खाते में पैसे"
        ]
        if any(p in lower or p in text for p in payment_patterns):
            return {
                "tactic": "PAYMENT_DEMAND",
                "probability": 0.99,
                "confidence": 0.99,
                "reason": "Direct payment / fund transfer demand detected by safety floor",
                "source": "DETERMINISTIC_SAFETY_GUARDRAIL"
            }
            
        # Explicit remote access takeover
        remote_patterns = [
            "install anydesk", "install teamviewer", "download anydesk", "screen share",
            "anydesk download", "செயலியை பதிவிறக்க", "ऐप इंस्टॉल"
        ]
        if any(p in lower or p in text for p in remote_patterns):
            return {
                "tactic": "REMOTE_ACCESS_PRESSURE",
                "probability": 0.99,
                "confidence": 0.99,
                "reason": "Explicit remote screen sharing app installation detected by safety floor",
                "source": "DETERMINISTIC_SAFETY_GUARDRAIL"
            }
            
        return None

class HybridSemanticDecisionEngine:
    """
    Hybrid Decision Engine:
    1. Primary: ML Neural Model Inference (Produces genuine probabilities across 9 tactics)
    2. Cloud LLM Enhancement: Groq / Gemini (Evaluates conversational nuance when API key is active)
    3. Semantic Prototype Matching: Local JEV (Ensures high recall on colloquial vernacular patterns)
    4. Safety Floor: Deterministic Safety Guardrail (Ensures explicit irreversible actions are never missed)
    """
    def __init__(self):
        from . import groq_provider
        from .jev_provider import LocalSemanticJEVProvider
        self.ml_provider = TrainedMLScamClassifierProvider()
        self.safety_floor = DeterministicSafetyFloorGuardrail()
        self.local_jev = LocalSemanticJEVProvider()
        self.groq_provider = groq_provider

    def evaluate_turn(self, text: str, context: Optional[List[str]] = None) -> Dict[str, Any]:
        # 1. Run local ML neural inference
        ml_result = self.ml_provider.predict(text)
        combined_tactics = dict(ml_result["tactic_probabilities"])
        scam_prob = ml_result["scam_probability"]
        decision_sources = [ml_result["source"]]

        # 2. Check Cloud LLM (Groq / Gemini)
        if self.groq_provider.enabled():
            try:
                groq_res = self.groq_provider.analyze(text)
                if groq_res and isinstance(groq_res, dict):
                    decision_sources.append("GROQ_CLOUD_LLM")
                    cloud_tactics = groq_res.get("tactics", [])
                    cloud_risk = float(groq_res.get("risk_score", 0.0))
                    scam_prob = max(scam_prob, cloud_risk)
                    for t in cloud_tactics:
                        # Map to canonical names
                        for full_t in RAKSHACALL_TACTICS:
                            if t in full_t or full_t.startswith(t):
                                combined_tactics[full_t] = max(combined_tactics.get(full_t, 0.0), 0.92)
            except Exception as e:
                pass

        # 3. Check Local Semantic JEV fallback
        jev_res = self.local_jev.analyze_window(text, context or [], "en")
        for short_t, prob in jev_res.tactic_probabilities.items():
            if prob >= 0.40:
                for full_t in RAKSHACALL_TACTICS:
                    if short_t in full_t or full_t.startswith(short_t):
                        combined_tactics[full_t] = max(combined_tactics.get(full_t, 0.0), prob)

        # 4. Check safety floor
        floor_trigger = self.safety_floor.check_safety_floor(text)
        safety_floor_activated = False

        if floor_trigger:
            floor_tactic = floor_trigger["tactic"]
            combined_tactics[floor_tactic] = max(combined_tactics.get(floor_tactic, 0.0), floor_trigger["probability"])
            scam_prob = max(scam_prob, 0.98)
            decision_sources.append("DETERMINISTIC_SAFETY_GUARDRAIL")
            safety_floor_activated = True

        detected = [t for t, p in combined_tactics.items() if p >= 0.40]

        return {
            "is_scam": scam_prob >= 0.45 or len(detected) >= 1,
            "scam_probability": round(scam_prob, 4),
            "tactic_probabilities": combined_tactics,
            "detected_tactics": detected,
            "decision_source": " + ".join(decision_sources),
            "safety_floor_activated": safety_floor_activated,
            "floor_trigger_detail": floor_trigger
        }
