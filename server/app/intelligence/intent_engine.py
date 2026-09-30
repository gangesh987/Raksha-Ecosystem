"""
RakshaCall Semantic Intent & Social Engineering Intelligence Engine.
Implements deep semantic intent classification across 8 languages,
code-switching, romanized dialects, and indirect phrasing.
NOT keyword-dependent — infers communicative meaning, role assignment,
and behavioral manipulation patterns.
"""

import re
from typing import Dict, List, Tuple, Any, Optional, Set
from dataclasses import dataclass, field

from .taxonomy import (
    TAXONOMY_CATEGORIES, CRITICAL_TACTICS, HIGH_TACTICS,
    MEDIUM_TACTICS, LOW_TACTICS
)
from .multilingual import MultilingualAnalysis, multilingual_engine


@dataclass
class DetectedTactic:
    type: str
    confidence: float
    evidence: str
    is_indirect: bool = False
    language: str = "en"


@dataclass
class IntentAnalysisResult:
    tactics: List[DetectedTactic]
    scam_probability: float
    top_tactic: str
    is_hard_negative: bool
    negative_reason: Optional[str]
    has_irreversible_action: bool
    summary: str


# --- HARD NEGATIVES / PROTECTIVE NEGATION RULES ---
# Explicit warnings and defensive awareness expressions must never trigger false alarms.
HARD_NEGATIVE_PATTERNS = [
    r"\b(never share|do not share|don't share|never send|do not transfer|don't transfer|never give|do not give|don't give)\b",
    r"\b(never install|do not install|don't install|never download|do not download|never use)\b",
    r"\b(never ask|will never ask|do not ask|don't ask|does not ask|never requests?|will not ask)\b",
    r"\b(bank employees? never|police will never|cbi will never|rbi never)\b",
    r"\b(example of (a )?.*?scam|demonstration of|awareness (session|seminar|campaign)|learn how to identify)\b",
    r"\b(bank refunded (my|the) money|money was refunded|transaction verified safely|verify a transaction at the branch|bank asked me to verify)\b",
    r"\b(beware of|stay alert|caution against|protect yourself from)\b",
    # Vernacular protective negations
    r"(otp.*(kudukkadheenga|sollaadheenga|kuduka koodathu|share pannaadheenga))",  # Tamil
    r"(otp.*(mat dena|kisi ko mat batao|share mat kijiye|nahi mangta))",             # Hindi
    r"(otp.*(ivvodhu|cheppavaddhu|share cheyyakandi))",                             # Telugu
    r"(otp.*(kodabedi|helabedi|share madabedi))",                                   # Kannada
    r"(otp.*(kodukkaruthu|parayaruthu|share cheyyaruthu))",                         # Malayalam
    r"(otp.*(deben na|bolben na|share korben na))",                                 # Bengali
    r"(otp.*(deu naka|sangu naka|share karu naka))",                                # Marathi
]

# --- ORDINARY NON-SCAM AGGRESSIVE UTTERANCES ---
BENIGN_AGGRESSION_PATTERNS = [
    r"\bwhy are you shouting( at me)?\b",
    r"\bstop yelling\b",
    r"\bdon't talk to me like that\b",
    r"\bcalm down\b",
    r"\bi am not angry\b",
    r"\bstop complaining\b",
    r"\bwhat is your problem\b",
]

# --- SEMANTIC INTENT PATTERNS (MULTILINGUAL & INDIRECT) ---
# Each tactic is detected via semantic frames: Action + Object + Contextual Direction

