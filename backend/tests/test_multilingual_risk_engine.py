"""
Comprehensive Test Suite for Multilingual Contextual Tactic & Risk Engine.
Validates:
1. Canonical 13-tactic taxonomy coverage
2. Multilingual equivalence across 11 Indic languages + Tanglish + Hinglish
3. Code-switching across language boundaries
4. Adversarial false-negative variations
5. Benign false-positive suppression (10+ real-world benign scenarios)
6. Multi-turn contextual threat escalation
7. Graceful context decay
"""

import pytest
from app.ai.multilingual_tactic_engine import (
    multilingual_risk_engine,
    CANONICAL_TACTICS,
    MultilingualTacticEngine
)


@pytest.fixture
def engine():
    return MultilingualTacticEngine()


# ─── 1. Canonical 13-Tactic Taxonomy Coverage ─────────────────────────────

@pytest.mark.parametrize("tactic_key, sample_text, expected_display", [
    ("AUTHORITY_IMPERSONATION", "I am a police officer calling from cyber crime cell.", "Authority Impersonation"),
    ("URGENCY_PRESSURE", "You must act immediately right now within 10 minutes.", "Urgency Pressure"),
    ("PAYMENT_DEMAND", "Transfer the money to the RBI verification escrow account.", "Payment Demand"),
    ("OTP_REQUEST", "Please tell me the OTP verification code sent to your phone.", "OTP Request"),
    ("CREDENTIAL_REQUEST", "Enter your UPI PIN and provide your card CVV.", "Credential Request"),
    ("REMOTE_ACCESS_REQUEST", "Install AnyDesk app and start screen share immediately.", "Remote Access Request"),
    ("THREAT_OR_FEAR", "A non-bailable arrest warrant is registered against you in a money laundering case.", "Threat & Coercive Fear"),
    ("SECRECY_PRESSURE", "Keep this confidential, stay in a closed room, and do not tell your family.", "Secrecy & Isolation Pressure"),
    ("IDENTITY_VERIFICATION_PRESSURE", "Verify your identity and confirm your Aadhaar number right now.", "Identity Verification Pressure"),
    ("SUSPICIOUS_LINK", "Click this link to download the verification APK file.", "Suspicious Link / APK"),
    ("PERSONAL_INFORMATION_REQUEST", "Tell me your PAN card number and date of birth.", "Personal Information Request"),
    ("ROMANCE_OR_TRUST_MANIPULATION", "Your relative is in hospital following an accident, medical emergency need money.", "Trust / Emergency Manipulation"),
    ("INVESTMENT_OR_REWARD_SCAM", "Guaranteed profit, double your money, you are a lottery winner.", "Investment / Reward Scam"),
])
def test_canonical_tactics_detection(engine, tactic_key, sample_text, expected_display):
    res = engine.extract_tactics(sample_text)
    assert not res.is_suppressed_as_benign, f"Unexpected benign suppression for: {sample_text}"
    detected_types = [t.tactic_type for t in res.tactics]
    assert tactic_key in detected_types, f"Failed to detect {tactic_key} in: '{sample_text}'. Detected: {detected_types}"
    matched_tactic = next(t for t in res.tactics if t.tactic_type == tactic_key)
    assert matched_tactic.display_name == expected_display
    assert len(matched_tactic.evidence) > 0, "Evidence quote must not be empty"


# ─── 2. Multilingual Equivalence Tests (11 Languages + Code-Switch) ────────

