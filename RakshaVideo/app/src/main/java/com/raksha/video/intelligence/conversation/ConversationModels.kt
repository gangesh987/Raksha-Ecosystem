package com.raksha.video.intelligence.conversation

enum class SpeakerContext { LOCAL, REMOTE, UNKNOWN }

data class TranscriptEvent(
    val eventId: String,
    val speaker: SpeakerContext,
    val text: String,
    val confidence: Float?,
    val timestamp: Long
)

data class ConversationRiskSignal(
    val type: String,
    val confidence: Float,
    val timestamp: Long,
    val source: String,
    val evidenceRef: String? = null
)

enum class ConversationStage { NEUTRAL, CONTACT, AUTHORITY, URGENCY, FEAR, ISOLATION, FINANCIAL_REQUEST, CREDENTIAL_REQUEST }

enum class ConversationState { DISABLED, READY, PROCESSING, UNAVAILABLE }

interface SpeechRecognitionEngine {
    suspend fun start()
    suspend fun stop()
    fun processAudio(audio: ShortArray)
    fun observeTranscript(): kotlinx.coroutines.flow.Flow<TranscriptEvent>
}

class UnavailableSpeechRecognitionEngine : SpeechRecognitionEngine {
    private val flow = kotlinx.coroutines.flow.emptyFlow<TranscriptEvent>()
    override suspend fun start() = Unit
    override suspend fun stop() = Unit
    override fun processAudio(audio: ShortArray) = Unit
    override fun observeTranscript() = flow
}