INTENT_SEMANTIC_FRAMES = {
    # 1. OTP / CREDENTIAL EXTRACTION (Direct & Indirect)
    "OTP_REQUEST": [
        # Direct & Indirect English
        r"\b(read|tell|send|give|share|provide|check|confirm|repeat)\b.*?\b(six|6|four|4)\s*(numbers?|digits?|code)\b",
        r"\b(check|read|tell|send)\b.*?\b(message|sms|notification)\b.*?\b(received|just came|on your phone)\b",
        r"\b(read|tell|give)\b.*?\b(those|the)\b.*?\b(numbers?|digits?|code)\b",
        r"\bcode that came to your phone\b",
        r"\bverification (code|number|digits?)\b",
        r"\b(read|tell|give|share|enter)\b.*?\b(otp|one[- ]time password)\b",
        r"\b(what is|what's) the (code|number|otp)\b",
        # Tamil (Native & Romanized)
        r"(otp|code|number|digit).*(solunga|kudunga|anupunga|parunga|read pannunga)",
        r"(vandha|vandhirukka).*(message|code|number).*(solunga|kudunga)",
        r"(அந்த|அந்த ஆறு|ஆறு).*(நம்பர்|எண்|கோட்).*(சொல்லுங்க|படிங்க)",
        # Hindi (Native & Romanized)
        r"(otp|code|number|digit).*(batao|bataiye|dijiye|bhejiye|padh ke sunao)",
        r"(message|sms).*(aaya hai|mila hai).*(code|number|batao|padho)",
        r"(bas|sirf).*(code|number).*(bata dijiye|batao)",
        r"(आया हुआ|मैसेज का).*(कोड|नंबर|ओटीपी).*(बताइए|दीजिए)",
        # Telugu (Native & Romanized)
        r"(otp|code|number).*(cheppandi|ivvandi|pampandi|chusi cheppandi)",
        r"(vachina|message).*(code|number).*(cheppandi)",
        # Kannada (Native & Romanized)
        r"(otp|code|number).*(heli|kodi|thilisiri|nodi heli)",
        r"(bandha|message).*(code|number).*(heli)",
        # Malayalam (Native & Romanized)
        r"(otp|code|number).*(parayamo|parayuka|kodukku|noki parayu)",
        # Bengali (Native & Romanized)
        r"(otp|code|number).*(bolun|din|pathan|dekhe bolun)",
        # Marathi (Native & Romanized)
        r"(otp|code|number).*(sanga|dya|pathva|vachun sanga)"
    ],

    "PIN_REQUEST": [
        r"\b(atm|upi|card|security)\s*pin\b",
        r"\b(enter|share|tell|give)\b.*?\b(pin number|secret pin)\b",
        r"(pin|upi pin).*(solunga|bataiye|cheppandi|heli|parayamo|bolun|sanga)"
    ],

    "PASSWORD_REQUEST": [
        r"\b(net\s*banking|login|account|portal)\s*password\b",
        r"\b(enter|share|tell)\b.*?\bpassword\b",
        r"(password).*(solunga|bataiye|cheppandi|heli|bolun|sanga)"
    ],

    "CVV_REQUEST": [
        r"\b(three|3)\s*digits?\s*(on the back|behind the card)\b",
        r"\b(cvv|cvc|security code on card)\b",
        r"\bcard\s*(expiry|validity)\b"
    ],

    "CREDENTIAL_REQUEST": [
        r"\b(verify|authenticate|confirm)\b.*?\b(credentials?|security details?|secret code)\b",
        r"\bjust need to verify something\b.*?\b(code|number|identity)\b"
    ],

    # 2. FINANCIAL & PAYMENT COERCION (Direct & Indirect)
    "BANK_TRANSFER_REQUEST": [
        r"\b(move|transfer|send|deposit|wire)\b.*?\b(money|balance|funds?|amount|savings?)\b.*?\b(temporary|secure|safe|verification|rbi|reserve|court)\b",
        r"\b(move the money|transfer the funds|move the balance)\b",
        r"\btemporary (protection|verification|security|reserve) (account|vault)\b",
        r"\bclear your name\b.*?\b(transfer\w*|pay\w*|deposit\w*)\b",
        # Vernacular
        r"(panam|kaasu).*(anupunga|transfer pannunga|safety account|potudunga)",
        r"(paise|balance).*(transfer kijiye|bhejiye|surakshit account|dal dijiye)",
        r"(dabbu|funds).*(pampandi|transfer cheyyandi|secure account)",
        r"(hana).*(kodi|transfer maadi|secure account)",
        r"(taka).*(pathan|transfer korun)",
        r"(paise).*(pathva|transfer kara)"
    ],

    "PAYMENT_REQUEST": [
        r"\b(pay|transfer|deposit|send)\b.*?\b(fee|fine|penalty|charges?|money|rupees?|lakhs?|thousands?|₹|\$)\b",
        r"\b(immediate|urgent)\s*(payment|clearance|settlement)\b",
        r"\brefundable (deposit|guarantee|bond)\b"
    ],

    "UPI_REQUEST": [
        r"\b(pay|send|transfer)\b.*?\b(upi|gpay|phonepe|paytm)\b",
        r"\b(approve|accept)\b.*?\b(collect request|upi mandate|request)\b",
        r"\bscan.*?(qr|code)\b.*?\b(receive|get|refund)\b"
    ],

    "BANKING_ACTION": [
        r"\b(open|login to|check)\b.*?\b(banking app|bank account|net banking|yono|imobile)\b",
        r"\bgo to the (nearest )?atm\b",
        r"\bopen your (phonepe|gpay|paytm|bank application)\b"
    ],

    # 3. AUTHORITY & DIGITAL ARREST
    "DIGITAL_ARREST": [
        r"\b(digital|video|virtual|online)\s*arrest\b",
        r"\byou are under (digital|video|camera|online) arrest\b",
        r"\bstay on (video|camera|call)\b.*?\b(warrant|police|cbi|investigation)\b",
        r"\bdo not disconnect\b.*?\b(under arrest|crime branch|custody)\b"
    ],

    "POLICE_IMPERSONATION": [
        r"\b(calling from|this is|i am)\b.*?\b(police|cyber crime|crime branch|cbi|cid|dcp|inspector|commissioner|sho)\b",
        r"\b(delhi|mumbai|chennai|bangalore|kolkata|hyderabad)\s*cyber\s*crime\b",
        r"(cyber crime|police station).*(la irundhu pesren|se bol raha hoon|nundi matladuthunnam|inda matanadutha idhini)"
    ],

    "GOVERNMENT_IMPERSONATION": [
        r"\b(calling from|this is|i am)\b.*?\b(trai|customs|narcotics|ncb|enforcement directorate|ed|income tax|telecom regulatory)\b",
        r"\bairport (customs|seizure|parcel)\b",
        r"\billegal (package|parcel|shipment|consignment)\b"
    ],

    "BANK_IMPERSONATION": [
        r"\b(calling from|this is)\b.*?\b(your bank|rbi|reserve bank|head office|card division|sbi|hdfc|icici)\b",
        r"\b(bank manager|verification officer|fraud department)\b"
    ],

    "COURT_IMPERSONATION": [
        r"\b(supreme court|high court|magistrate|chief justice|court order|non[- ]bailable warrant)\b",
        r"\blegal summons\b.*?\b(issued|pending)\b"
    ],

    "TELECOM_IMPERSONATION": [
        r"\b(sim|mobile number|telecom)\b.*?\b(blocked|deactivated|suspended|disconnected)\b.*?\b(within|hours?|trai)\b",
        r"\b(trai|telecom authority)\b.*?\b(sim block|illegal calls?)\b"
    ],

    "AUTHORITY_IMPERSONATION": [
        r"\bi('ll| will) connect you to our (senior|higher) officer\b",
        r"\bofficial verification department\b",
        r"\bauthorized government portal\b"
    ],

    # 4. FEAR & LEGAL THREATS
    "LEGAL_THREAT": [
        r"\b(criminal|money laundering|narcotics|mdma|drugs?|terrorist|illegal)\s*(case|activity|transaction|investigation)\b",
        r"\b(aadhaar|pan|account|number)\b.*?\b(linked to|involved in|used for)\b.*?\b(illegal|criminal|crime|fraud|drugs?)\b",
        r"\bnon[- ]bailable warrant\b",
        r"\blegal consequences\b"
    ],

    "ARREST_THREAT": [
        r"\b(arrest warrant|police will arrive|sent to jail|put behind bars|custody)\b",
        r"\bwill be arrested\b.*?\b(today|within|immediately)\b",
        r"(giraftaar|jail|arrest).*(kar lenge|hoge|aagidum|aipotharu)"
    ],

    "ACCOUNT_BLOCK_THREAT": [
        r"\b(account|cards?|assets?|property)\b.*?\b(block|freeze|seize|confiscate|suspend)\b",
        r"\b(freeze your account|block your bank|stop all transactions)\b",
        r"(account).*(block aagidum|band ho jayega|block aipothundi|block agutte|block aakum|block hoye jabe|block hoil)"
    ],

    "FEAR_INDUCTION": [
        r"\byou are in (serious|grave) trouble\b",
        r"\bnational security (matter|issue|threat)\b",
        r"\bconsequences will be very severe\b"
    ],

    # 5. URGENCY & PRESSURE
    "URGENCY": [
        r"\b(immediately|right now|urgent|hurry|without delay|at once|fast)\b",
        r"\b(within|in next)\s*(a\s+)?(few|\d+)\s*(minutes|seconds|hours)\b",
        r"\byou (have only|only have)\s*(a\s+)?(few|\d+)\s*minutes\b",
        r"\b(only|just)\s*(a\s+)?(few|\d+)\s*minutes\b",
        r"\bcooperate immediately\b",
        r"\bact right now\b",
        # Vernacular
        r"(turant|jaldi|abhi ke abhi|ekdum)",
        r"(seekiram|udane|ippove)",
        r"(ippude|thondaraga|ventane)",
        r"(ivagale|koodale|begane)",
        r"(ippol|pettannu|vegam)",
        r"(ekhoni|taratari)",
        r"(ata|lavkar)"
    ],

    "EMOTIONAL_PRESSURE": [
        r"\bif you care about your (family|reputation|future)\b",
        r"\bcooperate with us and no one needs to know\b",
        r"\bplease cooperate with us\b"
    ],

    # 6. ISOLATION, SECRECY & CONTROL
    "ISOLATION": [
        r"\b(don't|do not|never)\b.*?\b(tell|inform|speak to|discuss with)\b.*?\b(family|anyone|wife|husband|parents?|children|lawyer|friends?)\b",
        r"\bdo not involve any(body|one) else\b",
        r"\bkeep this strictly to yourself\b",
        r"\bdo not discuss this with anybody\b",
        # Vernacular
        r"(veetla|family|kitta).*(sollaadheenga|pesaadheenga)",
        r"(ghar|family|kisi ko).*(mat batana|mat bolo|kisi se baat mat karo)",
        r"(intlo|evariki).*(cheppakandi|cheppodhu)",
        r"(maneyalli|yarigoo).*(helabedi)",
        r"(veettil|aarenkilum).*(parayaruthu)",
        r"(barite|kauke).*(bolben na)",
        r"(gharat|konala).*(sangu naka)"
    ],

    "SECRECY": [
        r"\b(strictly confidential|official secret|secrecy act|do not disclose)\b",
        r"\bmaintain total secrecy\b"
    ],

    "CONTROL": [
        r"\b(stay on the line|don't disconnect|do not hang up|keep the call on)\b",
        r"\bput the phone on speaker\b",
        r"\bdo not put the call on hold\b",
        # Vernacular
        r"(call|line).*(disconnect pannadheenga|cut pannadheenga)",
        r"(phone|call).*(katna mat|disconnect mat karo|rakhna mat)",
        r"(call).*(cut cheyyakandi|disconnect cheyyodhu)",
        r"(call).*(cut madabedi)"
    ],

    "PERSISTENCE": [
        r"\byou must answer all questions\b",
        r"\bwhy did you try to disconnect\b",
        r"\bi will keep calling you until you cooperate\b"
    ],

    # 7. REMOTE ACCESS & SCREEN SHARE
    "REMOTE_ACCESS": [
        r"\b(install|download)\b.*?\b(application|app|software)\b.*?\b(help|assist|verify|resolve)\b",
        r"\b(anydesk|teamviewer|quicksupport|rustdesk|zoho assist)\b",
        r"\binstall this application so i can help you\b"
    ],

    "SCREEN_SHARE": [
        r"\b(share|broadcast)\b.*?\byour screen\b",
        r"\bturn on screen sharing\b",
        r"\bshow me your phone screen\b"
    ],

    "APP_INSTALLATION": [
        r"\b(install|download|sideload)\b.*?\b(apk|file|security app|support tool)\b"
    ],

    # 8. IDENTITY & DOCUMENT REQUEST
    "AADHAAR_REQUEST": [
        r"\b(tell|give|share|verify|send)\b.*?\baadhaar\b",
        r"\baadhaar (card|number|digits?)\b"
    ],

    "PAN_REQUEST": [
        r"\b(tell|give|share|verify)\b.*?\bpan (card|number)\b"
    ],

    "DOCUMENT_REQUEST": [
        r"\b(send|upload|share)\b.*?\b(bank statement|salary slip|id proof|passport copy)\b"
    ],

    "IDENTITY_REQUEST": [
        r"\b(confirm|tell me)\b.*?\byour (date of birth|mother's name|full address)\b"
    ],

    "SUSPICIOUS_LINK": [
        r"\b(click|open|tap)\b.*?\b(link|url|website|portal)\b",
        r"\bhttps?://[^\s]+\b"
    ],

    "SUSPICIOUS_QR": [
        r"\b(scan|open)\b.*?\b(qr code|barcode)\b.*?\b(receive|get|claim)\b"
    ]
}


