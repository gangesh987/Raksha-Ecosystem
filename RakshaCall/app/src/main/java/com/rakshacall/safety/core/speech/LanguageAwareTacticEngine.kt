package com.rakshacall.safety.core.speech

import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.TranscriptEvent
import java.util.UUID

/**
 * Multilingual scam tactic engine recognizing Indian language coercion patterns
 * (Hindi / Hinglish / Regional variants) mapped deterministically into the 9 ScamTactics.
 */
class LanguageAwareTacticEngine {

    private val hindiPatterns = mapOf(
        ScamTactic.AUTHORITY_IMPERSONATION to listOf(
            Regex("""(?i)(पुलिस|सीबीआई|साइबर क्राइम|सुप्रीम कोर्ट|कस्टम्स|नारकोटिक्स|ट्राई|इन्स्पेक्टर|थाना|मुंबई पुलिस|मुम्बई पुलिस|दिल्ली पुलिस|अधिकारी)"""),
            Regex("""(?i)\b(police|cbi|cyber cell|customs|court|rbi|trai)\b""")
        ),
        ScamTactic.CRIMINAL_ALLEGATION to listOf(
            Regex("""(?i)(आधार.*(दुरुपयोग|गैरकानूनी|केस)|पार्सल में ड्रग्स|मनी लॉन्ड्रिंग|वारंट जारी|गिरफ्तारी वारंट|अरेस्ट वारंट|गैर-जमानती|एफआईआर दर्ज|अपराध|जब्त)"""),
            Regex("""(?i)\b(fir darj|arrest warrant|narcotics parcel|illegal money)\b""")
        ),
        ScamTactic.URGENCY to listOf(
            Regex("""(?i)(तुरंत|अभी के अभी|फौरन|दस मिनट में|पंद्रह मिनट में|बिना किसी देरी|जल्दी करो|आखिरी चेतावनी)"""),
            Regex("""(?i)\b(turant|abhi ke abhi|fauran|jaldi karo|urgent)\b""")
        ),
        ScamTactic.ISOLATION to listOf(
            Regex("""(?i)(कॉल मत काटना|फोन चालू रखो|कमरे का दरवाजा बंद|अकेले रहो|किसी को मत बताना|डिजिटल अरेस्ट|गोपनीय जांच|कैमरा ऑन रखो)"""),
            Regex("""(?i)\b(phone mat kaato|kamre me akele|kisi ko mat batana|digital arrest)\b""")
        ),
        ScamTactic.PAYMENT_DEMAND to listOf(
            Regex("""(?i)(पैसे ट्रांसफर|रुपये भेजो|सिक्योरिटी डिपॉजिट|आरबीआई अकाउंट|जुर्माना भरो|जमानत राशि|पचास हजार|एक लाख|खाते में डालो|ट्रांसफर करो)"""),
            Regex("""(?i)\b(paise transfer|rupaye bhejo|fine bharo|security deposit)\b""")
        ),
        ScamTactic.CREDENTIAL_PRESSURE to listOf(
            Regex("""(?i)(ओटीपी बताओ|ओटीपी दो|यूपीआई पिन डालो|पासवर्ड बताओ|कार्ड का सीवीवी|वेरिफिकेशन कोड)"""),
            Regex("""(?i)\b(otp batao|upi pin|password do|cvv number)\b""")
        ),
        ScamTactic.REMOTE_ACCESS to listOf(
            Regex("""(?i)(एनीडेस्क डाउनलोड करो|टीमव्यूअर इंस्टॉल करो|स्क्रीन शेयर करो|क्विकसपोर्ट)"""),
            Regex("""(?i)\b(anydesk|teamviewer|screen share|quicksupport)\b""")
        ),
        ScamTactic.SUSPICIOUS_LINK to listOf(
            Regex("""(?i)(लिंक पर क्लिक करो|एपीके फाइल|फॉर्म भरो|वेरिफिकेशन लिंक)"""),
            Regex("""(?i)\b(link par click|apk download)\b""")
        ),
        ScamTactic.ESCALATION to listOf(
            Regex("""(?i)(घर पर पुलिस भेज रहे हैं|रेड पड़ेगी|संपत्ति कुर्क|खाता सीज|जेल होगी|लोकेशन ट्रेस)"""),
            Regex("""(?i)\b(ghar par police|raid hogi|khata freeze|jail bhejenge)\b""")
        )
    )

    fun analyze(event: TranscriptEvent, languageCode: String = "hi-IN"): List<RiskSignal> {
        val detected = mutableListOf<RiskSignal>()
        val text = event.text.trim()
        if (text.isEmpty()) return emptyList()

        for ((tactic, patterns) in hindiPatterns) {
            for (pattern in patterns) {
                val match = pattern.find(text)
                if (match != null) {
                    val signal = RiskSignal(
                        id = UUID.randomUUID().toString(),
                        timestamp = event.timestamp,
                        tactic = tactic,
                        confidence = 0.90f,
                        riskContribution = tactic.defaultWeight,
                        evidenceText = match.value,
                        source = "MULTILINGUAL_NLP ($languageCode)",
                        sessionId = event.sessionId
                    )
                    detected.add(signal)
                    break
                }
            }
        }
        return detected
    }
}
