package com.rakshacall.safety.intelligence.intent

/**
 * Recognised Entity Types for live conversation intelligence (Phase 9).
 * Note: Entity presence alone does NOT constitute a scam; entities must be
 * evaluated in context with speaker, intent, and behavioural pressure.
 */
enum class EntityType {
    BANK,
    POLICE,
    GOVERNMENT,
    ACCOUNT,
    MONEY,
    OTP,
    PIN,
    PASSWORD,
    DOCUMENT,
    AADHAAR,
    PAN,
    UPI,
    CRYPTO,
    REMOTE_ACCESS,
    SCREEN,
    APPLICATION
}

data class ExtractedEntity(
    val type: EntityType,
    val matchedText: String,
    val startIndex: Int,
    val endIndex: Int,
    val confidence: Float = 0.95f
)

/**
 * High-performance Multilingual Entity Extraction Engine.
 * Supports English, Hindi, Tamil, Telugu, Kannada, Malayalam, Bengali, Marathi,
 * Romanized variations, and colloquial expressions.
 */
class EntityExtractionEngine {

    private val entityPatterns: Map<EntityType, Regex> = mapOf(
        EntityType.BANK to Regex(
            """\b(bank|rbi|sbi|hdfc|icici|axis|punjab national|reserve bank|canara|kotak|federal bank|union bank|bhim|வங்கி|வங்கியின்|बैंक|खाता बैंक|బ్యాంకు|ಬ್ಯಾಂಕ್|ബാങ്ക്|ব্যাংক|बँक)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.POLICE to Regex(
            """\b(police|cbi|ed|enforcement directorate|cid|narcotics|ncb|cyber cell|inspector|dsp|sho|crime branch|dcp|காவல்துறை|காவல்|போலீஸ்|पुलिस|थाना|सीबीआइ|పోలీస్|ಪೊಲೀಸ್|പോലീസ്|পুলিশ|पोलीस)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.GOVERNMENT to Regex(
            """\b(court|supreme court|high court|customs|ministry|trai|dot|department of telecom|government|gov|அரசு|நீதிமன்றம்|सरकार|अदालत|న్యాయస్థానం|ಸರ್ಕಾರ|കോടതി|আদালত|न्यायालय|शासन)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.ACCOUNT to Regex(
            """\b(account|bank account|savings account|current account|acc|khata|a/c|கணக்கு|खाता|खाते|ఖాతా|ಖಾತೆ|അക്കൗണ്ട്|হিসাব|खाते)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.MONEY to Regex(
            """\b(money|amount|rupees|rs|inr|cash|lakh|crore|fund|funds|balance|panam|dabbulu|paise|hana|taka|ரூபாய்|பணம்|रुपये|पैसे|लाख|రూపాయలు|డబ్బులు|ರೂಪಾಯಿ|ಹಣ|രൂപ|പണം|টাকা|रुपये|पैसे)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.OTP to Regex(
            """\b(otp|one time password|verification code|security code|sms code|otb|six numbers|6 numbers|six digits|6 digits|ஓடிபி|சரிபார்ப்புக் குறியீடு|ओटीपी|पासवर्ड कोड|ఓటీపీ|ಒಟಿಪಿ|ഒടിപി|ওটিপি|ओटीपी)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.PIN to Regex(
            """\b(pin|mpin|upi pin|atm pin|secret pin|பின்|पिन|यूपीआई पिन|పిన్|ಪಿನ್|പിൻ|পিন|पिन)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.PASSWORD to Regex(
            """\b(password|passcode|login password|credentials|கடவுச்சொல்|पासवर्ड|పాస్‌వర్డ్|ಪಾಸ್‌ವರ್ಡ್|പാസ്‌വേഡ്|পাসওয়ার্ড|संकेतशब्द)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.DOCUMENT to Regex(
            """\b(document|documents|id card|identity proof|warrant|notice|summons|ஆவணம்|வாரண்ட்|दस्तावेज|वारंट|పత్రం|ದಾಖಲೆ|രേഖ|নথিপত্র|दस्तऐवज|वॉरंट)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.AADHAAR to Regex(
            """\b(aadhaar|aadhar|uidai|aadhaar number|आधार|ஆதார்|ఆధార్|ಆಧಾರ್|ആധാർ|আধার|आधार कार्ड)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.PAN to Regex(
            """\b(pan|pan card|pan number|பான்|பான் கார்டு|पैन|पैन कार्ड|పాన్|ಪ್ಯಾನ್|പാൻ|প্যান|पॅन)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.UPI to Regex(
            """\b(upi|gpay|google pay|phonepe|paytm|bhim|upi id|vpa|యూపీఐ|गूगल पे|फोन पे|പേടിഎം)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.CRYPTO to Regex(
            """\b(crypto|bitcoin|usdt|binance|ethereum|wallet address|क्रिप्टो|கிரிப்டோ)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.REMOTE_ACCESS to Regex(
            """\b(anydesk|teamviewer|rustdesk|quicksupport|zoho assist|airdroid|remote support|screen share|ஸ்கிரீன் ஷேர்|रिमोट|एनीडेस्क|టీమ్‌వ్యూయర్|स्क्रीन शेयर)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.SCREEN to Regex(
            """\b(screen|display|mobile screen|phone screen|திரை|स्क्रीन|స్క్రీన్|ಸ್ಕ್ರೀನ್|സ്ക്രീൻ|স্ক্রিন|पडदा)\b""",
            RegexOption.IGNORE_CASE
        ),
        EntityType.APPLICATION to Regex(
            """\b(app|application|apk|play store|download app|install app|செயலி|ऐप|ऐप इंस्टॉल|యాప్|ಆ್ಯಪ್|ആപ്പ്|অ্যাপ|अ‍ॅप)\b""",
            RegexOption.IGNORE_CASE
        )
    )

    fun extractEntities(text: String): List<ExtractedEntity> {
        val results = mutableListOf<ExtractedEntity>()
        for ((type, pattern) in entityPatterns) {
            val matches = pattern.findAll(text)
            for (match in matches) {
                results.add(
                    ExtractedEntity(
                        type = type,
                        matchedText = match.value,
                        startIndex = match.range.first,
                        endIndex = match.range.last + 1
                    )
                )
            }
        }
        return results
    }
}
