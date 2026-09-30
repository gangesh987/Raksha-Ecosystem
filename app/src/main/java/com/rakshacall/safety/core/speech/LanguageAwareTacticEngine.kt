package com.rakshacall.safety.core.speech

import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.TranscriptEvent
import java.util.UUID

/**
 * Multilingual scam tactic engine recognizing Indian language coercion patterns
 * (English, Tamil, Tanglish, Hindi, Hinglish, Telugu, Kannada, Malayalam, Bengali, etc.)
 * mapped deterministically into canonical ScamTactics with strict benign suppression.
 */
class LanguageAwareTacticEngine {

    val benignPatterns = listOf(
        Regex("""(?i)\b(near (the|my|our) (police station|bank|branch)|next to (the|my) (police station|bank))\b"""),
        Regex("""(?i)\b(going to (the|my) (police station|bank|branch)|visited (the|my) (bank|police station))\b"""),
        Regex("""(?i)\b(police station is (near|far|closed)|bank is (near|closed|open))\b"""),
        Regex("""(?i)\b(my bank asked me to visit the branch|visit the branch (tomorrow|today))\b"""),
        Regex("""(?i)\b(police officer helped me|police helped (us|me|him|them))\b"""),
        Regex("""(?i)\b(send money to (the|a) (grocery shop|store|market|vegetable vendor))\b"""),
        Regex("""(?i)\b((transfer|send) money to (my|our) (mother|father|mom|dad|sister|brother|friend))\b"""),
        Regex("""(?i)\b((this|my) (assignment|homework|project) is urgent)\b"""),
        Regex("""(?i)\b(urgently need to submit (my|the) (assignment|homework|project))\b"""),
        Regex("""(?i)\b(i received an otp from my bank|got an otp for my (order|delivery))\b"""),
        Regex("""(?i)(போலீஸ் ஸ்டேஷன் பக்கத்துல இருக்கு|என் அம்மாவுக்கு பணம் அனுப்பணும்|அசைன்மென்ட் அவசரம்)"""),
        Regex("""(?i)(police station paas mein hai|mummy ko paise bhejne hai|college assignment urgent hai)"""),
        Regex("""(?i)(police station daggara undi|amma ki dabbu pampali|assignment urgent)""")
    )

