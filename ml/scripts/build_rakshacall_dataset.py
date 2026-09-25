"""
RakshaCall Unified Domain Dataset Builder.
Generates and normalizes domain-specific multi-turn conversational datasets
across Tamil, Tanglish, Hindi, Hinglish, and English, covering 9 tactics,
scam stages, and essential negative controls (scammer speech vs protective advice).
"""

import os
import sys
import json
import uuid
import logging
from typing import List, Dict, Any

# Ensure project root is on sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(__file__))))

from ml.preprocessing.schema import TurnRecord, ConversationRecord
from ml.preprocessing.normalizer import TextNormalizer

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("DomainDatasetBuilder")

DATASETS_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "datasets")
PROCESSED_DIR = os.path.join(DATASETS_DIR, "processed")
FINAL_DIR = os.path.join(DATASETS_DIR, "final")

# Curated domain scenarios with multi-turn escalation and verified ground truth
DOMAIN_CONVERSATIONS: List[Dict[str, Any]] = [
    # 1. Digital Arrest Scam (Tamil / Tanglish)
    {
        "scenario": "Digital Arrest - CBI Money Laundering Extortion",
        "language": "ta-Latn",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION_FEAR", "ISOLATION", "URGENCY", "PAYMENT_DEMAND"],
        "stage_sequence": ["CONTACT", "AUTHORITY", "FEAR", "ISOLATION", "DEMAND", "CRITICAL_BRAKE"],
        "turns": [
            {
                "speaker": "caller",
                "text": "Vanakkam, naan New Delhi CBI Cyber Crime Headquarters-la irundhu Officer Rajesh Sharma pesuren.",
                "tactics": ["AUTHORITY_IMPERSONATION"],
                "stage": "AUTHORITY"
            },
            {
                "speaker": "receiver",
                "text": "Enna vishayam sir? Enakku onnum puriyala.",
                "tactics": [],
                "stage": "AUTHORITY"
            },
            {
                "speaker": "caller",
                "text": "Ungal Aadhaar card use panni Mumbai Canara Bank-la 25 illegal accounts open panni 45 crores money laundering pannirukaanga. Ungal peyarla non-bailable arrest warrant issue aagi irukku.",
                "tactics": ["CRIMINAL_ALLEGATION_FEAR"],
                "stage": "FEAR"
            },
            {
                "speaker": "receiver",
                "text": "Aiyo sir, enakku idhukkum entha sambandhamum illa sir! Naan oru school teacher.",
                "tactics": [],
                "stage": "FEAR"
            },
            {
                "speaker": "caller",
                "text": "Idhu national security matter. Neenga ippo digital arrest-la irukkeenga. Room kadhava moodittu yaar kittayum pesa koodaadhu. Family-kku kooda theriyappadutha koodaadhu. Call-a cut panna udane local police ungal veetukku varuvaanga.",
                "tactics": ["ISOLATION", "ESCALATION_COERCION"],
                "stage": "ISOLATION"
            },
            {
                "speaker": "caller",
                "text": "Ungal innocence prove panna, ungal bank accounts-la irukkura motha panathayum Supreme Court verification account-ku RTGS moolama ippove transfer pannunga. Verification mudinjavudan return vandhurum.",
                "tactics": ["PAYMENT_DEMAND", "URGENCY"],
                "stage": "DEMAND"
            }
        ]
    },

    # 2. TRAI SIM Card Block / Police Impersonation (Tamil)
    {
        "scenario": "TRAI SIM Blocking & Police Cyber Cell Coercion",
        "language": "ta",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION_FEAR", "URGENCY", "CREDENTIAL_OTP_PRESSURE"],
        "stage_sequence": ["CONTACT", "AUTHORITY", "FEAR", "DEMAND", "CRITICAL_BRAKE"],
        "turns": [
            {
                "speaker": "caller",
                "text": "வணக்கம், நாங்கள் தொலைத்தொடர்பு ஒழுங்குமுறை ஆணையத்திலிருந்து (TRAI) அழைக்கிறோம். உங்கள் மொபைல் எண் அடுத்த இரண்டு மணி நேரத்தில் துண்டிக்கப்படும்.",
                "tactics": ["AUTHORITY_IMPERSONATION", "URGENCY"],
                "stage": "AUTHORITY"
            },
            {
                "speaker": "caller",
                "text": "உங்கள் ஆதார் எண்ணைப் பயன்படுத்தி சட்டவிரோத விளம்பரங்கள் மற்றும் ஆபாச செய்திகள் அனுப்பப்பட்டதாக புகார் வந்துள்ளது. உடனடியாக சைபர் கிரைம் போலீசாரை தொடர்பு கொள்ள வேண்டும்.",
                "tactics": ["CRIMINAL_ALLEGATION_FEAR"],
                "stage": "FEAR"
            },
            {
                "speaker": "caller",
                "text": "எண் ரத்து செய்யப்படாமல் இருக்க உங்கள் ஆதார் சரிபார்ப்புக் குறியீட்டை (OTP) உடனடியாக எங்களிடம் கூறுங்கள்.",
                "tactics": ["CREDENTIAL_OTP_PRESSURE", "URGENCY"],
                "stage": "DEMAND"
            }
        ]
    },

    # 3. Electricity Bill Disconnection & Remote APK Scam (Hinglish)
    {
        "scenario": "Electricity Disconnection & AnyDesk Remote Access Scam",
        "language": "hi-Latn",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "URGENCY", "SUSPICIOUS_LINKS", "REMOTE_ACCESS_PRESSURE", "PAYMENT_DEMAND"],
        "stage_sequence": ["CONTACT", "AUTHORITY", "FEAR", "DEMAND", "CRITICAL_BRAKE"],
        "turns": [
            {
                "speaker": "caller",
                "text": "Dear consumer, aapka bijli connection aaj raat 9 baje disconnect ho jayega kyunki pichle mahine ka bill update nahi hua hai.",
                "tactics": ["AUTHORITY_IMPERSONATION", "URGENCY"],
                "stage": "AUTHORITY"
            },
            {
                "speaker": "receiver",
                "text": "Lekin maine toh bill bhar diya tha Google Pay se!",
                "tactics": [],
                "stage": "AUTHORITY"
            },
            {
                "speaker": "caller",
                "text": "Server update fail ho gaya tha sir. Line bachaane ke liye turant hamare officer ke diye hue WhatsApp link se Bijli-Update.apk install kariye.",
                "tactics": ["SUSPICIOUS_LINKS", "REMOTE_ACCESS_PRESSURE"],
                "stage": "DEMAND"
            },
            {
                "speaker": "caller",
                "text": "App open karke 10 rupaye ka verification charge pay karein aur screen share accept karein taaki hum meter update kar sakein.",
                "tactics": ["REMOTE_ACCESS_PRESSURE", "PAYMENT_DEMAND"],
                "stage": "DEMAND"
            }
        ]
    },

    # 4. Customs / FedEx Drugs In Parcel Scam (Hindi)
    {
        "scenario": "Customs Narcotics Parcel & ED Extortion",
        "language": "hi",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION_FEAR", "ISOLATION", "PAYMENT_DEMAND"],
        "stage_sequence": ["CONTACT", "AUTHORITY", "FEAR", "ISOLATION", "DEMAND", "CRITICAL_BRAKE"],
        "turns": [
            {
                "speaker": "caller",
                "text": "नमस्ते, मैं मुंबई कस्टम्स विभाग से सुपरिंटेंडेंट वर्मा बोल रहा हूँ।",
                "tactics": ["AUTHORITY_IMPERSONATION"],
                "stage": "AUTHORITY"
            },
            {
                "speaker": "caller",
                "text": "आपके नाम से ताइवान भेजा जा रहा पार्सल पकड़ा गया है जिसमें 150 ग्राम एमडीएमए ड्रग्स और 5 फर्जी पासपोर्ट बरामद हुए हैं।",
                "tactics": ["CRIMINAL_ALLEGATION_FEAR"],
                "stage": "FEAR"
            },
            {
                "speaker": "caller",
                "text": "यह गंभीर नारकोटिक्स मामला है। तुरंत स्काइप पर आइए। अपने घर वालों को बिल्कुल मत बताइए वरना उन्हें भी सह-आरोपी बनाया जाएगा।",
                "tactics": ["ISOLATION", "ESCALATION_COERCION"],
                "stage": "ISOLATION"
            },
            {
                "speaker": "caller",
                "text": "गिरफ्तारी वारंट रोकने के लिए हमारे वित्तीय जांच खाते में 2 लाख रुपये सिक्योरिटी डिपॉजिट के रूप में जमा कीजिए।",
                "tactics": ["PAYMENT_DEMAND", "URGENCY"],
                "stage": "DEMAND"
            }
        ]
    },

    # 5. Bank Impersonation / Urgent KYC OTP Scam (English)
    {
        "scenario": "HDFC / SBI KYC Expiration & OTP Soliciation",
        "language": "en",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "URGENCY", "CREDENTIAL_OTP_PRESSURE"],
        "stage_sequence": ["CONTACT", "AUTHORITY", "DEMAND", "CRITICAL_BRAKE"],
        "turns": [
            {
                "speaker": "caller",
                "text": "Good morning, this is an automated priority alert from State Bank of India security division.",
                "tactics": ["AUTHORITY_IMPERSONATION"],
                "stage": "AUTHORITY"
            },
            {
                "speaker": "caller",
                "text": "Your debit card and net banking services have been suspended due to overdue mandatory KYC verification.",
                "tactics": ["URGENCY", "CRIMINAL_ALLEGATION_FEAR"],
                "stage": "FEAR"
            },
            {
                "speaker": "caller",
                "text": "To restore access immediately and prevent permanent account block, please read out the 6-digit one-time password sent to your registered phone.",
                "tactics": ["CREDENTIAL_OTP_PRESSURE", "URGENCY"],
                "stage": "DEMAND"
            }
        ]
    },

    # 6. NEGATIVE CONTROL 1: Bank Safety Warning Advisory (English) - BENIGN
    {
        "scenario": "Bank Official Customer Awareness Call",
        "language": "en",
        "is_scam": False,
        "tactics": [],
        "stage_sequence": ["CONTACT"],
        "turns": [
            {
                "speaker": "caller",
                "text": "Hello Mr. Kumar, calling from HDFC Bank to inform you that your new contactless credit card has been dispatched.",
                "tactics": [],
                "stage": "CONTACT"
            },
            {
                "speaker": "caller",
                "text": "Please remember that bank officials will never ask for your confidential password, PIN, or OTP. Never share your OTP with anyone.",
                "tactics": [],
                "stage": "CONTACT"
            },
            {
                "speaker": "caller",
                "text": "If anyone calls demanding payment or asking you to install software, immediately hang up and report it to our customer care.",
                "tactics": [],
                "stage": "CONTACT"
            }
        ]
    },

    # 7. NEGATIVE CONTROL 2: Cyber Police Public Advisory (Tamil) - BENIGN
    {
        "scenario": "Tamil Nadu Cyber Crime Public Safety Awareness",
        "language": "ta",
        "is_scam": False,
        "tactics": [],
        "stage_sequence": ["CONTACT"],
        "turns": [
            {
                "speaker": "caller",
                "text": "வணக்கம், தமிழ்நாடு காவல்துறை சைபர் கிரைம் விழிப்புணர்வு செய்தி.",
                "tactics": [],
                "stage": "CONTACT"
            },
            {
                "speaker": "caller",
                "text": "போலீசார் அல்லது சிபிஐ அதிகாரிகள் ஒருபோதும் தொலைபேசியில் பணம் கேட்க மாட்டார்கள். டிஜிட்டல் அரெஸ்ட் என்ற பெயரில் வரும் மோசடி அழைப்புகளை நம்பாதீர்கள்.",
                "tactics": [],
                "stage": "CONTACT"
            },
            {
                "speaker": "caller",
                "text": "எந்த ஒரு காரணத்திற்காகவும் உங்கள் வங்கி விவரங்களை அல்லது ரகசிய கடவுச்சொற்களை யாரிடமும் பகிர வேண்டாம்.",
                "tactics": [],
                "stage": "CONTACT"
            }
        ]
    },

    # 8. NEGATIVE CONTROL 3: Legitimate Family Money Discussion (Tanglish) - BENIGN
    {
        "scenario": "Family Member Requesting College Fee Transfer",
        "language": "ta-Latn",
        "is_scam": False,
        "tactics": [],
        "stage_sequence": ["CONTACT"],
        "turns": [
            {
                "speaker": "caller",
                "text": "Appa, naan college-la irundhu Suresh pesuren. Exam fee kattanum pa.",
                "tactics": [],
                "stage": "CONTACT"
            },
            {
                "speaker": "receiver",
                "text": "Sari pa, evalavu panam anuppanum? GPay panna va?",
                "tactics": [],
                "stage": "CONTACT"
            },
            {
                "speaker": "caller",
                "text": "Aama pa, 2500 rupees college account-ku send pannunga. Naan evening veetukku vandhurren.",
                "tactics": [],
                "stage": "CONTACT"
            }
        ]
    },

    # 9. NEGATIVE CONTROL 4: Citizen Discussing Recent Scam Attempt (Hindi) - BENIGN
    {
        "scenario": "Citizen Reporting Scam Encounter to Friend",
        "language": "hi",
        "is_scam": False,
        "tactics": [],
        "stage_sequence": ["CONTACT"],
        "turns": [
            {
                "speaker": "caller",
                "text": "अरे भाई, आज सुबह मुझे एक बहुत अजीब कॉल आया था।",
                "tactics": [],
                "stage": "CONTACT"
            },
            {
                "speaker": "caller",
                "text": "कोई खुद को दिल्ली पुलिस का अफसर बता रहा था और कह रहा था कि आपका आधार मनी लॉन्ड्रिंग में फंस गया है।",
                "tactics": [],
                "stage": "CONTACT"
            },
            {
                "speaker": "caller",
                "text": "मैंने तुरंत फोन काट दिया क्योंकि असली पुलिस कभी फोन पर पैसे या ओटीपी नहीं मांगती।",
                "tactics": [],
                "stage": "CONTACT"
            }
        ]
    }
]

