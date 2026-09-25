"""
Generates the isolated Realistic Acceptance Dataset for RakshaCall.
These files are strictly held out from training and validation:
- evaluation/unseen_scam_conversations.jsonl
- evaluation/unseen_benign_conversations.jsonl
- evaluation/tamil_cases.jsonl
- evaluation/tanglish_cases.jsonl
- evaluation/hindi_cases.jsonl
- evaluation/english_cases.jsonl
- evaluation/hard_negatives.jsonl
"""

import os
import json
import uuid

EVAL_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "evaluation")
os.makedirs(EVAL_DIR, exist_ok=True)

# 1. HARD NEGATIVES (Protective advice, security discussions, legitimate police/bank procedures)
HARD_NEGATIVES = [
    {"text": "Never share your OTP with anyone, even if they claim to be from the bank.", "is_scam": False, "language": "en", "category": "security_advisory"},
    {"text": "Police will never call you asking for money or digital custody transfer.", "is_scam": False, "language": "en", "category": "security_advisory"},
    {"text": "Do not transfer money to unknown accounts for courier verification.", "is_scam": False, "language": "en", "category": "security_advisory"},
    {"text": "This sounds like a digital arrest scam, please disconnect immediately.", "is_scam": False, "language": "en", "category": "scam_discussion"},
    {"text": "Bank officials told me never to share my PIN or CVV number.", "is_scam": False, "language": "en", "category": "personal_experience"},
    {"text": "The caller asked for OTP, but I refused and blocked the number.", "is_scam": False, "language": "en", "category": "refusal"},
    {"text": "RBI has repeatedly stated that there is no such thing as an RBI verification account.", "is_scam": False, "language": "en", "category": "official_warning"},
    {"text": "Cyber crime cell issues public warning against fake electricity bill SMS.", "is_scam": False, "language": "en", "category": "public_warning"},
    {"text": "I am lodging a complaint at the local police station regarding a fraud call.", "is_scam": False, "language": "en", "category": "complaint"},
    {"text": "Please be careful, scammers are sending APK links on WhatsApp.", "is_scam": False, "language": "en", "category": "advisory"},
    
    # Tamil Hard Negatives
    {"text": "வங்கி அதிகாரிகள் ஒருபோதும் உங்கள் கடவுச்சொல் அல்லது ஓடிபியைக் கேட்க மாட்டார்கள்.", "is_scam": False, "language": "ta", "category": "security_advisory"},
    {"text": "காவல்துறை அதிகாரிகள் போனில் பணம் கேட்க மாட்டார்கள், பயப்பட வேண்டாம்.", "is_scam": False, "language": "ta", "category": "security_advisory"},
    {"text": "தெரியாத எண்களிலிருந்து வரும் இணைப்புகளை கிளிக் செய்யாதீர்கள் என்று வங்கி கூறுகிறது.", "is_scam": False, "language": "ta", "category": "security_advisory"},
    {"text": "யாராவது டிஜிட்டல் அரெஸ்ட் என்று மிரட்டினால் உடனே 1930 எண்ணை அழைக்கவும்.", "is_scam": False, "language": "ta", "category": "helpline"},
    {"text": "ஓடிபி கேட்டார்கள், ஆனால் நான் சொல்லாமல் இணைப்பை துண்டித்துவிட்டேன்.", "is_scam": False, "language": "ta", "category": "refusal"},

    # Tanglish Hard Negatives
    {"text": "Bank-la irunthu eppovume OTP kekka maatanga, yarukkum sollathinga.", "is_scam": False, "language": "ta-Latn", "category": "security_advisory"},
    {"text": "Police phone panni panam transfer panna solla maatanga, ithu pure scam.", "is_scam": False, "language": "ta-Latn", "category": "security_advisory"},
    {"text": "AnyDesk app download panna sonnanga, naan panna maatten nu solliten.", "is_scam": False, "language": "ta-Latn", "category": "refusal"},
    {"text": "RBI verification account nu sonna nambathinga, call cut pannidunga.", "is_scam": False, "language": "ta-Latn", "category": "advisory"},
    {"text": "Ungal Aadhaar number vera yarukkum share pannathinga nu solraanga.", "is_scam": False, "language": "ta-Latn", "category": "advisory"},

    # Hindi Hard Negatives
    {"text": "बैंक कभी भी आपसे फोन पर ओटीपी या पासवर्ड नहीं मांगता है।", "is_scam": False, "language": "hi", "category": "security_advisory"},
    {"text": "पुलिस कभी भी वीडियो कॉल पर डिजिटल अरेस्ट या पैसे की मांग नहीं करती।", "is_scam": False, "language": "hi", "category": "security_advisory"},
    {"text": "अज्ञात खातों में पैसा ट्रांसफर न करें, यह साइबर धोखाधड़ी हो सकती है।", "is_scam": False, "language": "hi", "category": "security_advisory"},
    {"text": "कॉलर ने मुझसे ओटीपी मांगा लेकिन मैंने फोन काट दिया और पुलिस को सूचना दी।", "is_scam": False, "language": "hi", "category": "refusal"},
    {"text": "साइबर हेल्पलाइन 1930 पर तुरंत फ्रॉड की शिकायत दर्ज कराएं।", "is_scam": False, "language": "hi", "category": "helpline"},

    # Hinglish Hard Negatives
    {"text": "Asli police kabhi bhi phone par paise transfer karne ko nahi bolti.", "is_scam": False, "language": "hi-Latn", "category": "security_advisory"},
    {"text": "Bank wale bolte hain apna PIN aur OTP kisi ke sath share mat karo.", "is_scam": False, "language": "hi-Latn", "category": "security_advisory"},
    {"text": "Ye digital arrest ek fraud hai, turant call cut kar do aur complaint karo.", "is_scam": False, "language": "hi-Latn", "category": "scam_discussion"},
    {"text": "Usne bola AnyDesk download karo, maine mana kar diya.", "is_scam": False, "language": "hi-Latn", "category": "refusal"},
    {"text": "Electricity bill ka fake link aaya tha, maine click nahi kiya.", "is_scam": False, "language": "hi-Latn", "category": "refusal"}
]

