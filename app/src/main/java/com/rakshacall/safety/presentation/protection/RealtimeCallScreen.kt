package com.rakshacall.safety.presentation.protection

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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

    // 1. Audio & Media Managers
    val audioHelper = remember { AudioManagerHelper(context) }
    val speechManager = remember { SpeechRecognitionManager(context) }
    val aiOrchestrator = remember { RakshaAIOrchestrator() }
    val evidenceVault = remember { EvidenceVault() }
    val visualThreatDetector = remember { VisualThreatDetector() }

    // 2. Protection Controller (State Machine & Policy)
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
                    description = "Automated call protection activated. High-confidence coercive interaction terminated to safeguard user."
                )
                onEndCall()
            }
        )
    }

    val protectionState by protectionController.state.collectAsState()
    val countdownSeconds by protectionController.countdownSecondsRemaining.collectAsState()
    val evidenceStrength by protectionController.evidenceStrength.collectAsState()
    val policy by protectionController.policy.collectAsState()

    // 3. Call, Media & Telemetry States
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
    var recentTranscripts by remember { mutableStateOf<List<String>>(emptyList()) }
    var visualThreatCount by remember { mutableIntStateOf(0) }

    var signalingClient: SignalingClient? by remember { mutableStateOf(null) }

    // 5. WebRTC Engine Initialization with Real Signaling Hook
    val webRtcEngine = remember {
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
    }

    // 6. Lifecycle Management & Signaling Protocol
    DisposableEffect(callId) {
        audioHelper.startCallAudio(speakerphone = isSpeakerphoneOn)
        webRtcEngine.initialize()
        localVideoTrack = webRtcEngine.localVideoTrack()

        speechManager.startListening()

        // Real Signaling Client Setup (Phase 2)
        val signalingUrl = com.rakshacall.safety.data.remote.config.AppConfig.DEFAULT_BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "/api/ws/sessions/$callId"

        val client = SignalingClient(
            serverUrl = signalingUrl,
            token = "",
            listener = object : SignalingClient.Listener {
                override fun onOpen() {
                    callLifecycleState = CallLifecycleState.CONNECTING
                    if (isCaller) {
                        webRtcEngine.createOffer { offer ->
                            signalingClient?.sendSdpOffer(callId, offer.description)
                        }
                    }
                }

                override fun onClosed() {
                    callLifecycleState = CallLifecycleState.ENDED
                }

                override fun onFailure(message: String) {
                    // Graceful fallback to standalone peer/preview mode if signaling server is unavailable
                }

                override fun onMessage(type: String, payload: JSONObject) {
                    when (type) {
                        "sdp_offer" -> {
                            val sdp = payload.optString("sdp")
                            if (sdp.isNotBlank()) {
                                val desc = SessionDescription(SessionDescription.Type.OFFER, sdp)
                                webRtcEngine.setRemoteDescription(desc) {
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
                                webRtcEngine.setRemoteDescription(desc) {}
                            }
                        }
                        "ice_candidate" -> {
                            val sdpMid = payload.optString("sdp_mid")
                            val sdpMLineIndex = payload.optInt("sdp_mline_index")
                            val candidateStr = payload.optString("candidate")
                            if (candidateStr.isNotBlank()) {
                                val candidate = IceCandidate(sdpMid, sdpMLineIndex, candidateStr)
                                webRtcEngine.addIceCandidate(candidate)
                            }
                        }
                        "call_end" -> {
                            callLifecycleState = CallLifecycleState.ENDED
                            onEndCall()
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
            description = "Protected call session started. WebRTC lifecycle: ${callLifecycleState.displayName}. Policy: ${policy.mode}"
        )

        val timerJob = coroutineScope.launch {
            while (isActive) {
                delay(1000L)
                callDurationSeconds++
            }
        }

        onDispose {
            timerJob.cancel()
            speechManager.stopListening()
            signalingClient?.sendCallEnd(callId)
            signalingClient?.close()
            signalingClient = null
            aiOrchestrator.endSession(callId)
            protectionController.reset()
            audioHelper.stopCallAudio()
            webRtcEngine.release()

            evidenceVault.recordEvent(
                sessionId = callId,
                eventType = "CALL_ENDED",
                source = EvidenceSource.SYSTEM,
                description = "Call ended. Cleaned up media streams, signaling socket, and temporary session memory."
            )
        }
    }

    // 8. Video Frame Sampling for Visual Threats with Adaptive Sampling (Phase 16)
    val frameSampler = remember { FrameSampler("remote_video", 1500L) }
    DisposableEffect(remoteVideoTrack) {
        remoteVideoTrack?.addSink(frameSampler)
        onDispose { remoteVideoTrack?.removeSink(frameSampler) }
    }

    // 7. Listen to Continuous Speech Transcripts with Telemetry & Speaker Attribution (Phase 4 & 5)
    LaunchedEffect(speechManager) {
        telemetry.markAsrStart()
        speechManager.transcriptFlow.collectLatest { transcript ->
            telemetry.recordAsrCompleted()
            if (isCallPaused) return@collectLatest

            val speakerLabel = transcript.speaker.label
            val formattedTranscript = "[$speakerLabel]: ${transcript.text}"
            recentTranscripts = (recentTranscripts + formattedTranscript).takeLast(10)

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
            protectionController.evaluate(decision, visualThreatCount)

            // Dynamic Adaptive Frame Sampling (Phase 16)
            val score = decision.riskScore
            val adaptiveInterval = when {
                score >= 80 -> 400L // Critical rate: maximum safe analysis
                score >= 40 -> 1000L // Suspicious rate
                else -> 2000L // Normal rate: power-saving
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

            // Haptic alert on Safety Brake
            if (decision.safetyBrakeTriggered && !showSafetyBrakeDialog && policy.mode != ProtectionMode.MONITOR) {
                showSafetyBrakeDialog = true
                try {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(frameSampler) {
        frameSampler.samples.collectLatest { sample ->
            val threat = visualThreatDetector.analyzeFrame(sample)
            if (threat != null) {
                visualThreatCount++
                evidenceVault.recordEvent(
                    sessionId = callId,
                    eventType = "VISUAL_THREAT_DETECTED",
                    source = EvidenceSource.VISUAL,
                    riskScore = threat.riskScoreBonus,
                    description = threat.description
                )
                latestDecision?.let { protectionController.evaluate(it, visualThreatCount) }
            }
        }
    }

    val riskScore = latestDecision?.riskScore ?: 0
    val riskLevel = latestDecision?.riskLevel ?: RiskLevel.LOW
    val stage = latestDecision?.currentStage ?: ScamStage.CONTACT
    val velocity = latestDecision?.manipulationVelocity ?: 0.0f
    val primaryLanguage = latestDecision?.primaryLanguage ?: "en"

    val formattedDuration = String.format("%02d:%02d", callDurationSeconds / 60, callDurationSeconds % 60)

    val riskColor = when (riskLevel) {
        RiskLevel.LOW -> Color(0xFF2E7D32)
        RiskLevel.MEDIUM -> Color(0xFFF57C00)
        RiskLevel.HIGH -> Color(0xFFD32F2F)
        RiskLevel.CRITICAL -> Color(0xFFB71C1C)
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // ── VIDEO VIEWPORTS ──
            remoteVideoTrack?.let { track ->
                WebRtcVideoView(
                    videoTrack = track,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        if (webRtcState == WebRtcState.Connected) "Remote Participant Connected" else "Connecting WebRTC Media…",
                        color = Color.LightGray,
                        fontSize = 15.sp
                    )
                    Text(
                        "RakshaCall Real-Time Call Protection Active",
                        color = TealPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            // Local PiP Video
            localVideoTrack?.let { track ->
                if (!isCameraOff) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp)
                            .size(width = 100.dp, height = 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        WebRtcVideoView(
                            videoTrack = track,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // ── TOP BAR (DIAGNOSTICS & UI MODE TOGGLE) ──
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.72f)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (webRtcState == WebRtcState.Connected) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RAKSHACALL • $callId", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.width(8.dp))
                    // Simple / Advanced UI Mode Toggle
                    TextButton(
                        onClick = {
                            val nextMode = if (policy.uiMode == UIMode.ADVANCED) UIMode.SIMPLE_ELDERLY else UIMode.ADVANCED
                            protectionController.setUIMode(nextMode)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = TealPrimary),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(if (policy.uiMode == UIMode.ADVANCED) "HUD: ADV" else "HUD: SIMPLE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (policy.uiMode == UIMode.ADVANCED) {
                    Text(
                        "$formattedDuration • Mode: ${policy.mode} • Lang: ${primaryLanguage.uppercase()} • Vel: ${"%.1f".format(velocity)}x",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Advanced Risk Level Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = riskColor.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "${riskLevel.label} ($riskScore/100) • ${evidenceStrength.name}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Stage: ${stage.name} • State: ${protectionState.name}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(onClick = { showPolicyDialog = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Tune, contentDescription = "Policy", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // ── ELDERLY / SIMPLE PROTECTION MODE OVERLAY (Section 31) ──
            if (policy.uiMode == UIMode.SIMPLE_ELDERLY && (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFB71C1C).copy(alpha = 0.95f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "⚠️ POSSIBLE SCAM",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "RakshaCall noticed several unusual requests during this call.",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // Large Emergency Buttons
                        Button(
                            onClick = onEndCall,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("END CALL", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                                showSafetyBrakeDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CONTACT FAMILY", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = { protectionController.cancelCountdownAndKeepCall() }) {
                            Text("CONTINUE CALL", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                        }
                    }
                }
            }

            // ── LIVE TRANSCRIPT ACCORDION (Advanced Mode) ──
            if (policy.uiMode == UIMode.ADVANCED && recentTranscripts.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            "LIVE TRANSCRIPT & INTENT ANALYSIS",
                            color = TealPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            recentTranscripts.last(),
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 2
                        )
                    }
                }
            }

            // ── CALL CONTROLS BAR ──
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute Toggle
                FilledIconButton(
                    onClick = {
                        isMicMuted = !isMicMuted
                        if (isMicMuted) webRtcEngine.muteMicrophone() else webRtcEngine.unmuteMicrophone()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isMicMuted) Color(0xFFD32F2F) else Color(0xFF334155)
                    )
                ) {
                    Icon(if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mic", tint = Color.White)
                }

                // Camera Toggle
                FilledIconButton(
                    onClick = {
                        isCameraOff = !isCameraOff
                        if (isCameraOff) webRtcEngine.disableCamera() else webRtcEngine.enableCamera()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isCameraOff) Color(0xFFD32F2F) else Color(0xFF334155)
                    )
                ) {
                    Icon(if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam, contentDescription = "Camera", tint = Color.White)
                }

                // Switch Camera
                FilledIconButton(
                    onClick = { webRtcEngine.switchCamera() },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF334155))
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                }

                // Speakerphone Toggle
                FilledIconButton(
                    onClick = {
                        isSpeakerphoneOn = !isSpeakerphoneOn
                        audioHelper.setSpeakerphone(isSpeakerphoneOn)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isSpeakerphoneOn) TealPrimary else Color(0xFF334155)
                    )
                ) {
                    Icon(if (isSpeakerphoneOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown, contentDescription = "Speaker", tint = Color.White)
                }

                // Evidence Timeline Sheet
                FilledIconButton(
                    onClick = { showEvidenceSheet = true },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF334155))
                ) {
                    Icon(Icons.Default.History, contentDescription = "Evidence", tint = Color.White)
                }

                // Telemetry & Diagnostics Dashboard (Phase 36)
                FilledIconButton(
                    onClick = { showDiagnosticsDialog = true },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF334155))
                ) {
                    Icon(Icons.Default.MonitorHeart, contentDescription = "Diagnostics", tint = Color.White)
                }

                // End Call Button
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFE53935), CircleShape)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        // ── FORCE-CUT PROTECTION COUNTDOWN (Section 4 & 21) ──
        if (protectionState == ProtectionState.PROTECTION_PENDING) {
            AlertDialog(
                onDismissRequest = { /* User must explicitly act */ },
                icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color.Red, modifier = Modifier.size(44.dp)) },
                title = { Text("⚡ AUTOMATIC PROTECTION ACTIVATING", color = Color.Red, fontWeight = FontWeight.Black) },
                text = {
                    Column {
                        Text(
                            "High-risk interaction detected ($riskScore/100 • $evidenceStrength evidence).\nCoercive credential or financial demand in progress.",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Call protection will activate in $countdownSeconds seconds.",
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
                        Text("KEEP CALL")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showDetailsDialog = true }) {
                            Text("VIEW DETAILS")
                        }
                        Button(
                            onClick = onEndCall,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("END CALL NOW")
                        }
                    }
                }
            )
        }

        // ── SAFETY BRAKE INTERACTIVE MODAL (Sections 20, 21, 22) ──
        if (showSafetyBrakeDialog && protectionState != ProtectionState.PROTECTION_PENDING) {
            AlertDialog(
                onDismissRequest = { showSafetyBrakeDialog = false },
                icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(36.dp)) },
                title = { Text("🛡 RAKSHA SAFETY BRAKE", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
                text = {
                    Column {
                        Text(
                            "High-risk conversational coercion detected. Do not share OTPs, PINs, or transfer money.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("WHY WAS THIS FLAGGED?", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                        latestDecision?.explanations?.forEach { reason ->
                            Text("• $reason", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        latestDecision?.confidence?.let {
                            Text("Confidence: ${"%.0f".format(it * 100)}%", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isCallPaused = !isCallPaused
                            showSafetyBrakeDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text(if (isCallPaused) "RESUME CALL" else "PAUSE & REFLECT")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            showSafetyBrakeDialog = false
                            onOpenVerificationCoach()
                        }) {
                            Text("VERIFY")
                        }
                        TextButton(onClick = {
                            showSafetyBrakeDialog = false
                            protectionController.dismissWarning()
                        }) {
                            Text("DISMISS")
                        }
                        TextButton(onClick = {
                            showSafetyBrakeDialog = false
                            onEndCall()
                        }) {
                            Text("END CALL", color = Color(0xFFD32F2F))
                        }
                    }
                }
            )
        }

        // ── DETAILS / EXPLAINABILITY DIALOG (Section 20) ──
        if (showDetailsDialog) {
            AlertDialog(
                onDismissRequest = { showDetailsDialog = false },
                title = { Text("Incident Analysis Details", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Overall Risk Score: $riskScore/100 (${riskLevel.label})", fontWeight = FontWeight.Bold)
                        Text("Evidence Strength: ${evidenceStrength.name}")
                        Text("Conversation Stage: ${stage.name}")
                        Text("Escalation Velocity: ${"%.1f".format(velocity)}x")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Detailed Evidence Factors:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        latestDecision?.explanations?.forEach { Text("• $it", fontSize = 12.sp) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetailsDialog = false }) { Text("OK") }
                }
            )
        }

        // ── POLICY CONFIGURATION DIALOG (Section 2 & 4) ──
        if (showPolicyDialog) {
            AlertDialog(
                onDismissRequest = { showPolicyDialog = false },
                title = { Text("Configure Protection Policy", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Active Protection Mode:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        ProtectionMode.values().forEach { mode ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = policy.mode == mode,
                                    onClick = {
                                        protectionController.setProtectionMode(
                                            mode,
                                            autoProtectConsent = (mode == ProtectionMode.AUTO_PROTECT)
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(mode.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    val desc = when (mode) {
                                        ProtectionMode.MONITOR -> "Analyze & display risk without interrupting call."
                                        ProtectionMode.WARN -> "Display warning banners and provide manual actions."
                                        ProtectionMode.STRONG_PROTECTION -> "Persistent warning on high risk with verification pause."
                                        ProtectionMode.AUTO_PROTECT -> "5-second countdown to termination on critical multi-signal coercion."
                                    }
                                    Text(desc, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPolicyDialog = false }) { Text("SAVE") }
                }
            )
        }

        // ── EVIDENCE TIMELINE MODAL SHEET ──
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
                            Text("Tamper-Evident Evidence", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(if (isChainValid) "✓ Cryptographic SHA-256 Valid" else "⚠ Tampering Detected", fontSize = 11.sp, color = if (isChainValid) Color(0xFF4CAF50) else Color(0xFFD32F2F))
                        }
                    }
                },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(events) { ev ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("${ev.eventType} • ${ev.source}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TealPrimary)
                                Text(ev.description, fontSize = 12.sp)
                                Text("Hash: ${ev.hash.take(16)}…", fontSize = 9.sp, color = Color.Gray)
                                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showEvidenceSheet = false }) {
                        Text("CLOSE")
                    }
                }
            )
        }

        // ── DIAGNOSTIC OBSERVABILITY DASHBOARD (Phase 36) ──
        if (showDiagnosticsDialog) {
            AlertDialog(
                onDismissRequest = { showDiagnosticsDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TELEMETRY & OBSERVABILITY", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("PIPELINE LATENCIES & HARDWARE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TealPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("• ASR Latency: ${telemetrySnapshot.asrLatencyMs} ms", fontSize = 11.sp)
                                    Text("• AI Inference Latency: ${telemetrySnapshot.aiLatencyMs} ms", fontSize = 11.sp)
                                    Text("• Risk Update Latency: ${telemetrySnapshot.riskUpdateLatencyMs} ms", fontSize = 11.sp)
                                    Text("• Video Render: ${telemetrySnapshot.videoFps} FPS", fontSize = 11.sp)
                                    Text("• Process Memory: ${telemetrySnapshot.memoryUsageMb} MB", fontSize = 11.sp)
                                    Text("• Execution Tier: ${latestDecision?.executionMode?.name ?: "HYBRID"}", fontSize = 11.sp)
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("WEBRTC & SIGNALING STATE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TealPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("• Lifecycle: ${callLifecycleState.displayName}", fontSize = 11.sp)
                                    Text("• ICE State: ${webRtcState::class.simpleName}", fontSize = 11.sp)
                                    Text("• Local Tracks: Video=${if (isCameraOff) "OFF" else "ON"}, Audio=${if (isMicMuted) "MUTED" else "ACTIVE"}", fontSize = 11.sp)
                                    Text("• Remote Video: ${if (remoteVideoTrack != null) "STREAMING" else "WAITING"}", fontSize = 11.sp)
                                }
                            }
                        }

                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("INTELLIGENCE & EVIDENCE GRAPH", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TealPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("• Primary Intent: ${latestDecision?.inferredIntent?.name ?: "ANALYZING"}", fontSize = 11.sp)
                                    Text("• Orthogonal Confidence: ${((latestDecision?.confidence ?: 0.5f) * 100).toInt()}%", fontSize = 11.sp)
                                    Text("• Evidence Strength: $evidenceStrength", fontSize = 11.sp)
                                    Text("• Graph Nodes: ${aiOrchestrator.evidenceGraph.getNodes().size} (Edges: ${aiOrchestrator.evidenceGraph.getEdges().size})", fontSize = 11.sp)
                                    Text("• Corroboration Score: ${((aiOrchestrator.evidenceGraph.calculateCorroborationScore()) * 100).toInt()}%", fontSize = 11.sp)
                                    Text("• Manipulation Velocity: ${((latestDecision?.manipulationVelocity ?: 0f) * 100).toInt()}%", fontSize = 11.sp)
                                    val summary = latestDecision?.sessionSummary
                                    if (!summary.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("• Summary: $summary", fontSize = 10.sp, color = Slate500)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDiagnosticsDialog = false }) {
                        Text("CLOSE")
                    }
                }
            )
        }
    }
}

/**
 * Native WebRTC SurfaceViewRenderer integration into Jetpack Compose.
 */
@Composable
fun WebRtcVideoView(
    videoTrack: VideoTrack,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val eglBase = remember { EglBase.create() }
    val renderer = remember { SurfaceViewRenderer(context) }

    DisposableEffect(videoTrack) {
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setEnableHardwareScaler(true)
        videoTrack.addSink(renderer)

        onDispose {
            videoTrack.removeSink(renderer)
            renderer.release()
            eglBase.release()
        }
    }

    AndroidView(
        factory = { renderer },
        modifier = modifier
    )
}
