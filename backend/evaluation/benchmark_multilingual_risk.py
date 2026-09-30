"""
RakshaCall Multilingual Contextual Risk Engine Benchmark & Validation.

Evaluates 13+ languages (English, Tamil, Hindi, Telugu, Kannada, Malayalam, Bengali,
Marathi, Gujarati, Punjabi, Odia, Tanglish, Hinglish) across:
1. Canonical multi-tactic scam scenarios (Police digital arrest, Bank OTP, Courier scam, etc.)
2. Code-switching combinations
3. Adversarial paraphrased payment/credential demands
4. Rigorous benign everyday false-positive test suite
5. Multi-turn progressive conversation escalation

Generates backend/evaluation/risk_engine_multilingual_results.json
"""

import sys
import os
import json
import time
from typing import Dict, List, Any

# Ensure backend root is on sys.path
backend_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if backend_dir not in sys.path:
    sys.path.insert(0, backend_dir)

from app.ai.multilingual_tactic_engine import (
    MultilingualTacticEngine,
    multilingual_risk_engine,
    CANONICAL_TACTICS,
    ConversationRiskState
)


BENCHMARK_CASES = [
    # ─── 1. Canonical Scenarios across Languages ───
    {
        "language": "English",
        "scenario": "Police Digital Arrest Scam",
        "input": "I'm a police officer. This is urgent. Send me money immediately.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Tamil",
        "scenario": "Tamil Police Money Demand",
        "input": "நான் போலீஸ்ல இருந்து பேசுறேன். இது அவசரம். உடனே பணம் அனுப்புங்க.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Tanglish",
        "scenario": "Tanglish Police Money Demand",
        "input": "Naan police la irundhu pesuren. Idhu romba urgent. Udane money send pannunga.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Hindi",
        "scenario": "Hindi Police Money Demand",
        "input": "Main police se bol raha hoon. Yeh bahut urgent hai. Abhi paise bhejo.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Hinglish",
        "scenario": "Hinglish Police Money Demand",
        "input": "Main police se bol raha hoon, urgent hai, abhi money send karo.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Telugu",
        "scenario": "Telugu Police Money Demand",
        "input": "Nenu police nundi matladutunnanu. Idi urgent. Ventane dabbu pampinchandi.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Kannada",
        "scenario": "Kannada Police Threat Scam",
        "input": "ನಾನು ಪೊಲೀಸ್ ಇಂದ ಮಾತನಾಡುತ್ತಿದ್ದೇನೆ. ಇದು ತುರ್ತು. ತಕ್ಷಣ ಹಣ ಕಳುಹಿಸಿ.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Malayalam",
        "scenario": "Malayalam Police Threat Scam",
        "input": "ഞാൻ പോലീസ് ഓഫീസർ ആണ് സംസാരിക്കുന്നത്. ഇത് അടിയന്തിരമാണ്. ഉടൻ പണം അയക്കുക.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Bengali",
        "scenario": "Bengali Police Threat Scam",
        "input": "আমি পুলিশ থেকে বলছি। এটা জরুরি। অবিলম্বে টাকা পাঠান।",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Marathi",
        "scenario": "Marathi Police Threat Scam",
        "input": "मी पोलीस मधून बोलतोय. हे तातडीचे आहे. लगेच पैसे पाठवा.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Gujarati",
        "scenario": "Gujarati Police Threat Scam",
        "input": "હું પોલીસ માંથી બોલું છું. આ તાકીદનું છે. તરત જ પૈસા મોકલો.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Punjabi",
        "scenario": "Punjabi Police Threat Scam",
        "input": "ਮੈਂ ਪੁਲਿਸ ਤੋਂ ਬੋਲ ਰਿਹਾ ਹਾਂ। ਇਹ ਬਹੁਤ ਜ਼ਰੂਰੀ ਹੈ। ਤੁਰੰਤ ਪੈਸੇ ਭੇਜੋ।",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Odia",
        "scenario": "Odia Police Threat Scam",
        "input": "ମୁଁ ପୋଲିସ ତରଫରୁ କହୁଛି। ଏହା ଜରୁରୀ। ତୁରନ୍ତ ଟଙ୍କା ପଠାନ୍ତୁ।",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },

    # ─── 2. Code-Switching Combinations ───
    {
        "language": "Tamil-English",
        "scenario": "Tamil-English Code-Switch Scam",
        "input": "Sir naan police department la irundhu pesuren, this is urgent, immediately money transfer pannunga.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 70,
        "is_benign": False
    },
    {
        "language": "Hindi-English",
        "scenario": "Hindi-English Code-Switch Scam",
        "input": "Main police se bol raha hoon, account block ho jayega, so immediately money send karo.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "THREAT_OR_FEAR", "URGENCY_PRESSURE", "PAYMENT_DEMAND"],
        "expected_min_score": 85,
        "is_benign": False
    },
    {
        "language": "Telugu-English",
        "scenario": "Telugu-English Code-Switch Scam",
        "input": "Nenu bank nundi matladutunnanu, OTP cheppandi immediately.",
        "expected_tactics": ["AUTHORITY_IMPERSONATION", "OTP_REQUEST", "URGENCY_PRESSURE"],
        "expected_min_score": 60,
        "is_benign": False
    },

    # ─── 3. High-Risk Tactic Variants (OTP, PIN, Remote Access, Secrecy) ───
    {
        "language": "English",
        "scenario": "Bank OTP Scam",
        "input": "Tell me OTP right now to unfreeze your bank account.",
        "expected_tactics": ["OTP_REQUEST", "URGENCY_PRESSURE"],
        "expected_min_score": 45,
        "is_benign": False
    },
    {
        "language": "Hindi",
        "scenario": "UPI PIN Request",
        "input": "Apna UPI PIN enter karo refund lene ke liye.",
        "expected_tactics": ["CREDENTIAL_REQUEST"],
        "expected_min_score": 30,
        "is_benign": False
    },
    {
        "language": "English",
        "scenario": "Remote Control AnyDesk Scam",
        "input": "Please install AnyDesk for verification so we can help you.",
        "expected_tactics": ["REMOTE_ACCESS_REQUEST"],
        "expected_min_score": 30,
        "is_benign": False
    },
    {
        "language": "Tamil",
        "scenario": "Secrecy & Isolation Pressure",
        "input": "வீட்டுல யார்கிட்டேயும் சொல்லாதீங்க, இது confidential investigation, ரூம் கதவை சாத்துங்க.",
        "expected_tactics": ["SECRECY_PRESSURE"],
        "expected_min_score": 20,
        "is_benign": False
    },
    {
        "language": "English",
        "scenario": "Suspicious APK Download",
        "input": "Click this link and install this APK immediately.",
        "expected_tactics": ["SUSPICIOUS_LINK", "URGENCY_PRESSURE"],
        "expected_min_score": 35,
        "is_benign": False
    },
    {
        "language": "English",
        "scenario": "Investment Double Money Scam",
        "input": "Guaranteed profit! Double your money within 7 days, 100% risk free.",
        "expected_tactics": ["INVESTMENT_OR_REWARD_SCAM"],
        "expected_min_score": 15,
        "is_benign": False
    },

    # ─── 4. Adversarial Paraphrased Payment Demands ───
    {
        "language": "English",
        "scenario": "Paraphrase: Transfer the amount",
        "input": "Transfer the amount right now.",
        "expected_tactics": ["PAYMENT_DEMAND", "URGENCY_PRESSURE"],
        "expected_min_score": 35,
        "is_benign": False
    },
    {
        "language": "English",
        "scenario": "Paraphrase: Make the payment",
        "input": "Make the payment immediately.",
        "expected_tactics": ["PAYMENT_DEMAND", "URGENCY_PRESSURE"],
        "expected_min_score": 35,
        "is_benign": False
    },
    {
        "language": "English",
        "scenario": "Paraphrase: Complete transaction",
        "input": "Please complete the transaction now.",
        "expected_tactics": ["PAYMENT_DEMAND"],
        "expected_min_score": 25,
        "is_benign": False
    },
    {
        "language": "English",
        "scenario": "Paraphrase: UPI it now",
        "input": "UPI it now.",
        "expected_tactics": ["PAYMENT_DEMAND"],
        "expected_min_score": 25,
        "is_benign": False
    },
    {
        "language": "Tanglish",
        "scenario": "Paraphrase: Tamil Romanized Amount Transfer",
        "input": "Andha amount-ai ippave transfer pannunga.",
        "expected_tactics": ["PAYMENT_DEMAND", "URGENCY_PRESSURE"],
        "expected_min_score": 35,
        "is_benign": False
    },
    {
        "language": "Hinglish",
        "scenario": "Paraphrase: Hindi Romanized Amount Transfer",
        "input": "Abhi amount transfer karo.",
        "expected_tactics": ["PAYMENT_DEMAND"],
        "expected_min_score": 25,
        "is_benign": False
    },
    {
        "language": "Telugu",
        "scenario": "Paraphrase: Telugu Romanized Amount Transfer",
        "input": "Nenu amount ippude pampinchandi.",
        "expected_tactics": ["PAYMENT_DEMAND", "URGENCY_PRESSURE"],
        "expected_min_score": 35,
        "is_benign": False
    },

    # ─── 5. Dedicated Benign Test Suite (False Positive Protection) ───
    {
        "language": "English",
        "scenario": "Benign: Police Station Location",
        "input": "I am going to the police station.",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    },
    {
        "language": "English",
        "scenario": "Benign: Police Station Near House",
        "input": "Police station is near my house.",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    },
    {
        "language": "English",
        "scenario": "Benign: Police Officer Helped",
        "input": "The police officer helped me yesterday.",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    },
    {
        "language": "English",
        "scenario": "Benign: Grocery Shop Payment",
        "input": "Please send money to the grocery shop.",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    },
    {
        "language": "English",
        "scenario": "Benign: Mother Money Transfer",
        "input": "Can you transfer money to my mother?",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    },
    {
        "language": "English",
        "scenario": "Benign: Bank In-Person Visit",
        "input": "My bank asked me to visit the branch tomorrow.",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    },
    {
        "language": "English",
        "scenario": "Benign: College Assignment Urgent",
        "input": "This assignment is urgent.",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    },
    {
        "language": "English",
        "scenario": "Benign: Submit Homework Urgently",
        "input": "I urgently need to submit my college assignment.",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    },
    {
        "language": "English",
        "scenario": "Benign: Received Bank OTP",
        "input": "I received an OTP from my bank for online purchase.",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    },
    {
        "language": "Tamil",
        "scenario": "Benign: Tamil Police Station Near",
        "input": "போலீஸ் ஸ்டேஷன் பக்கத்துல இருக்கு.",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    },
    {
        "language": "Tamil",
        "scenario": "Benign: Tamil Mother Money",
        "input": "என் அம்மாவுக்கு பணம் அனுப்பணும்.",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    },
    {
        "language": "Hindi",
        "scenario": "Benign: Hindi Police Station Near",
        "input": "police station paas mein hai.",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    },
    {
        "language": "Hindi",
        "scenario": "Benign: Hindi College Urgent",
        "input": "college assignment urgent hai.",
        "expected_tactics": [],
        "expected_max_score": 15,
        "is_benign": True
    }
]


