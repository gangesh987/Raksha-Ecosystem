package com.rakshacall.safety.core.export

import android.content.Context
import android.content.Intent
import com.rakshacall.safety.core.security.EvidenceHasher
import com.rakshacall.safety.domain.model.EvidenceEvent
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.TranscriptEvent
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IncidentReport(
    val rakshaCallId: String,
    val session: ProtectionSession,
    val transcripts: List<TranscriptEvent>,
    val riskSignals: List<RiskSignal>,
    val evidenceChain: List<EvidenceEvent>,
    val integrityValid: Boolean,
    val formattedText: String,
    val jsonString: String
)

object IncidentReportExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun generateReport(
        rakshaCallId: String,
        session: ProtectionSession,
        transcripts: List<TranscriptEvent>,
        riskSignals: List<RiskSignal>,
        evidenceChain: List<EvidenceEvent>
    ): IncidentReport {
        val integrityResult = EvidenceHasher.verifyChain(evidenceChain)
        val isChainValid = integrityResult is com.rakshacall.safety.core.security.IntegrityResult.Valid

        // Build JSON representation
        val rootJson = JSONObject().apply {
            put("application", "RakshaCall AI Safety Layer")
            put("rakshaCallId", rakshaCallId)
            put("generatedAt", dateFormat.format(Date()))
            put("session", JSONObject().apply {
                put("sessionId", session.id)
                put("startTime", dateFormat.format(Date(session.startTime)))
                put("endTime", session.endTime?.let { dateFormat.format(Date(it)) } ?: "Ongoing")
                put("status", session.status.name)
                put("peakRisk", session.peakRisk)
                put("finalRisk", session.finalRisk)
                put("highestStage", session.highestStage.name)
                put("safetyBrakeTriggered", session.safetyBrakeTriggered)
                put("totalTacticsDetected", session.totalTacticsDetected)
            })

            put("integrity", JSONObject().apply {
                put("status", if (isChainValid) "VALID" else "FAILED")
                put("tamperEvidentChainLength", evidenceChain.size)
                put("genesisHash", evidenceChain.firstOrNull()?.previousHash ?: "N/A")
                put("finalHash", evidenceChain.lastOrNull()?.currentHash ?: "N/A")
            })

            put("riskSignals", JSONArray().apply {
                for (sig in riskSignals) {
                    put(JSONObject().apply {
                        put("id", sig.id)
                        put("timestamp", dateFormat.format(Date(sig.timestamp)))
                        put("tactic", sig.tactic.displayName)
                        put("riskContribution", sig.riskContribution)
                        put("evidenceText", sig.evidenceText)
                    })
                }
            })

            put("transcripts", JSONArray().apply {
                for (tr in transcripts) {
                    put(JSONObject().apply {
                        put("timestamp", dateFormat.format(Date(tr.timestamp)))
                        put("speaker", tr.speaker)
                        put("text", tr.text)
                    })
                }
            })
        }

        // Build human-readable formatted report
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("RAKSHACALL INCIDENT REPORT")
        sb.appendLine("Tamper-Evident Digital-Arrest & Coercion Log")
        sb.appendLine("==================================================")
        sb.appendLine("RakshaCall ID: $rakshaCallId")
        sb.appendLine("Session ID:    ${session.id}")
        sb.appendLine("Date/Time:     ${dateFormat.format(Date(session.startTime))}")
        sb.appendLine("Status:        ${session.status.name}")
        sb.appendLine("Peak Risk:     ${session.peakRisk}/100")
        sb.appendLine("Final Risk:    ${session.finalRisk}/100")
        sb.appendLine("Highest Stage: ${session.highestStage.displayName}")
        sb.appendLine("Safety Brake:  ${if (session.safetyBrakeTriggered) "ACTIVATED" else "Not triggered"}")
        sb.appendLine("Chain Status:  ${if (isChainValid) "VALID (Integrity Verified)" else "FAILED (Tamper Detected)"}")
        sb.appendLine()
        sb.appendLine("---------------- DETECTED TACTICS ----------------")
        if (riskSignals.isEmpty()) {
            sb.appendLine("No coercive tactics detected in this session.")
        } else {
            for (sig in riskSignals) {
                sb.appendLine("[${dateFormat.format(Date(sig.timestamp))}] +${sig.riskContribution} ${sig.tactic.displayName}")
                sb.appendLine("   Evidence: \"${sig.evidenceText}\"")
            }
        }
        sb.appendLine()
        sb.appendLine("---------------- LIVE TRANSCRIPT -----------------")
        if (transcripts.isEmpty()) {
            sb.appendLine("No transcript events recorded.")
        } else {
            for (t in transcripts) {
                sb.appendLine("[${dateFormat.format(Date(t.timestamp))}] ${t.speaker}: ${t.text}")
            }
        }
        sb.appendLine()
        sb.appendLine("---------------- EVIDENCE INTEGRITY --------------")
        sb.appendLine("Total Block Events: ${evidenceChain.size}")
        sb.appendLine("Final SHA-256 Hash: ${evidenceChain.lastOrNull()?.currentHash ?: "None"}")
        sb.appendLine("Generated by RakshaCall On-Device Privacy Safety Layer.")

        return IncidentReport(
            rakshaCallId = rakshaCallId,
            session = session,
            transcripts = transcripts,
            riskSignals = riskSignals,
            evidenceChain = evidenceChain,
            integrityValid = isChainValid,
            formattedText = sb.toString(),
            jsonString = rootJson.toString(2)
        )
    }

    fun shareReport(context: Context, reportText: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "RakshaCall Incident Report")
            putExtra(Intent.EXTRA_TEXT, reportText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Incident Report"))
    }
}
