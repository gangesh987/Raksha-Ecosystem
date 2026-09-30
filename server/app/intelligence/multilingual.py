"""
RakshaCall Multilingual & Code-Switching Intelligence Layer.
Provides:
1. Native script & Romanized language identification across 8 languages:
   English (en), Tamil (ta), Hindi (hi), Telugu (te), Kannada (kn),
   Malayalam (ml), Bengali (bn), Marathi (mr).
2. Intra-utterance Code-Switching detection (primary, secondary, segments).
3. Romanized phonology normalization for Indic vernaculars in Latin script.
4. Slang and informal honorific handling (neutralized from threat scoring).
5. ASR phonetic corruption resilience (e.g. "OTB", "oh tee pee", "audahaar").
"""

import re
from typing import Dict, List, Tuple, Any, Optional
from dataclasses import dataclass, field

# --- SCRIPT DETECTION RANGES (UNICODE) ---
SCRIPT_RANGES = {
    "ta": (0x0B80, 0x0BFF),      # Tamil
    "hi": (0x0900, 0x097F),      # Devanagari (Hindi / Marathi)
    "te": (0x0C00, 0x0C7F),      # Telugu
    "kn": (0x0C80, 0x0CFF),      # Kannada
    "ml": (0x0D00, 0x0D7F),      # Malayalam
    "bn": (0x0980, 0x09FF),      # Bengali
}

# --- ROMANIZED LEXICON SIGNATURES (LATIN SCRIPT) ---
ROMANIZED_MARKERS = {
    "ta-Latn": {
        "unga", "ungaloda", "solunga", "panam", "kudunga", "anupunga", "koodathu",
        "aagidum", "iruku", "illa", "machan", "thambi", "parunga", "kaasu", "romba",
        "mudiyadhu", "vaanga", "ponga", "pannunga", "seekiram", "seri", "pesathenga"
    },
    "hi-Latn": {
        "aapka", "aapki", "karo", "kijiye", "bhejo", "batao", "paise", "khatre",
        "giraftaar", "bhai", "yaar", "mat", "raho", "turant", "kuch", "nahin", "nahi",
        "hoga", "hona", "dijiye", "de", "le", "boliye", "bataiye", "sambhal", "karo"
    },
    "te-Latn": {
        "mee", "meeru", "cheyyandi", "pampandi", "dabbu", "katha", "aipothundi",
        "chesi", "cheppandi", "undi", "lekapothe", "chudandi", "ippude", "thondaraga",
        "kadhu", "avuthundi", "chesaru", "ivvandi"
    },
    "kn-Latn": {
        "nimma", "maadi", "kodi", "hana", "khate", "agutte", "heli", "thilisiri",
        "beda", "koodale", "illi", "ivagale", "beku", "madabedi", "agide"
    },
    "ml-Latn": {
        "ningalude", "cheyyoo", "kodukku", "panam", "aakum", "parayamo", "parayuka",
        "arikkum", "ippol", "vegam", "alla", "cheyyanam", "pettannu"
    },
    "bn-Latn": {
        "apnar", "korun", "dekhun", "taka", "hoye", "jabe", "bolun", "pathan",
        "hobe", "ekhoni", "taratari", "kono", "koren", "din"
    },
    "mr-Latn": {
        "tumcha", "tumchi", "kara", "pathva", "paise", "hoil", "sanga", "thamba",
        "kahi", "ata", "lavkar", "nako", "ahe", "nahi", "dya"
    }
}

# --- SLANG & INFORMAL HONORIFICS (NEUTRAL CONTEXTUAL TOKENS) ---
INFORMAL_HONORIFICS = {
    "bro", "sir", "madam", "boss", "anna", "akka", "bhai", "bhaiya", "yaar",
    "machan", "thambi", "chetta", "dada", "didi", "saar", "sahab", "ji",
    "dost", "guru", "thala", "macha", "mame"
}

# --- COMMON ASR PHONETIC CORRUPTIONS ---
ASR_CORRECTION_MAP = {
    r"\b(o\s*t\s*b|oh\s*tee\s*pee|o\s*t\s*p|o-t-p|o\.t\.p)\b": "otp",
    r"\b(one\s*time\s*pass\s*word|one\s*time\s*code)\b": "otp",
    r"\b(audahaar|adhaar|aadhar|adhar|adahar)\b": "aadhaar",
    r"\b(tem\s*viewer|team\s*viewr|teamvewer)\b": "teamviewer",
    r"\b(any\s*desk|anidesk|ani\s*desk)\b": "anydesk",
    r"\b(quick\s*support|quiksupport)\b": "quicksupport",
    r"\b(see\s*vee\s*vee|c\s*v\s*v|c-v-v|c\.v\.v)\b": "cvv",
    r"\b(you\s*pee\s*eye|u\s*p\s*i|u-p-i|u\.p\.i)\b": "upi",
    r"\b(p\s*i\s*n|p-i-n|p\.i\.n)\b": "pin",
    r"\b(acct|a/c)\b": "account",
    r"\b(msg|txt)\b": "message"
}


@dataclass
class LanguageSegment:
    text: str
    language: str
    confidence: float


@dataclass
class MultilingualAnalysis:
    original_text: str
    normalized_text: str
    primary_language: str
    secondary_languages: List[str]
    language_confidence: float
    is_code_switched: bool
    language_segments: List[Dict[str, Any]]
    slang_tokens: List[str]
    asr_corrections_applied: List[str]


