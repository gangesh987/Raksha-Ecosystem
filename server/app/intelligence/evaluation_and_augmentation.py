"""
RakshaCall Dataset Schema, Augmentation Pipeline & Evaluation Framework.
Implements:
- Section 30: Training/Evaluation Dataset Schemas (conforming to legal & privacy bounds)
- Section 31: Automated Data Augmentation (ASR corruption, Romanization, Code-switching, Slang injection, Politeness variation)
"""

import random
from typing import Dict, List, Any, Optional
from dataclasses import dataclass, asdict


@dataclass
class DatasetRecord:
    """Standardized evaluation and benchmark record (Section 30)."""
    id: str
    language: str
    script: str               # "native", "latin", "mixed"
    romanized: bool
    text: str
    normalized_text: str
    tactic: str
    stage: str
    risk: str                 # "SAFE", "LOW", "MEDIUM", "HIGH", "CRITICAL"
    code_switch: bool
    speaker: str              # "caller", "victim", "agent"
    context: List[str]
    source: str               # "synthetic", "public_fir", "curated_eval"
    confidence: float

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


class DatasetAugmentor:
    """
    Automated data augmentation pipeline for multilingual conversational safety (Section 31).
    """

    SLANG_MARKERS = ["bro", "sir", "madam", "boss", "anna", "bhai", "yaar", "machan"]
    POLITENESS_PREFIXES = [
        "Please cooperate with us and ",
        "Sir, I completely understand your concern. Please ",
        "Kindly do not worry, just ",
        "For your own safety, please "
    ]
    ASR_CORRUPTIONS = {
        "otp": ["otb", "oh tee pee", "o t p"],
        "aadhaar": ["audahaar", "adhaar", "aadhar"],
        "anydesk": ["any desk", "anidesk"],
        "teamviewer": ["tem viewer", "team viewr"]
    }

    def inject_slang(self, text: str) -> str:
        """Inject benign colloquial honorifics without altering semantic intent."""
        slang = random.choice(self.SLANG_MARKERS)
        return f"{slang.title()}, {text}" if random.random() > 0.5 else f"{text}, {slang}."

    def inject_politeness(self, text: str) -> str:
        """Wrap coercive requests in polite customer service language."""
        prefix = random.choice(self.POLITENESS_PREFIXES)
        clean = text[0].lower() + text[1:] if len(text) > 1 else text
        return f"{prefix}{clean}"

    def inject_asr_noise(self, text: str) -> str:
        """Simulate acoustic and speech recognition noise."""
        noisy = text
        for token, variations in self.ASR_CORRUPTIONS.items():
            if token in noisy.lower():
                repl = random.choice(variations)
                noisy = re.sub(rf"\b{token}\b", repl, noisy, flags=re.IGNORECASE)
        return noisy

    def augment(self, base_text: str, tactic: str, stage: str, language: str) -> List[DatasetRecord]:
        """Generate diverse augmented variations from a seed sample."""
        variants = [
            base_text,
            self.inject_slang(base_text),
            self.inject_politeness(base_text),
            self.inject_asr_noise(base_text)
        ]

        records = []
        for idx, var in enumerate(variants):
            records.append(DatasetRecord(
                id=f"aug_{tactic}_{language}_{idx}",
                language=language,
                script="latin" if language.endswith("-Latn") or language == "en" else "native",
                romanized=language.endswith("-Latn"),
                text=var,
                normalized_text=var.lower(),
                tactic=tactic,
                stage=stage,
                risk="CRITICAL" if tactic in {"OTP_REQUEST", "DIGITAL_ARREST"} else "HIGH",
                code_switch=False,
                speaker="caller",
                context=[],
                source="synthetic_augmented",
                confidence=0.95
            ))
        return records


import re
dataset_augmentor = DatasetAugmentor()
