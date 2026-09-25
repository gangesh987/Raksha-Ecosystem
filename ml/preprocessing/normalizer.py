"""
RakshaCall Data Normalization & Cleaning Pipeline.
Normalizes multilingual conversational text (Tamil, Tanglish, Hindi, Hinglish, English),
preserves code-switching, standardizes whitespace, and extracts linguistic metadata.
"""

import re
import unicodedata
from typing import Dict, Any, Tuple

# Unicode code-block ranges for Indian scripts
TAMIL_RANGE = (0x0B80, 0x0BFF)
DEVANAGARI_RANGE = (0x0900, 0x097F)

class TextNormalizer:
    """Normalizes multilingual speech transcripts and raw conversational text."""

    @staticmethod
    def detect_script_and_language(text: str) -> str:
        """
        Detects language or code-mixed representation:
        - 'ta': Tamil in Tamil script
        - 'ta-Latn': Tanglish (Tamil romanized)
        - 'hi': Hindi in Devanagari script
        - 'hi-Latn': Hinglish (Hindi romanized)
        - 'en': English
        """
        if not text or not text.strip():
            return "en"

        tamil_chars = sum(1 for c in text if TAMIL_RANGE[0] <= ord(c) <= TAMIL_RANGE[1])
        deva_chars = sum(1 for c in text if DEVANAGARI_RANGE[0] <= ord(c) <= DEVANAGARI_RANGE[1])
        total_alpha = sum(1 for c in text if c.isalpha())

        if total_alpha == 0:
            return "en"

        tamil_ratio = tamil_chars / total_alpha
        deva_ratio = deva_chars / total_alpha

        if tamil_ratio > 0.25:
            return "ta"
        if deva_ratio > 0.25:
            return "hi"

        # If primarily Latin script, analyze vocabulary for Tanglish vs Hinglish vs English
        lower = text.lower()
        tanglish_markers = [
            "ungal", "panam", "anuppunga", "pesuren", "irukku", "pannunga", "aagum",
            "koodaadhu", "sollaadheenga", "sollaathinga", "vanakkam", "kavanam", "kaasu"
        ]
        hinglish_markers = [
            "aapka", "paise", "bhejo", "bol", "raha", "hun", "kariye", "mat",
            "karein", "turant", "paisa", "khata", "namaskar", "kripya", "nahi"
        ]

        tanglish_count = sum(1 for m in tanglish_markers if re.search(rf"\b{m}\b", lower))
        hinglish_count = sum(1 for m in hinglish_markers if re.search(rf"\b{m}\b", lower))

        if tanglish_count >= 1 and tanglish_count >= hinglish_count:
            return "ta-Latn"
        if hinglish_count >= 1:
            return "hi-Latn"

        return "en"

    @classmethod
    def normalize_text(cls, text: str) -> str:
        """
        Standardizes Unicode, cleans non-printable characters, normalizes whitespace.
        Preserves original script terms and code-mixed Latin transliterations.
        """
        if not text:
            return ""

        # Normalize Unicode to NFC
        text = unicodedata.normalize("NFC", text)
        
        # Replace non-standard whitespace, tabs, and newlines
        text = re.sub(r"[\r\n\t]+", " ", text)
        
        # Remove zero-width spaces, joiners, non-printable characters
        text = re.sub(r"[\u200B-\u200D\uFEFF]", "", text)
        
        # Strip excessive repeated punctuation (e.g. "!!!!!" -> "!")
        text = re.sub(r"([!?.,])\1+", r"\1", text)
        
        # Collapse multiple spaces
        text = re.sub(r"\s+", " ", text).strip()
        
        return text

    @classmethod
    def clean_turn(cls, raw_turn: Dict[str, Any]) -> Dict[str, Any]:
        """Cleans and annotates a raw turn record."""
        cleaned = dict(raw_turn)
        raw_text = raw_turn.get("text", "")
        norm_text = cls.normalize_text(raw_text)
        cleaned["text"] = norm_text
        
        # Infer language if missing or unspecified
        if not cleaned.get("language") or cleaned["language"] == "unknown":
            cleaned["language"] = cls.detect_script_and_language(norm_text)
            
        return cleaned
