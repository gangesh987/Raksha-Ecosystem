"""
RakshaCall JEV (Joint Embedding / Intent Verifier) Provider Abstraction.
Encapsulates semantic intent understanding behind a strict provider boundary.

Architecture (Phase 1 Integration):
- Primary Semantic Intelligence: Real PyTorch Neural Classifier (RakshaCall Multilingual Semantic Model V2)
  Bidirectional GRU + 768-dim subword representation + dual classification heads.
- Deterministic Safety Floor: RuleBasedSafetyFloor
  Multi-word phrase matching and discriminative keyword guardrail for explicit irreversible threats.
- Fusion Layer: Explainable synthesis prioritizing protective negation, combining neural probabilities
  with safety floor floors, and exposing complete diagnostic telemetry.
"""

from __future__ import annotations
import math
import os
import re
import time
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Any, Tuple

from ..ml.scam_classifier import get_scam_classifier, ScamClassifier

# Mapping from canonical 9-tactic names to JEV short-key taxonomy
CANONICAL_TO_JEV = {
    "AUTHORITY_IMPERSONATION": "AUTHORITY",
    "CRIMINAL_ALLEGATION_FEAR": "FEAR",
    "URGENCY": "URGENCY",
    "ISOLATION": "ISOLATION",
    "PAYMENT_DEMAND": "PAYMENT",
    "CREDENTIAL_OTP_PRESSURE": "CREDENTIAL",
    "REMOTE_ACCESS_PRESSURE": "REMOTE_ACCESS",
    "SUSPICIOUS_LINK": "SUSPICIOUS_LINK",
    "SUSPICIOUS_LINKS": "SUSPICIOUS_LINK",
    "ESCALATION_COERCION": "ESCALATION"
}
JEV_TO_CANONICAL = {v: k for k, v in CANONICAL_TO_JEV.items()}


@dataclass
class JEVAnalysisResult:
    """Standardized output of the JEV Semantic Intent Engine."""
    primary_intent: str
    tactic_probabilities: Dict[str, float]  # Tactic ID -> Probability (0.0 to 1.0)
    confidence: float
    supporting_evidence: Dict[str, str]     # Tactic ID -> Evidence quote
    explanation: str
    is_irreversible_action: bool
    model_provider: str
    latency_ms: float = 0.0

    # Phase 8 telemetry extensions
    scam_probability: float = 0.0
    top_tactic: str = ""
    top_tactic_probability: float = 0.0
    semantic_model: Dict[str, Any] = field(default_factory=dict)
    rule_floor: Dict[str, Any] = field(default_factory=dict)


class JEVProvider(ABC):
    """Abstract interface for JEV Semantic Intelligence Engine."""

    @property
    @abstractmethod
    def provider_name(self) -> str:
        pass

    @abstractmethod
    def analyze_window(
        self,
        current_utterance: str,
        conversation_context: List[str],
        detected_language: str
    ) -> JEVAnalysisResult:
        """Analyze a multi-turn conversation window for coercive scam intents."""
        pass


