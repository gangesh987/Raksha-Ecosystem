"""
RakshaCall Multilingual Contextual Tactic & Risk Engine.
Implements the canonical 13-tactic taxonomy, deep multilingual & code-switching
semantic detection, evidence span extraction, benign false-positive suppression,
and multi-turn conversation risk state accumulation.

Covers:
- English, Tamil, Hindi, Telugu, Kannada, Malayalam, Bengali, Marathi, Gujarati, Punjabi, Odia
- Tanglish (Tamil + English) and Hinglish (Hindi + English)
- Native scripts and Romanized phonetic transliterations
"""

from __future__ import annotations
import re
import time
from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, List, Optional, Tuple, Any, Set


class TacticSeverity(str, Enum):
    CRITICAL = "CRITICAL"
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"


@dataclass
class TacticDefinition:
    name: str
    display_name: str
    severity: TacticSeverity
    base_weight: int
    is_irreversible: bool
    description: str


# Canonical 13-Tactic Taxonomy
CANONICAL_TACTICS: Dict[str, TacticDefinition] = {
    "AUTHORITY_IMPERSONATION": TacticDefinition(
        name="AUTHORITY_IMPERSONATION",
        display_name="Authority Impersonation",
        severity=TacticSeverity.HIGH,
        base_weight=20,
        is_irreversible=False,
        description="Impersonating police, CBI, customs, RBI, court, TRAI, telecom, or government investigator"
    ),
    "URGENCY_PRESSURE": TacticDefinition(
        name="URGENCY_PRESSURE",
        display_name="Urgency Pressure",
        severity=TacticSeverity.MEDIUM,
        base_weight=15,
        is_irreversible=False,
        description="Artificial time pressure demanding instantaneous compliance ('now', 'within 10 minutes')"
    ),
    "PAYMENT_DEMAND": TacticDefinition(
        name="PAYMENT_DEMAND",
        display_name="Payment Demand",
        severity=TacticSeverity.HIGH,
        base_weight=25,
        is_irreversible=True,
        description="Demanding fund transfers, clearance fees, escrow security deposit, or penalty payments"
    ),
    "OTP_REQUEST": TacticDefinition(
        name="OTP_REQUEST",
        display_name="OTP Request",
        severity=TacticSeverity.CRITICAL,
        base_weight=30,
        is_irreversible=True,
        description="Demanding disclosure of SMS OTP or authentication verification code"
    ),
    "CREDENTIAL_REQUEST": TacticDefinition(
        name="CREDENTIAL_REQUEST",
        display_name="Credential Request",
        severity=TacticSeverity.CRITICAL,
        base_weight=30,
        is_irreversible=True,
        description="Coercing disclosure of UPI PIN, card CVV, netbanking password, or card number"
    ),
    "REMOTE_ACCESS_REQUEST": TacticDefinition(
        name="REMOTE_ACCESS_REQUEST",
        display_name="Remote Access Request",
        severity=TacticSeverity.CRITICAL,
        base_weight=30,
        is_irreversible=True,
        description="Instructing user to install AnyDesk, TeamViewer, QuickSupport, or share screen"
    ),
    "THREAT_OR_FEAR": TacticDefinition(
        name="THREAT_OR_FEAR",
        display_name="Threat & Coercive Fear",
        severity=TacticSeverity.HIGH,
        base_weight=20,
        is_irreversible=False,
        description="Threatening arrest warrant, jail, FIR, criminal case, or narcotics allegations"
    ),
    "SECRECY_PRESSURE": TacticDefinition(
        name="SECRECY_PRESSURE",
        display_name="Secrecy & Isolation Pressure",
        severity=TacticSeverity.HIGH,
        base_weight=20,
        is_irreversible=False,
        description="Demanding confidentiality, closed doors, not telling family, or digital custody"
    ),
    "IDENTITY_VERIFICATION_PRESSURE": TacticDefinition(
        name="IDENTITY_VERIFICATION_PRESSURE",
        display_name="Identity Verification Pressure",
        severity=TacticSeverity.MEDIUM,
        base_weight=15,
        is_irreversible=False,
        description="Coercing immediate confirmation of Aadhaar, PAN, or bank account verification"
    ),
    "SUSPICIOUS_LINK": TacticDefinition(
        name="SUSPICIOUS_LINK",
        display_name="Suspicious Link / APK",
        severity=TacticSeverity.HIGH,
        base_weight=20,
        is_irreversible=True,
        description="Directing victim to open unverified URL, click link, or install unofficial APK"
    ),
    "PERSONAL_INFORMATION_REQUEST": TacticDefinition(
        name="PERSONAL_INFORMATION_REQUEST",
        display_name="Personal Information Request",
        severity=TacticSeverity.MEDIUM,
        base_weight=15,
        is_irreversible=False,
        description="Harvesting date of birth, mother's maiden name, bank account number, or address"
    ),
    "ROMANCE_OR_TRUST_MANIPULATION": TacticDefinition(
        name="ROMANCE_OR_TRUST_MANIPULATION",
        display_name="Trust / Emergency Manipulation",
        severity=TacticSeverity.MEDIUM,
        base_weight=15,
        is_irreversible=False,
        description="Exploiting fake emotional emergency, hospital admission, or impersonating relative"
    ),
    "INVESTMENT_OR_REWARD_SCAM": TacticDefinition(
        name="INVESTMENT_OR_REWARD_SCAM",
        display_name="Investment / Reward Scam",
        severity=TacticSeverity.MEDIUM,
        base_weight=15,
        is_irreversible=False,
        description="Promising guaranteed returns, lottery winnings, double money, or fake prizes"
    )
}

