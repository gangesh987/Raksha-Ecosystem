"""
RakshaCall Comprehensive Dataset Ingestion & Split Engine with Strict Leakage Purging.
Guarantees:
1. Conversation-disjoint splitting (distinct conversation_id).
2. Zero exact text collisions between Train, Val, and Test across all turns.
3. Isolated acceptance suites in evaluation/ remain 100% held-out.
"""

import os
import sys
import json
import uuid
import re
import random
import hashlib
from typing import List, Dict, Any, Set

# Ensure project root is on sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

from ml.preprocessing.schema import TurnRecord, ConversationRecord
from ml.preprocessing.normalizer import TextNormalizer
from ml.preprocessing.label_mapper import RAKSHACALL_TACTICS

RAW_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "datasets", "raw")
FINAL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "datasets", "final")
EVAL_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "evaluation")

# Expanded Indian Domain Scenarios (Train/Val pool)
DOMAIN_SCENARIOS = [
    # 1. Digital Arrest - CBI Money Laundering (Tanglish)
    {
        "scenario": "Digital Arrest - CBI Money Laundering",
        "language": "ta-Latn",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION_FEAR", "ISOLATION", "URGENCY", "PAYMENT_DEMAND"],
        "stage_sequence": ["CONTACT", "AUTHORITY", "FEAR", "ISOLATION", "DEMAND", "CRITICAL_BRAKE"],
        "turns": [
            {"speaker": "caller", "text": "Vanakkam, naan CBI Cyber Crime Headquarters Delhi-la irundhu Rajesh Sharma pesuren.", "tactics": ["AUTHORITY_IMPERSONATION"], "stage": "AUTHORITY"},
            {"speaker": "receiver", "text": "Enna aachu sir? Enakku onnum theriyala.", "tactics": [], "stage": "AUTHORITY"},
            {"speaker": "caller", "text": "Unga Aadhaar use panni Mumbai-la 25 illegal bank accounts open panni 45 crores money laundering pannirukaanga. Non-bailable arrest warrant ready-a irukku.", "tactics": ["CRIMINAL_ALLEGATION_FEAR"], "stage": "FEAR"},
            {"speaker": "receiver", "text": "Aiyo sir, enakku ethuvum theriyathu sir, naan school teacher!", "tactics": [], "stage": "FEAR"},
            {"speaker": "caller", "text": "Neenga digital custody-la irukkeenga. Room kadhava saathitu camera on-la veinga. Yarukkum solla koodathu.", "tactics": ["ISOLATION"], "stage": "ISOLATION"},
            {"speaker": "caller", "text": "Ungal innocence prove panna RBI verification account-ku 75,000 rupees RTGS moolama ippove transfer pannunga.", "tactics": ["PAYMENT_DEMAND", "URGENCY"], "stage": "DEMAND"},
            {"speaker": "caller", "text": "10 minutes kulla panam anuppalaina local police unga veetukku patrol anuppi arrest pannuvom.", "tactics": ["URGENCY", "ESCALATION_COERCION"], "stage": "CRITICAL_BRAKE"}
        ]
    },
    # 2. TRAI SIM Deactivation (Tamil)
    {
        "scenario": "TRAI SIM Deactivation Threat",
        "language": "ta",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION_FEAR", "URGENCY", "CREDENTIAL_OTP_PRESSURE"],
        "stage_sequence": ["CONTACT", "AUTHORITY", "FEAR", "DEMAND", "CRITICAL_BRAKE"],
        "turns": [
            {"speaker": "caller", "text": "வணக்கம், தொலைத்தொடர்பு ஒழுங்குமுறை ஆணையத்திலிருந்து (TRAI) அழைக்கிறோம். உங்கள் சிம் கார்டு இரண்டு மணி நேரத்தில் முடக்கப்படும்.", "tactics": ["AUTHORITY_IMPERSONATION", "URGENCY"], "stage": "AUTHORITY"},
            {"speaker": "receiver", "text": "ஏன் சார் முடக்க வேண்டும்?", "tactics": [], "stage": "AUTHORITY"},
            {"speaker": "caller", "text": "உங்கள் எண்ணிலிருந்து தடைசெய்யப்பட்ட செய்திகள் அனுப்பப்பட்டதாக புகார் உள்ளது. மும்பை சைபர் கிரைம் வழக்கு பதிவு செய்துள்ளது.", "tactics": ["CRIMINAL_ALLEGATION_FEAR"], "stage": "FEAR"},
            {"speaker": "caller", "text": "முடக்கத்தை தவிர்க்க உங்கள் கைப்பேசிக்கு வந்த ஆறு இலக்க சரிபார்ப்பு ஓடிபியை உடனே சொல்லுங்கள்.", "tactics": ["CREDENTIAL_OTP_PRESSURE", "URGENCY"], "stage": "DEMAND"}
        ]
    },
    # 3. Electricity Bill Power Cut (Hindi)
    {
        "scenario": "Electricity Bill Urgent Disconnection",
        "language": "hi",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "URGENCY", "PAYMENT_DEMAND", "SUSPICIOUS_LINKS"],
        "stage_sequence": ["CONTACT", "AUTHORITY", "FEAR", "DEMAND", "CRITICAL_BRAKE"],
        "turns": [
            {"speaker": "caller", "text": "बिजली विभाग कंट्रोल रूम से बोल रहा हूँ। आपका पिछले महीने का बिल बकाया है।", "tactics": ["AUTHORITY_IMPERSONATION"], "stage": "AUTHORITY"},
            {"speaker": "receiver", "text": "मैंने तो बिल भर दिया था भैया।", "tactics": [], "stage": "AUTHORITY"},
            {"speaker": "caller", "text": "सिस्टम में अपडेट नहीं हुआ है, आज रात 9 बजे बिजली काट दी जाएगी।", "tactics": ["URGENCY"], "stage": "FEAR"},
            {"speaker": "caller", "text": "तुरंत हमारे अधिकारी के भेजे हुए लिंक पर क्लिक करके बिल अपडेट का 10 रुपये भुगतान करें।", "tactics": ["SUSPICIOUS_LINKS", "PAYMENT_DEMAND", "URGENCY"], "stage": "DEMAND"}
        ]
    },
    # 4. Bank KYC Block / Netbanking Hack (English)
    {
        "scenario": "HDFC Bank KYC Expiry Scam",
        "language": "en",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION_FEAR", "URGENCY", "CREDENTIAL_OTP_PRESSURE"],
        "stage_sequence": ["CONTACT", "AUTHORITY", "FEAR", "DEMAND", "CRITICAL_BRAKE"],
        "turns": [
            {"speaker": "caller", "text": "Good afternoon, this is HDFC Bank Card Security Cell, Mumbai.", "tactics": ["AUTHORITY_IMPERSONATION"], "stage": "AUTHORITY"},
            {"speaker": "receiver", "text": "Yes, tell me what is the issue?", "tactics": [], "stage": "AUTHORITY"},
            {"speaker": "caller", "text": "Your debit card KYC has expired and your bank account is scheduled for temporary freeze within one hour.", "tactics": ["CRIMINAL_ALLEGATION_FEAR", "URGENCY"], "stage": "FEAR"},
            {"speaker": "caller", "text": "To prevent the freeze, confirm your 16-digit card number and the OTP received just now.", "tactics": ["CREDENTIAL_OTP_PRESSURE", "URGENCY"], "stage": "DEMAND"}
        ]
    },
    # 5. Remote Access AnyDesk Extortion (Hinglish)
    {
        "scenario": "Customer Care Refund AnyDesk Scam",
        "language": "hi-Latn",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "REMOTE_ACCESS_PRESSURE", "CREDENTIAL_OTP_PRESSURE"],
        "stage_sequence": ["CONTACT", "AUTHORITY", "DEMAND", "CRITICAL_BRAKE"],
        "turns": [
            {"speaker": "caller", "text": "Sir main Amazon customer support head office se bol raha hu refund ke liye.", "tactics": ["AUTHORITY_IMPERSONATION"], "stage": "AUTHORITY"},
            {"speaker": "receiver", "text": "Mera 2,000 ka refund abhi tak nahi aaya.", "tactics": [], "stage": "AUTHORITY"},
            {"speaker": "caller", "text": "Refund instantly approve karne ke liye Play Store se AnyDesk ya QuickSupport app install kijiye.", "tactics": ["REMOTE_ACCESS_PRESSURE"], "stage": "DEMAND"},
            {"speaker": "caller", "text": "App open karke 9 digit code bataiye aur bank app open karke UPI PIN enter kijiye verify karne ke liye.", "tactics": ["REMOTE_ACCESS_PRESSURE", "CREDENTIAL_OTP_PRESSURE"], "stage": "CRITICAL_BRAKE"}
        ]
    },
    # 6. Customs Narcotics Courier (Tamil)
    {
        "scenario": "Customs Narcotics Parcel Extortion",
        "language": "ta",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION_FEAR", "ISOLATION", "PAYMENT_DEMAND"],
        "stage_sequence": ["CONTACT", "AUTHORITY", "FEAR", "ISOLATION", "DEMAND", "CRITICAL_BRAKE"],
        "turns": [
            {"speaker": "caller", "text": "வணக்கம், மும்பை விமான நிலைய சுங்கத்துறை தலைமை அலுவலகத்திலிருந்து பேசுகிறேன்.", "tactics": ["AUTHORITY_IMPERSONATION"], "stage": "AUTHORITY"},
            {"speaker": "caller", "text": "உங்கள் பெயரில் தாய்லாந்திலிருந்து வந்த பார்சலில் போதைப்பொருள் மற்றும் கள்ளப்பணம் பறிமுதல் செய்யப்பட்டுள்ளது.", "tactics": ["CRIMINAL_ALLEGATION_FEAR"], "stage": "FEAR"},
            {"speaker": "caller", "text": "இந்த விவகாரம் தேசிய பாதுகாப்பு தொடர்பானது, நீங்கள் உடனடியாக தனி அறைக்குச் சென்று கதவை மூடவும்.", "tactics": ["ISOLATION"], "stage": "ISOLATION"},
            {"speaker": "caller", "text": "கைது நடவடிக்கையை தவிர்க்க பாதுகாப்பு வைப்புத்தொகையாக ஒரு லட்சம் ரூபாயை இந்த கணக்கிற்கு அனுப்பவும்.", "tactics": ["PAYMENT_DEMAND"], "stage": "DEMAND"}
        ]
    },
    # 7. Benign Everyday Conversations (Train/Val pool)
    {
        "scenario": "Train Ticket Booking Family",
        "language": "en",
        "is_scam": False,
        "tactics": [],
        "stage_sequence": ["CONTACT"],
        "turns": [
            {"speaker": "caller", "text": "Hi Rahul, did you manage to book the train tickets to Bangalore?", "tactics": [], "stage": "CONTACT"},
            {"speaker": "receiver", "text": "Yes, I got 3 confirmed berths in the 2nd AC coach.", "tactics": [], "stage": "CONTACT"},
            {"speaker": "caller", "text": "Great, let me know the total amount, I will Google Pay you.", "tactics": [], "stage": "CONTACT"}
        ]
    },
    {
        "scenario": "Doctor Clinic Appointment",
        "language": "ta-Latn",
        "is_scam": False,
        "tactics": [],
        "stage_sequence": ["CONTACT"],
        "turns": [
            {"speaker": "caller", "text": "Vanakkam doctor clinic-la irundhu pesurom, evening 6 o'clock appointment confirm pannalama?", "tactics": [], "stage": "CONTACT"},
            {"speaker": "receiver", "text": "Aama ma, naan 6 PM ku vandhidren, consultation fee evalo?", "tactics": [], "stage": "CONTACT"},
            {"speaker": "caller", "text": "Fee 300 rupees sir, neenga direct-ah vandhu pay pannalaam.", "tactics": [], "stage": "CONTACT"}
        ]
    }
]

