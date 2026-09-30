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
import com.rakshacall.safety.webrtc.AudioManagerHelper
import com.rakshacall.safety.webrtc.WebRtcConfig
import com.rakshacall.safety.webrtc.WebRtcEngine
import com.rakshacall.safety.webrtc.WebRtcState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.EglBase
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

    // 2. Call & Media States
    var remoteVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var localVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var webRtcState by remember { mutableStateOf<WebRtcState>(WebRtcState.New) }

    var isMicMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var isSpeakerphoneOn by remember { mutableStateOf(true) }
    var callDurationSeconds by remember { mutableLongStateOf(0L) }

    // 3. AI Safety States
    var latestDecision by remember { mutableStateOf<ProtectionDecision?>(null) }
    var showSafetyBrakeDialog by remember { mutableStateOf(false) }
    var isCallPaused by remember { mutableStateOf(false) }
    var showEvidenceSheet by remember { mutableStateOf(false) }
    var recentTranscripts by remember { mutableStateOf<List<String>>(emptyList()) }

    // 4. WebRTC Engine Initialization
    val webRtcEngine = remember {
        val config = WebRtcConfig()
        WebRtcEngine(
            context = context,
            config = config,
            onLocalIceCandidate = { /* Signaling handles ICE */ },
            onRemoteVideo = { track -> remoteVideoTrack = track },
            onConnectionStateChange = { newState -> webRtcState = newState }
        )
    }

    // 5. Lifecycle Management
    DisposableEffect(callId) {
        audioHelper.startCallAudio(speakerphone = isSpeakerphoneOn)
        webRtcEngine.initialize()
        localVideoTrack = webRtcEngine.localVideoTrack()

        // Start Speech Recognition
        speechManager.startListening()

        evidenceVault.recordEvent(
            sessionId = callId,
            eventType = "CALL_STARTED",
            source = EvidenceSource.SYSTEM,
            description = "Protected call session started. WebRTC & AI safety monitoring active."
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
            aiOrchestrator.endSession(callId)
            audioHelper.stopCallAudio()
            webRtcEngine.release()

            evidenceVault.recordEvent(
                sessionId = callId,
                eventType = "CALL_ENDED",
                source = EvidenceSource.SYSTEM,
                description = "Call ended. Cleaned up media streams and cleared temporary session memory."
            )
        }
    }

    // 6. Listen to Continuous Speech Transcripts
    LaunchedEffect(speechManager) {
        speechManager.transcriptFlow.collectLatest { transcript ->
            if (isCallPaused) return@collectLatest

            recentTranscripts = (recentTranscripts + transcript.text).takeLast(10)

            val event = TranscriptEvent(
                id = UUID.randomUUID().toString(),
                sessionId = callId,
                timestamp = transcript.timestamp,
                speaker = "CALLER",
                text = transcript.text
            )

            val decision = aiOrchestrator.processTranscript(
                event = event,
                isFinal = transcript.isFinal
            )

            latestDecision = decision

            if (transcript.isFinal) {
                evidenceVault.recordEvent(
                    sessionId = callId,
                    eventType = "SPEECH_TRANSCRIPT",
                    source = EvidenceSource.TRANSCRIPT,
                    language = decision.primaryLanguage,
                    stage = decision.currentStage.name,
                    riskScore = decision.riskScore,
                    confidence = decision.confidence,
                    description = transcript.text
                )
            }

            // Haptic alert on Safety Brake
            if (decision.safetyBrakeTriggered && !showSafetyBrakeDialog) {
                showSafetyBrakeDialog = true
                try {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
                } catch (_: Exception) {}
            }
        }
    }

    // 7. Video Frame Sampling for Visual Threats
    val frameSampler = remember { FrameSampler("remote_video", 1500L) }
    DisposableEffect(remoteVideoTrack) {
        remoteVideoTrack?.addSink(frameSampler)
        onDispose { remoteVideoTrack?.removeSink(frameSampler) }
    }

    LaunchedEffect(frameSampler) {
        frameSampler.samples.collectLatest { sample ->
            val threat = visualThreatDetector.analyzeFrame(sample)
            if (threat != null) {
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
            // Remote Video Track (Full Screen)
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
                        "RakshaCall Real-Time Multilingual Safety Active",
                        color = TealPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            // Local Video Track (PiP Tile)
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

            // ── TOP SAFETY & RISK BANNER ──
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.70f)
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
                }
                Text(
                    "$formattedDuration • Lang: ${primaryLanguage.uppercase()} • Vel: ${"%.1f".format(velocity)}x",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Risk Level Card
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
                                "${riskLevel.label} ($riskScore/100)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                "Stage: ${stage.name}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            )
                        }
                        if (latestDecision?.safetyBrakeTriggered == true) {
                            TextButton(
                                onClick = { showSafetyBrakeDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Yellow)
                            ) {
                                Text("BRAKE", fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // ── LIVE TRANSCRIPT ACCORDION (Bottom-Center above controls) ──
            if (recentTranscripts.isNotEmpty()) {
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

            // ── CALL CONTROLS BAR (Bottom) ──
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

        // ── SAFETY BRAKE INTERACTIVE MODAL (Sections 22, 28, 29) ──
        if (showSafetyBrakeDialog) {
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
                        Text("DETECTED REASONS:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                        latestDecision?.explanations?.forEach { reason ->
                            Text("• $reason", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
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
                        Text(if (isCallPaused) "RESUME PROTECTION" else "PAUSE & REFLECT")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            showSafetyBrakeDialog = false
                            onOpenVerificationCoach()
                        }) {
                            Text("VERIFY CALLER")
                        }
                        TextButton(onClick = {
                            showSafetyBrakeDialog = false
                            onEndCall()
                        }) {
                            Text("DISCONNECT", color = Color(0xFFD32F2F))
                        }
                    }
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
                                Divider(modifier = Modifier.padding(top = 4.dp))
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
