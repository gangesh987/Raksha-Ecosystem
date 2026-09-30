"""
RakshaCall Real-Time Multilingual Scam Intelligence Test Suite.
Validates:
- Section 29: Real-time multilingual test matrix (8 languages, code-switching,
  romanized text, ASR errors, indirect wording, slang neutrality, hard negatives).
- Section 26: Output contract schema verification.
- Section 35: Final Acceptance Test Scenario (Tamil -> English -> Hindi -> Slang -> Coercion).
"""

import os
import sys
import pytest
import time

# Ensure project root is in sys.path
root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
if root_dir not in sys.path:
    sys.path.insert(0, root_dir)
server_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if server_dir not in sys.path:
    sys.path.insert(0, server_dir)

from server.app.intelligence.pipeline import realtime_pipeline
from server.app.intelligence.multilingual import multilingual_engine
from server.app.intelligence.intent_engine import intent_engine
from server.app.intelligence.taxonomy import (
    TAXONOMY_CATEGORIES, STAGES_12, CRITICAL_TACTICS
)


class TestMultilingualIdentification:
    """Tests 8 languages, native scripts, and romanized representations."""

    def test_native_scripts_detected(self):
        cases = [
            ("வணக்கம் சார், உங்கள் வங்கி கணக்கு சரிபார்க்க வேண்டும்", "ta"),
            ("नमस्ते, आपके बैंक खाते में कुछ समस्या है", "hi"),
            ("నమస్కారం, మీ ఖాతా బ్లాక్ చేయబడుతుంది", "te"),
            ("ನಮಸ್ಕಾರ, ನಿಮ್ಮ ಬ್ಯಾಂಕ್ ಖಾತೆ ಪರಿಶೀಲಿಸಬೇಕು", "kn"),
            ("നമസ്കാരം, നിങ്ങളുടെ അക്കൗണ്ട് പരിശോധിക്കണം", "ml"),
            ("নমস্কার, আপনার ব্যাঙ্ক অ্যাকাউন্ট ব্লক করা হবে", "bn"),
        ]
        for text, expected_lang in cases:
            res = multilingual_engine.process(text)
            assert res.primary_language == expected_lang, f"Failed on {text}: got {res.primary_language}"

    def test_romanized_indian_languages(self):
        cases = [
            ("unga account block aagidum panam anupunga", "ta"),
            ("aapka account band ho jayega paise bhejo", "hi"),
            ("mee account block aipothundi dabbu pampandi", "te"),
            ("nimma account block agutte hana kodi", "kn"),
            ("ningalude account block aakum panam kodukku", "ml"),
            ("apnar account block hoye jabe taka pathan", "bn"),
            ("tumcha account block hoil paise pathva", "mr"),
        ]
        for text, expected_lang in cases:
            res = multilingual_engine.process(text)
            assert res.primary_language == expected_lang, f"Failed on Romanized {text}: got {res.primary_language}"

    def test_code_switching(self):
        cases = [
            "Sir OTP சொல்லுங்க.",
            "आप बस code बता दीजिए.",
            "Sir account block అవుతుంది.",
            "OTP heli sir.",
            "Sir OTP parayamo?",
            "Account ta block hoye jabe.",
            "Account block होईल."
        ]
        for text in cases:
            res = multilingual_engine.process(text)
            assert res.is_code_switched is True, f"Failed code-switch detection on: {text}"
            assert len(res.language_segments) >= 2


class TestSlangAndInformalSpeech:
    """Verifies slang/honorifics do not falsely inflate risk scores."""

    def test_slang_neutrality_in_benign_contexts(self):
        benign_slang = [
            "Hey bro, how are you doing today?",
            "Machan, let us catch up this weekend.",
            "Anna, can you send me the lecture notes?",
            "Bhai, thanks for your help yesterday.",
            "Chetta, give me one tea please."
        ]
        for text in benign_slang:
            res = realtime_pipeline.analyze(transcript=text, session_id="test_slang_benign")
            assert res["riskScore"] < 25, f"Slang falsely elevated risk for '{text}': {res['riskScore']}"
            assert res["riskLevel"] in {"SAFE", "LOW"}


