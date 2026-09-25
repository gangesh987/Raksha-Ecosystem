"""
RakshaCall 9-Tactic Label Mapping Layer.
Maps heterogeneous source dataset labels to the unified 9-tactic ontology.
Logs mapping provenance, confidence, and justification.
"""

from typing import Dict, List, Optional, Tuple
from pydantic import BaseModel, Field

class MappingEntry(BaseModel):
    source_dataset: str
    source_label: str
    rakshacall_label: Optional[str]
    is_auxiliary: bool
    confidence: float
    reason: str

RAKSHACALL_TACTICS = [
    "AUTHORITY_IMPERSONATION",
    "CRIMINAL_ALLEGATION_FEAR",
    "URGENCY",
    "ISOLATION",
    "PAYMENT_DEMAND",
    "CREDENTIAL_OTP_PRESSURE",
    "REMOTE_ACCESS_PRESSURE",
    "SUSPICIOUS_LINKS",
    "ESCALATION_COERCION"
]

# Canonical mapping dictionary from diverse dataset nomenclatures
LABEL_ONTOLOGY_MAP: Dict[str, Dict[str, Tuple[Optional[str], float, str]]] = {
    # 1. scambench-training mappings
    "scambench": {
        "authority_impersonation": ("AUTHORITY_IMPERSONATION", 0.95, "Exact semantic match for institutional or police impersonation"),
        "impersonation": ("AUTHORITY_IMPERSONATION", 0.85, "General impersonation of banking or official figures"),
        "fear_intimidation": ("CRIMINAL_ALLEGATION_FEAR", 0.90, "Threat of arrest or legal penalty creating panic"),
        "arrest_threat": ("CRIMINAL_ALLEGATION_FEAR", 0.98, "Explicit criminal allegation and arrest coercion"),
        "urgency_pressure": ("URGENCY", 0.95, "Time-bounded ultimatum demanding immediate compliance"),
        "isolation": ("ISOLATION", 0.95, "Instructing victim not to tell family or disconnect call"),
        "financial_demand": ("PAYMENT_DEMAND", 0.92, "Direct demand for wire transfer, RTGS, or cryptocurrency"),
        "credential_theft": ("CREDENTIAL_OTP_PRESSURE", 0.95, "Soliciting OTP, password, netbanking PIN"),
        "malicious_software": ("REMOTE_ACCESS_PRESSURE", 0.90, "Instruction to install remote access tools like AnyDesk"),
        "phishing_link": ("SUSPICIOUS_LINKS", 0.95, "Directing victim to external spoofed portal or APK URL"),
        "coercion": ("ESCALATION_COERCION", 0.90, "Threatening escalating physical or financial harm"),
        "customer_service": (None, 0.0, "Legitimate customer service dialogue - benign / auxiliary"),
        "general_inquiry": (None, 0.0, "Benign conversation - auxiliary")
    },
    # 2. scam-dialogue mappings
    "scam_dialogue": {
        "tech_support": ("REMOTE_ACCESS_PRESSURE", 0.88, "Tech support scam traditionally coerces screen sharing/remote access"),
        "financial_fraud": ("PAYMENT_DEMAND", 0.85, "Financial extortion and fraudulent account transfer"),
        "tax_irs_scam": ("AUTHORITY_IMPERSONATION", 0.92, "Impersonating tax or revenue department"),
        "law_enforcement": ("CRIMINAL_ALLEGATION_FEAR", 0.95, "Impersonating police with criminal warrant"),
        "lottery_prize": ("PAYMENT_DEMAND", 0.80, "Demanding processing fee before lottery disbursement"),
        "phishing": ("SUSPICIOUS_LINKS", 0.85, "Phishing link or credential harvest"),
        "benign": (None, 0.0, "Non-scam baseline")
    },
    # 3. hinglish-scam-text-dataset mappings
    "hinglish_scam": {
        "aadhaar_fraud": ("CRIMINAL_ALLEGATION_FEAR", 0.85, "Aadhaar misuse allegation or SIM cancellation threat"),
        "electricity_bill": ("URGENCY", 0.88, "Immediate disconnection threat unless payment made"),
        "kyc_update": ("CREDENTIAL_OTP_PRESSURE", 0.90, "KYC expiry used to solicit OTP or bank credentials"),
        "part_time_job": ("PAYMENT_DEMAND", 0.80, "Prepaid task investment scam demanding deposits"),
        "lottery_loan": ("PAYMENT_DEMAND", 0.82, "Processing fee advance loan scam"),
        "bank_fraud": ("AUTHORITY_IMPERSONATION", 0.85, "Bank manager or RBI officer impersonation"),
        "normal": (None, 0.0, "Benign conversational text")
    },
    # 4. phishing-dataset mappings
    "phishing": {
        "phishing_url": ("SUSPICIOUS_LINKS", 0.95, "Malicious phishing domain or credential harvester link"),
        "smishing": ("SUSPICIOUS_LINKS", 0.90, "SMS containing coercive link or callback number"),
        "legitimate": (None, 0.0, "Benign URL / text")
    }
}

class LabelMapper:
    """Utility class to safely map source labels to RakshaCall ontology."""
    
    def __init__(self):
        self.mapping_audit_log: List[MappingEntry] = []

    def map_label(self, source_dataset: str, source_label: str) -> Tuple[Optional[str], bool, float, str]:
        """
        Maps a source label to a RakshaCall 9-tactic label.
        Returns: (rakshacall_label, is_auxiliary, confidence, reason)
        """
        norm_source = source_dataset.lower()
        norm_label = source_label.lower().strip()

        dataset_map = LABEL_ONTOLOGY_MAP.get(norm_source, {})
        if norm_label in dataset_map:
            rk_label, conf, reason = dataset_map[norm_label]
            is_aux = rk_label is None
            entry = MappingEntry(
                source_dataset=source_dataset,
                source_label=source_label,
                rakshacall_label=rk_label,
                is_auxiliary=is_aux,
                confidence=conf,
                reason=reason
            )
            self.mapping_audit_log.append(entry)
            return rk_label, is_aux, conf, reason

        # Fallback check across all datasets
        for ds, m in LABEL_ONTOLOGY_MAP.items():
            if norm_label in m:
                rk_label, conf, reason = m[norm_label]
                is_aux = rk_label is None
                entry = MappingEntry(
                    source_dataset=source_dataset,
                    source_label=source_label,
                    rakshacall_label=rk_label,
                    is_auxiliary=is_aux,
                    confidence=conf * 0.9,
                    reason=f"Cross-dataset match from {ds}: {reason}"
                )
                self.mapping_audit_log.append(entry)
                return rk_label, is_aux, conf * 0.9, entry.reason

        # Unmapped: mark as auxiliary rather than hallucinating a tactic
        entry = MappingEntry(
            source_dataset=source_dataset,
            source_label=source_label,
            rakshacall_label=None,
            is_auxiliary=True,
            confidence=0.0,
            reason="Unrecognized label mapped to auxiliary data to prevent label noise."
        )
        self.mapping_audit_log.append(entry)
        return None, True, 0.0, entry.reason
