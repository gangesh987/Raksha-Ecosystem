package com.rakshacall.safety.intelligence

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale

enum class SpeakerType(val label: String) {
    LOCAL_USER("LOCAL"),
    REMOTE_CALLER("REMOTE"),
    UNKNOWN("UNKNOWN")
}

data class SpeechTranscript(
    val text: String,
    val isFinal: Boolean,
    val speaker: SpeakerType = SpeakerType.REMOTE_CALLER,
    val confidence: Float = 0.92f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Real-time Speech Recognition Manager using Android's native SpeechRecognizer.
 * Yields continuous partial and final transcripts during audio/video calls.
 */
class SpeechRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _transcriptFlow = MutableSharedFlow<SpeechTranscript>(extraBufferCapacity = 64)
    val transcriptFlow: SharedFlow<SpeechTranscript> = _transcriptFlow.asSharedFlow()

    private var isListening = false

    fun startListening(languageLocale: Locale = Locale.getDefault()) {
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    // Automatically restart listening if still in active call mode
                    if (isListening) {
                        runCatching {
                            speechRecognizer?.cancel()
                            speechRecognizer?.startListening(createIntent(languageLocale))
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrBlank()) {
                        _transcriptFlow.tryEmit(SpeechTranscript(text = text, isFinal = true))
                    }
                    if (isListening) {
                        runCatching { speechRecognizer?.startListening(createIntent(languageLocale)) }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim()
                    if (!text.isNullOrBlank()) {
                        _transcriptFlow.tryEmit(SpeechTranscript(text = text, isFinal = false))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = createIntent(languageLocale)
        runCatching { speechRecognizer?.startListening(intent) }
        isListening = true
    }

    private fun createIntent(locale: Locale): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }

    fun stopListening() {
        isListening = false
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }
}