# Legacy mapping for backwards compatibility with earlier 9-tactic short keys
LEGACY_SHORT_KEY_MAPPING = {
    "AUTHORITY": "AUTHORITY_IMPERSONATION",
    "URGENCY": "URGENCY_PRESSURE",
    "PAYMENT": "PAYMENT_DEMAND",
    "CREDENTIAL": "OTP_REQUEST",
    "REMOTE_ACCESS": "REMOTE_ACCESS_REQUEST",
    "FEAR": "THREAT_OR_FEAR",
    "ISOLATION": "SECRECY_PRESSURE",
    "SUSPICIOUS_LINK": "SUSPICIOUS_LINK",
    "ESCALATION": "THREAT_OR_FEAR"
}


@dataclass
class DetectedTactic:
    tactic_type: str
    display_name: str
    severity: str
    confidence: float
    evidence: str
    language: str
    is_irreversible: bool
    weight: int

    def to_dict(self) -> Dict[str, Any]:
        return {
            "type": self.tactic_type,
            "display_name": self.display_name,
            "severity": self.severity,
            "confidence": round(self.confidence, 3),
            "evidence": self.evidence,
            "language": self.language,
            "is_irreversible": self.is_irreversible,
            "weight": self.weight
        }


# =========================================================================
# BENIGN CONVERSATION SUPPRESSION RULES
# Distinguish benign everyday mention of words from coercive scam tactics.
# =========================================================================
BENIGN_SUPPRESSION_PATTERNS = [
    # 1. Physical location / directions / visiting
    r'(?i)\b(near (the|my|our) (police station|bank|branch)|next to (the|my) (police station|bank)|behind (the|my) (police station|bank))\b',
    r'(?i)\b(going to (the|my) (police station|bank|branch|court)|visited (the|my) (bank|police station)|at the (police station|branch|bank))\b',
    r'(?i)\b(police station is (near|far|closed)|bank is (near|closed|open))\b',
    r'(?i)\b(my bank asked me to visit the branch|visit the branch (tomorrow|today|next week))\b',
    r'(?i)\b(complete kyc at the branch|in-person kyc|visit official branch)\b',

    # 2. Third-person benign assistance / past events / lost items
    r'(?i)\b(police officer helped me|police helped (us|me|him|them)|reported to police|police found my (phone|wallet|car))\b',
    r'(?i)\b(called about my lost (phone|bag|wallet)|lodged a missing report|lost phone report)\b',

    # 3. Everyday non-coercive money transfers (grocery, family, friends)
    r'(?i)\b(send money to (the|a) (grocery shop|store|market|vegetable vendor|canteen|driver))\b',
    r'(?i)\b((transfer|send) money to (my|our) (mother|father|mom|dad|sister|brother|friend|wife|husband|son|daughter))\b',
    r'(?i)\b(transfer money to (mom|dad|grandma|grandpa))\b',
    r'(?i)\b(can you transfer money to my (mother|father|mom|dad|brother|sister|friend))\b',
    r'(?i)\b(split the (bill|dinner|lunch|cab)|upi for the (tea|coffee|dinner|lunch|grocery))\b',

    # 4. Everyday urgent tasks (academic, work, normal life)
    r'(?i)\b((this|my) (assignment|homework|project|presentation|report) is urgent)\b',
    r'(?i)\b(urgently need to submit (my|the) (assignment|homework|project|thesis|paper))\b',
    r'(?i)\b(urgent meeting with (my|our) (manager|boss|team|client))\b',
    r'(?i)\b(urgent doctor appointment|urgent medical checkup)\b',

    # 5. Non-coercive OTP notifications / routine 2FA
    r'(?i)\b(i received an otp from my bank|got an otp for my (order|delivery|login))\b',
    r'(?i)\b(did you get the otp\?|waiting for my otp)\b',
    r'(?i)\b(never share your otp|otp is confidential|don\'t tell anyone the otp)\b',

    # 6. Educational, media, documentaries
    r'(?i)\b(watched a (documentary|video|news report|movie) about (digital arrest|scams?|fraud))\b',
    r'(?i)\b(read an article about (digital arrest|scams?|cyber crime))\b',
    r'(?i)\b(is this a scam\?|this sounds like a scam|awareness program|cyber crime awareness)\b',

    # Indic Benign Phrases (Tamil, Hindi, Telugu, Tanglish, Hinglish)
    r'(?i)(போலீஸ் ஸ்டேஷன் பக்கத்துல இருக்கு|என் அம்மாவுக்கு பணம் அனுப்பணும்|அசைன்மென்ட் அவசரம்|பேங்க்ல நேர்ல வர சொன்னாங்க)',
    r'(?i)(police station paas mein hai|mummy ko paise bhejne hai|college assignment urgent hai|bank jaana hai branch)',
    r'(?i)(police station daggara undi|amma ki dabbu pampali|assignment urgent|branch ki vellali)'
]