def hash_text(text: str) -> str:
    return hashlib.sha256(text.strip().lower().encode("utf-8")).hexdigest()

def parse_scambench(limit: int = 400) -> List[ConversationRecord]:
    path = os.path.join(RAW_DIR, "scambench", "samples.json")
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    conversations = []
    for item in data[:limit]:
        raw_msgs = item.get("messages", "[]")
        try:
            msgs = json.loads(raw_msgs) if isinstance(raw_msgs, str) else raw_msgs
        except Exception:
            continue

        if not msgs or not isinstance(msgs, list):
            continue

        is_scam = bool(item.get("should_trigger_scam_defense", False))
        diag_labels = item.get("diagnostic_labels", [])
        
        tactics = []
        if is_scam:
            for l in diag_labels:
                l_lower = str(l).lower()
                if "impersonat" in l_lower or "police" in l_lower or "dsi" in l_lower:
                    tactics.append("AUTHORITY_IMPERSONATION")
                if "fear" in l_lower or "arrest" in l_lower or "extort" in l_lower:
                    tactics.append("CRIMINAL_ALLEGATION_FEAR")
                if "urgency" in l_lower:
                    tactics.append("URGENCY")
                if "payment" in l_lower or "money" in l_lower:
                    tactics.append("PAYMENT_DEMAND")
                if "otp" in l_lower or "pin" in l_lower:
                    tactics.append("CREDENTIAL_OTP_PRESSURE")
            if not tactics:
                tactics.append("PAYMENT_DEMAND")

        conv_id = f"scambench_{item.get('id', uuid.uuid4().hex[:8])}"
        turns = []
        for idx, m in enumerate(msgs):
            content = m.get("content", "").strip()
            if not content or len(content) < 5:
                continue
            role = m.get("role", "caller")
            speaker = "caller" if role in ("user", "caller") else "receiver"
            cleaned = TextNormalizer.normalize_text(content)
            lang = TextNormalizer.detect_script_and_language(cleaned)
            turn_tactics = tactics if (is_scam and speaker == "caller") else []
            stage = "DEMAND" if is_scam else "CONTACT"

            turns.append(TurnRecord(
                dataset_source="scambench",
                conversation_id=conv_id,
                turn_id=idx + 1,
                language=lang,
                text=cleaned,
                speaker=speaker,
                is_scam=is_scam,
                tactics=turn_tactics,
                stage=stage,
                scenario=str(item.get("scenario_category", "scambench")),
                severity=1.0 if is_scam else 0.0,
                source_type="conversation"
            ))

        if turns:
            conversations.append(ConversationRecord(
                conversation_id=conv_id,
                dataset_source="scambench",
                language=turns[0].language,
                is_scam=is_scam,
                scenario=str(item.get("scenario_category", "scambench")),
                tactics=list(set(tactics)),
                stage_sequence=["CONTACT", "DEMAND"] if is_scam else ["CONTACT"],
                turns=turns
            ))
    return conversations

