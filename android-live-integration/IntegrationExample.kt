// Example wiring inside the existing ProtectionViewModel / ForegroundService.
// Keep microphone/camera permission and lifecycle handling in the app layer.
/*
val realtime = RakshaRealtimeClient(
    baseUrl = BuildConfig.RAKSHA_API_BASE,
    sessionId = sessionId,
    onMessage = { json ->
        // Decode: ai_status, transcript, ai_analysis, risk_update.
        // Persist risk_update into the existing Room repositories.
    },
    onState = { connected -> _aiConnected.value = connected }
)

fun startProtection() {
    realtime.connect()
    speechBridge.start()
}

speechBridge = SpeechRecognitionBridge(context,
    onText = { text ->
        realtime.sendTranscript(text)
        transcriptRepository.append(text)
    },
    onState = { listening -> _listening.value = listening }
)
*/