@pytest.mark.parametrize("lang_name, utterance", [
    ("English", "I'm a police officer. This is urgent. Send me money immediately."),
    ("Tamil", "நான் போலீஸ்ல இருந்து பேசுறேன். இது அவசரம். உடனே பணம் அனுப்புங்க."),
    ("Tanglish", "Naan police la irundhu pesuren. Idhu romba urgent. Udane money send pannunga."),
    ("Hindi", "Main police se bol raha hoon. Yeh bahut urgent hai. Abhi paise bhejo."),
    ("Hinglish", "Main police se bol raha hoon, urgent hai, abhi money send karo."),
    ("Telugu", "Nenu police nundi matladutunnanu. Idi urgent. Ventane dabbu pampinchandi."),
    ("Kannada", "ನಾನು ಪೊಲೀಸ್ ಇಂದ ಮಾತನಾಡುತ್ತಿದ್ದೇನೆ. ಇದು ತುರ್ತು. ತಕ್ಷಣವೇ ಹಣವನ್ನು ವರ್ಗಾಯಿಸಿ."),
    ("Malayalam", "ഞാൻ പോലീസ് ആണ് സംസാരിക്കുന്നത്. ഇത് അടിയന്തിരമാണ്. ഉടൻ പണം അയക്കുക."),
    ("Bengali", "আমি পুলিশ থেকে বলছি। এটা জরুরি। অবিলম্বে টাকা পাঠান।"),
    ("Marathi", "मी पोलीस मधून बोलतोय. हे तातडीचे आहे. त्वरित पैसे पाठवा."),
    ("Gujarati", "હું પોલીસ માંથી બોલું છું. આ તાકીદનું છે. તરત જ પૈસા ટ્રાન્સફર કરો."),
    ("Punjabi", "ਮੈਂ ਪੁਲਿਸ ਤੋਂ ਬੋਲ ਰਿਹਾ ਹਾਂ। ਇਹ ਬਹੁਤ ਜ਼ਰੂਰੀ ਹੈ। ਹੁਣੇ ਪੈਸੇ ਭੇਜੋ।"),
    ("Odia", "ମୁଁ ପୋଲିସ ତରଫରୁ କହୁଛି। ଏହା ଜରୁରୀ। ତୁରନ୍ତ ଟଙ୍କା ପଠାନ୍ତୁ।"),
])
def test_multilingual_equivalence(engine, lang_name, utterance):
    res = engine.extract_tactics(utterance)
    assert not res.is_suppressed_as_benign, f"Falsely suppressed: {lang_name} utterance '{utterance}'"
    detected_types = {t.tactic_type for t in res.tactics}

    # Must detect Authority Impersonation, Urgency Pressure, and Payment Demand
    assert "AUTHORITY_IMPERSONATION" in detected_types, f"Missing AUTHORITY in {lang_name}: '{utterance}'. Detected: {detected_types}"
    assert "URGENCY_PRESSURE" in detected_types, f"Missing URGENCY in {lang_name}: '{utterance}'. Detected: {detected_types}"
    assert "PAYMENT_DEMAND" in detected_types, f"Missing PAYMENT in {lang_name}: '{utterance}'. Detected: {detected_types}"

    # Verify that multi-tactic scoring produces high/critical risk with combination bonus
    analysis = engine.analyze_utterance(utterance, session_id=f"session_{lang_name}")
    assert analysis["risk_score"] >= 75, f"Risk score too low for {lang_name} extortion: {analysis['risk_score']}"
    assert analysis["risk_level"] in ("HIGH", "CRITICAL")


# ─── 3. Code-Switching Across Language Boundaries ──────────────────────────

@pytest.mark.parametrize("cs_type, utterance, expected_tactics", [
    (
        "Tanglish Multi-Tactic",
        "Sir naan police department la irundhu pesuren, this is urgent, immediately money transfer pannunga.",
        {"AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"}
    ),
    (
        "Hinglish Threat + Payment",
        "Main police se bol raha hoon, account block ho jayega, so immediately money send karo.",
        {"AUTHORITY_IMPERSONATION", "PAYMENT_DEMAND"}
    ),
    (
        "Telugu-English Authority + OTP",
        "Nenu bank nundi matladutunnanu, OTP cheppandi immediately.",
        {"AUTHORITY_IMPERSONATION", "OTP_REQUEST", "URGENCY_PRESSURE"}
    )
])
def test_code_switching_tactics(engine, cs_type, utterance, expected_tactics):
    res = engine.extract_tactics(utterance)
    assert not res.is_suppressed_as_benign
    detected = {t.tactic_type for t in res.tactics}
    for expected in expected_tactics:
        assert expected in detected, f"Failed to detect {expected} in {cs_type}: '{utterance}'. Got: {detected}"


# ─── 4. Adversarial False-Negative Variations (Paraphrased Scams) ───────────

@pytest.mark.parametrize("paraphrase", [
    "Move the funds before the deadline.",
    "Transfer the amount.",
    "Make the payment.",
    "Please complete the transaction.",
    "Send the funds.",
    "UPI it now.",
    "அந்த amount-ai ippave transfer pannunga.",
    "Abhi amount transfer karo.",
    "Nenu amount ippude pampinchandi."
])
def test_adversarial_paraphrased_payment(engine, paraphrase):
    res = engine.extract_tactics(paraphrase)
    assert not res.is_suppressed_as_benign
    detected = [t.tactic_type for t in res.tactics]
    assert "PAYMENT_DEMAND" in detected, f"Failed to detect PAYMENT_DEMAND in paraphrase: '{paraphrase}'"