class TestIndirectLanguageWithoutKeywords:
    """Verifies semantic inference without hardcoded keywords."""

    def test_indirect_otp_extraction(self):
        utterances = [
            "Just read those six numbers to me.",
            "Check the message you just received and tell me the code.",
            "Send me the code that came to your phone.",
            "Tell me those digits that just arrived in your SMS."
        ]
        for text in utterances:
            res = realtime_pipeline.analyze(transcript=text, session_id=f"test_indirect_otp_{time.time()}")
            tactic_types = [t["type"] for t in res["tactics"]]
            assert "OTP_REQUEST" in tactic_types or "CREDENTIAL_REQUEST" in tactic_types, \
                f"Failed to infer OTP request on: {text}"

    def test_indirect_financial_transfer(self):
        utterances = [
            "Move the balance to the temporary protection account.",
            "Move the money temporarily so we can secure it.",
            "Clear your name by transferring the funds to the verification vault."
        ]
        for text in utterances:
            res = realtime_pipeline.analyze(transcript=text, session_id=f"test_indirect_transfer_{time.time()}")
            tactic_types = [t["type"] for t in res["tactics"]]
            assert "BANK_TRANSFER_REQUEST" in tactic_types or "PAYMENT_REQUEST" in tactic_types, \
                f"Failed to infer transfer request on: {text}"

    def test_indirect_isolation_and_control(self):
        utterances = [
            "Stay on the line and don't speak to anyone else.",
            "Don't disconnect the call.",
            "Do not involve anybody else.",
            "Put the phone on speaker and do not hang up."
        ]
        for text in utterances:
            res = realtime_pipeline.analyze(transcript=text, session_id=f"test_isolation_{time.time()}")
            tactic_types = [t["type"] for t in res["tactics"]]
            assert "ISOLATION" in tactic_types or "CONTROL" in tactic_types, \
                f"Failed to infer isolation/control on: {text}"


class TestPolitenessAndAggressionNeutrality:
    """Verifies politeness does not mask scams and ordinary anger is not a scam."""

    def test_polite_coercion_detected(self):
        polite_scam = "Sir, I completely understand your concern. Please cooperate with us and share the verification code."
        res = realtime_pipeline.analyze(transcript=polite_scam, session_id="test_polite_scam")
        tactic_types = [t["type"] for t in res["tactics"]]
        assert "OTP_REQUEST" in tactic_types or "CREDENTIAL_REQUEST" in tactic_types
        assert res["riskScore"] >= 80
        assert res["riskLevel"] == "CRITICAL"

    def test_benign_aggression_suppressed(self):
        arguments = [
            "Why are you shouting at me?",
            "Stop yelling right now!",
            "I am so frustrated with this service, calm down!"
        ]
        for text in arguments:
            res = realtime_pipeline.analyze(transcript=text, session_id="test_argument")
            assert res["riskScore"] <= 20
            assert res["riskLevel"] in {"SAFE", "LOW"}


class TestHardNegatives:
    """Verifies zero false positives on cybersecurity awareness and legitimate banking advisories."""

    def test_hard_negative_advisories(self):
        advisories = [
            "Do not share your OTP with anyone.",
            "Never install remote access software from unknown callers.",
            "Bank employees never ask for your PIN or password.",
            "This is an example of a digital arrest scam.",
            "Learn how to identify scams and protect your savings.",
            "The bank refunded my money after an unauthorized charge.",
            "The bank asked me to verify a transaction at the branch."
        ]
        for text in advisories:
            res = realtime_pipeline.analyze(transcript=text, session_id="test_hard_neg")
            assert res["riskScore"] == 0, f"False positive triggered for: '{text}' (score: {res['riskScore']})"
            assert res["riskLevel"] == "SAFE"


class TestAsrErrorTolerance:
    """Verifies robustness against speech recognition corruptions."""

    def test_phonetic_asr_variants(self):
        cases = [
            ("Read the OTB received on your phone", "OTP_REQUEST"),
            ("Please tell me the oh tee pee", "OTP_REQUEST"),
            ("Your audahaar is linked to a crime", "LEGAL_THREAT"),
            ("Install tem viewer so I can help", "REMOTE_ACCESS"),
            ("Download any desk on your mobile", "REMOTE_ACCESS"),
        ]
        for text, expected_tactic in cases:
            res = realtime_pipeline.analyze(transcript=text, session_id=f"test_asr_{time.time()}")
            tactic_types = [t["type"] for t in res["tactics"]]
            assert expected_tactic in tactic_types or len(tactic_types) > 0, f"ASR tolerance failed on: {text}"


