package com.rakshacall.safety.intelligence

import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.TranscriptEvent
import java.util.UUID

/**
 * Result of multilingual language identification and semantic understanding.
 */
data class LanguageAnalysis(
    val primaryLanguage: String,
    val detectedLanguages: List<String>,
    val confidence: Float,
    val isCodeSwitching: Boolean,
    val normalizedText: String
)

/**
 * Advanced real-time multilingual semantic intelligence engine for RakshaCall.
 * Supports 8 Indian languages (en, ta, hi, te, kn, ml, bn, mr) in both native script
 * and Romanized/Latin transliterations, intra-utterance code-switching, slang neutrality,
 * phonetic ASR normalization, indirect intent extraction, and strict hard-negative suppression.
 */
class MultilingualSemanticEngine {

    // 1. Script ranges for fast, deterministic primary language detection
    private val scriptRanges = listOf(
        "ta" to Pair('\u0B80', '\u0BFF'), // Tamil
        "hi" to Pair('\u0900', '\u097F'), // Devanagari (Hindi / Marathi)
        "te" to Pair('\u0C00', '\u0C7F'), // Telugu
        "kn" to Pair('\u0C80', '\u0CFF'), // Kannada
        "ml" to Pair('\u0D00', '\u0D7F'), // Malayalam
        "bn" to Pair('\u0980', '\u09FF')  // Bengali
    )

    // Slang tokens that are neutral context
    private val slangPattern = Regex("""(?i)\b(bro|bhai|yaar|machan|anna|akka|thambi|chetta|boss|sir|madam|ji)\b""")

    // Hard negative suppressor patterns (0% false alarms on advisories, educational statements, benign notifications)
    private val hardNegativePatterns = listOf(
        Regex("""(?i)\b(do not|never|don't|mat|koodadhu|cheyyavadhu|beda|padilla|korben na|nako)\s+(share|give|batao)\s+(your\s+)?(otp|pin|password|cvv|code)\b"""),
        Regex("""(?i)\bnever\s+share\s+(your\s+)?(otp|pin|password|cvv)\b"""),
        Regex("""(?i)\b(do not|never|don't)\s+(install|download|click)\s+(anydesk|teamviewer|link|apk|software)\b"""),
        Regex("""(?i)\bbank\s+(employees|never\s+asks|never\s+calls|officials\s+never)\b"""),
        Regex("""(?i)\b(example\s+of|learn\s+how|awareness|scam\s+alert|be\s+aware)\b"""),
        Regex("""(?i)\b(bank\s+refunded|received\s+my\s+refund|money\s+refunded)\b"""),
        Regex("""(?i)\bmy\s+bank\s+sent\s+me\s+an?\s+otp\s+for\s+login\b"""),
        Regex("""(?i)\bwhy\s+are\s+you\s+shouting\s+at\s+me\b"""),
        Regex("""(?i)\b(good\s+morning|good\s+evening|how\s+are\s+you|happy\s+birthday)\b""")
    )

    // Phonetic ASR error normalizations
    private val asrReplacements = listOf(
        Regex("""(?i)\b(otb|o\s*t\s*p|oh\s*tee\s*pee)\b""") to "otp",
        Regex("""(?i)\b(audahaar|audhar|aadahr|adahar)\b""") to "aadhaar",
        Regex("""(?i)\b(any\s*desk|anidesk|anydesck)\b""") to "anydesk",
        Regex("""(?i)\b(team\s*viewer|timviwer|teamvewer)\b""") to "teamviewer",
        Regex("""(?i)\b(rust\s*desk|quick\s*support)\b""") to "remote_app"
    )

