package com.rakshacall.safety.domain.provider

import kotlinx.coroutines.flow.Flow

enum class SpeechState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    ANALYZING,
    ERROR,
    UNAVAILABLE
}

data class SpeechChunk(
    val id: String,
    val timestamp: Long,
    val text: String,
    val isFinal: Boolean,
    val confidence: Float = 1.0f
)

interface SpeechProvider {
    val state: Flow<SpeechState>
    val transcripts: Flow<SpeechChunk>
    fun startListening(languageLocale: String = "en-IN")
    fun stopListening()
    fun destroy()
}