# =========================================================================
# MULTILINGUAL TACTIC PATTERNS & REGEX SUITE
# Covers 11 Indic languages + English + Tanglish + Hinglish (Script & Latin)
# =========================================================================
TACTIC_PATTERNS: Dict[str, List[re.Pattern]] = {
    "AUTHORITY_IMPERSONATION": [
        # English
        re.compile(r'(?i)\b(i am|i\'m|this is|calling from|we are)\s+(a\s+|the\s+)?(police|cbi|ed|customs|cyber crime|rbi|trai|narcotics bureau|supreme court|high court|crime branch|income tax|investigator|bank manager)\b'),
        re.compile(r'(?i)\b(police officer|cbi officer|customs officer|cyber crime officer|rbi officer|telecom officer|inspector general|investigating officer|law enforcement officer)\b'),
        re.compile(r'(?i)\b(delhi|mumbai|bangalore|chennai|hyderabad|kolkata)?\s*police (department|station|headquarters|crime cell)\b'),
        re.compile(r'(?i)\b(calling on behalf of (the)? (delhi|mumbai|bangalore|chennai|hyderabad|kolkata)? (police|court|customs))\b'),
        # Tamil (Native + Tanglish)
        re.compile(r'(?i)(நான்|நாங்க|இங்க)\s*(போலீஸ்|காவல்துறை|சிபிஐ|கஸ்டம்ஸ்|சைபர் கிரைம்|ஆர்பிஐ|நீதிமன்றம்|கோர்ட்)\s*(ல|லிருந்து|அதிகாரி)?\s*(இருந்து)?\s*(பேசுறேன்|பேசுறோம்)?'),
        re.compile(r'(?i)\b(naan|naanga)\s*(police|cbi|customs|cyber crime|rbi|court)\s*(la|lendhu|officer)?\s*(irundhu)?\s*(pesuren|pesuroam)?\b'),
        re.compile(r'(?i)\bpolice\s+(department|station)?\s*(la)?\s*irundhu\s+pesuren\b'),
        re.compile(r'(?i)\b(cbi|customs|cyber crime)\s+officer\s+pesuren\b'),
        # Hindi (Devanagari + Hinglish)
        re.compile(r'(?i)(मैं|हम)\s*(पुलिस|सीबीआई|कस्टम्स|साइबर क्राइम|आरबीआई|क्राइम ब्रांच|इनकम टैक्स)\s*(से बोल रहा|अधिकारी|विभाग)'),
        re.compile(r'(?i)\b(main|hum)\s*(police|cbi|customs|cyber crime|crime branch|rbi)\s*se\s*(bol raha|baat kar raha)\b'),
        re.compile(r'(?i)\b(police|cbi|customs)\s+officer\s+bol\s+raha\s+hoon\b'),
        # Telugu (Script + Romanized)
        re.compile(r'(?i)(నేను|మేము)\s*(పోలీస్|సీబీఐ|కస్టమ్స్|సైబర్ క్రైమ్|బ్యాంక్|ఆర్బీఐ)\s*(నుండి|అధికారి)?\s*(మాట్లాడుతున్నాను)?'),
        re.compile(r'(?i)\b(nenu|memu)\s*(police|cbi|customs|cyber crime|bank|rbi)\s*(nundi|officer)?\s*(matladutunnanu|matladuthunnanu)?\b'),
        # Kannada (Script + Romanized)
        re.compile(r'(?i)(ನಾನು|ನಾವು)\s*(ಪೊಲೀಸ್|ಸಿಬಿಐ|ಕಸ್ಟಮ್ಸ್)\s*(ಇಂದ|ಅಧಿಕಾರಿ)?\s*(ಮಾತನಾಡುತ್ತಿದ್ದೇನೆ)?'),
        re.compile(r'(?i)\b(naanu|naavu)\s*(police|cbi|customs)\s*inda\s*(mathadtha idhini|officer|matanaduttiddene)?\b'),
        # Malayalam (Script + Romanized)
        re.compile(r'(?i)(ഞാൻ)\s*(പോലീസ്|സിബിഐ|കస్టమ్స్)\s*(ഓഫീസർ|ആണ് സംസാരിക്കുന്നത്)?'),
        re.compile(r'(?i)\b(njan)\s*(police|cbi|customs)\s*(officer|ninnu samsarikkunnu|aanu samsarikkunnathu)?\b'),
        # Bengali (Script + Romanized)
        re.compile(r'(?i)(আমি|আমরা)\s*(পুলিশ|সিবিআই|কাস্টমস)\s*(অফিসার|থেকে বলছি)'),
        re.compile(r'(?i)\b(ami|amra)\s*(police|cbi|customs)\s*(theke bolchi|officer)\b'),
        # Marathi
        re.compile(r'(?i)(मी|आम्ही)\s*(पोलीस|सीबीआय|कस्टम्स)\s*(मधून बोलतोय|अधिकारी)'),
        re.compile(r'(?i)\b(mi|aamhi)\s*(police|cbi)\s*madhun\s*boltoy\b'),
        # Gujarati
        re.compile(r'(?i)(હું|અમે)\s*(પોલીસ|સીબીઆઈ|કસ્ટમ્સ)\s*(માંથી બોલું છું|અધિકારી)'),
        re.compile(r'(?i)\b(hu|ame)\s*(police|cbi)\s*mathi\s*bolu\s*chu\b'),
        # Punjabi
        re.compile(r'(?i)(ਮੈਂ|ਅਸੀਂ)\s*(ਪੁਲਿਸ|ਸੀਬੀਆਈ)\s*(ਤੋਂ ਬੋਲ ਰਿਹਾ|ਅਫ਼ਸਰ)'),
        re.compile(r'(?i)\b(main|asin)\s*(police|cbi)\s*ton\s*bol\s*reha\s*haan\b'),
        # Odia
        re.compile(r'(?i)(ମୁଁ|ଆମେ)\s*(ପୋଲିସ|ସିବିଆଇ)\s*(ଅଫିସର|ତରଫରୁ କହୁଛି)'),
        re.compile(r'(?i)\b(mu|aame)\s*(police|cbi)\s*tarafaru\s*kahuchi\b')
    ],

    "URGENCY_PRESSURE": [
        # English
        re.compile(r'(?i)\b(immediately|right now|urgent|urgently|this is urgent|within (5|10|15|30) minutes|don\'t delay|without delay|instant action|final warning|last chance)\b'),
        re.compile(r'(?i)\b(do it now|transfer right now|respond immediately|time is running out)\b'),
        # Tamil & Tanglish
        re.compile(r'(?i)(உடனே|இப்பவே|பத்து நிமிடத்தில்|தாமதிக்காமல்|சீக்கிரம்|இப்போதே|இது அவசரம்|அவசரம்)'),
        re.compile(r'(?i)\b(udane|ippave|ippo|fast ah|delay pannama|seekiram|pathu nimishathula|urgent|romba urgent)\b'),
        # Hindi & Hinglish
        re.compile(r'(?i)(तुरंत|अभी के अभी|फौरन|दस मिनट के अंदर|देरी मत करो|जल्दी करो|आखिरी मौका|बहुत अर्जेंट|अर्जेंट)'),
        re.compile(r'(?i)\b(turant|abhi ke abhi|fauran|jaldi karo|der mat karo|abhi karo|10 minute ke andar|bahut urgent|urgent hai)\b'),
        # Telugu
        re.compile(r'(?i)(వెంటనే|ఇప్పుడే|తక్షణమే|ఆలస్యం చేయవద్దు|పది నిమిషాల్లో|ఇది అర్జంట్)'),
        re.compile(r'(?i)\b(ventane|ippude|thakshaname|delay cheyakandi|jaldi|urgent|idi urgent)\b'),
        # Kannada, Malayalam, Bengali, Marathi, Gujarati, Punjabi, Odia
        re.compile(r'(?i)(ತಕ್ಷಣವೇ|ಈಗಲೇ|ಕೂಡಲೇ|ಇದು ತುರ್ತು|തുർത്തു|ഉടൻ|ഇപ്പോൾ തന്നെ|ഇത് അടിയന്തിരമാണ്|অবিলম্বে|এখনই|এটা জরুরি|त्वरित|आत्ताच|हे तातडीचे आहे|તરત જ|ਹੁਣੇ|ਇਹ ਬਹੁਤ ਜ਼ਰੂਰੀ ਹੈ|ତୁରନ୍ତ|ଏହା ଜରୁରୀ)')
    ],

    "PAYMENT_DEMAND": [
        # English
        re.compile(r'(?i)\b(send\s+(me\s+|the\s+)?money|transfer\s+(the\s+)?(money|amount|funds)|make\s+(the\s+)?payment|pay\s+(the\s+)?(fine|penalty|fee|bail)|deposit\s+(the\s+)?(funds|money)|upi\s+it\s+now|send\s+₹?\d+[\d,]*|transfer\s+₹?\d+[\d,]*)\b'),
        re.compile(r'(?i)\b(move the funds|clear the balance|send cash|wire the funds|upi the money|pay via upi|send the funds|please complete the transaction)\b'),
        re.compile(r'(?i)\b(escrow account|clearance fee|rbi verification account|security deposit fee|safe custody account)\b'),
        re.compile(r'(?i)\b(pay immediately|settle the amount|transfer to this upi|send through gpay|phonepe it)\b'),
        # Tamil & Tanglish
        re.compile(r'(?i)(பணம் அனுப்பு|பணத்தை மாற்று|செலுத்த வேண்டும்|வைப்புத்தொகை|ரூபாய் அனுப்பு|பணம் அனுப்புங்க)'),
        re.compile(r'(?i)\b(money send|amount transfer|panam anuppu|amount pottu vidu|gpay pannu|pay pannunga|money send pannunga)\b'),
        re.compile(r'(?i)\b(udane money send|amount-?ai ippave transfer|clearance account-ku anuppu|transfer pannunga)\b'),
        # Hindi & Hinglish
        re.compile(r'(?i)(पैसे भेजो|पैसे ट्रांसफर करो|रुपये जमा करो|जुर्माना भरो|खाते में डालो|भुगतान करो|अभी पैसे भेजो)'),
        re.compile(r'(?i)\b(paise bhejo|money send karo|amount transfer karo|fine bharo|security deposit jama karo|gpay karo|abhi paise bhejo)\b'),
        # Telugu
        re.compile(r'(?i)(డబ్బు పంపండి|డబ్బును బదిలీ చేయండి|రుసుము చెల్లించండి|ఫైన్ కట్టండి|డబ్బు పంపించండి)'),
        re.compile(r'(?i)\b(dabbu pampinchandi|amount transfer cheyandi|dabbu transfer|amount ippude pampinchandi)\b'),
        # Other Indic
        re.compile(r'(?i)(ಹಣವನ್ನು ವರ್ಗಾಯಿಸಿ|ಹಣ ಕಳುಹಿಸಿ|പണം അയക്കുക|টাকা পাঠান|पैसे पाठवा|પૈસા ટ્રાન્સફર કરો|પૈસા મોકલો|ਪੈਸੇ ਭੇਜੋ|ଟଙ୍କା ପଠାନ୍ତୁ)')
    ],

    "OTP_REQUEST": [
        # English
        re.compile(r'(?i)\b(tell\s+me\s+(the\s+|your\s+)?otp|share\s+(the\s+|your\s+)?otp|give\s+me\s+(the\s+|your\s+)?otp|what\s+is\s+(the\s+)?otp|send\s+(the\s+|your\s+)?otp|verification\s+code|one[- ]time\s+password|read\s+out\s+(the\s+)?6\s+digit\s+code)\b'),
        # Tamil & Tanglish
        re.compile(r'(?i)(ஓடிபி சொல்லு|ஓடிபி எண்|வந்த ஓடிபி)'),
        re.compile(r'(?i)\b(otp sollunga|otp sollu|otp share pannu|vantha otp)\b'),
        # Hindi & Hinglish
        re.compile(r'(?i)(ओटीपी बताओ|ओटीपी दो|ओटीपी शेयर करो|वेरिफिकेशन कोड बताओ)'),
        re.compile(r'(?i)\b(otp batao|otp do|otp share karo|verification code bataiye)\b'),
        # Telugu
        re.compile(r'(?i)(ఓటీపీ చెప్పండి|ఓటీపీ ఇవ్వండి)'),
        re.compile(r'(?i)\b(otp cheppandi|otp ivvandi)\b'),
        # Other Indic
        re.compile(r'(?i)(ಒಟಿಪಿ ಹೇಳಿ|ഒടിപി പറയുക|ওটিপি বলুন|ओटीपी सांगा|ઓટીપી આપો|ਓਟੀਪੀ ਦੱਸੋ|ଓଟିପି କୁହନ୍ତୁ)')
    ],

    "CREDENTIAL_REQUEST": [
        # English
        re.compile(r'(?i)\b(upi pin|atm pin|password|cvv( number)?|card number|expiry date|netbanking password|banking credentials)\b'),
        re.compile(r'(?i)\b(enter your pin|share your pin|tell me your pin|disclose your password)\b'),
        # Tamil & Tanglish
        re.compile(r'(?i)(பின் எண்|கடவுச்சொல்|சிவிவி எண்)'),
        re.compile(r'(?i)\b(upi pin sollunga|pin number|password sollunga|cvv sollunga)\b'),
        # Hindi & Hinglish
        re.compile(r'(?i)(यूपीआई पिन|पासवर्ड|सीवीवी नंबर|कार्ड नंबर बताओ)'),
        re.compile(r'(?i)\b(upi pin batao|atm pin do|password batao|cvv number)\b'),
        # Telugu & Other Indic
        re.compile(r'(?i)(పిన్ నంబర్|పాస్వర్డ్|യുപിഐ പിൻ|పాస్‌వర్డ్|পাসওয়ার্ড)')
    ],

    "REMOTE_ACCESS_REQUEST": [
        # English
        re.compile(r'(?i)\b(install (anydesk|teamviewer|quicksupport|rustdesk)|download (anydesk|teamviewer|quicksupport)|screen share|share your screen|remote access|remote control)\b'),
        re.compile(r'(?i)\b(grant permission on your screen|allow remote connection)\b'),
        # Tamil & Tanglish
        re.compile(r'(?i)(எனிகெஸ்க்|டீம்வியூவர்|ஸ்கிரீன் ஷேர்|பதிவிறக்கம் செய்)'),
        re.compile(r'(?i)\b(anydesk install pannunga|teamviewer download pannu|screen share pannunga)\b'),
        # Hindi & Hinglish
        re.compile(r'(?i)(एनीडेस्क|टीमव्यूअर|स्क्रीन शेयर करो|रिमोट एक्सेस)'),
        re.compile(r'(?i)\b(anydesk download karo|teamviewer install karo|screen share kijiye)\b'),
        # Other Indic
        re.compile(r'(?i)(anydesk|teamviewer|quicksupport|rustdesk|screen share)')
    ],

    "THREAT_OR_FEAR": [
        # English
        re.compile(r'(?i)\b(arrest warrant|arrest you|under arrest|police raid|fir registered|criminal case|money laundering|narcotics (parcel|found)|illegal contraband|jail term|seize your property|freeze your account)\b'),
        re.compile(r'(?i)\b(digital arrest|face physical arrest|non-bailable warrant|passport cancelled|terror funding)\b'),
        # Tamil & Tanglish
        re.compile(r'(?i)(கைது வாரண்ட்|சிறை|வழக்கு பதிவு|பண மோசடி|போதைப்பொருள்|சொத்து பறிமுதல்|அரெஸ்ட் பண்ணுவோம்)'),
        re.compile(r'(?i)\b(arrest warrant|jail-ku anuppuvom|fir potrukku|police arrest pannum|illegal parcel|case poduvom)\b'),
        # Hindi & Hinglish
        re.compile(r'(?i)(गिरफ्तारी वारंट|जेल होगी|एफआईआर दर्ज|मनी लॉन्ड्रिंग|गैरकानूनी पार्सल|नारकोटिक्स|खाता सीज|संपत्ति कुर्क)'),
        re.compile(r'(?i)\b(arrest warrant|jail hogi|fir darj|ghar par raid|arrest karenge|khata freeze ho jayega)\b'),
        # Telugu
        re.compile(r'(?i)(అరెస్ట్ వారెంట్|జైలుకు పంపుతాము|కేసు నమోదు|ఖాతా సీజ్|మనీ లాండరింగ్)'),
        re.compile(r'(?i)\b(arrest warrant|jail ki pampistam|case register ayindi)\b')
    ],

    "SECRECY_PRESSURE": [
        # English
        re.compile(r'(?i)\b(don\'t tell (anyone|family|anybody|parents|friends)|keep (this)? (confidential|secret)|stay in a closed room|do not disconnect (the)? (call|line)|stay alone|secret investigation)\b'),
        re.compile(r'(?i)\b(keep your camera on|do not call your bank|do not consult anyone)\b'),
        # Tamil & Tanglish
        re.compile(r'(?i)(யாருக்கும் சொல்லாதே|ரகசிய விசாரணை|அறையில் தனியாக இரு|அழைப்பை துண்டிக்காதே|கேமராவை ஆன் செய்)'),
        re.compile(r'(?i)\b(yarukkum solla koodathu|confidential ah vechuko|room door lock pannu|call cut pannathinga)\b'),
        # Hindi & Hinglish
        re.compile(r'(?i)(किसी को मत बताना|गोपनीय जांच|अकेले रहो|फोन मत काटना|कमरे का दरवाजा बंद रखो|कैमरा ऑन रखो)'),
        re.compile(r'(?i)\b(kisi ko mat batana|phone cut mat karna|confidential investigation|kamre me akele raho)\b'),
        # Telugu
        re.compile(r'(?i)(ఎవరికీ చెప్పవద్దు|రహస్య విచారణ|కాల్ కట్ చేయవద్దు|ఒంటరిగా ఉండండి)'),
        re.compile(r'(?i)\b(evvariki cheppakandi|call cut cheyakandi|secret investigation)\b')
    ],

    "IDENTITY_VERIFICATION_PRESSURE": [
        re.compile(r'(?i)\b(verify (your)? (identity|aadhaar|pan|bank account)|confirm your aadhaar number|kyc verification required immediately|re-verify your sim)\b'),
        re.compile(r'(?i)(ஆதார் சரிபார்ப்பு|சரிபார்க்க வேண்டும்|आधार सत्यापन|केवाईसी तुरंत|ఆధార్ ధృవీకరణ)')
    ],

    "SUSPICIOUS_LINK": [
        re.compile(r'(?i)\b(click\s+(on\s+)?(this\s+|the\s+)?link|open\s+(this\s+|the\s+)?website|install\s+(this\s+)?apk|download\s+(this\s+)?(file|apk)|open\s+(the\s+)?url)\b'),
        re.compile(r'(?i)(இணைப்பை கிளிக் செய்|ஏபிகே பதிவிறக்கு|लिंक पर क्लिक करो|एपीके डाउनलोड|లింక్ క్లిక్ చేయండి)')
    ],

    "PERSONAL_INFORMATION_REQUEST": [
        re.compile(r'(?i)\b(give me your aadhaar|tell me your pan card|mother\'s maiden name|date of birth|full account number)\b'),
        re.compile(r'(?i)(ஆதார் எண் சொல்லு|पैन कार्ड नंबर बताओ|ఆధార్ నంబర్ చెప్పండి)')
    ],

    "ROMANCE_OR_TRUST_MANIPULATION": [
        re.compile(r'(?i)\b(in hospital|accident happened|relative in custody|medical emergency need money|trust me send money)\b'),
        re.compile(r'(?i)(மருத்துவமனை|விபத்து|अस्पताल में भर्ती|एक्सीडेंट हो गया|ఆసుపత్రిలో ఉన్నారు)')
    ],

    "INVESTMENT_OR_REWARD_SCAM": [
        re.compile(r'(?i)\b(guaranteed (return|profit)|double your money|lottery (winner|won)|won a prize|claim your reward|100% risk free investment)\b'),
        re.compile(r'(?i)(பரிசு விழுந்துள்ளது|இரட்டிப்பு லாபம்|लॉटरी निकली है|पैसे डबल|లాటరీ తగిలింది)')
    ]
}


