"""
Test suite for RakshaCall Multilingual Speech & Semantic Intelligence.
Verifies HuBERT representation, Language Identification, vernacular normalization,
and anti-false-alarm negative controls.
"""

import pytest
import numpy as np

from app.ai.multilingual_asr import asr_engine, HuBERTAcousticBackbone, LanguageIdentifier
from app.ai.jev_provider import LocalSemanticJEVProvider


def test_hubert_acoustic_feature_extraction():
    """Verify HuBERT acoustic backbone extracts 768-dim features from 16kHz PCM."""
    hubert = HuBERTAcousticBackbone(sample_rate=16000, hidden_dim=768)
    
    # 0.5s of synthetic sine wave audio (8000 samples @ 16kHz)
    t = np.linspace(0, 0.5, 8000)
    samples = (np.sin(2 * np.pi * 440 * t) * 30000).astype(np.int16)
    pcm_bytes = samples.tobytes()

    features = hubert.extract_features(pcm_bytes)
    assert features.frame_count > 0
    assert features.hidden_dimension == 768
    assert features.feature_embeddings.shape[1] == 768
    assert features.is_voiced is True
    assert features.mean_energy > 0.0


def test_language_identification():
    """Verify Language Identifier across all 5 target linguistic variants."""
    lid = LanguageIdentifier()

    # Tamil script
    lang, conf = lid.identify_language("உங்கள் ஆதார் கார்டு முடக்கப்பட்டுள்ளது")
    assert lang == "ta-IN"
    assert conf >= 0.90

    # Hindi script
    lang, conf = lid.identify_language("आपका बैंक खाता फ्रीज कर दिया गया है")
    assert lang == "hi-IN"
    assert conf >= 0.90

    # Tanglish (Romanized Tamil)
    lang, conf = lid.identify_language("Unga Aadhaar card police station-la irunthu verify panrom")
    assert lang == "ta-Latn"
    assert conf >= 0.70

    # Hinglish (Romanized Hindi)
    lang, conf = lid.identify_language("Aapka case police station me darj hua hai")
    assert lang == "hi-Latn"
    assert conf >= 0.70

    # English
    lang, conf = lid.identify_language("This is Cyber Crime Branch calling regarding a legal matter")
    assert lang == "en-IN"


def test_negative_controls_anti_false_alarm():
    """
    CRITICAL TEST: Negative controls must NEVER trigger false positive scam alarms.
    Statements advising against scams or discussing warnings must evaluate to ZERO risk.
    """
    jev = LocalSemanticJEVProvider()

    negative_samples = [
        ("Never share your OTP with anyone.", "en-IN"),
        ("Police will never ask you to transfer money to a private account.", "en-IN"),
        ("This call sounds like a scam, I am disconnecting.", "en-IN"),
        ("Bank-la irunthu yaaravathu OTP kettanga na eppovume sollatha.", "ta-Latn"),
        ("Panam anuppathinga, ithu fraud call.", "ta-Latn"),
        ("Apna OTP kisi ko mat dena, ye fraud hai.", "hi-Latn"),
    ]

    for utterance, lang in negative_samples:
        result = jev.analyze_window(
            current_utterance=utterance,
            conversation_context=[],
            detected_language=lang
        )
        assert result.primary_intent == "PROTECTIVE_ADVISORY", f"Failed for: {utterance}"
        assert result.is_irreversible_action is False
        assert all(prob == 0.0 for prob in result.tactic_probabilities.values()), f"Non-zero tactics for: {utterance}"


def test_multilingual_scam_intent_detection():
    """Verify 9-tactic detection across diverse Indian language scripts."""
    jev = LocalSemanticJEVProvider()

    # Tamil script Payment Demand
    ta_res = jev.analyze_window(
        current_utterance="ஆர்பிஐ சரிபார்ப்பு கணக்கிற்கு பணம் உடனே மாற்றவும்",
        conversation_context=[],
        detected_language="ta-IN"
    )
    assert "PAYMENT" in ta_res.tactic_probabilities
    assert ta_res.tactic_probabilities["PAYMENT"] >= 0.50
    assert ta_res.is_irreversible_action is True

    # Tanglish Credential Pressure
    tanglish_res = jev.analyze_window(
        current_utterance="Mobile-ku vantha 6 digit OTP fast-ah sollunga",
        conversation_context=[],
        detected_language="ta-Latn"
    )
    assert "CREDENTIAL" in tanglish_res.tactic_probabilities
    assert tanglish_res.tactic_probabilities["CREDENTIAL"] >= 0.50
    assert tanglish_res.is_irreversible_action is True

    # Hindi script Authority & Fear
    hi_res = jev.analyze_window(
        current_utterance="सीबीआई हेडक्वार्टर से अरेस्ट वारंट जारी हुआ है",
        conversation_context=[],
        detected_language="hi-IN"
    )
    assert any(t in hi_res.tactic_probabilities for t in ("AUTHORITY", "FEAR"))
