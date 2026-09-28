package com.rakshacall.safety.data.provider

import android.content.Context
import com.rakshacall.safety.core.speech.SpeechRecognitionManager
import com.rakshacall.safety.domain.provider.SpeechChunk
import com.rakshacall.safety.domain.provider.SpeechProvider
import com.rakshacall.safety.domain.provider.SpeechState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AndroidSpeechProvider(
    private val context: Context
) : SpeechProvider {

    private val _state = MutableStateFlow(SpeechState.IDLE)
    override val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val _transcripts = MutableSharedFlow<SpeechChunk>(extraBufferCapacity = 64)
    override val transcripts: SharedFlow<SpeechChunk> = _transcripts.asSharedFlow()

    private var speechManager: SpeechRecognitionManager? = null

    override fun startListening(languageLocale: String) {
        _state.value = SpeechState.LISTENING
        if (speechManager == null) {
            speechManager = SpeechRecognitionManager(context) { text, isFinal ->
                if (text.isNotBlank()) {
                    _state.value = if (isFinal) SpeechState.ANALYZING else SpeechState.TRANSCRIBING
                    _transcripts.tryEmit(
                        SpeechChunk(
                            id = UUID.randomUUID().toString(),
                            timestamp = System.currentTimeMillis(),
                            text = text,
                            isFinal = isFinal
                        )
                    )
                }
            }
        }
        speechManager?.startListening()
    }

    override fun stopListening() {
        speechManager?.stopListening()
        _state.value = SpeechState.IDLE
    }

    override fun destroy() {
        speechManager?.destroy()
        speechManager = null
        _state.value = SpeechState.IDLE
    }
}