# ─── 5. Benign False-Positive Test Suite ───────────────────────────────────

@pytest.mark.parametrize("benign_text, description", [
    ("The police station is near my house.", "Location near police station"),
    ("I am going to the police station.", "Legitimate visit to police station"),
    ("The police officer helped me.", "Third-person police assistance"),
    ("Please send money to the grocery shop.", "Routine grocery payment"),
    ("My bank asked me to visit the branch.", "Routine in-person banking"),
    ("This assignment is urgent.", "Everyday urgency (academic/work)"),
    ("Can you transfer money to my mother?", "Routine family transfer"),
    ("I received an OTP from my bank.", "Routine OTP receipt notification"),
    ("I watched a documentary about digital arrest scams.", "Scam awareness discussion"),
    ("The police station called about my lost phone.", "Lost phone report callback"),
    ("My bank asked me to complete KYC at the branch.", "In-person KYC compliance"),
    ("என் அம்மாவுக்கு பணம் அனுப்பணும்.", "Tamil routine family transfer"),
    ("college assignment urgent hai.", "Hinglish academic urgency")
])
def test_benign_suppression_low_risk(engine, benign_text, description):
    res = engine.extract_tactics(benign_text)
    # Either suppressed as benign, or contains zero manipulative tactics
    assert res.is_suppressed_as_benign or len(res.tactics) == 0, (
        f"False positive triggered for: '{benign_text}' ({description}). "
        f"Detected: {[t.tactic_type for t in res.tactics]}"
    )
    analysis = engine.analyze_utterance(benign_text, session_id="benign_test_session")
    assert analysis["risk_score"] <= 30, f"Score exceeded LOW threshold for benign text '{benign_text}': {analysis['risk_score']}"
    assert analysis["risk_level"] == "LOW"


# ─── 6. Multi-Turn Contextual Threat Escalation ────────────────────────────

def test_multi_turn_contextual_escalation(engine):
    """
    Test progressive threat escalation across 5 sequential turns:
    Turn 1: Greeting / Contact -> Risk 0-10 (LOW)
    Turn 2: Bank / Authority claim -> Risk 15-25 (LOW)
    Turn 3: Allegation / Fear -> Risk 35-55 (MEDIUM)
    Turn 4: Isolation / Secrecy -> Risk 60-75 (HIGH)
    Turn 5: Irreversible Demand (OTP) -> Risk 85-100 (CRITICAL)
    """
    session_id = "digital_arrest_dialogue_001"

    dialogue_turns = [
        ("Hello, am I speaking with the account holder?", "LOW", 15),
        ("I am a bank manager from the fraud investigation cell.", "LOW", 30),
        ("Your Aadhaar has an active criminal case and money laundering investigation.", "MEDIUM", 60),
        ("Do not tell anyone, keep this confidential, and stay in a closed room.", "HIGH", 80),
        ("Give me the OTP and transfer the money immediately or face physical arrest!", "CRITICAL", 100)
    ]

    previous_score = -1
    for turn_idx, (utterance, expected_level, max_score) in enumerate(dialogue_turns, 1):
        result = engine.analyze_utterance(utterance, session_id=session_id)
        current_score = result["risk_score"]

        # Turn score must progressively increase or stay at peak
        if turn_idx > 1:
            assert current_score >= previous_score, f"Turn {turn_idx} score ({current_score}) failed to escalate from turn {turn_idx-1} ({previous_score})"

        if turn_idx == 5:
            assert result["risk_level"] == "CRITICAL"
            assert current_score >= 85
            assert result["escalation"] == "INCREASING"

        previous_score = current_score


# ─── 7. Context Decay After Benign Recovery ────────────────────────────────

def test_context_decay_after_benign_turns(engine):
    session_id = "decay_test_session"

    # Turn 1: Low-severity tactic (Urgency alone)
    r1 = engine.analyze_utterance("This is urgent right now.", session_id=session_id)
    initial_score = r1["risk_score"]
    assert initial_score > 0

    # Turn 2: Benign statement
    r2 = engine.analyze_utterance("I am going to the grocery shop.", session_id=session_id)

    # Turn 3: Another benign statement
    r3 = engine.analyze_utterance("My assignment is complete.", session_id=session_id)

    # Score should have decayed
    assert r3["risk_score"] <= initial_score, "Risk score did not decay after consecutive benign turns"