    private val multilingualPatterns = mapOf(
        ScamTactic.AUTHORITY_IMPERSONATION to listOf(
            Regex("""(?i)\b(i am|i'm|this is|calling from)\s+(a\s+|the\s+)?(police|cbi|ed|customs|cyber crime|rbi|trai|supreme court|court|bank manager)\b"""),
            Regex("""(?i)\b(police officer|cbi officer|customs officer|cyber crime officer|rbi officer)\b"""),
            Regex("""(?i)(போலீஸ்|காவல்துறை|சிபிஐ|கஸ்டம்ஸ்|சைபர் கிரைம்|நீதிமன்றம்|அதிகாரி)"""),
            Regex("""(?i)\b(naan|naanga)\s*(police|cbi|customs|cyber crime)\s*(la|lendhu|officer)\s*(irundhu)?\s*(pesuren)?\b"""),
            Regex("""(?i)\bpolice\s+(department|station)?\s*(la)?\s*irundhu\s+pesuren\b"""),
            Regex("""(?i)(पुलिस|सीबीआई|साइबर क्राइम|सुप्रीम कोर्ट|कस्टम्स|नारकोटिक्स|आरबीआई|अधिकारी)"""),
            Regex("""(?i)\b(main|hum)\s*(police|cbi|customs|cyber crime|rbi)\s*se\s*(bol raha|baat kar raha)\b"""),
            Regex("""(?i)(నేను|మేము)\s*(పోలీస్|సీబీఐ|కస్టమ్స్|సైబర్ క్రైమ్|బ్యాంక్|ఆర్బీఐ)\s*(నుండి|అధికారి)"""),
            Regex("""(?i)\b(nenu|memu)\s*(police|cbi|customs|bank|rbi)\s*(nundi|officer)\s*(matladutunnanu)?\b"""),
            Regex("""(?i)(ನಾನು|ನಾವು)\s*(ಪೊಲೀಸ್|ಸಿಬಿಐ|ಕಸ್ಟಮ್ಸ್)\s*(ಇಂದ|ಅಧಿಕಾರಿ)"""),
            Regex("""(?i)(ഞാൻ)\s*(പോലീസ്|സിബിഐ|കസ്റ്റംസ്)\s*(ഓഫീസർ|ആണ് സംസാരിക്കുന്നത്)"""),
            Regex("""(?i)(আমি|আমরা)\s*(পুলিশ|সিবিআই|কাস্টমস)\s*(অফিসার|থেকে বলছি)"""),
            Regex("""(?i)(मी|आम्ही)\s*(पोलीस|सीबीआय|कस्टम्स)\s*(मधून बोलतोय|अधिकारी)"""),
            Regex("""(?i)(હું|અમે)\s*(પોલીસ|સીબીઆઈ|કસ્ટમ્સ)\s*(માંથી બોલું છું|અધિકારી)"""),
            Regex("""(?i)(ਮੈਂ|ਅਸੀਂ)\s*(ਪੁਲਿਸ|ਸੀਬੀਆਈ)\s*(ਤੋਂ ਬੋਲ ਰਿਹਾ|ਅਫ਼ਸਰ)"""),
            Regex("""(?i)(ମୁଁ|ଆମେ)\s*(ପୋଲିସ|ସିବିଆଇ)\s*(ଅଫିସର|ତରଫରୁ କହୁଛି)""")
        ),
        ScamTactic.CRIMINAL_ALLEGATION to listOf(
            Regex("""(?i)\b(arrest warrant|non-bailable|fir registered|criminal case|money laundering|narcotics parcel|seized|illegal activity|suspicious activity)\b"""),
            Regex("""(?i)(வழக்கு பதிவு|கைது வாரண்ட்|சட்டவிரோத|குற்றப்பிரிவு|போதைப்பொருள் பார்சல்)"""),
            Regex("""(?i)\b(case register|arrest warrant|illegal parcel|drugs parcel|jail la poduvom)\b"""),
            Regex("""(?i)(गिरफ्तारी वारंट|गैर-जमानती|मनी लॉन्ड्रिंग|ड्रग्स पार्सल|एफआईआर दर्ज|अपराध|खाता ब्लॉक)"""),
            Regex("""(?i)\b(arrest warrant|account block ho jayega|jail bhejenge|fir darj)\b"""),
            Regex("""(?i)(అరెస్ట్ వారెంట్|నాన్-బెయిలబుల్|మనీ లాండరింగ్|డ్రగ్స్ పార్సెల్|కేసు నమోదైంది)""")
        ),
        ScamTactic.URGENCY to listOf(
            Regex("""(?i)\b(urgent|urgently|immediately|right now|within 10 minutes|without delay|hurry up|final warning)\b"""),
            Regex("""(?i)(அவசரம்|உடனே|இப்பவே|தாமதிக்காமல்|கடைசி எச்சரிக்கை)"""),
            Regex("""(?i)\b(urgent|romba urgent|udane|ippave|ippove)\b"""),
            Regex("""(?i)(तुरंत|अभी के अभी|फौरन|दस मिनट में|जल्दी करो|आखिरी चेतावनी)"""),
            Regex("""(?i)\b(urgent|abhi|turant|jaldi karo|abhi ke abhi)\b"""),
            Regex("""(?i)(వెంటనే|ఇప్పుడే|ఆలస్యం చేయవద్దు|చివరి హెచ్చరిక)"""),
            Regex("""(?i)\b(ventane|ippude|urgent)\b"""),
            Regex("""(?i)(ತಕ್ಷಣ|ತುರ್ತು|உடனே|ഇത് അടിയന്തിരമാണ്|অবিলম্বে|तातडीचे|તરત જ|ਤੁਰੰਤ|ତୁରନ୍ତ)""")
        ),
        ScamTactic.ISOLATION to listOf(
            Regex("""(?i)\b(do not tell anyone|confidential investigation|keep this confidential|lock your room|do not inform family|stay alone)\b"""),
            Regex("""(?i)(யார்கிட்டேயும் சொல்லாதீங்க|ரகசிய விசாரணை|ரூம் கதவை சாத்துங்க|தனியா இருங்க)"""),
            Regex("""(?i)\b(kitta solladheenga|confidential|room door close pannunga|yaarkittayum solla koodadhu)\b"""),
            Regex("""(?i)(किसी को मत बताना|गोपनीय जांच|अकेले रहो|फोन मत काटना|कमरे का दरवाजा बंद रखो)"""),
            Regex("""(?i)\b(kisi ko mat batana|confidential investigation|kamre me akele raho)\b"""),
            Regex("""(?i)(ఎవరికీ చెప్పవద్దు|రహస్య విచారణ|కాల్ కట్ చేయవద్దు|ఒంటరిగా ఉండండి)""")
        ),
        ScamTactic.PAYMENT_DEMAND to listOf(
            Regex("""(?i)\b(send\s+(me\s+|the\s+)?money|transfer\s+(the\s+)?(money|amount|funds)|make\s+(the\s+)?payment|pay\s+(the\s+)?(fine|penalty|fee|bail)|deposit|upi\s+it\s+now|send\s+₹?\d+|transfer\s+₹?\d+)\b"""),
            Regex("""(?i)\b(pay immediately|settle the amount|send the funds|please complete the transaction)\b"""),
            Regex("""(?i)(பணம் அனுப்பு|பணத்தை மாற்று|உடனே பணம் அனுப்புங்க|செலுத்த வேண்டும்)"""),
            Regex("""(?i)\b(money send|amount transfer|panam anuppunga|transfer pannunga|udane money send pannunga)\b"""),
            Regex("""(?i)(पैसे भेजो|पैसे ट्रांसफर करो|रुपये जमा करो|जुर्माना भरो|अभी पैसे भेजो)"""),
            Regex("""(?i)\b(paise bhejo|money send karo|amount transfer karo|abhi paise bhejo|abhi amount transfer karo)\b"""),
            Regex("""(?i)(డబ్బు పంపండి|డబ్బును బదిలీ చేయండి|రుసుము చెల్లించండి|ఫైన్ కట్టండి|డబ్బు పంపించండి)"""),
            Regex("""(?i)\b(dabbu pampinchandi|amount transfer|amount ippude pampinchandi)\b"""),
            Regex("""(?i)(ಹಣವನ್ನು ವರ್ಗಾಯಿಸಿ|ಹಣ ಕಳುಹಿಸಿ|പണം അയക്കുക|টাকা পাঠান|पैसे पाठवा|પૈસા ટ્રાન્સફર કરો|પૈસા મોકલો|ਪੈਸੇ ਭੇਜੋ|ਟଙ୍କା ପଠାନ୍ତୁ)""")
        ),
        ScamTactic.CREDENTIAL_PRESSURE to listOf(
            Regex("""(?i)\b(tell\s+me\s+(the\s+|your\s+)?otp|share\s+(the\s+|your\s+)?otp|give\s+me\s+(the\s+|your\s+)?otp|verification\s+code|one[- ]time\s+password|upi pin|atm pin|password|cvv)\b"""),
            Regex("""(?i)(ஓடிபி சொல்லு|ஓடிபி எண்|பின் எண்|கடவுச்சொல்)"""),
            Regex("""(?i)\b(otp sollunga|otp sollu|upi pin sollunga|cvv sollunga)\b"""),
            Regex("""(?i)(ओटीपी बताओ|ओटीपी दो|यूपीआई पिन|पासवर्ड बताओ|सीवीवी)"""),
            Regex("""(?i)\b(otp batao|otp do|upi pin|password batao|cvv)\b"""),
            Regex("""(?i)(ఓటీపీ చెప్పండి|పిన్ నంబర్|పాస్వర్డ్)"""),
            Regex("""(?i)\b(otp cheppandi|upi pin|password)\b""")
        ),
        ScamTactic.REMOTE_ACCESS to listOf(
            Regex("""(?i)\b(install\s+(anydesk|teamviewer|quicksupport|rustdesk)|download\s+(anydesk|teamviewer|quicksupport)|screen share|remote access)\b"""),
            Regex("""(?i)(எனிகெஸ்க்|டீம்வியூவர்|ஸ்கிரீன் ஷேர்|பதிவிறக்கம் செய்)"""),
            Regex("""(?i)\b(anydesk install pannunga|teamviewer download pannu|screen share pannunga)\b"""),
            Regex("""(?i)(एनीडेस्क|टीमव्यूअर|स्क्रीन शेयर करो|रिमोट एक्सेस)"""),
            Regex("""(?i)\b(anydesk download karo|teamviewer install karo|screen share kijiye)\b""")
        ),
        ScamTactic.SUSPICIOUS_LINK to listOf(
            Regex("""(?i)\b(click\s+(on\s+)?(this\s+|the\s+)?link|open\s+(this\s+|the\s+)?website|install\s+(this\s+)?apk|download\s+(this\s+)?(file|apk)|open\s+(the\s+)?url)\b"""),
            Regex("""(?i)(இணைப்பை கிளிக் செய்|ஏபிகே பதிவிறக்கு|लिंक पर क्लिक करो|एपीके डाउनलोड|లింక్ క్లిక్ చేయండి)""")
        ),
        ScamTactic.ESCALATION to listOf(
            Regex("""(?i)\b(police will reach your house|patrol van sent|raid your home|direct digital arrest)\b"""),
            Regex("""(?i)(வீட்டுக்கு போலீஸ் வரும்|நேர்ல கைது செய்வோம்|घर पर पुलिस भेज रहे हैं|রেਡ ਪਵੇਗੀ)""")
        )
    )