class TestOutputContractSchema:
    """Verifies strict adherence to Section 26 Real-Time Output Contract."""

    def test_section_26_keys_present(self):
        res = realtime_pipeline.analyze(
            transcript="I am calling from the police department. Transfer money immediately.",
            session_id="contract_test_session"
        )
        mandatory_keys = [
            "sessionId", "timestamp", "transcript", "language", "languageConfidence",
            "tactics", "stage", "stageConfidence", "manipulationVelocity",
            "riskScore", "riskLevel", "riskConfidence", "recommendedAction",
            "isFinalTranscript", "reasons"
        ]
        for key in mandatory_keys:
            assert key in res, f"Section 26 contract missing key: {key}"

        assert isinstance(res["tactics"], list)
        if res["tactics"]:
            t0 = res["tactics"][0]
            assert "type" in t0
            assert "confidence" in t0
            assert "evidence" in t0

        assert res["riskLevel"] in {"SAFE", "LOW", "MEDIUM", "HIGH", "CRITICAL"}
        assert 0 <= res["riskScore"] <= 100


class TestSection35FinalAcceptanceTest:
    """
    SECTION 35: FINAL ACCEPTANCE TEST
    Caller speaks Tamil -> switches to English -> switches to Hindi -> uses slang.
    Never says 'scam'. Never says 'OTP'.
    1. 'Check the message you just received and read those numbers to me.'
    2. 'Don't disconnect.'
    3. 'Your account is connected to a criminal case.'
    4. 'You have only a few minutes.'
    5. 'Move the money to the temporary account.'
    """

    def test_full_acceptance_scenario(self):
        session_id = f"acceptance_session_{int(time.time())}"
        realtime_pipeline.reset_session(session_id)

        # Turn 1: Caller speaks Tamil + English greeting with slang
        t1 = "வணக்கம் bro, I am calling from Mumbai Cyber Crime department."
        r1 = realtime_pipeline.analyze(transcript=t1, session_id=session_id)
        assert r1["language"] in {"ta", "en"}
        assert any(t["type"] in {"POLICE_IMPERSONATION", "AUTHORITY_IMPERSONATION"} for t in r1["tactics"])
        assert r1["stage"] == "AUTHORITY_CLAIM"
        assert r1["riskLevel"] in {"LOW", "MEDIUM"}

        # Turn 2: Indirect OTP request (never says 'OTP')
        t2 = "Check the message you just received and read those numbers to me."
        r2 = realtime_pipeline.analyze(transcript=t2, session_id=session_id)
        assert any(t["type"] in {"OTP_REQUEST", "CREDENTIAL_REQUEST"} for t in r2["tactics"])

        # Turn 3: Isolation & Control
        t3 = "Don't disconnect. Stay on the line."
        r3 = realtime_pipeline.analyze(transcript=t3, session_id=session_id)
        assert any(t["type"] in {"ISOLATION", "CONTROL"} for t in r3["tactics"])

        # Turn 4: Hindi + Romanized legal threat
        t4 = "Aapka account ek criminal case se connected hai, turant cooperate kijiye."
        r4 = realtime_pipeline.analyze(transcript=t4, session_id=session_id)
        assert any(t["type"] in {"LEGAL_THREAT", "FEAR_INDUCTION", "URGENCY"} for t in r4["tactics"])

        # Turn 5: Time pressure / Urgency
        t5 = "You have only a few minutes left."
        r5 = realtime_pipeline.analyze(transcript=t5, session_id=session_id)
        assert any(t["type"] == "URGENCY" for t in r5["tactics"])

        # Turn 6: Financial extraction (never says 'scam')
        t6 = "Move the money to the temporary account."
        r6 = realtime_pipeline.analyze(transcript=t6, session_id=session_id)
        assert any(t["type"] in {"BANK_TRANSFER_REQUEST", "PAYMENT_REQUEST"} for t in r6["tactics"])

        # Final assertions for the acceptance test:
        # 1. Risk must escalate to CRITICAL
        assert r6["riskScore"] >= 80, f"Expected CRITICAL risk score >= 80, got {r6['riskScore']}"
        assert r6["riskLevel"] == "CRITICAL"

        # 2. Safety Brake triggered
        assert r6["safety_brake_triggered"] is True

        # 3. Transparent, numbered explainability
        assert len(r6["reasons"]) >= 3
        assert any("1." in r for r in r6["reasons"])

        # 4. Stage must reach CRITICAL_INTERVENTION
        assert r6["stage"] == "CRITICAL_INTERVENTION"

        # 5. Manipulation velocity must reflect rapid escalation
        assert r6["manipulationVelocity"] > 0.30

        # Clean up
        realtime_pipeline.reset_session(session_id)