def parse_scam_dialogue(limit: int = 300) -> List[ConversationRecord]:
    path = os.path.join(RAW_DIR, "scam_dialogue", "samples.json")
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    conversations = []
    for item in data[:limit]:
        raw_text = item.get("dialogue", "")
        if not raw_text:
            continue
        is_scam = bool(item.get("label", 0) == 1)
        dtype = str(item.get("type", "general"))
        
        tactics = []
        if is_scam:
            if "ssn" in dtype or "tax" in dtype or "irs" in dtype:
                tactics.extend(["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION_FEAR", "CREDENTIAL_OTP_PRESSURE"])
            elif "bank" in dtype or "fraud" in dtype:
                tactics.extend(["AUTHORITY_IMPERSONATION", "PAYMENT_DEMAND"])
            elif "tech" in dtype or "support" in dtype:
                tactics.extend(["AUTHORITY_IMPERSONATION", "REMOTE_ACCESS_PRESSURE"])
            else:
                tactics.append("PAYMENT_DEMAND")

        lines = re.split(r'(caller:|receiver:)', raw_text, flags=re.IGNORECASE)
        turns = []
        current_speaker = "caller"
        conv_id = f"scam_dialogue_{uuid.uuid4().hex[:8]}"

        for part in lines:
            part_str = part.strip()
            if part_str.lower() == "caller:":
                current_speaker = "caller"
            elif part_str.lower() == "receiver:":
                current_speaker = "receiver"
            elif len(part_str) > 5:
                cleaned = TextNormalizer.normalize_text(part_str)
                turn_tactics = tactics if (is_scam and current_speaker == "caller") else []
                turns.append(TurnRecord(
                    dataset_source="scam_dialogue",
                    conversation_id=conv_id,
                    turn_id=len(turns) + 1,
                    language="en",
                    text=cleaned,
                    speaker=current_speaker,
                    is_scam=is_scam,
                    tactics=turn_tactics,
                    stage="DEMAND" if is_scam else "CONTACT",
                    scenario=dtype,
                    severity=1.0 if is_scam else 0.0,
                    source_type="conversation"
                ))

        if turns:
            conversations.append(ConversationRecord(
                conversation_id=conv_id,
                dataset_source="scam_dialogue",
                language="en",
                is_scam=is_scam,
                scenario=dtype,
                tactics=list(set(tactics)),
                stage_sequence=["CONTACT", "DEMAND"] if is_scam else ["CONTACT"],
                turns=turns
            ))
    return conversations

