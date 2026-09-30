package com.rakshacall.safety.presentation.protection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.rakshacall.R
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.evidence.EvidenceSource
import com.rakshacall.safety.evidence.EvidenceVault
import com.rakshacall.safety.intelligence.ProtectionDecision
import com.rakshacall.safety.intelligence.RakshaAIOrchestrator
import com.rakshacall.safety.intelligence.SpeechRecognitionManager
import com.rakshacall.safety.intelligence.visual.FrameSampler
import com.rakshacall.safety.intelligence.visual.VisualThreatDetector
import com.rakshacall.safety.presentation.theme.*
import com.rakshacall.safety.protection.*
import com.rakshacall.safety.webrtc.AudioManagerHelper
import com.rakshacall.safety.webrtc.CallLifecycleState
import com.rakshacall.safety.webrtc.WebRtcConfig
import com.rakshacall.safety.webrtc.WebRtcEngine
import com.rakshacall.safety.webrtc.WebRtcState
import com.rakshacall.safety.signaling.SignalingClient
import com.rakshacall.safety.intelligence.audio.RealtimeAudioPipeline
import com.rakshacall.safety.intelligence.events.MultimodalEventBus
import com.rakshacall.safety.intelligence.events.EventSource
import com.rakshacall.safety.intelligence.events.EventType
import com.rakshacall.safety.intelligence.telemetry.PerformanceTelemetry
import com.rakshacall.safety.intelligence.action.UserActionTracker
import com.rakshacall.safety.intelligence.action.SafetyUserActionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeCallScreen(
    callId: String = "RC-" + String.format("%06d", (100000..999999).random()),
    isCaller: Boolean = true,
    onEndCall: () -> Unit,
    onOpenVerificationCoach: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ── PERMISSION CHECK ──
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val hasMicPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    // ── CRASH-SAFE INITIALIZATION ──
    var initError by remember { mutableStateOf<String?>(null) }

    // 1. Audio & Media Managers (safe — no native lib needed)
    val audioHelper = remember { runCatching { AudioManagerHelper(context) }.getOrNull() }
    val speechManager = remember { runCatching { SpeechRecognitionManager(context) }.getOrNull() }
    val realtimeAudioPipeline = remember { runCatching { RealtimeAudioPipeline(callId) }.getOrNull() }
    val multimodalEventBus = remember { MultimodalEventBus(callId) }
    val aiOrchestrator = remember { RakshaAIOrchestrator() }
    val evidenceVault = remember { EvidenceVault() }
    val visualThreatDetector = remember { VisualThreatDetector() }
    var currentAudioRms by remember { mutableFloatStateOf(0.0f) }
    var isUserSpeaking by remember { mutableStateOf(false) }
    var videoScansCount by remember { mutableIntStateOf(0) }
    var riskHistory by remember { mutableStateOf<List<Int>>(listOf(0)) }

    // 2. Protection Controller
    val protectionController = remember {
        ProtectionController(
            initialPolicy = ProtectionPolicy(
                mode = ProtectionMode.STRONG_PROTECTION,
                autoProtectEnabled = false,
                countdownSeconds = 5,
                uiMode = UIMode.ADVANCED
            ),
            onAutoTerminate = {
                evidenceVault.recordEvent(
                    sessionId = callId,
                    eventType = "AUTO_PROTECT_TERMINATED",
                    source = EvidenceSource.SYSTEM,
                    description = "Automated call protection activated."
                )
                onEndCall()
            }
        )
    }

    val protectionState by protectionController.state.collectAsState()
    val countdownSeconds by protectionController.countdownSecondsRemaining.collectAsState()
    val evidenceStrength by protectionController.evidenceStrength.collectAsState()
    val policy by protectionController.policy.collectAsState()

    // 3. Telemetry & States
    val telemetry = remember { PerformanceTelemetry() }
    val userActionTracker = remember { UserActionTracker() }
    var callLifecycleState by remember { mutableStateOf(if (isCaller) CallLifecycleState.OUTGOING else CallLifecycleState.RINGING) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    val telemetrySnapshot by telemetry.snapshot.collectAsState()

    var remoteVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var localVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var webRtcState by remember { mutableStateOf<WebRtcState>(WebRtcState.New) }

    var isMicMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var isSpeakerphoneOn by remember { mutableStateOf(true) }
    var callDurationSeconds by remember { mutableLongStateOf(0L) }

    // 4. AI Safety States
    var latestDecision by remember { mutableStateOf<ProtectionDecision?>(null) }
    var showSafetyBrakeDialog by remember { mutableStateOf(false) }
    var isCallPaused by remember { mutableStateOf(false) }
    var showEvidenceSheet by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showTranscriptPanel by remember { mutableStateOf(false) }
    var recentTranscripts by remember { mutableStateOf<List<String>>(emptyList()) }
    var visualThreatCount by remember { mutableIntStateOf(0) }
    var signalingClient: SignalingClient? by remember { mutableStateOf(null) }

    // 5. CRASH-SAFE WebRTC Engine Initialization
    val webRtcEngine = remember {
        try {
            val config = WebRtcConfig()
            WebRtcEngine(
                context = context,
                config = config,
                onLocalIceCandidate = { candidate ->
                    signalingClient?.sendIceCandidate(
                        callId = callId,
                        sdpMid = candidate.sdpMid,
                        sdpMLineIndex = candidate.sdpMLineIndex,
                        candidate = candidate.sdp
                    )
                },
                onRemoteVideo = { track ->
                    remoteVideoTrack = track
                    callLifecycleState = CallLifecycleState.CONNECTED
                },
                onConnectionStateChange = { newState ->
                    webRtcState = newState
                    callLifecycleState = when (newState) {
                        WebRtcState.Connected -> CallLifecycleState.CONNECTED
                        WebRtcState.Connecting -> CallLifecycleState.CONNECTING
                        WebRtcState.Disconnected -> CallLifecycleState.RECONNECTING
                        WebRtcState.Closed, WebRtcState.Failed -> CallLifecycleState.DISCONNECTED
                        else -> callLifecycleState
                    }
                }
            )
        } catch (e: Exception) {
            initError = e.message ?: "WebRTC initialization failed"
            null
        }
    }

    // 6. Lifecycle Management with full crash protection
    DisposableEffect(callId) {
        try {
            audioHelper?.startCallAudio(speakerphone = isSpeakerphoneOn)
        } catch (_: Exception) {}

        try {
            webRtcEngine?.initialize()
            localVideoTrack = webRtcEngine?.localVideoTrack()
        } catch (e: Exception) {
            initError = "Camera initialization failed: ${e.message}"
        }

        try {
            if (hasMicPermission) {
                speechManager?.startListening()
                realtimeAudioPipeline?.startCapture()
            }
        } catch (_: Exception) {}

        // Signaling Client Setup (safe connect with runCatching)
        val signalingUrl = com.rakshacall.safety.data.remote.config.AppConfig.DEFAULT_BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "/api/ws/sessions/$callId"

        val client = SignalingClient(
            serverUrl = signalingUrl,
            token = "",
            listener = object : SignalingClient.Listener {
                override fun onOpen() {
                    coroutineScope.launch {
                        callLifecycleState = CallLifecycleState.CONNECTING
                        if (isCaller) {
                            webRtcEngine?.createOffer { offer ->
                                signalingClient?.sendSdpOffer(callId, offer.description)
                            }
                        }
                    }
                }
                override fun onClosed() {
                    coroutineScope.launch {
                        callLifecycleState = CallLifecycleState.ENDED
                    }
                }
                override fun onFailure(message: String) {
                    // Graceful fallback — app continues in local preview mode
                }
                override fun onMessage(type: String, payload: JSONObject) {
                    coroutineScope.launch {
                        when (type) {
                            "sdp_offer" -> {
                                val sdp = payload.optString("sdp")
                                if (sdp.isNotBlank()) {
                                    val desc = SessionDescription(SessionDescription.Type.OFFER, sdp)
                                    webRtcEngine?.setRemoteDescription(desc) {
                                        webRtcEngine.createAnswer { answer ->
                                            signalingClient?.sendSdpAnswer(callId, answer.description)
                                        }
                                    }
                                }
                            }
                            "sdp_answer" -> {
                                val sdp = payload.optString("sdp")
                                if (sdp.isNotBlank()) {
                                    val desc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                                    webRtcEngine?.setRemoteDescription(desc) {}
                                }
                            }
                            "ice_candidate" -> {
                                val sdpMid = payload.optString("sdp_mid")
                                val sdpMLineIndex = payload.optInt("sdp_mline_index")
                                val candidateStr = payload.optString("candidate")
                                if (candidateStr.isNotBlank()) {
                                    val candidate = IceCandidate(sdpMid, sdpMLineIndex, candidateStr)
                                    webRtcEngine?.addIceCandidate(candidate)
                                }
                            }
                            "call_end" -> {
                                callLifecycleState = CallLifecycleState.ENDED
                                onEndCall()
                            }
                        }
                    }
                }
            }
        )
        signalingClient = client
        runCatching { client.connect() }

        evidenceVault.recordEvent(
            sessionId = callId,
            eventType = "CALL_STARTED",
            source = EvidenceSource.SYSTEM,
            description = "Protected call session started. Policy: ${policy.mode}"
        )

        val timerJob = coroutineScope.launch {
            while (isActive) {
                delay(1000L)
                callDurationSeconds++
            }
        }

        onDispose {
            timerJob.cancel()
            try { realtimeAudioPipeline?.stopCapture() } catch (_: Exception) {}
            runCatching { speechManager?.stopListening() }
            runCatching { signalingClient?.sendCallEnd(callId) }
            runCatching { signalingClient?.close() }
            signalingClient = null
            runCatching { aiOrchestrator.endSession(callId) }
            runCatching { protectionController.reset() }
            runCatching { audioHelper?.stopCallAudio() }
            runCatching { webRtcEngine?.release() }

            evidenceVault.recordEvent(
                sessionId = callId,
                eventType = "CALL_ENDED",
                source = EvidenceSource.SYSTEM,
                description = "Call ended. Resources released."
            )
        }
    }

    // 7. Adaptive Video Frame Sampling (samples remote track or active local preview)
    val frameSampler = remember { FrameSampler("video_stream", 1500L) }
    val activeSamplingTrack = remoteVideoTrack ?: if (!isCameraOff) localVideoTrack else null
    DisposableEffect(activeSamplingTrack) {
        activeSamplingTrack?.addSink(frameSampler)
        onDispose { activeSamplingTrack?.removeSink(frameSampler) }
    }

    // 8. Speech Transcript Processing
    if (speechManager != null) {
        LaunchedEffect(speechManager) {
            telemetry.markAsrStart()
            speechManager.transcriptFlow.collectLatest { transcript ->
                telemetry.recordAsrCompleted()
                if (isCallPaused) return@collectLatest

                val speakerLabel = transcript.speaker.label
                val formattedTranscript = "[$speakerLabel]: ${transcript.text}"
                recentTranscripts = (recentTranscripts + formattedTranscript).takeLast(50)

                val event = TranscriptEvent(
                    id = UUID.randomUUID().toString(),
                    sessionId = callId,
                    timestamp = transcript.timestamp,
                    speaker = speakerLabel,
                    text = transcript.text
                )

                telemetry.markAiStart()
                val decision = aiOrchestrator.processTranscript(
                    event = event,
                    isFinal = transcript.isFinal,
                    visualRiskBonus = visualThreatCount * 10,
                    visualThreatCount = visualThreatCount
                )
                telemetry.recordAiCompleted()

                latestDecision = decision
                riskHistory = (riskHistory + decision.riskScore).takeLast(25)
                protectionController.evaluate(decision, visualThreatCount)

                multimodalEventBus.emitEvent(
                    source = EventSource.SPEECH_RECOGNITION,
                    eventType = EventType.TRANSCRIPT_CHUNK,
                    payload = mapOf(
                        "speaker" to speakerLabel,
                        "text" to transcript.text,
                        "isFinal" to transcript.isFinal,
                        "riskScore" to decision.riskScore,
                        "stage" to decision.currentStage.name
                    )
                )

                val score = decision.riskScore
                val adaptiveInterval = when {
                    score >= 80 -> 400L
                    score >= 40 -> 1000L
                    else -> 2000L
                }
                frameSampler.setAdaptiveInterval(adaptiveInterval)

                if (transcript.isFinal) {
                    evidenceVault.recordEvent(
                        sessionId = callId,
                        eventType = "SPEECH_TRANSCRIPT",
                        source = EvidenceSource.TRANSCRIPT,
                        language = decision.primaryLanguage,
                        stage = decision.currentStage.name,
                        riskScore = decision.riskScore,
                        confidence = decision.confidence,
                        description = formattedTranscript
                    )
                }

                if (decision.safetyBrakeTriggered && !showSafetyBrakeDialog && policy.mode != ProtectionMode.MONITOR) {
                    showSafetyBrakeDialog = true
                    try {
                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // 9. Visual Threat Processing
    LaunchedEffect(frameSampler) {
        frameSampler.samples.collectLatest { sample ->
            videoScansCount++
            val threat = visualThreatDetector.analyzeFrame(sample)
            if (threat != null) {
                visualThreatCount++
                val decision = aiOrchestrator.processVisualThreat(threat, callId)
                if (decision != null) {
                    latestDecision = decision
                    riskHistory = (riskHistory + decision.riskScore).takeLast(25)
                    protectionController.evaluate(decision, visualThreatCount)
                }
                multimodalEventBus.emitEvent(
                    source = EventSource.CAMERA_X,
                    eventType = EventType.VISUAL_CONTEXT_SIGNAL,
                    payload = mapOf(
                        "threatType" to threat.type.name,
                        "confidence" to threat.confidence,
                        "bonus" to threat.riskScoreBonus,
                        "desc" to threat.description
                    )
                )
                evidenceVault.recordEvent(
                    sessionId = callId,
                    eventType = "VISUAL_THREAT_DETECTED",
                    source = EvidenceSource.VISUAL,
                    riskScore = threat.riskScoreBonus,
                    description = threat.description
                )
            }
        }
    }

    // 10. Live 16kHz PCM Audio Pipeline & Acoustic Telemetry
    if (realtimeAudioPipeline != null) {
        LaunchedEffect(realtimeAudioPipeline) {
            launch {
                realtimeAudioPipeline.acousticMetrics.collectLatest { metrics ->
                    currentAudioRms = metrics.rmsEnergy
                    isUserSpeaking = metrics.isSpeech
                    aiOrchestrator.updateAcousticMetrics(metrics)
                    multimodalEventBus.emitEvent(
                        source = EventSource.AUDIO_CAPTURE,
                        eventType = EventType.AUDIO_METRICS_UPDATE,
                        payload = mapOf(
                            "rms" to metrics.rmsEnergy,
                            "isSpeech" to metrics.isSpeech,
                            "speechRate" to metrics.speechActivityRate,
                            "centroid" to metrics.spectralCentroid,
                            "flux" to metrics.spectralFlux
                        )
                    )
                }
            }
            launch {
                realtimeAudioPipeline.audioChunks.collect { chunk ->
                    signalingClient?.let { sc ->
                        if (sc.isSocketConnected()) {
                            val b64 = android.util.Base64.encodeToString(chunk.pcmData, android.util.Base64.NO_WRAP)
                            val json = JSONObject().apply {
                                put("type", "audio_pcm16")
                                put("data", b64)
                                put("sample_rate", chunk.sampleRate)
                            }
                            sc.sendRaw(json)
                        }
                    }
                }
            }
        }
    }

    // ── COMPUTED VALUES ──
    val riskScore = latestDecision?.riskScore ?: 0
    val riskLevel = latestDecision?.riskLevel ?: RiskLevel.LOW
    val stage = latestDecision?.currentStage ?: ScamStage.CONTACT
    val velocity = latestDecision?.manipulationVelocity ?: 0.0f
    val primaryLanguage = latestDecision?.primaryLanguage ?: "en"
    val formattedDuration = String.format("%02d:%02d", callDurationSeconds / 60, callDurationSeconds % 60)

    val riskColor = when (riskLevel) {
        RiskLevel.LOW -> RiskLow
        RiskLevel.MEDIUM -> RiskMedium
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.CRITICAL -> RiskCritical
    }

    val riskBgColor = when (riskLevel) {
        RiskLevel.LOW -> RiskLowBg
        RiskLevel.MEDIUM -> RiskMediumBg
        RiskLevel.HIGH -> RiskHighBg
        RiskLevel.CRITICAL -> RiskCriticalBg
    }

    val lifecycleStatusText = when (callLifecycleState) {
        CallLifecycleState.OUTGOING -> stringResource(R.string.call_connecting)
        CallLifecycleState.RINGING -> stringResource(R.string.call_connecting)
        CallLifecycleState.CONNECTING -> stringResource(R.string.call_connecting)
        CallLifecycleState.CONNECTED -> stringResource(R.string.call_connected)
        CallLifecycleState.RECONNECTING -> stringResource(R.string.call_reconnecting)
        CallLifecycleState.ENDED -> stringResource(R.string.call_ended)
        CallLifecycleState.DISCONNECTED -> stringResource(R.string.call_ended)
    }

    // ═══════════════════════════════════════════════════════════
    //                    MAIN UI LAYOUT
    // ═══════════════════════════════════════════════════════════

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1120))
    ) {
        // ── LAYER 1: VIDEO VIEWPORT ──
        if (webRtcEngine != null && remoteVideoTrack != null) {
            WebRtcVideoView(
                videoTrack = remoteVideoTrack!!,
                eglContext = webRtcEngine?.getEglBaseContext(),
                modifier = Modifier.fillMaxSize()
            )
        } else if (webRtcEngine != null && localVideoTrack != null && !isCameraOff) {
            // Live local camera feed rendered full-screen until remote peer connects
            WebRtcVideoView(
                videoTrack = localVideoTrack!!,
                eglContext = webRtcEngine?.getEglBaseContext(),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Standby / Connecting View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0B1120), Color(0xFF131D3F), Color(0xFF1C2B59))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Pulsating shield icon
                    Surface(
                        shape = CircleShape,
                        color = TealPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(120.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    if (initError != null) {
                        Text(
                            "⚠ ${initError}",
                            color = RiskMedium,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.call_protection_active),
                            color = TealPrimary,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            lifecycleStatusText,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.call_protection_active),
                            color = TealPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // ── LAYER 2: TOP GRADIENT OVERLAY ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )

        // ── LAYER 3: TOP STATUS BAR ──
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .statusBarsPadding()
        ) {
            // Header Row: Call ID + HUD Toggle + End Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection indicator
                Surface(
                    shape = CircleShape,
                    color = when {
                        webRtcState == WebRtcState.Connected -> Color(0xFF4CAF50)
                        callLifecycleState == CallLifecycleState.CONNECTING -> Color(0xFFFF9800)
                        else -> Color(0xFF78909C)
                    },
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "RAKSHACALL",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "$formattedDuration • $callId",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                // HUD Mode Toggle
                FilledTonalButton(
                    onClick = {
                        val nextMode = if (policy.uiMode == UIMode.ADVANCED) UIMode.SIMPLE_ELDERLY else UIMode.ADVANCED
                        protectionController.setUIMode(nextMode)
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        if (policy.uiMode == UIMode.ADVANCED) stringResource(R.string.call_hud_advanced) else stringResource(R.string.call_hud_simple),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── RISK INDICATOR CARD ──
            if (policy.uiMode == UIMode.ADVANCED) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = riskColor.copy(alpha = 0.92f),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Risk score circle
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "$riskScore",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${riskLevel.label} • ${evidenceStrength.name}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                "${stringResource(R.string.call_risk_stage)}: ${stage.name} • ${stringResource(R.string.call_lang_label)}: ${primaryLanguage.uppercase()} • ${stringResource(R.string.call_velocity_label)}: ${"%.1f".format(velocity)}x",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            )
                        }

                        IconButton(onClick = { showPolicyDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.policy_title), tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ── LIVE MULTIMODAL TELEMETRY ROW ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Audio live indicator
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isUserSpeaking) Color(0xFF1B5E20).copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isUserSpeaking) Color(0xFF00E676) else Color.Gray,
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                if (isUserSpeaking) "AUDIO: SPEECH (${(currentAudioRms * 100).toInt()})" else "AUDIO: MONITORING",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Vision scanning badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (visualThreatCount > 0) Color(0xFFB71C1C).copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (visualThreatCount > 0) Color(0xFFFF5252) else TealPrimary,
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                "VISION: $videoScansCount FRAMES" + if (visualThreatCount > 0) " (THREAT: $visualThreatCount)" else "",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ── LAYER 4: LOCAL PIP VIDEO ──
        if (remoteVideoTrack != null && localVideoTrack != null && !isCameraOff && webRtcEngine != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = if (policy.uiMode == UIMode.ADVANCED) 180.dp else 80.dp, end = 16.dp)
                    .size(width = 100.dp, height = 140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, TealPrimary.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .shadow(8.dp, RoundedCornerShape(14.dp))
            ) {
                WebRtcVideoView(
                    videoTrack = localVideoTrack!!,
                    eglContext = webRtcEngine?.getEglBaseContext(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ── LAYER 5: ELDERLY / SIMPLE PROTECTION MODE OVERLAY ──
        if (policy.uiMode == UIMode.SIMPLE_ELDERLY && (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL)) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFB71C1C).copy(alpha = 0.96f),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.elderly_scam_warning),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.brake_unusual_requests),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onEndCall,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.elderly_end_call), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            evidenceVault.recordEvent(
                                sessionId = callId,
                                eventType = "TRUSTED_CONTACT_ALERTED",
                                source = EvidenceSource.USER_ACTION,
                                description = "User requested emergency trusted contact notification."
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.elderly_contact_family), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { protectionController.cancelCountdownAndKeepCall() }) {
                        Text(stringResource(R.string.elderly_continue_call), color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                    }
                }
            }
        }

        // ── LAYER 6: LIVE TRANSCRIPT PANEL (Expandable) ──
        if (policy.uiMode == UIMode.ADVANCED && recentTranscripts.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 12.dp, end = 12.dp)
                    .fillMaxWidth()
            ) {
                // Expand/Collapse toggle
                Surface(
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = if (showTranscriptPanel) 0.dp else 14.dp, bottomEnd = if (showTranscriptPanel) 0.dp else 14.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTranscriptPanel = !showTranscriptPanel }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Subtitles, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.call_transcript_title),
                            color = TealPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${recentTranscripts.size} ${stringResource(R.string.label_active)}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (showTranscriptPanel) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Collapsed: show latest line only
                if (!showTranscriptPanel) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            recentTranscripts.last(),
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // Expanded: scrollable transcript list
                AnimatedVisibility(
                    visible = showTranscriptPanel,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val listState = rememberLazyListState()
                    LaunchedEffect(recentTranscripts.size) {
                        if (recentTranscripts.isNotEmpty()) listState.animateScrollToItem(recentTranscripts.size - 1)
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(recentTranscripts) { line ->
                                val isRemote = line.startsWith("[REMOTE")
                                Text(
                                    line,
                                    color = if (isRemote) Color(0xFFFF8A80) else Color(0xFF80CBC4),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── LAYER 7: BOTTOM GRADIENT ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )

        // ── LAYER 8: CALL CONTROLS BAR ──
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 20.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic Toggle
            CallControlButton(
                icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = stringResource(R.string.call_mic),
                isActive = !isMicMuted,
                activeColor = Color(0xFF334155),
                inactiveColor = Color(0xFFD32F2F),
                onClick = {
                    isMicMuted = !isMicMuted
                    if (isMicMuted) webRtcEngine?.muteMicrophone() else webRtcEngine?.unmuteMicrophone()
                }
            )

            // Camera Toggle
            CallControlButton(
                icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                label = stringResource(R.string.call_camera),
                isActive = !isCameraOff,
                activeColor = Color(0xFF334155),
                inactiveColor = Color(0xFFD32F2F),
                onClick = {
                    isCameraOff = !isCameraOff
                    if (isCameraOff) webRtcEngine?.disableCamera() else webRtcEngine?.enableCamera()
                }
            )

            // Switch Camera
            CallControlButton(
                icon = Icons.Default.Cameraswitch,
                label = stringResource(R.string.call_switch_cam),
                isActive = true,
                activeColor = Color(0xFF334155),
                inactiveColor = Color(0xFF334155),
                onClick = { webRtcEngine?.switchCamera() }
            )

            // Speaker
            CallControlButton(
                icon = if (isSpeakerphoneOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                label = stringResource(R.string.call_speaker),
                isActive = isSpeakerphoneOn,
                activeColor = TealPrimary,
                inactiveColor = Color(0xFF334155),
                onClick = {
                    isSpeakerphoneOn = !isSpeakerphoneOn
                    audioHelper?.setSpeakerphone(isSpeakerphoneOn)
                }
            )

            // Evidence
            CallControlButton(
                icon = Icons.Default.History,
                label = stringResource(R.string.call_evidence_btn),
                isActive = true,
                activeColor = Color(0xFF334155),
                inactiveColor = Color(0xFF334155),
                onClick = { showEvidenceSheet = true }
            )

            // Diagnostics
            CallControlButton(
                icon = Icons.Default.MonitorHeart,
                label = stringResource(R.string.call_diagnostics),
                isActive = true,
                activeColor = Color(0xFF334155),
                inactiveColor = Color(0xFF334155),
                onClick = { showDiagnosticsDialog = true }
            )

            // End Call
            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFE53935), CircleShape)
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = stringResource(R.string.btn_end_call), tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        // ── PROTECTION PENDING COUNTDOWN DIALOG ──
        if (protectionState == ProtectionState.PROTECTION_PENDING) {
            AlertDialog(
                onDismissRequest = { /* User must explicitly act */ },
                icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color.Red, modifier = Modifier.size(44.dp)) },
                title = {
                    Text(
                        stringResource(R.string.brake_auto_protect_title),
                        color = Color.Red,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            stringResource(R.string.brake_auto_protect_desc, riskScore, evidenceStrength.name),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            stringResource(R.string.brake_countdown, countdownSeconds),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53935)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { protectionController.cancelCountdownAndKeepCall() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64))
                    ) {
                        Text(stringResource(R.string.brake_keep_call))
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showDetailsDialog = true }) {
                            Text(stringResource(R.string.brake_view_details))
                        }
                        Button(
                            onClick = onEndCall,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text(stringResource(R.string.brake_end_now))
                        }
                    }
                }
            )
        }

        // ── SAFETY BRAKE DIALOG ──
        if (showSafetyBrakeDialog && protectionState != ProtectionState.PROTECTION_PENDING) {
            AlertDialog(
                onDismissRequest = { showSafetyBrakeDialog = false },
                icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(36.dp)) },
                title = {
                    Text(
                        "🛡 ${stringResource(R.string.safety_brake_title)}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                },
                text = {
                    Column {
                        Text(
                            stringResource(R.string.safety_brake_desc),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(stringResource(R.string.brake_why_flagged), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                        latestDecision?.explanations?.forEach { reason ->
                            Text("• $reason", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        latestDecision?.confidence?.let {
                            Text(
                                "${stringResource(R.string.brake_confidence)}: ${"%.0f".format(it * 100)}%",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isCallPaused = !isCallPaused
                            showSafetyBrakeDialog = false
                            if (isCallPaused) {
                                userActionTracker.recordAction(SafetyUserActionType.PRESSED_SAFETY_BRAKE_PAUSE)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text(if (isCallPaused) stringResource(R.string.btn_resume) else stringResource(R.string.btn_pause))
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            showSafetyBrakeDialog = false
                            onOpenVerificationCoach()
                        }) {
                            Text(stringResource(R.string.btn_verify))
                        }
                        TextButton(onClick = {
                            showSafetyBrakeDialog = false
                            protectionController.dismissWarning()
                        }) {
                            Text(stringResource(R.string.brake_dismiss))
                        }
                        TextButton(onClick = {
                            showSafetyBrakeDialog = false
                            onEndCall()
                        }) {
                            Text(stringResource(R.string.btn_end_call), color = Color(0xFFD32F2F))
                        }
                    }
                }
            )
        }

        // ── DETAILS / EXPLAINABILITY DIALOG ──
        if (showDetailsDialog) {
            AlertDialog(
                onDismissRequest = { showDetailsDialog = false },
                title = { Text(stringResource(R.string.detail_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("${stringResource(R.string.detail_risk_score)}: $riskScore/100 (${riskLevel.label})", fontWeight = FontWeight.Bold)
                        Text("${stringResource(R.string.detail_evidence_strength)}: ${evidenceStrength.name}")
                        Text("${stringResource(R.string.detail_stage)}: ${stage.name}")
                        Text("${stringResource(R.string.detail_velocity)}: ${"%.1f".format(velocity)}x")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${stringResource(R.string.detail_factors)}:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        latestDecision?.explanations?.forEach { Text("• $it", fontSize = 12.sp) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetailsDialog = false }) { Text(stringResource(R.string.btn_ok)) }
                }
            )
        }

        // ── POLICY CONFIGURATION DIALOG ──
        if (showPolicyDialog) {
            AlertDialog(
                onDismissRequest = { showPolicyDialog = false },
                title = { Text(stringResource(R.string.policy_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        ProtectionMode.values().forEach { mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        protectionController.setProtectionMode(
                                            mode,
                                            autoProtectConsent = (mode == ProtectionMode.AUTO_PROTECT)
                                        )
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = policy.mode == mode,
                                    onClick = {
                                        protectionController.setProtectionMode(
                                            mode,
                                            autoProtectConsent = (mode == ProtectionMode.AUTO_PROTECT)
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(mode.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    val desc = when (mode) {
                                        ProtectionMode.MONITOR -> stringResource(R.string.policy_monitor_desc)
                                        ProtectionMode.WARN -> stringResource(R.string.policy_warn_desc)
                                        ProtectionMode.STRONG_PROTECTION -> stringResource(R.string.policy_strong_desc)
                                        ProtectionMode.AUTO_PROTECT -> stringResource(R.string.policy_auto_desc)
                                    }
                                    Text(desc, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPolicyDialog = false }) { Text(stringResource(R.string.policy_save)) }
                }
            )
        }

        // ── EVIDENCE TIMELINE MODAL ──
        if (showEvidenceSheet) {
            val events = evidenceVault.getSessionEvidence(callId)
            val isChainValid = evidenceVault.verifyChainIntegrity(callId)

            AlertDialog(
                onDismissRequest = { showEvidenceSheet = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(stringResource(R.string.evidence_vault), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                if (isChainValid) "✓ ${stringResource(R.string.evidence_chain_valid)}" else "⚠ ${stringResource(R.string.evidence_chain_tampered)}",
                                fontSize = 11.sp,
                                color = if (isChainValid) Color(0xFF4CAF50) else Color(0xFFD32F2F)
                            )
                        }
                    }
                },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(events) { ev ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("${ev.eventType} • ${ev.source}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TealPrimary)
                                Text(ev.description, fontSize = 12.sp)
                                Text("${stringResource(R.string.evidence_hash_label)}: ${ev.hash.take(16)}…", fontSize = 9.sp, color = Color.Gray)
                                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showEvidenceSheet = false }) { Text(stringResource(R.string.btn_close)) }
                }
            )
        }

        // ── DIAGNOSTICS DASHBOARD ──
        if (showDiagnosticsDialog) {
            AlertDialog(
                onDismissRequest = { showDiagnosticsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.diag_title), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        item {
                            DiagCard(stringResource(R.string.diag_pipeline_title)) {
                                DiagRow(stringResource(R.string.diag_asr_latency), "${telemetrySnapshot.asrLatencyMs} ms")
                                DiagRow(stringResource(R.string.diag_ai_latency), "${telemetrySnapshot.aiLatencyMs} ms")
                                DiagRow(stringResource(R.string.diag_risk_latency), "${telemetrySnapshot.riskUpdateLatencyMs} ms")
                                DiagRow(stringResource(R.string.diag_video_fps), "${telemetrySnapshot.videoFps} FPS")
                                DiagRow(stringResource(R.string.diag_memory), "${telemetrySnapshot.memoryUsageMb} MB")
                                DiagRow(stringResource(R.string.diag_execution_tier), latestDecision?.executionMode?.name ?: "HYBRID")
                            }
                        }
                        item {
                            DiagCard(stringResource(R.string.diag_webrtc_title)) {
                                DiagRow(stringResource(R.string.diag_lifecycle), callLifecycleState.displayName)
                                DiagRow(stringResource(R.string.diag_ice_state), webRtcState::class.simpleName ?: "Unknown")
                                DiagRow(stringResource(R.string.diag_local_tracks), "Video=${if (isCameraOff) "OFF" else "ON"}, Audio=${if (isMicMuted) "MUTED" else "ACTIVE"}")
                                DiagRow(stringResource(R.string.diag_remote_video), if (remoteVideoTrack != null) stringResource(R.string.label_streaming) else stringResource(R.string.label_waiting))
                            }
                        }
                        item {
                            DiagCard(stringResource(R.string.diag_intel_title)) {
                                DiagRow(stringResource(R.string.diag_primary_intent), latestDecision?.inferredIntent?.name ?: "ANALYZING")
                                DiagRow(stringResource(R.string.diag_confidence), "${((latestDecision?.confidence ?: 0.5f) * 100).toInt()}%")
                                DiagRow(stringResource(R.string.diag_evidence_strength), evidenceStrength.name)
                                DiagRow(stringResource(R.string.diag_graph_nodes), "${aiOrchestrator.evidenceGraph.getNodes().size} (Edges: ${aiOrchestrator.evidenceGraph.getEdges().size})")
                                DiagRow(stringResource(R.string.diag_corroboration), "${((aiOrchestrator.evidenceGraph.calculateCorroborationScore()) * 100).toInt()}%")
                                DiagRow(stringResource(R.string.diag_velocity), "${((latestDecision?.manipulationVelocity ?: 0f) * 100).toInt()}%")
                                val summary = latestDecision?.sessionSummary
                                if (!summary.isNullOrBlank()) {
                                    DiagRow(stringResource(R.string.diag_summary), summary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDiagnosticsDialog = false }) { Text(stringResource(R.string.btn_close)) }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
//                   REUSABLE COMPOSABLES
// ═══════════════════════════════════════════════════════════

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isActive) activeColor else inactiveColor
            ),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DiagCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TealPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 1.dp)) {
        Text("• $label: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Native WebRTC SurfaceViewRenderer integration into Jetpack Compose.
 * Fully crash-protected with safe shared EGL context and proper AndroidView lifecycle.
 */
@Composable
fun WebRtcVideoView(
    videoTrack: VideoTrack,
    eglContext: EglBase.Context? = null,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                val egl = eglContext ?: runCatching { EglBase.create().eglBaseContext }.getOrNull()
                if (egl != null) {
                    runCatching {
                        init(egl, null)
                        setEnableHardwareScaler(true)
                    }
                }
            }
        },
        update = { renderer ->
            runCatching {
                videoTrack.addSink(renderer)
            }
        },
        onRelease = { renderer ->
            runCatching {
                videoTrack.removeSink(renderer)
                renderer.release()
            }
        },
        modifier = modifier
    )
}