class MultilingualPreprocessor:
    """
    Intelligent language identifier, code-switching analyzer,
    and phonetic normalizer for conversational speech.
    """

    def __init__(self):
        self._compiled_asr_patterns = [
            (re.compile(pattern, re.IGNORECASE), repl)
            for pattern, repl in ASR_CORRECTION_MAP.items()
        ]

    def normalize_asr_corruptions(self, text: str) -> Tuple[str, List[str]]:
        """Normalize common speech recognition acoustic errors."""
        normalized = text
        applied = []
        for pattern, repl in self._compiled_asr_patterns:
            if pattern.search(normalized):
                normalized = pattern.sub(repl, normalized)
                applied.append(repl)
        return normalized, applied

    def detect_script(self, text: str) -> Dict[str, int]:
        """Count characters in respective native Unicode scripts."""
        counts = {lang: 0 for lang in SCRIPT_RANGES}
        counts["latin"] = 0
        for char in text:
            code = ord(char)
            matched = False
            for lang, (start, end) in SCRIPT_RANGES.items():
                if start <= code <= end:
                    counts[lang] += 1
                    matched = True
                    break
            if not matched and (0x0041 <= code <= 0x005A or 0x0061 <= code <= 0x007A):
                counts["latin"] += 1
        return counts

    def analyze_romanized_vocabulary(self, words: List[str]) -> Dict[str, int]:
        """Matches lowercase words against romanized Indic lexicons."""
        scores = {lang: 0 for lang in ROMANIZED_MARKERS}
        for word in words:
            clean = re.sub(r"[^\w]", "", word.lower())
            if not clean:
                continue
            for lang, vocab in ROMANIZED_MARKERS.items():
                if clean in vocab:
                    scores[lang] += 1
        return scores

    def extract_slang_and_honorifics(self, words: List[str]) -> List[str]:
        """Extract conversational markers and honorifics."""
        found = []
        for word in words:
            clean = re.sub(r"[^\w]", "", word.lower())
            if clean in INFORMAL_HONORIFICS:
                found.append(clean)
        return found

    def process(self, text: str) -> MultilingualAnalysis:
        """
        Complete multilingual analysis pipeline:
        ASR correction -> Script ID -> Romanized ID -> Code-switch parsing.
        """
        if not text or not text.strip():
            return MultilingualAnalysis(
                original_text="",
                normalized_text="",
                primary_language="en",
                secondary_languages=[],
                language_confidence=0.5,
                is_code_switched=False,
                language_segments=[],
                slang_tokens=[],
                asr_corrections_applied=[]
            )

        # 1. ASR phonetic correction
        normalized, corrections = self.normalize_asr_corruptions(text)

        # 2. Tokenize words
        words = re.findall(r"[\w]+|[^\w\s]", normalized, re.UNICODE)
        word_tokens = [w for w in words if re.match(r"^\w+$", w)]

        # 3. Slang extraction
        slang_tokens = self.extract_slang_and_honorifics(word_tokens)

        # 4. Script distribution
        script_counts = self.detect_script(normalized)
        total_indic_chars = sum(v for k, v in script_counts.items() if k != "latin")
        latin_chars = script_counts["latin"]

        indic_counts = {k: v for k, v in script_counts.items() if k != "latin"}
        detected_native = max(indic_counts.items(), key=lambda x: x[1]) if indic_counts else ("en", 0)

        segments: List[LanguageSegment] = []
        primary_lang = "en"
        secondary_langs = []
        lang_confidence = 0.85
        is_code_switched = False

        if total_indic_chars > 3:
            # Native Indic script is present
            native_lang = detected_native[0]
            if native_lang == "hi":
                # Check Marathi specific words if Devanagari
                if any(w.lower() in {"aahe", "ahe", "nahi", "hoil", "karaycha"} for w in word_tokens):
                    native_lang = "mr"
            
            if latin_chars > 3:
                # Code-switched Native + English
                is_code_switched = True
                primary_lang = native_lang
                secondary_langs = ["en"]
                lang_confidence = 0.92
                segments = [
                    LanguageSegment(text=normalized, language=native_lang, confidence=0.92),
                    LanguageSegment(text=normalized, language="en", confidence=0.88)
                ]
            else:
                primary_lang = native_lang
                lang_confidence = 0.95
                segments = [LanguageSegment(text=normalized, language=native_lang, confidence=0.95)]
        else:
            # Primarily Latin characters — analyze Romanized vocabulary vs English
            romanized_scores = self.analyze_romanized_vocabulary(word_tokens)
            best_romanized = max(romanized_scores.items(), key=lambda x: x[1])

            if best_romanized[1] >= 1:
                # At least one clear Romanized Indic marker
                base_lang = best_romanized[0].split("-")[0]
                primary_lang = base_lang
                secondary_langs = ["en"]
                is_code_switched = True
                lang_confidence = min(0.70 + 0.10 * best_romanized[1], 0.96)
                segments = [
                    LanguageSegment(text=normalized, language=base_lang, confidence=lang_confidence),
                    LanguageSegment(text=normalized, language="en", confidence=0.85)
                ]
            else:
                # English default
                primary_lang = "en"
                lang_confidence = 0.90
                segments = [LanguageSegment(text=normalized, language="en", confidence=0.90)]

        segment_dicts = [{"language": s.language, "confidence": s.confidence} for s in segments]

        return MultilingualAnalysis(
            original_text=text,
            normalized_text=normalized,
            primary_language=primary_lang,
            secondary_languages=secondary_langs,
            language_confidence=round(lang_confidence, 2),
            is_code_switched=is_code_switched,
            language_segments=segment_dicts,
            slang_tokens=slang_tokens,
            asr_corrections_applied=corrections
        )


# Global preprocessor instance
multilingual_engine = MultilingualPreprocessor()