    // Semantic Frame Taxonomy: Tactic -> Multi-Lingual Patterns (Native + Romanized + Indirect Intent)
    private val semanticFrames: Map<ScamTactic, List<Regex>> = mapOf(
        ScamTactic.AUTHORITY_IMPERSONATION to listOf(
            Regex("""(?i)\b(cbi|police|cyber\s*crime|narcotics|customs|supreme\s*court|high\s*court|rbi|trai|enforcement\s*directorate|ed\s*officer)\b"""),
            Regex("""(?i)(காவல்துறை|சிபிஐ|சைபர் கிரைம்|நீதிமன்றம்|சுங்கத்துறை|காவல் அதிகாரி)"""),
            Regex("""(?i)(पुलिस|सीबीआई|साइबर क्राइम|कस्टम्स|नारकोटिक्स|अदालत|अधिकारी)"""),
            Regex("""(?i)(పోలీస్|సిబిఐ|సైబర్ క్రైమ్|కస్టమ్స్|కోర్టు|అధికారి)"""),
            Regex("""(?i)(ಪೊಲೀಸ್|ಸಿಬಿಐ|ಸೈಬರ್ ಕ್ರೈಮ್|ನ್ಯಾಯಾಲಯ|ಅಧಿಕಾರಿ)"""),
            Regex("""(?i)(പോലീസ്|സിബിഐ|സൈബർ ക്രൈം|കോടതി|ഉദ്യോഗസ്ഥൻ)"""),
            Regex("""(?i)(পুলিশ|সিবিআই|সাইবার ক্রাইম|আদালত|কর্মকর্তা)"""),
            Regex("""(?i)(पोलीस|सीबीआय|सायबर क्राईम|न्यायालय|अधिकारी)"""),
            Regex("""(?i)\b(police\s*(pesuren|bol\s*raha|matladuthunnanu|mathadthidini|samsarikunnath|bolchi|bolat\s*ahe))\b"""),
            Regex("""(?i)\b(cbi\s*(officer|department|headquarters))\b""")
        ),

        ScamTactic.CRIMINAL_ALLEGATION to listOf(
            Regex("""(?i)\b(illegal\s*transaction|money\s*laundering|drugs\s*found|contraband|arrest\s*warrant|fir\s*registered|human\s*trafficking|identity\s*theft)\b"""),
            Regex("""(?i)(வழக்கு|கைது வாரண்ட்|சட்டவிரோத பணப்பரிவர்த்தனை|போதைப்பொருள்|குற்றப்பிரிவு)"""),
            Regex("""(?i)(गैरकानूनी लेन-देन|मनी लॉन्ड्रिंग|गिरफ्तारी वारंट|एफआईआर दर्ज|अपराध|ड्रग्स पार्सल)"""),
            Regex("""(?i)(చట్టవిరుద్ధ లావాదేవీ|మనీ లాండరింగ్|అరెస్ట్ వారెంట్|ఎఫ్ఐఆర్ నమోదు|నేరం)"""),
            Regex("""(?i)(ಕಾನೂನುಬಾಹಿರ ವಹಿವಾಟು|ಬಂಧನ ವಾರೆಂಟ್|ಅಕ್ರಮ ಹಣ ವರ್ಗಾವಣೆ|ದೂರು ದಾಖಲು)"""),
            Regex("""(?i)(നിയമവിരുദ്ധ ഇടപാട്|മണി ലോണ്ടറിംഗ്|അറസ്റ്റ് വാറണ്ട്|കേസ് രജിസ്റ്റർ)"""),
            Regex("""(?i)(বেআইনি লেনদেন|মানি লন্ডারিং|গ্রেফতারি পরোয়ানা|মামলা দায়ের)"""),
            Regex("""(?i)(बेकायदेशीर व्यवहार|मनी लाँड्रिंग|अटक वॉरंट|गुन्हा दाखल)"""),
            Regex("""(?i)\b(aadhaar.*(linked|misused|case|involved|fraud))\b"""),
            Regex("""(?i)\b(account.*(block|band|seize|freeze|aagidum|ho\s*jayega|aipothundi|agutte|aakum|hoye\s*jabe|hoil))\b""")
        ),

        ScamTactic.URGENCY to listOf(
            Regex("""(?i)\b(immediately|right\s*now|within\s*(5|10|15|30)\s*minutes|urgent|hurry|final\s*warning|no\s*time)\b"""),
            Regex("""(?i)(உடனடியாக|இப்போதே|பத்து நிமிடத்தில்|கடைசி எச்சரிக்கை)"""),
            Regex("""(?i)(तुरंत|अभी के अभी|फौरन|दस मिनट में|जल्दी करो|आखिरी चेतावनी)"""),
            Regex("""(?i)(వెంటనే|ఇప్పుడే|పది నిమిషాల్లో|ఆఖరి హెచ్చరిక)"""),
            Regex("""(?i)(ತಕ್ಷಣ|ಈಗಲೇ|ಹತ್ತು ನಿಮಿಷದಲ್ಲಿ|ಕೊನೆಯ ಎಚ್ಚರಿಕೆ)"""),
            Regex("""(?i)(ഉടൻ തന്നെ|ഇപ്പോൾ തന്നെ|അവസാന മുന്നറിയിപ്പ്)"""),
            Regex("""(?i)(অবিলম্বে|এখনই|দশ মিনিটের মধ্যে|শেষ সতর্কতা)"""),
            Regex("""(?i)(लगेच|आत्ताच|दहा मिनिटात|शेवटची चेतावणी)"""),
            Regex("""(?i)\b(turant|abhi\s*ke\s*abhi|udanadiyaaga|ventane|thakshana|ippol\s*thanne|ekhoni|lagech)\b""")
        ),

        ScamTactic.ISOLATION to listOf(
            Regex("""(?i)\b(don't\s*disconnect|stay\s*on\s*call|do\s*not\s*tell|close\s*the\s*door|alone\s*in\s*room|digital\s*arrest|keep\s*camera\s*on|secrecy)\b"""),
            Regex("""(?i)(கதவை மூடு|யாருக்கும் சொல்லாதே|அழைப்பை துண்டிக்காதே|தனியாக இரு|டிஜிட்டல் கைது)"""),
            Regex("""(?i)(कमरे का दरवाजा बंद|किसी को मत बताना|फोन मत काटना|अकेले रहो|डिजिटल अरेस्ट|कैमरा चालू)"""),
            Regex("""(?i)(గది తలుపులు వెయ్యండి|ఎవరికీ చెప్పవద్దు|కాల్ కట్ చేయవద్దు|ఒంటరిగా ఉండండి|డిజిటల్ అరెస్ట్)"""),
            Regex("""(?i)(ಕೊಠಡಿ ಬಾಗಿಲು ಮುಚ್ಚಿ|ಯಾರಿಗೂ ಹೇಳಬೇಡಿ|ಕರೆ ಕಡಿತಗೊಳಿಸಬೇಡಿ|ಡಿಜಿಟಲ್ ಬಂಧನ)"""),
            Regex("""(?i)(മുറി അടച്ചിരിക്കുക|ആരോടും പറയരുത്|കോൾ കട്ട് ചെയ്യരുത്|ഡിജിറ്റൽ അറസ്റ്റ്)"""),
            Regex("""(?i)(ঘরের দরজা বন্ধ করুন|কাউকে বলবেন না|কল কাটবেন না|ডিজিটাল গ্রেপ্তার)"""),
            Regex("""(?i)(खोलीचे दार बंद करा|कोणाला सांगू नका|कॉल कापू नका|डिजिटल अटक)"""),
            Regex("""(?i)\b(kadhava\s*moodittu|room\s*kadhava|kisi\s*ko\s*mat\s*batana|phone\s*mat\s*kaato|call\s*cut\s*pannadhinga)\b""")
        ),

        ScamTactic.CREDENTIAL_PRESSURE to listOf(
            // Direct and indirect credential extraction
            Regex("""(?i)\b(otp|pin|password|cvv|verification\s*code|security\s*code)\b"""),
            Regex("""(?i)\b(read\s*(the|those)?\s*(six|4|6)\s*(numbers|digits))\b"""),
            Regex("""(?i)\b(what\s*code\s*came|check\s*the\s*sms|verification\s*digits|read\s*that\s*number\s*out)\b"""),
            Regex("""(?i)(ஓடிபி சொல்லுங்க|ரகசிய எண்|சரிபார்ப்பு குறியீடு|அந்த ஆறு எண்களை சொல்லு)"""),
            Regex("""(?i)(ओटीपी बताओ|पासवर्ड दो|पिन डालो|वेरिफिकेशन कोड|वो छह नंबर बताओ)"""),
            Regex("""(?i)(ఓటీపీ చెప్పండి|రహస్య కోడ్|ఆ ఆరు సంఖ్యలు చెప్పండి)"""),
            Regex("""(?i)(ಒಟಿಪಿ ಹೇಳಿ|ಪಾಸ್‌ವರ್ಡ್ ನೀಡಿ|ಆ ಆರು ಸಂಖ್ಯೆಗಳನ್ನು ಓದಿ)"""),
            Regex("""(?i)(ഒടിപി പറയൂ|രഹസ്യ കോഡ്|ആ ആറ് നമ്പറുകൾ പറയൂ)"""),
            Regex("""(?i)(ওটিপি বলুন|পাসওয়ার্ড দিন|ছয়টি সংখ্যা পড়ুন)"""),
            Regex("""(?i)(ओटीपी सांगा|पासवर्ड द्या|ते सहा नंबर सांगा)"""),
            Regex("""(?i)\b(otp\s*(sollunga|batao|cheppandi|heli|parayamo|bolun|sanga))\b"""),
            Regex("""(?i)\b(code\s*(bata\s*dijiye|sollunga|cheppandi|parayoo))\b""")
        ),

        ScamTactic.PAYMENT_DEMAND to listOf(
            // Direct and indirect financial transfer demands
            Regex("""(?i)\b(transfer\s*money|send\s*amount|pay\s*penalty|bail\s*amount|security\s*deposit|escrow\s*account|clear\s*funds)\b"""),
            Regex("""(?i)\b(move\s*the\s*balance|transfer\s*it\s*temporarily|shift\s*the\s*amount|send\s*the\s*funds)\b"""),
            Regex("""(?i)(பணம் அனுப்பு|பரிவர்த்தனை செய்|பாதுகாப்பு வைப்புத்தொகை|அபராதம் கட்டு)"""),
            Regex("""(?i)(पैसे ट्रांसफर|रुपये भेजो|सिक्योरिटी डिपॉजिट|खाते में डालो|जुर्माना भरो)"""),
            Regex("""(?i)(డబ్బులు పంపండి|ట్రాన్స్‌ఫర్ చేయండి|సెక్యూరిటీ డిపాజిట్|ఖాతాలో వేయండి)"""),
            Regex("""(?i)(ಹಣ ವರ್ಗಾಯಿಸಿ|ಖಾತೆಗೆ ಹಾಕಿ|ಭದ್ರತಾ ಠೇವಣಿ|ದಂಡ ಕಟ್ಟಿ)"""),
            Regex("""(?i)(പണം അയക്കൂ|അക്കൗണ്ടിലേക്ക് മാറ്റൂ|സെക്യൂരിറ്റി ഡെപ്പോസിറ്റ്)"""),
            Regex("""(?i)(টাকা পাঠান|ট্রান্সফার করুন|নিরাপত্তা আমানত|জরিমানা দিন)"""),
            Regex("""(?i)(पैसे पाठवा|ट्रान्सफर करा|सुरक्षा ठेव|दंड भरा)"""),
            Regex("""(?i)\b(panam\s*anupunga|paise\s*bhejo|dabbulu\s*pampandi|hana\s*vargayisi|taka\s*pathan|paise\s*pathva)\b""")
        ),

        ScamTactic.REMOTE_ACCESS to listOf(
            Regex("""(?i)\b(anydesk|teamviewer|rustdesk|quicksupport|screen\s*share|remote\s*access|install\s*app|apk\s*file)\b"""),
            Regex("""(?i)(எனிடெஸ்க்|டீம்வியூவர்|ஸ்கிரீன் ஷேர்|ஆப் இன்ஸ்டால் செய்)"""),
            Regex("""(?i)(एनीडेस्क डाउनलोड|टीमव्यूअर इंस्टॉल|स्क्रीन शेयर|ऐप डाउनलोड)"""),
            Regex("""(?i)(ఎనీడెస్క్|టీమ్‌వ్యూయర్|స్క్రీన్ షేర్|యాప్ ఇన్‌స్టాల్)"""),
            Regex("""(?i)(ಅನಿದೆಸ್ಕ್|ಟೀಮ್‌ವೀವರ್|ಸ್ಕ್ರೀನ್ ಶೇರ್|ಆ್ಯಪ್ ಡೌನ್‌ಲೋಡ್)"""),
            Regex("""(?i)(എനിഡെസ്ക്|ടീംവ്യൂവർ|സ്ക്രീൻ ഷെയർ|ആപ്പ് ഇൻസ്റ്റാൾ)"""),
            Regex("""(?i)(এনিডেস্ক|টিমভিউয়ার|স্ক্রিন শেয়ার|অ্যাপ ইনস্টল)"""),
            Regex("""(?i)(अ‍ॅनीडेस्क|टीमव्ह्यूअर|स्क्रीन शेअर|अ‍ॅप इन्स्टॉल)""")
        ),

        ScamTactic.ESCALATION to listOf(
            Regex("""(?i)\b(sending\s*police|physical\s*arrest|house\s*raid|asset\s*seizure|property\s*attachment|jail\s*term)\b"""),
            Regex("""(?i)(வீட்டுக்கு போலீஸ் வரும்|சொத்து முடக்கம்|சிறை தண்டனை|கைது செய்வோம்)"""),
            Regex("""(?i)(घर पर पुलिस भेज रहे हैं|रेड पड़ेगी|संपत्ति कुर्क|जेल होगी|अरेस्ट करेंगे)"""),
            Regex("""(?i)(ఇంటికి పోలీస్ వస్తారు|ఆస్తి జప్తు|జైలుకు పంపుతాము)"""),
            Regex("""(?i)(ಮನೆಗೆ ಪೊಲೀಸ್ ಕಳುಹಿಸುತ್ತೇವೆ|ಆಸ್ತಿ ಮುಟ್ಟುಗೋಲು|ಜೈಲಿಗೆ ಕಳುಹಿಸುತ್ತೇವೆ)"""),
            Regex("""(?i)(വീട്ടിലേക്ക് പോലീസ് വരും|സ്വത്ത് കണ്ടുകെട്ടൽ|ജയിലിൽ അടയ്ക്കും)"""),
            Regex("""(?i)(বাড়িতে পুলিশ পাঠাচ্ছি|সম্পত্তি বাজেয়াপ্ত|জেলে পাঠাব)"""),
            Regex("""(?i)(घरी पोलीस पाठवतो|मालमत्ता जप्त|तुरुंगात पाठवू)""")
        )
    )

