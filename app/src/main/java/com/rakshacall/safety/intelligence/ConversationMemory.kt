package com.rakshacall.safety.intelligence

import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.TranscriptEvent
import java.util.concurrent.ConcurrentHashMap

import com.rakshacall.safety.intelligence.intent.EntityType

data class CallSegment(
    val id: String,
    val timestamp: Long,
    val speaker: String,
    val text: String,
    val detectedLanguage: String,
    val tactics: List<RiskSignal>
)

data class SessionMemory(
    val sessionId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val segments: MutableList<CallSegment> = mutableListOf(),
    val cumulativeTactics: MutableList<RiskSignal> = mutableListOf(),
    val cumulativeEntities: MutableSet<EntityType> = mutableSetOf(),
    val detectedLanguages: MutableSet<String> = mutableSetOf(),
    var currentStage: ScamStage = ScamStage.CONTACT,
    var currentRiskScore: Int = 0
)

/**
 * Short-lived in-memory conversational context store per call.
 * Retains rolling context window, enables cross-turn pronoun and intent resolution,
 * and clears on call termination to adhere to zero-retention privacy principles.
 */
class ConversationMemory(private val maxWindowSegments: Int = 20) {

    private val sessions = ConcurrentHashMap<String, SessionMemory>()

    fun getOrCreateSession(sessionId: String): SessionMemory {
        return sessions.computeIfAbsent(sessionId) {
            SessionMemory(sessionId = it)
        }
    }

    fun recordTurn(
        event: TranscriptEvent,
        language: String,
        signals: List<RiskSignal>
    ): SessionMemory {
        val session = getOrCreateSession(event.sessionId)
        synchronized(session) {
            val segment = CallSegment(
                id = event.id,
                timestamp = event.timestamp,
                speaker = event.speaker,
                text = event.text,
                detectedLanguage = language,
                tactics = signals
            )
            session.segments.add(segment)
            if (session.segments.size > maxWindowSegments) {
                session.segments.removeAt(0)
            }

            session.cumulativeTactics.addAll(signals)
            session.detectedLanguages.add(language)
        }
        return session
    }

    fun getPriorTactics(sessionId: String): List<RiskSignal> {
        val session = sessions[sessionId] ?: return emptyList()
        synchronized(session) {
            return session.cumulativeTactics.toList()
        }
    }

    fun updateStageAndRisk(sessionId: String, stage: ScamStage, score: Int) {
        sessions[sessionId]?.let { session ->
            synchronized(session) {
                session.currentStage = stage
                session.currentRiskScore = score
            }
        }
    }

    fun generateSessionSummary(sessionId: String): String {
        val session = sessions[sessionId] ?: return "No session context available."
        synchronized(session) {
            val tactics = session.cumulativeTactics.map { it.tactic.displayName }.distinct()
            val entities = session.cumulativeEntities.map { it.name }.distinct()
            val stages = session.currentStage.displayName

            val parts = mutableListOf<String>()
            if (tactics.isNotEmpty()) {
                parts.add("Observed Coercive Patterns: ${tactics.joinToString(", ")}.")
            }
            if (entities.isNotEmpty()) {
                parts.add("Referenced Entities: ${entities.joinToString(", ")}.")
            }
            parts.add("Current Progression: $stages (Risk: ${session.currentRiskScore}/100).")
            return parts.joinToString(" ")
        }
    }

    fun clearSession(sessionId: String) {
        sessions.remove(sessionId)
    }

    fun clearAll() {
        sessions.clear()
    }
}
