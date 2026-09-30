"""
RakshaCall Scam Taxonomy & Threat Categories.
Defines the complete canonical taxonomy of social engineering tactics,
stage mappings, severity weights, and descriptive metadata.
Adheres strictly to Section 15 and Section 19 of the Real-Time Multilingual
Scam Intelligence specification.
"""

from typing import Dict, List, Set

# --- SECTION 15: COMPLETE SCAM TAXONOMY ---

TAXONOMY_CATEGORIES = {
    # 1. Authority & Impersonation
    "AUTHORITY_IMPERSONATION": "General claim of institutional authority or official standing",
    "POLICE_IMPERSONATION": "Claiming to be police officer, inspector, or law enforcement agency",
    "GOVERNMENT_IMPERSONATION": "Claiming to represent government bodies (CBI, ED, Customs, Telecom, TRAI, IT Dept)",
    "BANK_IMPERSONATION": "Claiming to represent a bank, credit union, or financial institution manager",
    "COURT_IMPERSONATION": "Claiming to represent Supreme Court, High Court, magistrates, or legal judiciary",
    "TELECOM_IMPERSONATION": "Claiming to represent telecom provider (TRAI, Airtel, Jio) threatening sim deactivation",

    # 2. Coercion, Threats & Digital Arrest
    "DIGITAL_ARREST": "Ordering victim to remain on camera or line under fabricated video/call warrant",
    "LEGAL_THREAT": "Threatening criminal charges, non-bailable warrants, or court action",
    "ARREST_THREAT": "Direct threat of immediate physical arrest or police raid",
    "ACCOUNT_BLOCK_THREAT": "Threatening to freeze, block, or confiscate bank account or Aadhaar card",

    # 3. Urgency & Psychological Manipulation
    "URGENCY": "Creating extreme artificial time pressure to eliminate rational thinking",
    "FEAR_INDUCTION": "Instilling terror through allegations of contraband, drugs, or money laundering",
    "INTIMIDATION": "Bullying, aggressive commands, or hostile legal browbeating",
    "EMOTIONAL_PRESSURE": "Exploiting guilt, family honor, or panic to compel submission",

    # 4. Isolation & Control
    "ISOLATION": "Instructing victim not to tell family, lawyers, or third parties",
    "SECRECY": "Demanding strict confidentiality under penalty of criminal obstruction",
    "CONTROL": "Directing physical actions (stay on line, do not disconnect, put on speaker)",
    "PERSISTENCE": "Relentless refusal to allow disconnection, repeated redialing or questioning",

    # 5. Credential & Authentication Extraction
    "OTP_REQUEST": "Asking for one-time password or verification code received via SMS/app",
    "PIN_REQUEST": "Asking for ATM PIN, UPI PIN, or security passcode",
    "PASSWORD_REQUEST": "Asking for online banking password or login credentials",
    "CVV_REQUEST": "Asking for card CVV, expiry date, or full card details",
    "CREDENTIAL_REQUEST": "General extraction of security credentials or authentication secrets",

    # 6. Financial & Payment Demands
    "PAYMENT_REQUEST": "Demanding monetary payment under any guise",
    "BANK_TRANSFER_REQUEST": "Instructing funds transfer to 'secure account', 'RBI vault', or third-party IBAN/UPI",
    "UPI_REQUEST": "Demanding immediate transfer via UPI ID or payment link",
    "CRYPTO_REQUEST": "Instructing purchase or transfer of cryptocurrency/Bitcoin",
    "GIFT_CARD_REQUEST": "Demanding payment via retail gift cards, vouchers, or coupons",

    # 7. Device & Remote Access
    "REMOTE_ACCESS": "Instructing victim to install or grant remote device control software",
    "SCREEN_SHARE": "Demanding screen sharing to monitor two-factor codes or banking apps",
    "APP_INSTALLATION": "Directing installation of unknown APKs or remote tools (AnyDesk, TeamViewer)",

    # 8. Identity & Document Harvesting
    "IDENTITY_REQUEST": "Harvesting personal identity information",
    "AADHAAR_REQUEST": "Demanding Aadhaar number, biometrics, or linked phone details",
    "PAN_REQUEST": "Demanding Permanent Account Number (PAN) or tax documents",
    "DOCUMENT_REQUEST": "Demanding copies of passport, property papers, or signed forms",

    # 9. Technical & Behavioral Vectors
    "SUSPICIOUS_LINK": "Sending unverified external URLs, phishing portals, or shortlinks",
    "SUSPICIOUS_QR": "Instructing user to scan incoming QR code (which debits money)",
    "BANKING_ACTION": "Coercing user to open banking app, go to ATM, or approve UPI mandate",

    # 10. Fallback Extensible Category
    "OTHER_SUSPICIOUS_SOCIAL_ENGINEERING": "Novel or uncatalogued manipulative psychological vectors"
}

