package com.rakshacall.safety.core.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechState {
    data object Idle : SpeechState()
    data object Listening : SpeechState()
    data class Recognized(val text: String, val isFinal: Boolean) : SpeechState()
    data class Error(val message: String) : SpeechState()
    data object Unavailable : SpeechState()
}

/**
 * Manages Android SpeechRecognizer for live microphone audio chunk analysis.
 * Does NOT permanently record or save raw audio to disk (privacy by design).
 */
class SpeechRecognitionManager(
    private val context: Context,
    private val onTranscriptReady: (text: String, isFinal: Boolean) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var isListening = false

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (!isAvailable()) {
            _state.value = SpeechState.Unavailable
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            isListening = true
            speechRecognizer?.startListening(intent)
            _state.value = SpeechState.Listening
        } catch (e: Exception) {
            _state.value = SpeechState.Error(e.message ?: "Failed to start speech recognition")
        }
    }

    fun stopListening() {
        isListening = false
        try {
            speechRecognizer?.stopListening()
            _state.value = SpeechState.Idle
        } catch (_: Exception) {}
    }

    fun destroy() {
        isListening = false
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            _state.value = SpeechState.Idle
        } catch (_: Exception) {}
    }

    private fun restartIfActive() {
        if (isListening) {
            startListening()
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = SpeechState.Listening
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network required for cloud speech recognition"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server recognition error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Recognition error ($error)"
            }

            // For timeout or no-match, automatically restart if session is active
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                restartIfActive()
            } else {
                _state.value = SpeechState.Error(message)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim()
            if (!recognizedText.isNullOrEmpty()) {
                _state.value = SpeechState.Recognized(recognizedText, isFinal = true)
                onTranscriptReady(recognizedText, true)
            }
            restartIfActive()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = matches?.firstOrNull()?.trim()
            if (!partialText.isNullOrEmpty()) {
                _state.value = SpeechState.Recognized(partialText, isFinal = false)
                onTranscriptReady(partialText, false)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