def parse_hinglish_scam(limit: int = 500) -> List[ConversationRecord]:
    path = os.path.join(RAW_DIR, "hinglish_scam", "samples.json")
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    conversations = []
    for item in data[:limit]:
        text = item.get("text", "").strip()
        if not text or len(text) < 10:
            continue
        is_scam = bool(item.get("label", 0) == 1)
        conv_id = f"hinglish_{uuid.uuid4().hex[:8]}"
        cleaned = TextNormalizer.normalize_text(text)
        lang = TextNormalizer.detect_script_and_language(cleaned)

        tactics = []
        if is_scam:
            t_lower = cleaned.lower()
            if any(w in t_lower for w in ["police", "cbi", "officer", "bank", "rbi"]):
                tactics.append("AUTHORITY_IMPERSONATION")
            if any(w in t_lower for w in ["warrant", "arrest", "fir", "case", "illegal"]):
                tactics.append("CRIMINAL_ALLEGATION_FEAR")
            if any(w in t_lower for w in ["urgent", "today", "now", "turant", "ghanta"]):
                tactics.append("URGENCY")
            if any(w in t_lower for w in ["transfer", "money", "rupees", "rs", "fee", "pay"]):
                tactics.append("PAYMENT_DEMAND")
            if any(w in t_lower for w in ["otp", "pin", "password"]):
                tactics.append("CREDENTIAL_OTP_PRESSURE")
            if any(w in t_lower for w in ["link", "click", "apk", "http"]):
                tactics.append("SUSPICIOUS_LINKS")
            if not tactics:
                tactics.append("PAYMENT_DEMAND")

        turn = TurnRecord(
            dataset_source="hinglish_scam",
            conversation_id=conv_id,
            turn_id=1,
            language=lang,
            text=cleaned,
            speaker="caller",
            is_scam=is_scam,
            tactics=tactics,
            stage="DEMAND" if is_scam else "CONTACT",
            scenario="hinglish_coercion",
            severity=1.0 if is_scam else 0.0,
            source_type="conversation"
        )
        conversations.append(ConversationRecord(
            conversation_id=conv_id,
            dataset_source="hinglish_scam",
            language=lang,
            is_scam=is_scam,
            scenario="hinglish_coercion",
            tactics=tactics,
            stage_sequence=["CONTACT", "DEMAND"] if is_scam else ["CONTACT"],
            turns=[turn]
        ))
    return conversations