class RuleBasedSafetyFloor:
    """
    Deterministic Safety Guardrail (Safety Floor).
    Provides rule-based regex patterns, discriminative term matching,
    and negation/protective filtering.
    Does NOT claim to be a neural embedding model.
    Acts as a guaranteed minimum detection floor for unambiguous critical scam phrases.
    """

    PROTECTIVE_PATTERNS = [
        r'(never share|do not share|don\'t share|never send|do not transfer|don\'t transfer|never transfer)',
        r'(eppovume sollatha|sollathinga|anuppathinga|panam kudukathinga|அனுப்பாதீர்கள்|கேட்க மாட்டார்கள்)',
        r'(kisi ko mat dena|mat batao|mat bhejo|kabhi mat do)',
        r'(is a scam|this is fraud|will never ask|மோசடி)',
        r'(sounds like a scam|like a scam|scam call|scam warning|scam aayirukku|fraud call|fraud hai)',
        r'(i am disconnecting|disconnecting this call|ithu fraud|polise-kitta complaint pannunga)',
        r'(there is no fee|no fee for this|free of cost|no charge)'
    ]

    INTENT_PROTOTYPES = {
        "AUTHORITY": [
            "cbi officer cyber crime branch supreme court high court mumbai police delhi police customs department enforcement directorate trai telecom authority rbi headquarters inspector commissioner",
            "mumbai police station-la irunthu pesuren cbi officer pesuren customs officer pesuren cyber crime cell",
            "mai cbi headquarters se bol raha hu police officer arrest warrant issue customs officer bol raha hu",
            "सीबीआई हेडक्वार्टर पुलिस ऑफिसर साइबर क्राइम सेल कस्टम्स अधिकारी",
            "மும்பை காவல் துறை சிபிஐ குற்றப்பிரிவு தலைமை அதிகாரி பேசுகிறேன்"
        ],
        "FEAR": [
            "illegal parcel seized narcotics drugs found money laundering terror funding arrest warrant non-bailable fir criminal case charges passport cancelled identity theft",
            "unga aadhaar card drugs case parcel maatirukku illegal transaction aayirukku non bailable warrant",
            "aapke naam par illegal drugs courier mila hai money laundering ka case darj hua hai giraftari warrant",
            "अरेस्ट वारंट गैरकानूनी पार्सल मनी लॉन्ड्रिंग केस दर्ज हुआ है जेल होगी",
            "போதைப்பொருள் பார்சல் பிடிபட்டுள்ளது பண மோசடி வழக்கு உங்கள் மீது பதிவு செய்யப்பட்டுள்ளது"
        ],
        "URGENCY": [
            "immediately right now within 10 minutes within 15 minutes instant transfer urgent matter do not delay no time to waste",
            "10 minutes-kulla panam anuppanum ippove pannanum delay pannathinga fast ah pannunga",
            "15 minute ke andar karwayi karni hogi turant kare der mat karo samay nahi hai",
            "பத்து நிமிடத்திற்குள் நடவடிக்கை எடுக்க வேண்டும் உடனே செய்யுங்கள் தாமதிக்காதீர்கள்"
        ],
        "ISOLATION": [
            "do not disconnect stay on the call keep camera on do not tell anyone private closed room digital custody digital arrest secret investigation confidential",
            "call disconnect pannathinga yarukkum solla koodathu room door lock pannunga camera on digital arrest",
            "phone cut mat karna kisi ko mat batana chupchap kamre ka darwaza band rakho camera chalu rakho",
            "அழைப்பை துண்டிக்காதீர்கள் யாரிடமும் பேசக்கூடாது தனி அறையில் கதவை மூடி இருக்கவும்"
        ],
        "PAYMENT": [
            "transfer money funds to clearance account escrow account rbi verification security deposit clearance fee penalty fee deposit immediately",
            "rbi verification account-ku panam anuppunga security deposit transfer pannunga refund aagidum clearance fee",
            "rbi clearance account me paise transfer karo security deposit jama karo verification fees",
            "ஆர்பிஐ சரிபார்ப்பு கணக்கிற்கு பணம் உடனே மாற்றவும் பாதுகாப்பு வைப்புத்தொகை செலுத்தவும்"
        ],
        "CREDENTIAL": [
            "give me otp share the otp enter upi pin card cvv banking password verification code read out 6 digit otp",
            "mobile-ku vantha otp sollunga upi pin enter pannunga netbanking password read out pannunga",
            "phone par aaya hua 6 digit otp bataiye pin enter kijiye netbanking password verify karo",
            "வந்த ஆறு இலக்க ஓடிபி எண்ணை சொல்லுங்கள் யுபிஐ பின் எண்ணை உள்ளிடவும்"
        ],
        "REMOTE_ACCESS": [
            "install download anydesk teamviewer quicksupport rustdesk screen share remote access tool grant permission",
            "anydesk app install pannunga quicksupport download panni screen share access kudunga code sollunga",
            "anydesk ya quicksupport app download karke screen share code bataiye remote access",
            "எனிகெஸ்க் அல்லது டீம்வியூவர் செயலியை பதிவிறக்கம் செய்து குறியீட்டை பகிரவும்"
        ],
        "SUSPICIOUS_LINK": [
            "click link download apk verify account fill this form open url short link police certificate",
            "intha link click panni apk download pannunga details enter pannunga verification link",
            "is link par click karke form bhariye apk download karo",
            "இந்த சரிபார்ப்பு இணைப்பை கிளிக் செய்து படிவத்தை நிரப்பவும்"
        ],
        "ESCALATION": [
            "sending police patrol to your house raid your home physical arrest seize property blacklist aadhaar jail term immediate action",
            "police patrol unga veetukku vanthu arrest pannuvom veedu raid pannuvom property seize",
            "police ki team aapke ghar dispatch ho chuki hai jail hogi ghar par raid padegi",
            "காவல்துறை படை உங்கள் வீட்டிற்கு அனுப்பப்படும் சொத்து பறிமுதல் செய்யப்படும் உடனடி கைது"
        ]
    }

    STOPWORDS = {
        "the", "a", "an", "is", "in", "it", "on", "at", "to", "for", "with", "from",
        "of", "and", "or", "by", "your", "my", "our", "you", "we", "they", "this",
        "that", "there", "here", "10", "15", "5", "be", "was", "been", "have", "has",
        "are", "do", "does", "did", "as", "if", "so", "up", "out", "me", "him", "her",
        "us", "them", "nga", "ah", "ku", "la", "se", "ka", "ki", "ke", "hai",
        "ho", "hu", "mai", "aap", "unga", "en", "da", "pa", "na", "irunthu", "pesurom"
    }

    DISCRIMINATIVE_TERMS = {
        "cbi", "otp", "pin", "cvv", "anydesk", "teamviewer", "quicksupport", "rustdesk",
        "apk", "narcotics", "mdma", "contraband", "warrant", "fir", "escrow", "custody",
        "போதைப்பொருள்", "ஓடிபி", "யுபிஐ", "எனிகெஸ்க்", "அரெஸ்ட்"
    }

    TACTIC_PHRASES = {
        "AUTHORITY": ["cbi officer", "cyber crime", "crime branch", "supreme court", "high court", "police station", "customs officer", "enforcement directorate", "telecom authority", "trai", "rbi headquarters", "சிபிஐ", "காவல் துறை", "सीबीआई हेडक्वार्टर", "पुलिस"],
        "FEAR": ["narcotics parcel", "illegal drugs", "drugs case", "money laundering", "arrest warrant", "non bailable", "fir registered", "criminal case", "போதைப்பொருள்", "பண மோசடி", "கைது வாரண்ட்", "अरेस्ट वारंट", "मनी लॉन्ड्रिंग", "गैरकानूनी पार्सल"],
        "URGENCY": ["within 10 minutes", "within 15 minutes", "right now", "immediately", "urgent matter", "instant transfer", "delay pannathinga", "உடனே", "தாமதிக்காதீர்கள்", "तुरंत"],
        "ISOLATION": ["do not disconnect", "stay on the call", "keep camera on", "do not tell anyone", "digital custody", "digital arrest", "closed room", "room door lock", "தனி அறையில்", "அழைப்பை துண்டிக்காதீர்கள்", "किसी को मत बताना", "दरवाजा बंद"],
        "PAYMENT": ["clearance account", "escrow account", "rbi verification", "security deposit", "transfer 50,000", "panam anuppunga", "transfer money", "clearance fee", "penalty fee", "பணம் உடனே மாற்றவும்", "क्लीयरेंस खाते", "पैसे जमा करें"],
        "CREDENTIAL": ["give me otp", "share the otp", "upi pin", "card cvv", "banking password", "6 digit otp", "verification code", "read out", "fast-ah sollunga", "fast ah sollunga", "sollunga verify", "ओटीपी", "ஓடிபி", "பின் எண்"],
        "REMOTE_ACCESS": ["anydesk", "teamviewer", "quicksupport", "rustdesk", "screen share", "remote access", "செயலியை பதிவிறக்கம்"],
        "SUSPICIOUS_LINK": ["click link", "download apk", "verification link", "open url", "short link", "இணைப்பை கிளிக்"],
        "ESCALATION": ["police patrol", "raid your", "physical arrest", "seize property", "blacklist aadhaar", "jail term", "veetukku vanthu", "வீட்டிற்கு அனுப்பப்படும்"]
    }

    def is_protective(self, text: str) -> bool:
        lower = text.lower().strip()
        return any(re.search(pat, lower) for pat in self.PROTECTIVE_PATTERNS)

    def evaluate_tactic_match(
        self,
        full_window: str,
        current_utterance: str,
        tactic: str
    ) -> Tuple[float, str]:
        prototypes = self.INTENT_PROTOTYPES.get(tactic, [])
        phrases = self.TACTIC_PHRASES.get(tactic, [])
        best_score = 0.0
        best_match = ""

        # 1. Multi-word phrase matching
        for phrase in phrases:
            pattern = r'(?i)\b' + re.escape(phrase) + r'\b'
            if re.search(pattern, current_utterance):
                return 0.92, phrase
            elif re.search(pattern, full_window):
                best_score = max(best_score, 0.75)
                best_match = phrase

        # 2. Token overlap
        proto_words = {
            w for p in prototypes for w in p.split()
            if w not in self.STOPWORDS and len(w) >= 3
        }

        def _match_token(token: str, p_word: str) -> bool:
            if token == p_word:
                return True
            if any('\u0B80' <= c <= '\u0BFF' or '\u0900' <= c <= '\u097F' for c in token):
                if len(p_word) >= 3 and token.startswith(p_word):
                    return True
            if len(token) > len(p_word) and token.startswith(p_word) and len(p_word) >= 4:
                suffix = token[len(p_word):]
                if suffix in ("s", "ing", "ed", "es", "la", "ku"):
                    return True
            return False

        import string
        translator = str.maketrans('', '', string.punctuation)
        clean_current = current_utterance.translate(translator)
        clean_window = full_window.translate(translator)

        current_tokens = [w for w in clean_current.split() if w not in self.STOPWORDS]
        current_hits = [w for w in current_tokens if any(_match_token(w, p) for p in proto_words)]

        window_tokens = [w for w in clean_window.split() if w not in self.STOPWORDS]
        window_hits = [w for w in window_tokens if any(_match_token(w, p) for p in proto_words)]

        has_discrim = any(w in self.DISCRIMINATIVE_TERMS for w in current_hits + window_hits)

        if has_discrim or len(current_hits) >= 2 or (len(current_hits) >= 1 and len(window_hits) >= 2):
            match_count = len(current_hits) * 1.5 + len(window_hits) * 0.5
            score = min(0.96, max(0.40, 0.35 + 0.15 * match_count))
            if score > best_score:
                best_score = score
                best_match = " ".join(current_hits[:3]) if current_hits else " ".join(window_hits[:3])

        return best_score, best_match