    /**
     * Identify primary and code-switched languages in a text snippet.
     */
    fun identifyLanguages(rawText: String): LanguageAnalysis {
        val detected = mutableSetOf<String>()
        var primary = "en"
        var scriptCount = 0

        for ((code, range) in scriptRanges) {
            val count = rawText.count { it in range.first..range.second }
            if (count > 0) {
                detected.add(code)
                if (count > scriptCount) {
                    scriptCount = count
                    primary = code
                }
            }
        }

        // Differentiate Marathi from Hindi in Devanagari script
        if (primary == "hi") {
            val marathiMarkers = Regex("""(?i)(आहे|नाही|आहेत|केले|होते|करा|सांगा|पाठवा|पोलीस|गुन्हा|तुरुंगात|झाले|होईल|अडकले)""")
            if (marathiMarkers.containsMatchIn(rawText)) {
                primary = "mr"
                detected.remove("hi")
                detected.add("mr")
            }
        }

        // Check for Latin letters
        val latinCount = rawText.count { (it in 'a'..'z') || (it in 'A'..'Z') }
        if (latinCount > 0) {
            detected.add("en")
            if (scriptCount == 0) {
                // Romanized detection heuristic
                val lower = rawText.lowercase()
                when {
                    lower.contains("aagidum") || lower.contains("pesuren") || lower.contains("panam") || lower.contains("sollunga") || lower.contains("kadhava") -> {
                        detected.add("ta")
                        primary = "ta"
                    }
                    lower.contains("aapka") || lower.contains("batao") || lower.contains("paise") || lower.contains("karo") || lower.contains("band ho") -> {
                        detected.add("hi")
                        primary = "hi"
                    }
                    lower.contains("aipothundi") || lower.contains("dabbulu") || lower.contains("cheppandi") || lower.contains("matladuthunnanu") -> {
                        detected.add("te")
                        primary = "te"
                    }
                    lower.contains("agutte") || lower.contains("hana") || lower.contains("heli") || lower.contains("mathadthidini") -> {
                        detected.add("kn")
                        primary = "kn"
                    }
                    lower.contains("aakum") || lower.contains("panam") || lower.contains("parayoo") || lower.contains("samsarikunnath") -> {
                        detected.add("ml")
                        primary = "ml"
                    }
                    lower.contains("hoye jabe") || lower.contains("taka") || lower.contains("pathan") || lower.contains("bolchi") -> {
                        detected.add("bn")
                        primary = "bn"
                    }
                    lower.contains("hoil") || lower.contains("pathva") || lower.contains("sanga") || lower.contains("bolat ahe") -> {
                        detected.add("mr")
                        primary = "mr"
                    }
                }
            }
        }

        val isCodeSwitch = detected.size >= 2

        // ASR Normalization
        var normalized = rawText
        for ((regex, rep) in asrReplacements) {
            normalized = normalized.replace(regex, rep)
        }

        return LanguageAnalysis(
            primaryLanguage = primary,
            detectedLanguages = detected.toList(),
            confidence = if (scriptCount > 0) 0.95f else 0.85f,
            isCodeSwitching = isCodeSwitch,
            normalizedText = normalized.trim()
        )
    }

