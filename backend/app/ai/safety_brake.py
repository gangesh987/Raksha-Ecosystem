"""
RakshaCall Safety Brake & Low-Literacy Vernacular Intervention Engine.
Provides low-literacy Tamil and English voice alert prompts, 7-step Verification Coach guidance,
and honest trusted contact handoff states.
"""

from __future__ import annotations
from dataclasses import dataclass, field
from typing import List, Dict, Optional


@dataclass
class LocalizedAudioAlert:
    language_code: str
    audio_key: str
    vernacular_text: str
    phonetic_romanized: str
    english_translation: str
    haptic_pattern: str = "URGENT_DOUBLE_PULSE"


@dataclass
class VerificationCoachStep:
    step_number: int
    title_vernacular: str
    title_english: str
    instruction_vernacular: str
    instruction_english: str
    voice_prompt_key: str


class SafetyBrakeInterventionEngine:
    """
    Safety Brake & Village-Friendly Intervention Dispatcher.
    Designed for rapid cognition under panic and low-literacy vernacular comprehension.
    """

    # Vernacular Voice Alerts for Village & Semi-Urban Users
    AUDIO_ALERTS = {
        "ta_payment": LocalizedAudioAlert(
            language_code="ta",
            audio_key="ta_stop_payment",
            vernacular_text="நில்லுங்கள்! பணம் அனுப்பாதீர்கள். உங்கள் குடும்பத்தினரை அழையுங்கள்.",
            phonetic_romanized="STOP. PANAM ANUPPATHINGA. FAMILY-A CALL PANNUNGA.",
            english_translation="STOP! Do not transfer money. Call your family immediately."
        ),
        "ta_otp": LocalizedAudioAlert(
            language_code="ta",
            audio_key="ta_stop_otp",
            vernacular_text="நில்லுங்கள்! வந்த OTP எண்ணை சொல்லாதீர்கள். அழைப்பை துண்டியுங்கள்.",
            phonetic_romanized="STOP. OTP SOLLAATHINGA. CALL-A DISCONNECT PANNUNGA.",
            english_translation="STOP! Do not share your OTP. Disconnect the call."
        ),
        "ta_remote": LocalizedAudioAlert(
            language_code="ta",
            audio_key="ta_stop_remote",
            vernacular_text="நில்லுங்கள்! AnyDesk செயலியை பதிவிறக்காதீர்கள்.",
            phonetic_romanized="STOP. ANYDESK DOWNLOAD PANNATHINGA.",
            english_translation="STOP! Do not install AnyDesk or screen sharing apps."
        ),
        "en_payment": LocalizedAudioAlert(
            language_code="en",
            audio_key="en_stop_payment",
            vernacular_text="STOP! Do not transfer money under pressure. Verify independently.",
            phonetic_romanized="STOP. DO NOT TRANSFER MONEY. CALL YOUR FAMILY.",
            english_translation="STOP! Do not transfer money under pressure. Verify independently."
        ),
        "en_otp": LocalizedAudioAlert(
            language_code="en",
            audio_key="en_stop_otp",
            vernacular_text="STOP! Never disclose your OTP or UPI PIN to any caller.",
            phonetic_romanized="STOP. NEVER DISCLOSE YOUR OTP OR UPI PIN.",
            english_translation="STOP! Never disclose your OTP or UPI PIN to any caller."
        )
    }

    # 7-Step Verification Coach Checklist
    VERIFICATION_STEPS: List[VerificationCoachStep] = [
        VerificationCoachStep(
            step_number=1,
            title_vernacular="நிறுத்துங்கள் (Pause)",
            title_english="Pause Immediately",
            instruction_vernacular="மூச்சை ஆழ்ந்து இழுங்கள். அவசரத்தில் முடிவெடுக்காதீர்கள்.",
            instruction_english="Take a deep breath. Refuse to take irreversible financial action under pressure.",
            voice_prompt_key="coach_step_1"
        ),
        VerificationCoachStep(
            step_number=2,
            title_vernacular="அழைப்பை துண்டியுங்கள் (Disconnect)",
            title_english="Disconnect the Call",
            instruction_vernacular="அழைப்பை உடனே துண்டியுங்கள். உண்மையான அதிகாரிகள் பேசவிடாமல் தடுக்க மாட்டார்கள்.",
            instruction_english="Hang up immediately. Legitimate law enforcement never forbids ending a call.",
            voice_prompt_key="coach_step_2"
        ),
        VerificationCoachStep(
            step_number=3,
            title_vernacular="எண்ணை நிராகரியுங்கள் (Reject Caller Number)",
            title_english="Do Not Use Caller-Provided Number",
            instruction_vernacular="அழைப்பாளர் தந்த தொலைபேசி எண்ணை மீண்டும் அழைக்காதீர்கள்.",
            instruction_english="Never dial numbers sent via SMS, WhatsApp, or given by the suspicious caller.",
            voice_prompt_key="coach_step_3"
        ),
        VerificationCoachStep(
            step_number=4,
            title_vernacular="சுயசரிபார்ப்பு (Independent Lookup)",
            title_english="Independently Locate Official Contact",
            instruction_vernacular="அரசு வலைத்தளம் (sancharsaathi.gov.in) அல்லது வங்கியின் அசல் எண்ணை தேடுங்கள்.",
            instruction_english="Search official government portals or your bank card for verified support numbers.",
            voice_prompt_key="coach_step_4"
        ),
        VerificationCoachStep(
            step_number=5,
            title_vernacular="நேரடி சரிபார்ப்பு (Direct Verification)",
            title_english="Verify the Allegation Directly",
            instruction_vernacular="அருகிலுள்ள காவல் நிலையம் அல்லது வங்கி கிளைக்கு நேரில் சென்று விசாரியுங்கள்.",
            instruction_english="Visit the local police station or branch directly to verify any alleged warrant.",
            voice_prompt_key="coach_step_5"
        ),
        VerificationCoachStep(
            step_number=6,
            title_vernacular="குடும்பத்தினர் உதவி (Contact Trusted Person)",
            title_english="Alert a Trusted Family Member",
            instruction_vernacular="பணம் அனுப்பும் முன் உங்கள் பெற்றோர், துணைவர் அல்லது உறவினருக்கு தெரிவியுங்கள்.",
            instruction_english="Inform your spouse, parent, sibling, or caregiver before moving any money.",
            voice_prompt_key="coach_step_6"
        ),
        VerificationCoachStep(
            step_number=7,
            title_vernacular="பாதுகாப்பான தொடர்ச்சி (Resume Safely)",
            title_english="Resume Action Only After Confirmation",
            instruction_vernacular="முழுமையான உறுதிப்படுத்தலுக்குப் பிறகே எந்த நடவடிக்கையும் எடுக்கவும்.",
            instruction_english="Only proceed after official independent confirmation clears all suspicions.",
            voice_prompt_key="coach_step_7"
        )
    ]

    def select_audio_alert(self, detected_tactics: Dict[str, float], language_code: str) -> LocalizedAudioAlert:
        """Select appropriate high-urgency voice alert matching tactic and language."""
        lang_prefix = "ta" if "ta" in language_code.lower() else "en"

        if "PAYMENT" in detected_tactics:
            return self.AUDIO_ALERTS.get(f"{lang_prefix}_payment", self.AUDIO_ALERTS["en_payment"])
        elif "CREDENTIAL" in detected_tactics:
            return self.AUDIO_ALERTS.get(f"{lang_prefix}_otp", self.AUDIO_ALERTS["en_otp"])
        elif "REMOTE_ACCESS" in detected_tactics and lang_prefix == "ta":
            return self.AUDIO_ALERTS["ta_remote"]
        
        # Default safety stop alert
        return self.AUDIO_ALERTS.get(f"{lang_prefix}_payment", self.AUDIO_ALERTS["en_payment"])


# Global singleton
safety_brake_engine = SafetyBrakeInterventionEngine()
