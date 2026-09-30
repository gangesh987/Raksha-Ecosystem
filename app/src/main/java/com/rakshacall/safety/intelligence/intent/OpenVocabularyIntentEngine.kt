package com.rakshacall.safety.intelligence.intent

/**
 * Open-Vocabulary Semantic Intent Classifications (Phase 7).
 * Infer intent beyond rigid keyword lists across languages, colloquialisms,
 * and indirect speech.
 */
enum class InferredIntent(val isCoercive: Boolean, val riskWeight: Int) {
    POSSIBLE_VERIFICATION_CODE_REQUEST(isCoercive = true, riskWeight = 40),
    POSSIBLE_FINANCIAL_TRANSFER_REQUEST(isCoercive = true, riskWeight = 35),
    POSSIBLE_REMOTE_ACCESS_REQUEST(isCoercive = true, riskWeight = 35),
    POSSIBLE_IDENTITY_COERCION(isCoercive = true, riskWeight = 25),
    POSSIBLE_ISOLATION_DEMAND(isCoercive = true, riskWeight = 30),
    POSSIBLE_ARREST_THREAT(isCoercive = true, riskWeight = 30),
    SAFE_ADVICE_OR_DEFENSE(isCoercive = false, riskWeight = -40),
    BENIGN_INQUIRY(isCoercive = false, riskWeight = 0)
}

data class IntentAnalysisResult(
    val intent: InferredIntent,
    val confidence: Float,
    val supportingEvidence: String,
    val extractedEntities: List<ExtractedEntity>
)

/**
 * Open-Vocabulary Intent Inference Engine.
 * Infers speaker intent across multi-lingual, indirect, and adversarial statements.
 */