def run_benchmark():
    print(f"Starting Multilingual Risk Engine Benchmark ({len(BENCHMARK_CASES)} cases)...")
    results = []
    engine = MultilingualTacticEngine()

    total_cases = len(BENCHMARK_CASES)
    false_positives = 0
    false_negatives = 0
    total_latency_ms = 0.0

    for idx, case in enumerate(BENCHMARK_CASES):
        session_id = f"bench-session-{idx}"
        t0 = time.perf_counter()
        state = engine.evaluate_turn(utterance=case["input"], session_id=session_id)
        latency_ms = round((time.perf_counter() - t0) * 1000.0, 3)
        total_latency_ms += latency_ms

        detected_tactics = [t.tactic_type for t in state.detected_tactics]
        score = state.current_score
        level = state.risk_level

        # Evaluate FP and FN
        fp = False
        fn = False

        if case["is_benign"]:
            if score > case["expected_max_score"] or level in ("HIGH", "CRITICAL"):
                fp = True
                false_positives += 1
        else:
            # For scam scenarios, must detect expected tactics or cross min score
            matched_expected = any(t in detected_tactics for t in case["expected_tactics"])
            if not matched_expected or score < case["expected_min_score"]:
                fn = True
                false_negatives += 1

        res_entry = {
            "case_id": idx + 1,
            "language": case["language"],
            "scenario": case["scenario"],
            "input": case["input"],
            "detected_tactics": [t.to_dict() for t in state.detected_tactics],
            "risk_score": score,
            "risk_level": level,
            "confidence": state.confidence,
            "is_suppressed_as_benign": state.is_suppressed_as_benign,
            "benign_reason": state.benign_reason,
            "false_positive": fp,
            "false_negative": fn,
            "latency_ms": latency_ms
        }
        results.append(res_entry)

    # ─── Multi-turn Progressive Escalation Test (Digital Arrest) ───
    multi_turn_session = "bench-multi-turn-escalation"
    turns = [
        ("Hello sir, is this Kamalesh?", "Turn 1: Greeting", 0, 15),
        ("I'm calling from the cyber crime department.", "Turn 2: Authority", 25, 45),
        ("The account linked to your Aadhaar has suspicious activity.", "Turn 3: Threat/Verification", 45, 65),
        ("Do not tell your family, keep this confidential.", "Turn 4: Secrecy Isolation", 65, 80),
        ("Transfer ₹25,000 immediately for verification.", "Turn 5: Urgency & Payment", 85, 100)
    ]
    multi_turn_results = []
    print("\nEvaluating Multi-Turn Progressive Escalation...")
    for t_idx, (text, label, min_s, max_s) in enumerate(turns):
        t0 = time.perf_counter()
        state = engine.evaluate_turn(utterance=text, session_id=multi_turn_session)
        lat = round((time.perf_counter() - t0) * 1000.0, 3)
        multi_turn_results.append({
            "turn": t_idx + 1,
            "label": label,
            "utterance": text,
            "score": state.current_score,
            "level": state.risk_level,
            "escalation": state.escalation_trend,
            "tactics": [t.tactic_type for t in state.detected_tactics],
            "latency_ms": lat
        })
        print(f"  Turn {t_idx+1}: Score={state.current_score} ({state.risk_level}) [{state.escalation_trend}] - {label}")

    avg_latency = round(total_latency_ms / total_cases, 2)
    output_data = {
        "summary": {
            "total_benchmark_cases": total_cases,
            "false_positives": false_positives,
            "false_negatives": false_negatives,
            "fp_rate": f"{(false_positives / total_cases) * 100:.1f}%",
            "fn_rate": f"{(false_negatives / total_cases) * 100:.1f}%",
            "benign_cases_tested": sum(1 for c in BENCHMARK_CASES if c["is_benign"]),
            "benign_pass_count": sum(1 for c in BENCHMARK_CASES if c["is_benign"]) - false_positives,
            "adversarial_cases_tested": sum(1 for c in BENCHMARK_CASES if not c["is_benign"]),
            "adversarial_pass_count": sum(1 for c in BENCHMARK_CASES if not c["is_benign"]) - false_negatives,
            "average_latency_ms": avg_latency,
            "canonical_tactics_count": len(CANONICAL_TACTICS),
            "status": "PASS" if (false_positives == 0 and false_negatives == 0) else "FAIL"
        },
        "multi_turn_escalation": multi_turn_results,
        "benchmark_cases": results
    }

    out_file = os.path.join(backend_dir, "evaluation", "risk_engine_multilingual_results.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(output_data, f, indent=2, ensure_ascii=False)

    print(f"\n==================================================================")
    print(f"BENCHMARK COMPLETE")
    print(f"Total Cases: {total_cases}")
    print(f"False Positives: {false_positives}/{sum(1 for c in BENCHMARK_CASES if c['is_benign'])}")
    print(f"False Negatives: {false_negatives}/{sum(1 for c in BENCHMARK_CASES if not c['is_benign'])}")
    print(f"Average Latency: {avg_latency} ms")
    print(f"Output saved to: {out_file}")
    print(f"==================================================================")
    return output_data


if __name__ == "__main__":
    run_benchmark()