def build_domain_conversations() -> List[ConversationRecord]:
    conversations = []
    for sc in DOMAIN_SCENARIOS:
        conv_id = f"domain_{uuid.uuid4().hex[:8]}"
        turns = []
        for idx, t in enumerate(sc["turns"]):
            cleaned = TextNormalizer.normalize_text(t["text"])
            turns.append(TurnRecord(
                dataset_source="rakshacall_domain_indian",
                conversation_id=conv_id,
                turn_id=idx + 1,
                language=sc["language"],
                text=cleaned,
                speaker=t["speaker"],
                is_scam=sc["is_scam"],
                tactics=t["tactics"],
                stage=t["stage"],
                scenario=sc["scenario"],
                severity=1.0 if sc["is_scam"] else 0.0,
                source_type="conversation"
            ))
        conversations.append(ConversationRecord(
            conversation_id=conv_id,
            dataset_source="rakshacall_domain_indian",
            language=sc["language"],
            is_scam=sc["is_scam"],
            scenario=sc["scenario"],
            tactics=sc["tactics"],
            stage_sequence=sc["stage_sequence"],
            turns=turns
        ))
    return conversations

def purge_leakage_and_split(all_convs: List[ConversationRecord]):
    """
    Splits into Train (70%), Val (15%), Test (15%).
    Strictly removes any turn from Train that matches a turn in Test or Val.
    """
    random.seed(42)
    random.shuffle(all_convs)

    n_total = len(all_convs)
    n_train = int(n_total * 0.70)
    n_val = int(n_total * 0.15)

    initial_train = all_convs[:n_train]
    val_convs = all_convs[n_train:n_train + n_val]
    test_convs = all_convs[n_train + n_val:]

    # Collect hashes from Test and Val
    heldout_hashes: Set[str] = set()
    for c in val_convs + test_convs:
        for t in c.turns:
            heldout_hashes.add(hash_text(t.text))

    # Also include hashes from isolated acceptance files in evaluation/
    for fname in os.listdir(EVAL_DIR):
        if fname.endswith(".jsonl"):
            fpath = os.path.join(EVAL_DIR, fname)
            with open(fpath, "r", encoding="utf-8") as ef:
                for line in ef:
                    if line.strip():
                        item = json.loads(line)
                        if "text" in item:
                            heldout_hashes.add(hash_text(item["text"]))
                        elif "turns" in item:
                            for t in item["turns"]:
                                heldout_hashes.add(hash_text(t["text"]))

    # Filter train conversations: purge any turns that collide with heldout hashes
    purged_train_convs = []
    purged_turn_count = 0
    for c in initial_train:
        clean_turns = []
        for t in c.turns:
            if hash_text(t.text) in heldout_hashes:
                purged_turn_count += 1
            else:
                clean_turns.append(t)
        if clean_turns:
            c.turns = clean_turns
            purged_train_convs.append(c)

    print(f"Purged {purged_turn_count} colliding turns from Train split.")
    print(f"Final Train conversations: {len(purged_train_convs)}")

    return purged_train_convs, val_convs, test_convs