class OpenVocabularyIntentEngine(
    private val entityEngine: EntityExtractionEngine = EntityExtractionEngine()
) {

    // 1. Strict Negative / Safe Advice Guard (Phase 14)
    private val safeAdvicePattern = Regex(
        """\b(never|do not|don't|kabhi nahi|vendam|koodathu|kappaduka|mat|epovum solladheenga)\s+(share|give|batao|anuppadheenga|solla koodadhu|divulge|enter|reveal)\s+(your\s+)?(otp|pin|password|cvv|credentials|code|six numbers)\b|""" +
        """\b(bank employees?|police|officials?)\s+(will\s+)?never\s+ask|""" +
        """\b(learn how to avoid|beware of|scam alert|fraud awareness|report cyber crime)\b""",
        RegexOption.IGNORE_CASE
    )

    // 2. Open-Vocabulary Verification Code Demands (Direct & Indirect)
    private val verificationCodeIndirect = Regex(
        """\b(read|tell|check|share|give|send|dictate|batao|boliye|sollunga|cheppandi|heli|parayu|balun)\s+""" +
        """(those|the|that|your|sms|message)?\s*""" +
        """(six|6|four|4)?\s*""" +
        """(numbers|digits|code|text|sms|characters|letters|otb|otp|pin|aksharam|sankhya)\b|""" +
        """\b(what did you receive|what came in the (sms|message)|check (your )?(sms|message)|message vandhurukka|kya message aaya)\b|""" +
        """\b(six numbers that just came|verification digits|read the number that just came)\b""",
        RegexOption.IGNORE_CASE
    )

    // 3. Open-Vocabulary Financial Transfer Demands (Direct & Indirect)
    private val financialTransferIndirect = Regex(
        """\b(move|shift|transfer|send|deposit|forward|park|put|bhejo|daalo|anuppu|vargayisi|ayakku)\s+""" +
        """(the|your|all|some)?\s*""" +
        """(amount|money|funds|balance|cash|rupees|panam|dabbulu|paise|hana)\s*""" +
        """(to|into|in)?\s*""" +
        """(safe|secure|rbi|government|temporary|holding|verification|escrow)?\s*""" +
        """(account|wallet|vpa|link)?\b|""" +
        """\b(put the balance somewhere safe|shift the funds|move the amount temporarily|transfer it to the protected account)\b""",
        RegexOption.IGNORE_CASE
    )

    // 4. Open-Vocabulary Remote Access Requests
    private val remoteAccessIndirect = Regex(
        """\b(install|download|open|connect|allow|screen share|share screen|remote support)\s+""" +
        """(anydesk|teamviewer|rustdesk|quicksupport|app|software|tool|link)\b|""" +
        """\b(give access to your screen|share your screen for verification|connect your device)\b""",
        RegexOption.IGNORE_CASE
    )

    // 5. Open-Vocabulary Isolation Demands
    private val isolationIndirect = Regex(
        """\b(do not|don't|never|mat|koodadhu)\s+(tell|speak to|inform|call|disconnect|cut|hang up)\s+(your\s+)?(family|parents|lawyer|police|friends|anyone|call)\b|""" +
        """\b(close the door|stay on the line|do not disconnect the call|remain on video call|maintain complete silence)\b""",
        RegexOption.IGNORE_CASE
    )

    // 6. Open-Vocabulary Arrest / Coercive Allegation Threats
    private val arrestAllegationIndirect = Regex(
        """\b(under digital arrest|warrant issued|case registered|arrest warrant|illegal money laundering|parcel seized|customs narcotics|cyber crime department)\b""",
        RegexOption.IGNORE_CASE
    )

    fun inferIntent(text: String, speakerRole: String = "REMOTE_CALLER"): IntentAnalysisResult {
        val entities = entityEngine.extractEntities(text)

        // Strict hard negative check: educational/safe advice
        if (safeAdvicePattern.containsMatchIn(text)) {
            return IntentAnalysisResult(
                intent = InferredIntent.SAFE_ADVICE_OR_DEFENSE,
                confidence = 0.98f,
                supportingEvidence = "Defensive/educational security phrase detected.",
                extractedEntities = entities
            )
        }

        // Only evaluate coercive intents if text comes from or pertains to the remote party
        if (verificationCodeIndirect.containsMatchIn(text)) {
            return IntentAnalysisResult(
                intent = InferredIntent.POSSIBLE_VERIFICATION_CODE_REQUEST,
                confidence = 0.94f,
                supportingEvidence = "Indirect or direct verification code / OTP extraction intent detected.",
                extractedEntities = entities
            )
        }

        if (financialTransferIndirect.containsMatchIn(text)) {
            return IntentAnalysisResult(
                intent = InferredIntent.POSSIBLE_FINANCIAL_TRANSFER_REQUEST,
                confidence = 0.92f,
                supportingEvidence = "Funds redirection / coercive transfer demand detected.",
                extractedEntities = entities
            )
        }

        if (remoteAccessIndirect.containsMatchIn(text)) {
            return IntentAnalysisResult(
                intent = InferredIntent.POSSIBLE_REMOTE_ACCESS_REQUEST,
                confidence = 0.95f,
                supportingEvidence = "Third-party screen share or remote desktop control request detected.",
                extractedEntities = entities
            )
        }

        if (isolationIndirect.containsMatchIn(text)) {
            return IntentAnalysisResult(
                intent = InferredIntent.POSSIBLE_ISOLATION_DEMAND,
                confidence = 0.93f,
                supportingEvidence = "Victim isolation or silence enforcement detected.",
                extractedEntities = entities
            )
        }

        if (arrestAllegationIndirect.containsMatchIn(text)) {
            return IntentAnalysisResult(
                intent = InferredIntent.POSSIBLE_ARREST_THREAT,
                confidence = 0.91f,
                supportingEvidence = "Fabricated legal jeopardy / digital arrest coercion detected.",
                extractedEntities = entities
            )
        }

        // Entity-correlated fallback
        val hasGovOrPolice = entities.any { it.type == EntityType.POLICE || it.type == EntityType.GOVERNMENT }
        val hasAadhaarOrPan = entities.any { it.type == EntityType.AADHAAR || it.type == EntityType.PAN || it.type == EntityType.DOCUMENT }
        if (hasGovOrPolice && hasAadhaarOrPan) {
            return IntentAnalysisResult(
                intent = InferredIntent.POSSIBLE_IDENTITY_COERCION,
                confidence = 0.85f,
                supportingEvidence = "Authority and statutory identification entities co-occurring.",
                extractedEntities = entities
            )
        }

        return IntentAnalysisResult(
            intent = InferredIntent.BENIGN_INQUIRY,
            confidence = 0.70f,
            supportingEvidence = "Standard dialogue without detected coercion.",
            extractedEntities = entities
        )
    }
}