class LocalSemanticJEVProvider(JEVProvider):
    """
    Hybrid Production JEV Intent Engine.
    PRIMARY: Real PyTorch Neural Classifier (SemanticSubwordEncoder + Dual Heads).
    SAFETY FLOOR: RuleBasedSafetyFloor guardrail for guaranteed explicit pattern detection.
    """

    def __init__(self, classifier: Optional[ScamClassifier] = None):
        self.safety_floor = RuleBasedSafetyFloor()
        try:
            self.neural_classifier = classifier or get_scam_classifier()
        except Exception as e:
            # Fallback if torch / weights are unavailable
            self.neural_classifier = None

    @property
    def provider_name(self) -> str:
        version = self.neural_classifier.model_version if self.neural_classifier else "unavailable"
        return f"LocalSemanticJEVProvider (PyTorch Neural Engine v2: {version} + Rule Safety Floor)"

    def analyze_window(
        self,
        current_utterance: str,
        conversation_context: List[str],
        detected_language: str
    ) -> JEVAnalysisResult:
        t0 = time.perf_counter()
        full_text = " ".join(conversation_context + [current_utterance]).lower().strip()
        current_lower = current_utterance.lower().strip()

        # -------------------------------------------------------------
        # 1. Strict Negation / Protective Filtering
        # -------------------------------------------------------------
        if self.safety_floor.is_protective(current_lower):
            return JEVAnalysisResult(
                primary_intent="PROTECTIVE_ADVISORY",
                tactic_probabilities={k: 0.0 for k in self.safety_floor.INTENT_PROTOTYPES},
                confidence=0.96,
                supporting_evidence={"PROTECTIVE": current_utterance},
                explanation="Protective utterance advising against sharing sensitive information or money.",
                is_irreversible_action=False,
                model_provider=self.provider_name,
                latency_ms=round((time.perf_counter() - t0) * 1000.0, 2),
                scam_probability=0.0,
                top_tactic="PROTECTIVE_ADVISORY",
                top_tactic_probability=0.0,
                semantic_model={"scam_probability": 0.0, "status": "suppressed_by_protective_context"},
                rule_floor={"triggered": False, "reason": "protective_context_detected"}
            )

        # -------------------------------------------------------------
        # 2. Primary Neural Classification (PyTorch)
        # -------------------------------------------------------------
        neural_scam_prob = 0.0
        neural_tactic_probs: Dict[str, float] = {}
        neural_meta: Dict[str, Any] = {}

        if self.neural_classifier and self.neural_classifier.is_loaded:
            try:
                # Classify the current utterance combined with recent context
                pred_text = f"{conversation_context[-1]} {current_utterance}" if conversation_context else current_utterance
                neural_res = self.neural_classifier.predict(pred_text)
                neural_scam_prob = neural_res.get("scam_probability", 0.0)
                neural_tactic_probs = neural_res.get("tactic_probabilities", {})
                neural_meta = {
                    "model_version": neural_res.get("model_version"),
                    "scam_probability": neural_scam_prob,
                    "representation_dim": neural_res.get("representation_dim", 768)
                }
            except Exception as e:
                neural_meta = {"error": str(e)}

        # -------------------------------------------------------------
        # 3. Deterministic Safety Floor Evaluation
        # -------------------------------------------------------------
        floor_probs: Dict[str, float] = {}
        floor_evidence: Dict[str, str] = {}
        for tactic in self.safety_floor.INTENT_PROTOTYPES.keys():
            score, ev = self.safety_floor.evaluate_tactic_match(full_text, current_lower, tactic)
            if score > 0.0:
                floor_probs[tactic] = score
                floor_evidence[tactic] = ev

        # -------------------------------------------------------------
        # 4. Explainable Fusion Policy
        # -------------------------------------------------------------
        # For each tactic, the fused probability is the combination of
        # the neural model probability and the deterministic safety floor.
        # The safety floor guarantees that explicit keyword/regex hits
        # (e.g., OTP solicitation or payment account details) are never missed,
        # while the neural model detects subtle, paraphrased semantic coercions.
        final_tactic_probs: Dict[str, float] = {}
        final_evidence: Dict[str, str] = {}
        reasons: List[str] = []

        for tactic in self.safety_floor.INTENT_PROTOTYPES.keys():
            canonical_name = JEV_TO_CANONICAL.get(tactic, tactic)
            n_prob = neural_tactic_probs.get(canonical_name, 0.0)
            f_prob = floor_probs.get(tactic, 0.0)

            # Fusion rule:
            # - If safety floor triggered: fused = max(n_prob, f_prob)
            # - If neural model detected semantic signal without rule trigger: fused = n_prob
            if f_prob > 0.0:
                fused = round(max(n_prob, f_prob), 3)
                ev = floor_evidence.get(tactic, "Deterministic pattern match")
            else:
                fused = round(n_prob, 3)
                ev = f"Neural semantic pattern (p={n_prob:.3f})"

            if fused >= 0.30:
                final_tactic_probs[tactic] = fused
                final_evidence[tactic] = ev
                reasons.append(f"{tactic}: {ev} (p={fused})")

        # Overall scam probability: maximum of neural scam head and peak tactic probability
        peak_tactic_prob = max(final_tactic_probs.values()) if final_tactic_probs else 0.0
        fused_scam_prob = round(max(neural_scam_prob, peak_tactic_prob), 4)

        # Primary intent
        if final_tactic_probs:
            top_tactic = max(final_tactic_probs.keys(), key=lambda k: final_tactic_probs[k])
            top_prob = final_tactic_probs[top_tactic]
        else:
            top_tactic = "BENIGN_INQUIRY"
            top_prob = 0.85

        irreversible = any(
            t in final_tactic_probs and final_tactic_probs[t] >= 0.50
            for t in ("PAYMENT", "CREDENTIAL", "REMOTE_ACCESS")
        )

        explanation = "; ".join(reasons) if reasons else "No coercive scam tactics detected in conversation window."
        elapsed_ms = round((time.perf_counter() - t0) * 1000.0, 2)

        return JEVAnalysisResult(
            primary_intent=top_tactic,
            tactic_probabilities=final_tactic_probs,
            confidence=round(top_prob, 3),
            supporting_evidence=final_evidence,
            explanation=explanation,
            is_irreversible_action=irreversible,
            model_provider=self.provider_name,
            latency_ms=elapsed_ms,
            scam_probability=fused_scam_prob,
            top_tactic=top_tactic,
            top_tactic_probability=round(top_prob, 3),
            semantic_model=neural_meta,
            rule_floor={
                "triggered": len(floor_probs) > 0,
                "floor_matches": floor_probs,
                "floor_evidence": floor_evidence
            }
        )