def build_dataset():
    os.makedirs(PROCESSED_DIR, exist_ok=True)
    os.makedirs(FINAL_DIR, exist_ok=True)

    all_turns: List[Dict[str, Any]] = []
    all_conversations: List[Dict[str, Any]] = []

    for conv_data in DOMAIN_CONVERSATIONS:
        conv_id = str(uuid.uuid4())
        conv_lang = conv_data["language"]
        is_scam = conv_data["is_scam"]
        conv_tactics = conv_data["tactics"]
        stage_seq = conv_data["stage_sequence"]
        scenario = conv_data["scenario"]

        turn_records: List[TurnRecord] = []
        for idx, turn_data in enumerate(conv_data["turns"]):
            cleaned_text = TextNormalizer.normalize_text(turn_data["text"])
            turn_record = TurnRecord(
                dataset_source="rakshacall_domain_indian",
                conversation_id=conv_id,
                turn_id=idx + 1,
                language=conv_lang,
                text=cleaned_text,
                speaker=turn_data["speaker"],
                is_scam=is_scam,
                tactics=turn_data["tactics"],
                stage=turn_data["stage"],
                scenario=scenario,
                severity=1.0 if is_scam else 0.0,
                source_type="conversation"
            )
            turn_records.append(turn_record)
            all_turns.append(turn_record.model_dump())

        conv_record = ConversationRecord(
            conversation_id=conv_id,
            dataset_source="rakshacall_domain_indian",
            language=conv_lang,
            is_scam=is_scam,
            scenario=scenario,
            tactics=conv_tactics,
            stage_sequence=stage_seq,
            turns=turn_records
        )
        all_conversations.append(conv_record.model_dump())

    # Write processed turns and conversations
    turns_path = os.path.join(PROCESSED_DIR, "domain_turns.jsonl")
    with open(turns_path, "w", encoding="utf-8") as f:
        for t in all_turns:
            f.write(json.dumps(t, ensure_ascii=False) + "\n")

    convs_path = os.path.join(PROCESSED_DIR, "domain_conversations.jsonl")
    with open(convs_path, "w", encoding="utf-8") as f:
        for c in all_conversations:
            f.write(json.dumps(c, ensure_ascii=False) + "\n")

    logger.info(f"Generated {len(all_conversations)} conversations and {len(all_turns)} turns in {PROCESSED_DIR}")
    return all_conversations, all_turns

if __name__ == "__main__":
    build_dataset()