class SemanticIntentEngine:
    """
    Evaluates multi-turn conversation text for underlying psychological
    coercion and social engineering intent across 8 languages.
    """

    def __init__(self):
        # Compile all semantic intent patterns
        self.compiled_intents: Dict[str, List[re.Pattern]] = {}
        for tactic, patterns in INTENT_SEMANTIC_FRAMES.items():
            self.compiled_intents[tactic] = [
                re.compile(p, re.IGNORECASE | re.UNICODE) for p in patterns
            ]

        self.compiled_negatives = [
            re.compile(p, re.IGNORECASE | re.UNICODE) for p in HARD_NEGATIVE_PATTERNS
        ]

        self.compiled_benign_aggression = [
            re.compile(p, re.IGNORECASE | re.UNICODE) for p in BENIGN_AGGRESSION_PATTERNS
        ]

    def check_hard_negatives(self, text: str) -> Tuple[bool, Optional[str]]:
        """
        Check if the utterance is an explicit educational, protective,
        or banking awareness advisory that should never trigger an alert.
        """
        text_lower = text.lower()
        for pat in self.compiled_negatives:
            if pat.search(text_lower):
                return True, "Protective negation or cyber-awareness statement detected"
        return False, None

    def check_benign_aggression(self, text: str) -> bool:
        """Filter out ordinary domestic arguments that are not scam threats."""
        text_lower = text.lower()
        return any(pat.search(text_lower) for pat in self.compiled_benign_aggression)

    def analyze_utterance(
        self,
        multilingual_info: MultilingualAnalysis,
        conversation_context: List[str] = None
    ) -> IntentAnalysisResult:
        """
        Evaluate normalized transcript against semantic frames with context.
        """
        norm_text = multilingual_info.normalized_text.strip()
        orig_text = multilingual_info.original_text.strip()

        if not norm_text:
            return IntentAnalysisResult(
                tactics=[],
                scam_probability=0.0,
                top_tactic="NONE",
                is_hard_negative=False,
                negative_reason=None,
                has_irreversible_action=False,
                summary="Empty utterance"
            )

        # 1. Hard-negative check
        is_neg, neg_reason = self.check_hard_negatives(norm_text)
        if is_neg:
            return IntentAnalysisResult(
                tactics=[],
                scam_probability=0.0,
                top_tactic="NONE",
                is_hard_negative=True,
                negative_reason=neg_reason,
                has_irreversible_action=False,
                summary=f"Safe: {neg_reason}"
            )

        # 2. Benign argument check
        if self.check_benign_aggression(norm_text):
            return IntentAnalysisResult(
                tactics=[],
                scam_probability=0.05,
                top_tactic="NONE",
                is_hard_negative=False,
                negative_reason=None,
                has_irreversible_action=False,
                summary="Benign conversational frustration (not a scam threat)"
            )

        # 3. Multi-label semantic intent scanning
        detected: List[DetectedTactic] = []
        has_irreversible = False

        for tactic_name, compiled_pats in self.compiled_intents.items():
            for pat in compiled_pats:
                match = pat.search(norm_text)
                if match:
                    evidence_str = match.group(0)
                    # Assign base confidence
                    if tactic_name in CRITICAL_TACTICS:
                        base_conf = 0.95
                        has_irreversible = True
                    elif tactic_name in HIGH_TACTICS:
                        base_conf = 0.88
                    elif tactic_name in MEDIUM_TACTICS:
                        base_conf = 0.78
                    else:
                        base_conf = 0.65

                    # Check indirectness
                    is_indirect = not any(kw in norm_text.lower() for kw in ["otp", "pin", "cvv", "arrest", "scam"])
                    
                    detected.append(DetectedTactic(
                        type=tactic_name,
                        confidence=base_conf,
                        evidence=evidence_str,
                        is_indirect=is_indirect,
                        language=multilingual_info.primary_language
                    ))
                    break  # Found best match for this tactic

        # 4. Contextual pronoun & indirect resolution from history
        if conversation_context and len(conversation_context) > 0:
            recent_context_text = " ".join(conversation_context[-3:]).lower()
            # If recent context had verification/bank/code mention and current turn says "tell me", "read it", "send it"
            generic_action_match = re.search(r"\b(tell me|read it|send it|give it|show me|forward it)\b", norm_text.lower())
            if generic_action_match and not any(d.type in {"OTP_REQUEST", "CREDENTIAL_REQUEST"} for d in detected):
                if any(w in recent_context_text for w in ["bank", "message", "verification", "code", "sms", "number"]):
                    detected.append(DetectedTactic(
                        type="OTP_REQUEST",
                        confidence=0.85,
                        evidence=f"Indirect reference '{generic_action_match.group(0)}' resolved via prior context",
                        is_indirect=True,
                        language=multilingual_info.primary_language
                    ))
                    has_irreversible = True

        # Deduplicate tactics by highest confidence
        unique_tactics: Dict[str, DetectedTactic] = {}
        for d in detected:
            if d.type not in unique_tactics or d.confidence > unique_tactics[d.type].confidence:
                unique_tactics[d.type] = d
        final_tactics = list(unique_tactics.values())

        # Calculate base scam probability
        if not final_tactics:
            scam_prob = 0.05
            top_tactic = "NONE"
            summary = "Neutral speech"
        else:
            # Aggregate severity
            crit_count = sum(1 for t in final_tactics if t.type in CRITICAL_TACTICS)
            high_count = sum(1 for t in final_tactics if t.type in HIGH_TACTICS)
            med_count = sum(1 for t in final_tactics if t.type in MEDIUM_TACTICS)

            if crit_count > 0:
                scam_prob = min(0.85 + 0.05 * crit_count + 0.03 * high_count, 0.99)
            elif high_count > 0:
                scam_prob = min(0.65 + 0.06 * high_count + 0.03 * med_count, 0.88)
            elif med_count > 0:
                scam_prob = min(0.40 + 0.08 * med_count, 0.65)
            else:
                scam_prob = 0.25

            # Identify top tactic by confidence & severity
            top_tactic = max(final_tactics, key=lambda t: (
                3 if t.type in CRITICAL_TACTICS else 2 if t.type in HIGH_TACTICS else 1,
                t.confidence
            )).type

            tactic_names = [t.type for t in final_tactics]
            summary = f"Detected: {', '.join(tactic_names)}"

        return IntentAnalysisResult(
            tactics=final_tactics,
            scam_probability=round(scam_prob, 2),
            top_tactic=top_tactic,
            is_hard_negative=False,
            negative_reason=None,
            has_irreversible_action=has_irreversible,
            summary=summary
        )


# Global intent engine instance
intent_engine = SemanticIntentEngine()