class CloudLLMJEVProvider(JEVProvider):
    """
    Cloud LLM Provider boundary (Groq / Gemini).
    Executes when cloud API credentials are configured on the backend.
    """
    def __init__(self, backend_groq_func=None):
        self._groq_func = backend_groq_func
        self._local_fallback = LocalSemanticJEVProvider()

    @property
    def provider_name(self) -> str:
        return "CloudLLMJEVProvider (Groq LLaMA-3 / Gemini)"

    def analyze_window(
        self,
        current_utterance: str,
        conversation_context: List[str],
        detected_language: str
    ) -> JEVAnalysisResult:
        groq_key = os.getenv("GROQ_API_KEY")
        if not groq_key or not self._groq_func:
            return self._local_fallback.analyze_window(current_utterance, conversation_context, detected_language)

        try:
            full_text = " ".join(conversation_context + [current_utterance])
            res = self._groq_func(full_text)
            if not res or not isinstance(res, dict):
                return self._local_fallback.analyze_window(current_utterance, conversation_context, detected_language)

            tactics = res.get("tactics", [])
            probs = {t: float(res.get("risk_score", 0.8)) for t in tactics}
            return JEVAnalysisResult(
                primary_intent=tactics[0] if tactics else "BENIGN",
                tactic_probabilities=probs,
                confidence=float(res.get("confidence", 0.90)),
                supporting_evidence={t: current_utterance for t in tactics},
                explanation="; ".join(res.get("reasons", ["Cloud model inference"])),
                is_irreversible_action=bool(res.get("irreversible_action", False)),
                model_provider=self.provider_name,
                scam_probability=float(res.get("risk_score", 0.8)),
                top_tactic=tactics[0] if tactics else "BENIGN",
                top_tactic_probability=float(res.get("confidence", 0.90))
            )
        except Exception:
            return self._local_fallback.analyze_window(current_utterance, conversation_context, detected_language)


class JEVProviderFactory:
    """Factory creating configured JEV Semantic Provider."""
    @staticmethod
    def get_provider() -> JEVProvider:
        if os.getenv("GROQ_API_KEY"):
            try:
                from . import groq_provider
                return CloudLLMJEVProvider(backend_groq_func=groq_provider.analyze)
            except Exception:
                pass
        return LocalSemanticJEVProvider()