# 2. UNSEEN MULTI-TURN SCAM CONVERSATIONS (Paraphrased, completely new phrasing)
UNSEEN_SCAM_CONVERSATIONS = [
    {
        "id": "unseen_scam_01_digital_arrest",
        "scenario": "National Cyber Crime Bureau Extortion",
        "language": "en",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION_FEAR", "ISOLATION", "URGENCY", "PAYMENT_DEMAND"],
        "turns": [
            {"speaker": "caller", "text": "This is Inspector Deshmukh from the Cyber Crime Investigation Wing in New Delhi.", "tactics": ["AUTHORITY_IMPERSONATION"], "stage": "AUTHORITY"},
            {"speaker": "receiver", "text": "Why are you calling my personal number?", "tactics": [], "stage": "AUTHORITY"},
            {"speaker": "caller", "text": "A consignment sent under your identity was intercepted containing contraband narcotics and forged passports.", "tactics": ["CRIMINAL_ALLEGATION_FEAR"], "stage": "FEAR"},
            {"speaker": "receiver", "text": "I never sent any parcel, this must be a mistake!", "tactics": [], "stage": "FEAR"},
            {"speaker": "caller", "text": "You are under virtual custody right now. Do not terminate this connection or discuss with anyone in your house.", "tactics": ["ISOLATION"], "stage": "ISOLATION"},
            {"speaker": "caller", "text": "To clear your name before immediate arrest, deposit forty thousand rupees into the judicial holding escrow.", "tactics": ["PAYMENT_DEMAND", "URGENCY"], "stage": "DEMAND"},
            {"speaker": "caller", "text": "Forward the bank transaction reference number within 5 minutes or a patrol car will arrive.", "tactics": ["URGENCY", "ESCALATION_COERCION"], "stage": "CRITICAL_BRAKE"}
        ]
    },
    {
        "id": "unseen_scam_02_trai_tamil",
        "scenario": "Telecom Authority Disconnection Tamil",
        "language": "ta",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION_FEAR", "URGENCY", "CREDENTIAL_OTP_PRESSURE"],
        "turns": [
            {"speaker": "caller", "text": "தொலைத்தொடர்பு கட்டுப்பாட்டு அலுவலகத்திலிருந்து மூத்த மேற்பார்வையாளர் பேசுகிறேன்.", "tactics": ["AUTHORITY_IMPERSONATION"], "stage": "AUTHORITY"},
            {"speaker": "receiver", "text": "என்ன பிரச்சனை சார்?", "tactics": [], "stage": "AUTHORITY"},
            {"speaker": "caller", "text": "உங்கள் எண்ணிலிருந்து ஆயிரக்கணக்கான மோசடி செய்திகள் அனுப்பப்பட்டதால் உடனே உங்கள் இணைப்பு முடக்கப்படும்.", "tactics": ["CRIMINAL_ALLEGATION_FEAR", "URGENCY"], "stage": "FEAR"},
            {"speaker": "caller", "text": "இணைப்பை உடனடியாக காப்பாற்ற உங்கள் திரையில் வரும் ஆறு இலக்க குறியீட்டை வாசிக்கவும்.", "tactics": ["CREDENTIAL_OTP_PRESSURE", "URGENCY"], "stage": "DEMAND"}
        ]
    },
    {
        "id": "unseen_scam_03_tanglish_anydesk",
        "scenario": "Netbanking Compromise AnyDesk Tanglish",
        "language": "ta-Latn",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION_FEAR", "REMOTE_ACCESS_PRESSURE", "CREDENTIAL_OTP_PRESSURE"],
        "turns": [
            {"speaker": "caller", "text": "Naan state bank central security division-la irundhu call pandren sir.", "tactics": ["AUTHORITY_IMPERSONATION"], "stage": "AUTHORITY"},
            {"speaker": "caller", "text": "Unga account-la irundhu unauthorized transfer attempt nadanthurukku, card block aaga poguthu.", "tactics": ["CRIMINAL_ALLEGATION_FEAR"], "stage": "FEAR"},
            {"speaker": "caller", "text": "Play store-la poitu TeamViewer QuickSupport app-a download panni 9 digit code-a enakku padichu kaatunga.", "tactics": ["REMOTE_ACCESS_PRESSURE"], "stage": "DEMAND"},
            {"speaker": "caller", "text": "Security clear panna unga netbanking password-a screen-la type pannunga.", "tactics": ["CREDENTIAL_OTP_PRESSURE"], "stage": "CRITICAL_BRAKE"}
        ]
    },
    {
        "id": "unseen_scam_04_hindi_electricity",
        "scenario": "Electricity Bill Threat Hindi",
        "language": "hi",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "URGENCY", "SUSPICIOUS_LINKS", "PAYMENT_DEMAND"],
        "turns": [
            {"speaker": "caller", "text": "बिजली विभाग के मुख्य कार्यालय से बात कर रहा हूं।", "tactics": ["AUTHORITY_IMPERSONATION"], "stage": "AUTHORITY"},
            {"speaker": "caller", "text": "पिछले महीने का बिल अपडेट नहीं हुआ है, आज शाम सात बजे आपकी बिजली काट दी जाएगी।", "tactics": ["URGENCY"], "stage": "FEAR"},
            {"speaker": "caller", "text": "व्हाट्सएप पर भेजे गए अपडेट लिंक को खोलकर तुरंत पंद्रह रुपये का भुगतान करें।", "tactics": ["SUSPICIOUS_LINKS", "PAYMENT_DEMAND"], "stage": "DEMAND"}
        ]
    },
    {
        "id": "unseen_scam_05_hinglish_customs",
        "scenario": "Customs Contraband Seizure Hinglish",
        "language": "hi-Latn",
        "is_scam": True,
        "tactics": ["AUTHORITY_IMPERSONATION", "CRIMINAL_ALLEGATION_FEAR", "ISOLATION", "PAYMENT_DEMAND"],
        "turns": [
            {"speaker": "caller", "text": "Customs air cargo terminal Mumbai se Chief Superintendent बोल रहा हूँ।", "tactics": ["AUTHORITY_IMPERSONATION"], "stage": "AUTHORITY"},
            {"speaker": "caller", "text": "Aapke naam ka parcel mila hai jisme foreign currency aur synthetic narcotics hai, non-bailable FIR ban chuki hai.", "tactics": ["CRIMINAL_ALLEGATION_FEAR"], "stage": "FEAR"},
            {"speaker": "caller", "text": "Kisi ko call mat lagana, case confidential hai warna abhi special team bhejenge.", "tactics": ["ISOLATION", "ESCALATION_COERCION"], "stage": "ISOLATION"},
            {"speaker": "caller", "text": "Bail verification clearance ke liye court ke safe deposit account me turant transfer kijiye.", "tactics": ["PAYMENT_DEMAND"], "stage": "DEMAND"}
        ]
    }
]