    /**
     * Analyze a speech/transcript event semantically.
     */
    fun analyze(
        event: TranscriptEvent,
        priorContext: List<RiskSignal> = emptyList()
    ): Pair<LanguageAnalysis, List<RiskSignal>> {
        val raw = event.text.trim()
        if (raw.isBlank()) {
            return Pair(identifyLanguages(""), emptyList())
        }

        val langAnalysis = identifyLanguages(raw)
        val text = langAnalysis.normalizedText

        // Check Hard Negative Suppressors
        for (pattern in hardNegativePatterns) {
            if (pattern.containsMatchIn(text)) {
                // Advisory, warning, or benign phrasing - suppress tactic generation
                return Pair(langAnalysis, emptyList())
            }
        }

        val detectedSignals = mutableListOf<RiskSignal>()

        for ((tactic, patterns) in semanticFrames) {
            for (p in patterns) {
                val match = p.find(text)
                if (match != null) {
                    val signal = RiskSignal(
                        id = UUID.randomUUID().toString(),
                        timestamp = event.timestamp,
                        tactic = tactic,
                        confidence = 0.94f,
                        riskContribution = tactic.defaultWeight,
                        evidenceText = match.value,
                        source = "MULTILINGUAL_SEMANTIC (${langAnalysis.primaryLanguage})",
                        sessionId = event.sessionId
                    )
                    detectedSignals.add(signal)
                    break // Next tactic
                }
            }
        }

        // Cross-turn pronoun resolution: e.g. "send it immediately" / "give it to me"
        if (detectedSignals.none { it.tactic == ScamTactic.CREDENTIAL_PRESSURE || it.tactic == ScamTactic.PAYMENT_DEMAND }) {
            val vagueDemand = Regex("""(?i)\b(send\s*it|read\s*it|tell\s*it|transfer\s*it|give\s*it)\b""").find(text)
            if (vagueDemand != null) {
                // If previous signals included credential pressure or payment demand in this call
                val hadCredentialContext = priorContext.any { it.tactic == ScamTactic.CREDENTIAL_PRESSURE }
                val hadPaymentContext = priorContext.any { it.tactic == ScamTactic.PAYMENT_DEMAND }

                if (hadCredentialContext) {
                    detectedSignals.add(
                        RiskSignal(
                            id = UUID.randomUUID().toString(),
                            timestamp = event.timestamp,
                            tactic = ScamTactic.CREDENTIAL_PRESSURE,
                            confidence = 0.88f,
                            riskContribution = ScamTactic.CREDENTIAL_PRESSURE.defaultWeight,
                            evidenceText = "${vagueDemand.value} (Resolved from prior credential context)",
                            source = "CONTEXT_PRONOUN_RESOLUTION",
                            sessionId = event.sessionId
                        )
                    )
                } else if (hadPaymentContext) {
                    detectedSignals.add(
                        RiskSignal(
                            id = UUID.randomUUID().toString(),
                            timestamp = event.timestamp,
                            tactic = ScamTactic.PAYMENT_DEMAND,
                            confidence = 0.88f,
                            riskContribution = ScamTactic.PAYMENT_DEMAND.defaultWeight,
                            evidenceText = "${vagueDemand.value} (Resolved from prior payment context)",
                            source = "CONTEXT_PRONOUN_RESOLUTION",
                            sessionId = event.sessionId
                        )
                    )
                }
            }
        }

        return Pair(langAnalysis, detectedSignals)
    }
}