# --- SECTION 19: THE 12 SCAM STAGES ---
STAGES_12 = [
    "NORMAL",
    "CONTACT",
    "TRUST_BUILDING",
    "AUTHORITY_CLAIM",
    "FEAR",
    "URGENCY",
    "ISOLATION",
    "INFORMATION_REQUEST",
    "PAYMENT_REQUEST",
    "REMOTE_ACCESS",
    "THREAT_ESCALATION",
    "CRITICAL_INTERVENTION"
]

STAGE_HIERARCHY: Dict[str, int] = {stage: idx for idx, stage in enumerate(STAGES_12)}

# Tactic to Stage mapping
TACTIC_TO_STAGE: Dict[str, str] = {
    "AUTHORITY_IMPERSONATION": "AUTHORITY_CLAIM",
    "POLICE_IMPERSONATION": "AUTHORITY_CLAIM",
    "GOVERNMENT_IMPERSONATION": "AUTHORITY_CLAIM",
    "BANK_IMPERSONATION": "AUTHORITY_CLAIM",
    "COURT_IMPERSONATION": "AUTHORITY_CLAIM",
    "TELECOM_IMPERSONATION": "AUTHORITY_CLAIM",
    "FEAR_INDUCTION": "FEAR",
    "ACCOUNT_BLOCK_THREAT": "FEAR",
    "INTIMIDATION": "FEAR",
    "URGENCY": "URGENCY",
    "EMOTIONAL_PRESSURE": "URGENCY",
    "ISOLATION": "ISOLATION",
    "SECRECY": "ISOLATION",
    "CONTROL": "ISOLATION",
    "PERSISTENCE": "ISOLATION",
    "IDENTITY_REQUEST": "INFORMATION_REQUEST",
    "AADHAAR_REQUEST": "INFORMATION_REQUEST",
    "PAN_REQUEST": "INFORMATION_REQUEST",
    "DOCUMENT_REQUEST": "INFORMATION_REQUEST",
    "REMOTE_ACCESS": "REMOTE_ACCESS",
    "SCREEN_SHARE": "REMOTE_ACCESS",
    "APP_INSTALLATION": "REMOTE_ACCESS",
    "LEGAL_THREAT": "THREAT_ESCALATION",
    "ARREST_THREAT": "THREAT_ESCALATION",
    "DIGITAL_ARREST": "THREAT_ESCALATION",
    "PAYMENT_REQUEST": "PAYMENT_REQUEST",
    "BANK_TRANSFER_REQUEST": "PAYMENT_REQUEST",
    "UPI_REQUEST": "PAYMENT_REQUEST",
    "CRYPTO_REQUEST": "PAYMENT_REQUEST",
    "GIFT_CARD_REQUEST": "PAYMENT_REQUEST",
    "BANKING_ACTION": "PAYMENT_REQUEST",
    "SUSPICIOUS_LINK": "PAYMENT_REQUEST",
    "SUSPICIOUS_QR": "PAYMENT_REQUEST",
    "OTP_REQUEST": "CRITICAL_INTERVENTION",
    "PIN_REQUEST": "CRITICAL_INTERVENTION",
    "PASSWORD_REQUEST": "CRITICAL_INTERVENTION",
    "CVV_REQUEST": "CRITICAL_INTERVENTION",
    "CREDENTIAL_REQUEST": "CRITICAL_INTERVENTION",
    "OTHER_SUSPICIOUS_SOCIAL_ENGINEERING": "INFORMATION_REQUEST"
}

# Severity tier definitions for Risk Scoring
CRITICAL_TACTICS: Set[str] = {
    "OTP_REQUEST", "PIN_REQUEST", "PASSWORD_REQUEST", "CVV_REQUEST",
    "CREDENTIAL_REQUEST", "DIGITAL_ARREST", "BANK_TRANSFER_REQUEST"
}

HIGH_TACTICS: Set[str] = {
    "PAYMENT_REQUEST", "UPI_REQUEST", "REMOTE_ACCESS", "SCREEN_SHARE",
    "APP_INSTALLATION", "ARREST_THREAT", "LEGAL_THREAT", "ISOLATION",
    "SECRECY", "SUSPICIOUS_QR"
}

MEDIUM_TACTICS: Set[str] = {
    "AUTHORITY_IMPERSONATION", "POLICE_IMPERSONATION", "GOVERNMENT_IMPERSONATION",
    "BANK_IMPERSONATION", "COURT_IMPERSONATION", "TELECOM_IMPERSONATION",
    "URGENCY", "FEAR_INDUCTION", "ACCOUNT_BLOCK_THREAT", "CONTROL",
    "BANKING_ACTION", "SUSPICIOUS_LINK"
}

LOW_TACTICS: Set[str] = {
    "IDENTITY_REQUEST", "AADHAAR_REQUEST", "PAN_REQUEST", "DOCUMENT_REQUEST",
    "INTIMIDATION", "EMOTIONAL_PRESSURE", "PERSISTENCE",
    "OTHER_SUSPICIOUS_SOCIAL_ENGINEERING"
}