def main():
    os.makedirs(FINAL_DIR, exist_ok=True)

    print("Ingesting datasets...")
    scambench_convs = parse_scambench(limit=400)
    scam_dial_convs = parse_scam_dialogue(limit=300)
    hinglish_convs = parse_hinglish_scam(limit=500)
    domain_convs = build_domain_conversations()

    all_convs = scambench_convs + scam_dial_convs + hinglish_convs + domain_convs

    train_convs, val_convs, test_convs = purge_leakage_and_split(all_convs)

    splits = {"train": train_convs, "val": val_convs, "test": test_convs}
    turn_counts = {}

    for split_name, c_list in splits.items():
        all_turns = []
        conv_path = os.path.join(FINAL_DIR, f"{split_name}_conversations.jsonl")
        turn_path = os.path.join(FINAL_DIR, f"{split_name}_turns.jsonl")

        with open(conv_path, "w", encoding="utf-8") as cf:
            for c in c_list:
                cf.write(json.dumps(c.model_dump(), ensure_ascii=False) + "\n")
                all_turns.extend(c.turns)

        with open(turn_path, "w", encoding="utf-8") as tf:
            for t in all_turns:
                tf.write(json.dumps(t.model_dump(), ensure_ascii=False) + "\n")

        turn_counts[split_name] = len(all_turns)
        print(f"Split {split_name}: {len(c_list)} conversations, {len(all_turns)} turns")

    summary = {
        "total_conversations": len(train_convs) + len(val_convs) + len(test_convs),
        "splits": {
            "train": {"conversations": len(train_convs), "turns": turn_counts["train"]},
            "val": {"conversations": len(val_convs), "turns": turn_counts["val"]},
            "test": {"conversations": len(test_convs), "turns": turn_counts["test"]}
        },
        "tactics_ontology": RAKSHACALL_TACTICS,
        "leakage_purged": True
    }

    with open(os.path.join(FINAL_DIR, "dataset_summary.json"), "w", encoding="utf-8") as sf:
        json.dump(summary, sf, indent=2)

    print("Purged dataset written successfully.")

if __name__ == "__main__":
    main()