# 3. UNSEEN BENIGN MULTI-TURN CONVERSATIONS
UNSEEN_BENIGN_CONVERSATIONS = [
    {
        "id": "unseen_benign_01_grocery",
        "scenario": "Supermarket Delivery Coordination",
        "language": "en",
        "is_scam": False,
        "tactics": [],
        "turns": [
            {"speaker": "caller", "text": "Hello, I am calling from FreshMart regarding your grocery order.", "tactics": [], "stage": "CONTACT"},
            {"speaker": "receiver", "text": "Yes, is everything available in the cart?", "tactics": [], "stage": "CONTACT"},
            {"speaker": "caller", "text": "The almond milk is out of stock, should I replace it with oat milk?", "tactics": [], "stage": "CONTACT"},
            {"speaker": "receiver", "text": "Yes, that is fine. Please leave the bags at the doorstep.", "tactics": [], "stage": "CONTACT"}
        ]
    },
    {
        "id": "unseen_benign_02_tamil_family",
        "scenario": "Family Bus Arrival Tamil",
        "language": "ta",
        "is_scam": False,
        "tactics": [],
        "turns": [
            {"speaker": "caller", "text": "அம்மா, நான் இப்போதுதான் கோயம்பேடு பேருந்து நிலையத்தில் இறங்கினேன்.", "tactics": [], "stage": "CONTACT"},
            {"speaker": "receiver", "text": "சரிப்பா, ஆட்டோ பிடித்து பத்திரமாக வீட்டுக்கு வா.", "tactics": [], "stage": "CONTACT"},
            {"speaker": "caller", "text": "சாப்பாடு தயாரா இருக்காம்மா? எனக்கு ரொம்ப பசிக்குது.", "tactics": [], "stage": "CONTACT"}
        ]
    },
    {
        "id": "unseen_benign_03_tanglish_office",
        "scenario": "Colleague Meeting Reschedule Tanglish",
        "language": "ta-Latn",
        "is_scam": False,
        "tactics": [],
        "turns": [
            {"speaker": "caller", "text": "Machi, innaiku afternoon client presentation-a 3 PM ku postpone pannirukanga.", "tactics": [], "stage": "CONTACT"},
            {"speaker": "receiver", "text": "Appadiya? Super, enakku slide ready panna konjam time kedaikkum.", "tactics": [], "stage": "CONTACT"},
            {"speaker": "caller", "text": "Seri, meet pannitu lunch polaam.", "tactics": [], "stage": "CONTACT"}
        ]
    },
    {
        "id": "unseen_benign_04_hindi_plumber",
        "scenario": "Home Maintenance Hindi",
        "language": "hi",
        "is_scam": False,
        "tactics": [],
        "turns": [
            {"speaker": "caller", "text": "नमस्ते भैया, मैं पाइप रिपेयर करने के लिए सोसायटी के गेट पर आ गया हूँ।", "tactics": [], "stage": "CONTACT"},
            {"speaker": "receiver", "text": "हाँ, मैं गार्ड को बोल देता हूँ आपको अंदर आने देगा, फ्लैट 402 में आ जाइए।", "tactics": [], "stage": "CONTACT"}
        ]
    }
]

