"""
Unit tests for Production ScamClassifier PyTorch Service.
Verifies all 10 Phase 10 requirements:
1. model loads
2. model produces output
3. output shape correct
4. probabilities range [0,1]
5. all 9 tactics present
6. model version exposed
7. deterministic inference for identical input
8. batch inference
9. malformed input handling
10. empty input handling
"""

import pytest
import os
import sys

from app.ml.scam_classifier import ScamClassifier, get_scam_classifier, CANONICAL_TACTIC_KEYS


@pytest.fixture(scope="module")
def classifier():
    clf = get_scam_classifier()
    assert clf.is_loaded is True
    return clf


def test_1_model_loads(classifier):
    """Test 1: Model loads successfully into memory."""
    assert classifier.is_loaded is True
    assert classifier.model is not None
    assert classifier.parameter_count > 6_000_000
    assert classifier.load_time_ms > 0.0


def test_2_model_produces_output(classifier):
    """Test 2: Model produces output dictionary for real input."""
    text = "This is Mumbai Police Cyber Crime branch. An illegal narcotics parcel was seized in your name."
    res = classifier.predict(text)
    assert isinstance(res, dict)
    assert "scam_probability" in res
    assert "tactic_probabilities" in res


def test_3_output_shape_correct(classifier):
    """Test 3: Internal tensor representations match architecture."""
    text = "You must immediately transfer 50000 rupees to clearance account."
    token_ids = classifier._tokenize_single(text)
    assert len(token_ids) == 64
    res = classifier.predict(text)
    assert res.get("representation_dim") == 768


def test_4_probabilities_range(classifier):
    """Test 4: All probabilities fall strictly in [0.0, 1.0]."""
    test_phrases = [
        "Hello, how are you doing today?",
        "Transfer 50,000 rupees immediately to avoid arrest warrant.",
        "Bank officials will never ask for your confidential password."
    ]
    for text in test_phrases:
        res = classifier.predict(text)
        assert 0.0 <= res["scam_probability"] <= 1.0
        for tactic, p in res["tactic_probabilities"].items():
            assert 0.0 <= p <= 1.0, f"Tactic {tactic} has invalid probability: {p}"


def test_5_all_9_tactics_present(classifier):
    """Test 5: All 9 canonical tactics are present in the output dictionary."""
    res = classifier.predict("Please give me your 6 digit OTP right now.")
    probs = res["tactic_probabilities"]
    assert len(probs) == 9
    for canonical_tactic in CANONICAL_TACTIC_KEYS:
        assert canonical_tactic in probs, f"Missing canonical tactic: {canonical_tactic}"


def test_6_model_version_exposed(classifier):
    """Test 6: Model version and source metadata are exposed."""
    res = classifier.predict("Sample text")
    assert "model_version" in res
    assert res["model_version"] != ""
    assert res["model_version"] != "unknown"
    assert "model_source" in res
    assert os.path.isdir(res["model_source"])


def test_7_deterministic_inference_identical_input(classifier):
    """Test 7: Identical inputs produce identical probabilities (deterministic eval mode)."""
    text = "Room door lock pannunga. Yaar kittayum pesa koodaadhu."
    res1 = classifier.predict(text)
    res2 = classifier.predict(text)
    assert res1["scam_probability"] == res2["scam_probability"]
    assert res1["tactic_probabilities"] == res2["tactic_probabilities"]


def test_8_batch_inference(classifier):
    """Test 8: Batch inference matches single prediction outputs."""
    texts = [
        "Good morning, this is customer service.",
        "Your Aadhaar card was linked to money laundering.",
        "Install AnyDesk application to fix your internet router."
    ]
    batch_res = classifier.predict_batch(texts)
    assert len(batch_res) == 3
    for i, t in enumerate(texts):
        single_res = classifier.predict(t)
        assert batch_res[i]["scam_probability"] == single_res["scam_probability"]
        assert batch_res[i]["tactic_probabilities"] == single_res["tactic_probabilities"]


def test_9_malformed_input_handling(classifier):
    """Test 9: Malformed inputs (symbols, numbers, long repeated text) are handled gracefully."""
    malformed_inputs = [
        "!!! ??? ### $$$ %%% ^^^ &&& ***",
        "1234567890 9876543210 0000000000",
        "a" * 1000,
        "   \t\n   \r\n   ",
        "😀 😃 😄 😁 😆 😅 😂 🤣"
    ]
    for inp in malformed_inputs:
        res = classifier.predict(inp)
        assert isinstance(res, dict)
        assert 0.0 <= res["scam_probability"] <= 1.0


def test_10_empty_input_handling(classifier):
    """Test 10: Empty string does not crash and produces valid schema."""
    res = classifier.predict("")
    assert isinstance(res, dict)
    assert 0.0 <= res["scam_probability"] <= 1.0
    assert len(res["tactic_probabilities"]) == 9