    fun isBenign(text: String): Boolean {
        val trimmed = text.trim()
        return benignPatterns.any { it.containsMatchIn(trimmed) }
    }

    fun detectLanguage(text: String): Pair<String, Float> {
        val hasTamil = text.any { it in '\u0B80'..'\u0BFF' }
        val hasHindi = text.any { it in '\u0900'..'\u097F' }
        val hasTelugu = text.any { it in '\u0C00'..'\u0C7F' }
        val hasKannada = text.any { it in '\u0C80'..'\u0CFF' }
        val hasMalayalam = text.any { it in '\u0D00'..'\u0D7F' }
        val hasBengali = text.any { it in '\u0980'..'\u09FF' }
        val hasGujarati = text.any { it in '\u0A80'..'\u0AFF' }
        val hasPunjabi = text.any { it in '\u0A00'..'\u0A7F' }
        val hasOdia = text.any { it in '\u0B00'..'\u0B7F' }
        val lower = text.lowercase()

        val isTanglish = anyWord(lower, listOf("unga", "pannunga", "pesuren", "irundhu", "solraen", "panam", "udane"))
        val isHinglish = anyWord(lower, listOf("aapka", "kijiye", "raha hoon", "paise", "bhejo", "karo", "turant"))

        return when {
            isTanglish -> "Tanglish" to 0.94f
            isHinglish -> "Hinglish" to 0.93f
            hasTamil -> "Tamil" to 0.96f
            hasHindi -> "Hindi" to 0.95f
            hasTelugu -> "Telugu" to 0.94f
            hasKannada -> "Kannada" to 0.94f
            hasMalayalam -> "Malayalam" to 0.94f
            hasBengali -> "Bengali" to 0.94f
            hasGujarati -> "Gujarati" to 0.94f
            hasPunjabi -> "Punjabi" to 0.94f
            hasOdia -> "Odia" to 0.94f
            else -> "English" to 0.92f
        }
    }

    private fun anyWord(text: String, words: List<String>): Boolean {
        return words.any { text.contains(it) }
    }

    fun analyze(event: TranscriptEvent, languageCode: String = "auto"): List<RiskSignal> {
        val text = event.text.trim()
        if (text.isEmpty()) return emptyList()

        // 1. Strict Benign Suppression Check
        if (isBenign(text)) {
            return emptyList()
        }

        val detected = mutableListOf<RiskSignal>()
        val (detectedLang, conf) = detectLanguage(text)

        for ((tactic, patterns) in multilingualPatterns) {
            for (pattern in patterns) {
                val match = pattern.find(text)
                if (match != null) {
                    val signal = RiskSignal(
                        id = UUID.randomUUID().toString(),
                        timestamp = event.timestamp,
                        tactic = tactic,
                        confidence = conf,
                        riskContribution = tactic.defaultWeight,
                        evidenceText = match.value,
                        source = "$detectedLang ($languageCode)",
                        sessionId = event.sessionId
                    )
                    detected.add(signal)
                    break // One match per tactic per turn
                }
            }
        }
        return detected
    }
}