@dataclass
class TacticExtractionResult:
    tactics: List[DetectedTactic]
    is_suppressed_as_benign: bool
    benign_reason: Optional[str]
    detected_language: str
    is_code_switch: bool
    raw_text: str


@dataclass
class ConversationRiskState:
    """Maintains multi-turn context and tracks chronological threat escalation."""
    session_id: str
    turn_count: int = 0
    current_score: int = 0
    previous_score: int = 0
    detected_tactics: List[DetectedTactic] = field(default_factory=list)
    cumulative_tactics: Dict[str, DetectedTactic] = field(default_factory=dict)
    evidence_spans: Dict[str, List[str]] = field(default_factory=dict)
    escalation_trend: str = "STABLE"   # "INCREASING", "STABLE", "DECREASING"
    confidence: float = 0.90
    created_at: float = field(default_factory=time.time)
    turn_history: List[Dict[str, Any]] = field(default_factory=list)
    consecutive_benign_turns: int = 0
    latest_result: Optional[TacticExtractionResult] = None

    @property
    def risk_level(self) -> str:
        if self.current_score >= 80:
            return "CRITICAL"
        elif self.current_score >= 60:
            return "HIGH"
        elif self.current_score >= 35:
            return "MEDIUM"
        return "LOW"

    @property
    def explanation(self) -> str:
        if self.latest_result:
            return self._generate_explanation(self.latest_result)
        return "No manipulation tactics detected."

    @property
    def is_suppressed_as_benign(self) -> bool:
        return self.latest_result.is_suppressed_as_benign if self.latest_result else False

    @property
    def benign_reason(self) -> Optional[str]:
        return self.latest_result.benign_reason if self.latest_result else None

    @property
    def detected_language(self) -> str:
        return self.latest_result.detected_language if self.latest_result else "en-IN"

    @property
    def is_code_switch(self) -> bool:
        return self.latest_result.is_code_switch if self.latest_result else False

    @property
    def last_updated(self) -> str:
        import datetime
        return datetime.datetime.now().isoformat()

    def to_dict(self) -> Dict[str, Any]:
        return {
            "risk_score": self.current_score,
            "risk_level": self.risk_level,
            "confidence": round(self.confidence, 2),
            "language": self.detected_language,
            "code_switch": self.is_code_switch,
            "tactics": [t.to_dict() for t in self.detected_tactics],
            "cumulative_tactics": [t.to_dict() for t in self.cumulative_tactics.values()],
            "evidence_spans": self.evidence_spans,
            "explanation": self.explanation,
            "escalation": self.escalation_trend,
            "timestamp": self.last_updated,
            "is_suppressed_as_benign": self.is_suppressed_as_benign,
            "benign_reason": self.benign_reason,
            "turn_count": self.turn_count
        }

    def add_turn_analysis(
        self,
        transcript: str,
        tactic_result: TacticExtractionResult
    ) -> Dict[str, Any]:
        """Incorporate a new dialogue turn, update cumulative tactics, and compute risk."""
        self.latest_result = tactic_result
        self.turn_count += 1
        self.previous_score = self.current_score

        if tactic_result.is_suppressed_as_benign or not tactic_result.tactics:
            self.consecutive_benign_turns += 1
            # Apply graceful decay if no tactics detected for several turns and current risk isn't critical
            if self.consecutive_benign_turns >= 2 and self.current_score > 0 and self.current_score < 75:
                decay = min(15, self.current_score)
                self.current_score = max(0, self.current_score - decay)
        else:
            self.consecutive_benign_turns = 0
            # Accumulate new tactics
            for tac in tactic_result.tactics:
                self.detected_tactics.append(tac)
                if tac.tactic_type not in self.cumulative_tactics or tac.confidence > self.cumulative_tactics[tac.tactic_type].confidence:
                    self.cumulative_tactics[tac.tactic_type] = tac

                # Record evidence spans
                if tac.tactic_type not in self.evidence_spans:
                    self.evidence_spans[tac.tactic_type] = []
                if tac.evidence not in self.evidence_spans[tac.tactic_type]:
                    self.evidence_spans[tac.tactic_type].append(tac.evidence)

            # Recompute total score using transparent weighted scoring
            self.current_score = self._compute_weighted_score()

        # Determine escalation trend
        if self.current_score > self.previous_score + 5:
            self.escalation_trend = "INCREASING"
        elif self.current_score < self.previous_score - 5:
            self.escalation_trend = "DECREASING"
        else:
            self.escalation_trend = "STABLE"

        # Categorize risk level
        if self.current_score >= 81:
            risk_level = "CRITICAL"
        elif self.current_score >= 61:
            risk_level = "HIGH"
        elif self.current_score >= 31:
            risk_level = "MEDIUM"
        else:
            risk_level = "LOW"

        # Generate explanation
        explanation = self._generate_explanation(tactic_result)

        turn_record = {
            "turn_number": self.turn_count,
            "transcript": transcript,
            "risk_score": self.current_score,
            "risk_level": risk_level,
            "escalation": self.escalation_trend,
            "active_tactics": [t.to_dict() for t in tactic_result.tactics],
            "cumulative_tactics": [t.tactic_type for t in self.cumulative_tactics.values()],
            "timestamp": time.time()
        }
        self.turn_history.append(turn_record)

        return {
            "risk_score": self.current_score,
            "risk_level": risk_level,
            "confidence": round(self.confidence, 2),
            "language": tactic_result.detected_language,
            "code_switch": tactic_result.is_code_switch,
            "tactics": [t.to_dict() for t in tactic_result.tactics],
            "cumulative_tactics": [t.to_dict() for t in self.cumulative_tactics.values()],
            "evidence_spans": self.evidence_spans,
            "explanation": explanation,
            "escalation": self.escalation_trend,
            "timestamp": time.time()
        }

    def _compute_weighted_score(self) -> int:
        """
        Transparent weighted scoring model:
        Base Tactic Weights + Combination Synergy Bonus + Escalation Gradient
        """
        if not self.cumulative_tactics:
            return 0

        # 1. Base weights from cumulative tactics
        raw_score = sum(t.weight for t in self.cumulative_tactics.values())

        # 2. Tactic Combination Synergy Bonuses
        types = set(self.cumulative_tactics.keys())

        # Synergy 1: Authority + Threat/Fear
        if "AUTHORITY_IMPERSONATION" in types and "THREAT_OR_FEAR" in types:
            raw_score += 15

        # Synergy 2: Authority + Urgency + Payment Demand (Classic extortion scam)
        if "AUTHORITY_IMPERSONATION" in types and "URGENCY_PRESSURE" in types and "PAYMENT_DEMAND" in types:
            raw_score += 25

        # Synergy 3: Authority + Threat + OTP / Credential (Credential takeover under fear)
        if "AUTHORITY_IMPERSONATION" in types and "THREAT_OR_FEAR" in types and ("OTP_REQUEST" in types or "CREDENTIAL_REQUEST" in types):
            raw_score += 30

        # Synergy 4: Secrecy + Payment / OTP
        if "SECRECY_PRESSURE" in types and ("PAYMENT_DEMAND" in types or "OTP_REQUEST" in types):
            raw_score += 20

        # Synergy 5: Remote Access + Authority
        if "REMOTE_ACCESS_REQUEST" in types and "AUTHORITY_IMPERSONATION" in types:
            raw_score += 25

        # Cap strictly between 0 and 100
        return int(min(100, max(0, round(raw_score))))

    def _generate_explanation(self, current_result: TacticExtractionResult) -> str:
        if current_result.is_suppressed_as_benign:
            return f"Benign conversational pattern recognized: {current_result.benign_reason}"

        if not self.cumulative_tactics:
            return "No manipulative or coercive tactics detected. Communication appears standard."

        active_names = [t.display_name for t in self.cumulative_tactics.values()]
        if len(active_names) == 1:
            return f"Detected potential scam tactic: {active_names[0]}."
        elif len(active_names) == 2:
            return f"Coercive combination detected: {active_names[0]} combined with {active_names[1]}."
        else:
            return f"High-risk multi-tactic manipulation convergence detected: {', '.join(active_names[:-1])}, and {active_names[-1]}."