def generate_acceptance_datasets():
    # Save hard negatives
    hn_path = os.path.join(EVAL_DIR, "hard_negatives.jsonl")
    with open(hn_path, "w", encoding="utf-8") as f:
        for item in HARD_NEGATIVES:
            f.write(json.dumps(item, ensure_ascii=False) + "\n")
    print(f"Saved {len(HARD_NEGATIVES)} hard negatives to {hn_path}")

    # Save unseen scam conversations
    scam_conv_path = os.path.join(EVAL_DIR, "unseen_scam_conversations.jsonl")
    with open(scam_conv_path, "w", encoding="utf-8") as f:
        for conv in UNSEEN_SCAM_CONVERSATIONS:
            f.write(json.dumps(conv, ensure_ascii=False) + "\n")
    print(f"Saved {len(UNSEEN_SCAM_CONVERSATIONS)} unseen scam conversations to {scam_conv_path}")

    # Save unseen benign conversations
    benign_conv_path = os.path.join(EVAL_DIR, "unseen_benign_conversations.jsonl")
    with open(benign_conv_path, "w", encoding="utf-8") as f:
        for conv in UNSEEN_BENIGN_CONVERSATIONS:
            f.write(json.dumps(conv, ensure_ascii=False) + "\n")
    print(f"Saved {len(UNSEEN_BENIGN_CONVERSATIONS)} unseen benign conversations to {benign_conv_path}")

    # Save language specific test cases (isolated turns)
    lang_files = {
        "tamil_cases.jsonl": "ta",
        "tanglish_cases.jsonl": "ta-Latn",
        "hindi_cases.jsonl": "hi",
        "english_cases.jsonl": "en"
    }

    all_turns = []
    for conv in UNSEEN_SCAM_CONVERSATIONS + UNSEEN_BENIGN_CONVERSATIONS:
        for turn in conv["turns"]:
            all_turns.append({
                "conversation_id": conv["id"],
                "text": turn["text"],
                "speaker": turn["speaker"],
                "is_scam": conv["is_scam"],
                "tactics": turn["tactics"],
                "language": conv["language"],
                "stage": turn["stage"]
            })
    for hn in HARD_NEGATIVES:
        all_turns.append({
            "conversation_id": f"hn_{uuid.uuid4().hex[:8]}",
            "text": hn["text"],
            "speaker": "caller",
            "is_scam": False,
            "tactics": [],
            "language": hn["language"],
            "stage": "CONTACT"
        })

    for fname, lang_code in lang_files.items():
        subset = [t for t in all_turns if t["language"] == lang_code]
        fpath = os.path.join(EVAL_DIR, fname)
        with open(fpath, "w", encoding="utf-8") as f:
            for item in subset:
                f.write(json.dumps(item, ensure_ascii=False) + "\n")
        print(f"Saved {len(subset)} cases to {fpath} for language {lang_code}")

if __name__ == "__main__":
    generate_acceptance_datasets()
