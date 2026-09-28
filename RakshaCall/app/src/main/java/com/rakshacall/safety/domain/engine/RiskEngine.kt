package com.rakshacall.safety.domain.engine

import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamTactic
import com.rakshacall.safety.domain.model.TranscriptEvent
import java.util.UUID

/**
 * Deterministic local NLP and pattern-matching risk engine.
 * Analyzes actual transcript events and computes risk score (0-100)
 * with duplicate suppression, temporal decay, and escalation multipliers.
 */
class RiskEngine(
    var lowThreshold: Int = 30,
    var highThreshold: Int = 60,
    var criticalThreshold: Int = 80
) {

    private val tacticPatterns = mapOf(
        ScamTactic.AUTHORITY_IMPERSONATION to listOf(
            Regex("""(?i)\b(cbi|central bureau|police|mumbai police|delhi police|cyber crime|cyber cell|supreme court|high court|customs|enforcement directorate|ed department|narcotics control|ncb|trai|telecom authority|telecom regulatory|inspector|sub-inspector|commissioner|investigating officer|headquarters)\b"""),
            Regex("""(?i)\b(calling from (the )?(police|cbi|customs|court|rbi|trai|government))\b"""),
            Regex("""(?i)\b(authorized officer|digital arrest team|special branch)\b""")
        ),
        ScamTactic.CRIMINAL_ALLEGATION to listOf(
            Regex("""(?i)\b(aadhaar.*(illegal|misused|crime|involved)|illegal parcel|drugs found|narcotics|contraband|money laundering|terror funding|arrest warrant|non-bailable|fir registered|criminal case|identity theft|bank fraud|passport (suspended|cancelled))\b"""),
            Regex("""(?i)\b(you are accused|charges against you|found in customs|consignment seized|taiwan parcel|mumbai airport parcel)\b"""),
            Regex("""(?i)\b(seized|warrant issued|penal code|ipc section)\b""")
        ),
        ScamTactic.URGENCY to listOf(
            Regex("""(?i)\b(immediately|right now|within 10 minutes|within 15 minutes|within 5 minutes|without delay|instant|instantaneous|urgent matter|no time to waste|hurry up)\b"""),
            Regex("""(?i)\b(before we take action|final warning|do it now|matter of minutes)\b""")
        ),
        ScamTactic.ISOLATION to listOf(
            Regex("""(?i)\b(do not disconnect|do not hang up|stay on the call|keep (the )?camera on|stay in (a |your )?(quiet|private|closed) room|do not tell (anyone|family|parents|friends|spouse)|digital custody|digital arrest|confidential investigation|secrecy)\b"""),
            Regex("""(?i)\b(do not speak to anyone|cannot disclose to anybody|private hearing)\b""")
        ),
        ScamTactic.PAYMENT_DEMAND to listOf(
            Regex("""(?i)\b(transfer (the )?(money|funds|amount|₹|rs|rupees)|security deposit|escrow (account)?|clear your funds|rbi verification account|penalty amount|bail amount|clearance fee|fine of|pay immediately|deposit to account)\b"""),
            Regex("""(?i)\b(50,?000|1,?00,?000|25,?000|lakh|refund verification)\b""")
        ),
        ScamTactic.CREDENTIAL_PRESSURE to listOf(
            Regex("""(?i)\b(give me (your )?otp|share (the |your )?otp|enter (your )?upi pin|card cvv|netbanking password|atm pin|banking password|aadhaar otp|verification code|one time password)\b"""),
            Regex("""(?i)\b(read out the code|tell me the (6|4) digit code)\b""")
        ),
        ScamTactic.REMOTE_ACCESS to listOf(
            Regex("""(?i)\b(install|download).*(anydesk|teamviewer|quicksupport|rustdesk|screen share|zoho assist)\b"""),
            Regex("""(?i)\b(share your screen|remote access|connect to support app)\b""")
        ),
        ScamTactic.SUSPICIOUS_LINK to listOf(
            Regex("""(?i)\b(click (on )?(the )?link|download (the )?apk|open this url|verification link|fill this form)\b"""),
            Regex("""(?i)\b(http[s]?://|bit\.ly/|t\.co/)\b""")
        ),
        ScamTactic.ESCALATION to listOf(
            Regex("""(?i)\b(send(ing)? police (to your house|team)|raid your home|physical arrest|seize your property|freeze (all )?your bank accounts|blacklist your aadhaar|jail term)\b"""),
            Regex("""(?i)\b(we are tracing your location|patrol vehicle dispatched)\b""")
        )
    )

    /**
     * Analyze a transcript text directly.
     */
    fun analyzeTranscript(
        text: String,
        timestamp: Long = System.currentTimeMillis()
    ): List<RiskSignal> {
        val event = TranscriptEvent(
            id = UUID.randomUUID().toString(),
            sessionId = "session-local",
            timestamp = timestamp,
            speaker = "CALLER",
            text = text
        )
        return analyzeTranscript(event, emptyList())
    }

    /**
     * Analyze a transcript event and return detected RiskSignals.
     */
    fun analyzeTranscript(
        event: TranscriptEvent,
        pastSignals: List<RiskSignal>
    ): List<RiskSignal> {

        val detected = mutableListOf<RiskSignal>()
        val text = event.text.trim()
        if (text.isEmpty()) return emptyList()

        for ((tactic, patterns) in tacticPatterns) {
            for (pattern in patterns) {
                val match = pattern.find(text)
                if (match != null) {
                    val evidence = match.value
                    // Duplicate suppression: check if same tactic was detected recently (< 45s)
                    val recentSameTactic = pastSignals.filter {
                        it.tactic == tactic && (event.timestamp - it.timestamp) < 45_000L
                    }

                    val weightFactor = when {
                        recentSameTactic.isEmpty() -> 1.0f
                        recentSameTactic.size == 1 -> 0.4f
                        else -> 0.15f
                    }

                    val contribution = (tactic.defaultWeight * weightFactor).toInt().coerceAtLeast(2)

                    detected.add(
                        RiskSignal(
                            id = UUID.randomUUID().toString(),
                            timestamp = event.timestamp,
                            tactic = tactic,
                            confidence = 0.92f,
                            riskContribution = contribution,
                            evidenceText = evidence,
                            source = event.speaker,
                            sessionId = event.sessionId
                        )
                    )
                    break // one signal per tactic category per event
                }
            }
        }

        return detected
    }

    /**
     * Compute the current cumulative risk score (0-100) taking into account
     * past signals, distinct tactic count (escalation multiplier), and temporal decay.
     */
    fun calculateCurrentScore(
        allSignals: List<RiskSignal>,
        sessionStartTime: Long,
        currentTime: Long = System.currentTimeMillis()
    ): Int {
        if (allSignals.isEmpty()) return 0

        // 1. Raw sum of contributions
        val rawSum = allSignals.sumOf { it.riskContribution }

        // 2. Escalation multiplier if multiple distinct tactics are present
        val distinctTactics = allSignals.map { it.tactic }.distinct().size
        val escalationMultiplier = when {
            distinctTactics >= 4 -> 1.35f
            distinctTactics >= 2 -> 1.15f
            else -> 1.0f
        }

        var score = (rawSum * escalationMultiplier).toInt()

        // 3. Temporal decay: if last detected signal was long ago, apply decay
        val latestSignalTime = allSignals.maxOfOrNull { it.timestamp } ?: sessionStartTime
        val quietSeconds = ((currentTime - latestSignalTime) / 1000L).coerceAtLeast(0L)
        val decayPoints = ((quietSeconds / 45L) * 2).toInt() // -2 points per 45s of quiet

        // Baseline floor: cannot decay below the baseline of highest irreversible tactic
        val hasIrreversible = allSignals.any { it.tactic.isIrreversibleAction }
        val floor = if (hasIrreversible) 50 else (distinctTactics * 10).coerceAtMost(40)

        score = (score - decayPoints).coerceAtLeast(floor)

        return score.coerceIn(0, 100)
    }

    fun getRiskLevel(score: Int): RiskLevel = when {
        score >= criticalThreshold -> RiskLevel.CRITICAL
        score >= highThreshold -> RiskLevel.HIGH
        score >= lowThreshold -> RiskLevel.MEDIUM
        else -> RiskLevel.LOW
    }

    /**
     * Checks whether the Safety Brake intervention must be activated.
     * Required when:
     * (Score >= highThreshold) AND (Context includes payment/OTP/credential/remote access pressure)
     */
    fun isSafetyBrakeTriggered(score: Int, signals: List<RiskSignal>): Boolean {
        val highRisk = score >= highThreshold
        val hasIrreversiblePressure = signals.any { it.tactic.isIrreversibleAction }
        return highRisk && hasIrreversiblePressure
    }
}