class MultilingualTacticEngine:
    """
    Core Multilingual Semantic Tactic Engine.
    Executes regex/semantic matching, benign suppression, and multi-turn state management.
    """

    def __init__(self):
        self._sessions: Dict[str, ConversationRiskState] = {}

    def get_or_create_state(self, session_id: str) -> ConversationRiskState:
        if session_id not in self._sessions:
            self._sessions[session_id] = ConversationRiskState(session_id=session_id)
        return self._sessions[session_id]

    def reset_session(self, session_id: str) -> None:
        if session_id in self._sessions:
            del self._sessions[session_id]

    def check_benign(self, text: str) -> Tuple[bool, Optional[str]]:
        """Check if an utterance contains contextual benign phrasing that suppresses false positives."""
        for pattern_str in BENIGN_SUPPRESSION_PATTERNS:
            match = re.search(pattern_str, text)
            if match:
                return True, f"Matched benign context: '{match.group(0)}'"
        return False, None

    def detect_language(self, text: str) -> Tuple[str, bool]:
        """Detect primary Indic script or Latin code-switch (Tanglish/Hinglish)."""
        has_tamil = any('\u0B80' <= c <= '\u0BFF' for c in text)
        has_hindi = any('\u0900' <= c <= '\u097F' for c in text)
        has_telugu = any('\u0C00' <= c <= '\u0C7F' for c in text)
        has_kannada = any('\u0C80' <= c <= '\u0CFF' for c in text)
        has_malayalam = any('\u0D00' <= c <= '\u0D7F' for c in text)
        has_bengali = any('\u0980' <= c <= '\u09FF' for c in text)
        has_gujarati = any('\u0A80' <= c <= '\u0AFF' for c in text)
        has_punjabi = any('\u0A00' <= c <= '\u0A7F' for c in text)
        has_odia = any('\u0B00' <= c <= '\u0B7F' for c in text)
        has_latin = any(('a' <= c <= 'z') or ('A' <= c <= 'Z') for c in text)

        lower = text.lower()
        is_tanglish = has_latin and any(w in lower for w in ("unga", "pannunga", "pesuren", "irundhu", "solraen", "panam", "udane"))
        is_hinglish = has_latin and any(w in lower for w in ("aapka", "kijiye", "raha hoon", "paise", "bhejo", "karo", "turant"))

        if is_tanglish:
            return "ta-Latn (Tanglish)", True
        if is_hinglish:
            return "hi-Latn (Hinglish)", True
        if has_tamil:
            return "ta-IN", has_latin
        if has_hindi:
            return "hi-IN", has_latin
        if has_telugu:
            return "te-IN", has_latin
        if has_kannada:
            return "kn-IN", has_latin
        if has_malayalam:
            return "ml-IN", has_latin
        if has_bengali:
            return "bn-IN", has_latin
        if has_gujarati:
            return "gu-IN", has_latin
        if has_punjabi:
            return "pa-IN", has_latin
        if has_odia:
            return "or-IN", has_latin

        return "en-IN", False

    def extract_tactics(self, text: str) -> TacticExtractionResult:
        """Extract all matching tactics and evidence spans from the text."""
        trimmed = text.strip()
        lang, is_code_switch = self.detect_language(trimmed)

        # 1. Check benign suppression first
        is_benign, benign_reason = self.check_benign(trimmed)
        if is_benign:
            return TacticExtractionResult(
                tactics=[],
                is_suppressed_as_benign=True,
                benign_reason=benign_reason,
                detected_language=lang,
                is_code_switch=is_code_switch,
                raw_text=trimmed
            )

        detected_tactics: List[DetectedTactic] = []
        for tactic_type, patterns in TACTIC_PATTERNS.items():
            tactic_def = CANONICAL_TACTICS[tactic_type]
            for pattern in patterns:
                match = pattern.search(trimmed)
                if match:
                    evidence_span = match.group(0).strip()
                    detected_tactics.append(DetectedTactic(
                        tactic_type=tactic_type,
                        display_name=tactic_def.display_name,
                        severity=tactic_def.severity.value,
                        confidence=0.92,
                        evidence=evidence_span,
                        language=lang,
                        is_irreversible=tactic_def.is_irreversible,
                        weight=tactic_def.base_weight
                    ))
                    break  # Found best match for this tactic type

        return TacticExtractionResult(
            tactics=detected_tactics,
            is_suppressed_as_benign=False,
            benign_reason=None,
            detected_language=lang,
            is_code_switch=is_code_switch,
            raw_text=trimmed
        )

    def analyze_utterance(
        self,
        transcript: str,
        session_id: str = "default_session"
    ) -> Dict[str, Any]:
        """Analyze a transcript utterance within its session conversation state."""
        state = self.get_or_create_state(session_id)
        tactic_result = self.extract_tactics(transcript)
        return state.add_turn_analysis(transcript, tactic_result)

    def evaluate_turn(
        self,
        utterance: str,
        session_id: str = "default_session",
        detected_language: Optional[str] = None
    ) -> ConversationRiskState:
        """Evaluate a turn and return the updated ConversationRiskState."""
        state = self.get_or_create_state(session_id)
        tactic_result = self.extract_tactics(utterance)
        if detected_language and detected_language != "auto":
            tactic_result.detected_language = detected_language
        state.add_turn_analysis(utterance, tactic_result)
        return state


# Global singleton instance
multilingual_risk_engine = MultilingualTacticEngine()

