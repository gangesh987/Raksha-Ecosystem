package com.rakshacall.safety.presentation.protection

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rakshacall.R
import com.rakshacall.safety.core.export.IncidentReportExporter
import com.rakshacall.safety.core.security.EvidenceHasher
import com.rakshacall.safety.core.security.IntegrityResult
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.EvidenceEvent
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.SessionStatus
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.domain.model.VelocityLevel
import com.rakshacall.safety.presentation.theme.Navy900
import com.rakshacall.safety.presentation.theme.RiskCritical
import com.rakshacall.safety.presentation.theme.RiskCriticalBg
import com.rakshacall.safety.presentation.theme.RiskHigh
import com.rakshacall.safety.presentation.theme.RiskHighBg
import com.rakshacall.safety.presentation.theme.RiskLow
import com.rakshacall.safety.presentation.theme.RiskLowBg
import com.rakshacall.safety.presentation.theme.RiskMedium
import com.rakshacall.safety.presentation.theme.RiskMediumBg
import com.rakshacall.safety.presentation.theme.Slate500
import com.rakshacall.safety.presentation.theme.Slate700
import com.rakshacall.safety.presentation.theme.TealPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Data structure representing an authentic simulated digital-arrest dialogue step.
 */
data class SimulationStep(
    val phaseNumber: Int,
    val phaseTitle: String,
    val speaker: String,
    val text: String,
    val expectedTactic: String,
    val delayMs: Long = 3500L
)

val SIMULATED_SCAM_PROGRESSION = listOf(
    SimulationStep(
        phaseNumber = 1,
        phaseTitle = "CONTACT",
        speaker = "CALLER",
        text = "Hello, this is Officer Vikram from the Central Cyber Crime Investigation Cell. Am I speaking with the registered account holder?",
        expectedTactic = "Contact Initialized"
    ),
    SimulationStep(
        phaseNumber = 2,
        phaseTitle = "AUTHORITY",
        speaker = "CALLER",
        text = "We have intercepted 24 fraudulent international transactions registered under your Aadhaar and passport number. I am conducting an official CBI inquiry.",
        expectedTactic = "Authority Impersonation"
    ),
    SimulationStep(
        phaseNumber = 3,
        phaseTitle = "FEAR",
        speaker = "CALLER",
        text = "A non-bailable arrest warrant has been sanctioned against you under money laundering and anti-national funding sections. Police custody is immediate unless cleared.",
        expectedTactic = "Criminal Allegation / Fear"
    ),
    SimulationStep(
        phaseNumber = 4,
        phaseTitle = "ISOLATION",
        speaker = "CALLER",
        text = "This is a secret investigation under national security secrecy laws. Lock your room doors, do not inform family, lawyers or friends. Keep video active continuously.",
        expectedTactic = "Isolation"
    ),
    SimulationStep(
        phaseNumber = 5,
        phaseTitle = "PAYMENT DEMAND",
        speaker = "CALLER",
        text = "To verify non-proceeds of crime, transfer 50,000 rupees to the designated Supreme Court verification security escrow account immediately.",
        expectedTactic = "Payment Demand"
    ),
    SimulationStep(
        phaseNumber = 6,
        phaseTitle = "OTP / CREDENTIAL PRESSURE",
        speaker = "CALLER",
        text = "Read out the six-digit verification OTP and bank PIN sent to your phone right now to authorize the court clearance, or your bank accounts will be seized!",
        expectedTactic = "Credential / OTP Pressure"
    )
)

/**
 * Video Call Simulation Lab.
 * An interactive, controlled demonstration environment for judges and evaluators.
 * Runs simulated dialogue through the real RakshaCall RiskEngine, ScamStageMachine,
 * ManipulationVelocityEngine, and SHA-256 Evidence Chain while strictly isolating data from production.
 */
@Composable
fun VideoCallSimulationScreen(
    onNavigateBack: () -> Unit,
    onOpenVerificationCoach: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sessionId by remember { mutableStateOf("DEMO-SIM-" + UUID.randomUUID().toString().take(8)) }
    val sessionStartTime = remember { System.currentTimeMillis() }

    var currentStepIndex by remember { mutableIntStateOf(-1) }
    var isAutoPlaying by remember { mutableStateOf(false) }
    var callDurationSeconds by remember { mutableIntStateOf(0) }

    var currentRiskScore by remember { mutableIntStateOf(0) }
    var currentStage by remember { mutableStateOf(ScamStage.CONTACT) }
    val detectedSignals = remember { mutableStateListOf<RiskSignal>() }
    val transcriptList = remember { mutableStateListOf<TranscriptEvent>() }
    val evidenceEvents = remember { mutableStateListOf<EvidenceEvent>() }
    var showSafetyBrake by remember { mutableStateOf(false) }
    var integrityVerifiedMsg by remember { mutableStateOf<String?>(null) }

    val velocityEngine = ServiceLocator.velocityEngine
    var currentVelocity by remember { mutableStateOf(velocityEngine.calculateVelocity(emptyList())) }

    val listState = rememberLazyListState()

    // Call duration ticker
    LaunchedEffect(currentStepIndex, isAutoPlaying) {
        if (currentStepIndex >= 0) {
            while (true) {
                delay(1000L)
                callDurationSeconds++
            }
        }
    }

    fun injectDialogueStep(step: SimulationStep) {
        coroutineScope.launch {
            val event = TranscriptEvent(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                speaker = step.speaker,
                text = step.text
            )
            transcriptList.add(event)

            // 1. Run real deterministic Risk Engine
            val newSignals = ServiceLocator.riskEngine.analyzeTranscript(event, detectedSignals)
            for (sig in newSignals) {
                detectedSignals.add(sig)

                // 2. Scam stage machine transition
                val transition = ServiceLocator.scamStageMachine.processSignal(sig)
                if (transition != null) {
                    currentStage = transition.toStage
                }

                // 3. Append SHA-256 cryptographic evidence block
                val lastHash = ServiceLocator.evidenceRepository.getLastEvidenceHash(sessionId)
                val ev = EvidenceHasher.createEvent(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    timestamp = sig.timestamp,
                    eventType = "DEMO_TACTIC_DETECTED",
                    payloadJson = "{\"tactic\":\"${sig.tactic.name}\",\"weight\":${sig.riskContribution},\"snippet\":\"${sig.evidenceText}\"}",
                    lastKnownHash = lastHash
                )
                ServiceLocator.evidenceRepository.appendEvidence(ev)
                evidenceEvents.add(ev)
            }

            // 4. Calculate explainable risk score
            val newScore = ServiceLocator.riskEngine.calculateCurrentScore(
                allSignals = detectedSignals,
                sessionStartTime = sessionStartTime
            )
            currentRiskScore = newScore

            // 5. Update manipulation velocity
            currentVelocity = velocityEngine.calculateVelocity(detectedSignals)

            // 6. Check Safety Brake condition (Risk >= 60 + Irreversible action)
            if (ServiceLocator.riskEngine.isSafetyBrakeTriggered(newScore, detectedSignals)) {
                showSafetyBrake = true
                isAutoPlaying = false
            }

            // Scroll down
            if (transcriptList.isNotEmpty()) {
                listState.animateScrollToItem(transcriptList.size)
            }
        }
    }

    // Auto-play loop
    LaunchedEffect(isAutoPlaying, currentStepIndex) {
        if (isAutoPlaying && currentStepIndex < SIMULATED_SCAM_PROGRESSION.size - 1) {
            delay(3500L)
            currentStepIndex++
            injectDialogueStep(SIMULATED_SCAM_PROGRESSION[currentStepIndex])
        } else if (currentStepIndex >= SIMULATED_SCAM_PROGRESSION.size - 1) {
            isAutoPlaying = false
        }
    }

    // Sound wave animation values for active speaker
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w3"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SIMULATION LAB",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp,
                            color = TealPrimary
                        )
                    }
                    Text(
                        text = "DEMO MODE — SYNTHETIC CONVERSATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RiskHigh
                    )
                }
            }

            IconButton(onClick = {
                // Reset Simulation
                sessionId = "DEMO-SIM-" + UUID.randomUUID().toString().take(8)
                currentStepIndex = -1
                isAutoPlaying = false
                callDurationSeconds = 0
                currentRiskScore = 0
                currentStage = ScamStage.CONTACT
                detectedSignals.clear()
                transcriptList.clear()
                evidenceEvents.clear()
                integrityVerifiedMsg = null
                ServiceLocator.scamStageMachine.reset()
                currentVelocity = velocityEngine.calculateVelocity(emptyList())
                showSafetyBrake = false
            }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Simulation", tint = Slate500)
            }
        }

        // Prominent Disclaimer Banner: Honest Boundary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(RiskMediumBg)
                .border(1.dp, RiskMedium.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Controlled demonstration environment. Simulates a digital-arrest coercion sequence through the real AI safety engine. Never claims WhatsApp/VoIP call interception.",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Dual 3D Video Call Tiles Presentation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Simulated Caller Video Tile
            Card(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.85f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.sim_caller_video),
                        contentDescription = "Simulated Caller 3D Stream",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay Badges
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(RiskCritical)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CALLER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Call timer
                        val formattedDuration = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            callDurationSeconds / 60,
                            callDurationSeconds % 60
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(formattedDuration, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Live sound wave bars when caller is speaking
                    if (isAutoPlaying || (currentStepIndex >= 0 && currentStepIndex < SIMULATED_SCAM_PROGRESSION.size)) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                                .height(22.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Box(modifier = Modifier.width(3.dp).height((18 * wave1).dp).clip(RoundedCornerShape(1.dp)).background(RiskHigh))
                            Box(modifier = Modifier.width(3.dp).height((22 * wave2).dp).clip(RoundedCornerShape(1.dp)).background(RiskHigh))
                            Box(modifier = Modifier.width(3.dp).height((16 * wave3).dp).clip(RoundedCornerShape(1.dp)).background(RiskHigh))
                        }
                    }

                    // Bottom caller label
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alleged \"CBI Official\"",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "HD 720p",
                                fontSize = 8.sp,
                                color = Slate500
                            )
                        }
                    }
                }
            }

            // User Self-View Video Tile
            Card(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.85f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.sim_user_video),
                        contentDescription = "Simulated User Self View",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay Badges
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(TealPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TARGET USER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("PROTECTED", fontSize = 8.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Bottom self view label
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Protected Endpoint",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "MIC: ON",
                                fontSize = 8.sp,
                                color = TealPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Scam Stage Progression (Section 6)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "SCAM STAGE PROGRESSION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Slate500
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allStages = listOf(
                        ScamStage.CONTACT to "CONTACT",
                        ScamStage.AUTHORITY to "AUTH",
                        ScamStage.FEAR to "FEAR",
                        ScamStage.ISOLATION to "ISOL",
                        ScamStage.DEMAND to "DEMAND",
                        ScamStage.PAYMENT_CREDENTIAL to "PAY/OTP",
                        ScamStage.ESCALATION to "ESCAL"
                    )

                    allStages.forEachIndexed { idx, (st, label) ->
                        val isCurrent = currentStage == st
                        val isPassed = currentStage.ordinal > st.ordinal

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isCurrent -> TealPrimary
                                        isPassed -> TealPrimary.copy(alpha = 0.25f)
                                        else -> Slate500.copy(alpha = 0.12f)
                                    }
                                )
                                .border(
                                    width = if (isCurrent) 1.5.dp else 0.dp,
                                    color = if (isCurrent) Color.White.copy(alpha = 0.8f) else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isPassed) "✓ $label" else label,
                                fontSize = 8.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                color = when {
                                    isCurrent -> Color.White
                                    isPassed -> TealPrimary
                                    else -> Slate500
                                }
                            )
                        }

                        if (idx < allStages.size - 1) {
                            Text("›", fontSize = 10.sp, color = Slate500)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Real-Time Risk Intelligence Dashboard (Section 5)
        val level = RiskLevel.fromScore(currentRiskScore)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (level) {
                    RiskLevel.CRITICAL -> RiskCriticalBg
                    RiskLevel.HIGH -> RiskHighBg
                    RiskLevel.MEDIUM -> RiskMediumBg
                    RiskLevel.LOW -> RiskLowBg
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "REAL-TIME RISK: ${level.label}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = when (level) {
                                    RiskLevel.CRITICAL -> RiskCritical
                                    RiskLevel.HIGH -> RiskHigh
                                    RiskLevel.MEDIUM -> RiskMedium
                                    RiskLevel.LOW -> RiskLow
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• 98% Confidence",
                                fontSize = 10.sp,
                                color = Slate500
                            )
                        }
                        Text(
                            text = "$currentRiskScore / 100",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "VELOCITY: ${currentVelocity.level.name}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (currentVelocity.level) {
                                VelocityLevel.HIGH -> RiskCritical
                                VelocityLevel.MODERATE -> RiskMedium
                                VelocityLevel.LOW -> RiskLow
                            }
                        )
                        Text(
                            text = "${detectedSignals.size} tactics in 90s window",
                            fontSize = 10.sp,
                            color = Slate500
                        )
                    }
                }

                // AI Explanation Cards (Section 8)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "WHY IS RISK INCREASING?",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        val explanation = when {
                            currentRiskScore >= 60 -> "Risk increased because the conversation escalated from authority impersonation to fear, isolation, and irreversible payment/OTP demands."
                            currentRiskScore >= 35 -> "Risk increased as caller introduced criminal arrest threats and strict room secrecy."
                            currentRiskScore > 0 -> "Risk initiated by unverified authority claims under cybercrime cell pretext."
                            else -> "Awaiting conversation signals to initiate risk analysis."
                        }
                        Text(text = explanation, fontSize = 11.sp, lineHeight = 14.sp)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "WHAT WOULD REDUCE RISK?",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                        Text(
                            text = "Pause conversation immediately. Disconnect and independently contact official authorities (1930 / cybercrime.gov.in).",
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Simulation Controller Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (currentStepIndex < SIMULATED_SCAM_PROGRESSION.size - 1) {
                        currentStepIndex++
                        injectDialogueStep(SIMULATED_SCAM_PROGRESSION[currentStepIndex])
                    }
                },
                enabled = currentStepIndex < SIMULATED_SCAM_PROGRESSION.size - 1 && !isAutoPlaying,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (currentStepIndex == -1) "Start Phase 1" else "Next Step (${currentStepIndex + 2}/6)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    if (!isAutoPlaying) {
                        if (currentStepIndex == -1) {
                            currentStepIndex = 0
                            injectDialogueStep(SIMULATED_SCAM_PROGRESSION[0])
                        }
                        isAutoPlaying = true
                    } else {
                        isAutoPlaying = false
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAutoPlaying) RiskHigh else Slate700
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = if (isAutoPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isAutoPlaying) "Pause Auto" else "Auto-Play (3.5s)", fontSize = 11.sp)
            }
        }

        // Live Transcript & Incident Timeline
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Real-Time AI Transcript & Signal Stream", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate500)
                    if (transcriptList.isNotEmpty()) {
                        Text(text = "${transcriptList.size} lines", fontSize = 10.sp, color = Slate500)
                    }
                }
            }

            if (transcriptList.isEmpty()) {
                item {
                    Text(
                        text = "Simulation ready. Tap \"Start Phase 1\" or \"Auto-Play\" to begin the interactive coercion sequence.",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
            } else {
                items(transcriptList) { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${t.speaker}: ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = RiskHigh
                        )
                        Text(text = t.text, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }

            // Tactic accumulation delta breakdown
            if (detectedSignals.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tactic Accumulation (${detectedSignals.size} Signals)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                }

                items(detectedSignals) { sig ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (sig.tactic.isIrreversibleAction) RiskHighBg else RiskMediumBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠ +${sig.riskContribution} ${sig.tactic.displayName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sig.tactic.isIrreversibleAction) RiskHigh else RiskMedium
                        )
                        Text(
                            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sig.timestamp)),
                            fontSize = 10.sp,
                            color = Slate500
                        )
                    }
                }
            }

            // Incident Timeline & Cryptographic Evidence Section (Section 12)
            if (currentStepIndex >= 3 || currentRiskScore >= 60) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "INCIDENT TIMELINE & EVIDENCE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp,
                                        color = TealPrimary
                                    )
                                }

                                Text(
                                    text = "${evidenceEvents.size} SHA-256 Blocks",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate500
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Verification confirmation
                            if (integrityVerifiedMsg != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TealPrimary.copy(alpha = 0.15f))
                                        .padding(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = integrityVerifiedMsg ?: "", fontSize = 11.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val chain = ServiceLocator.evidenceRepository.getEvidenceForSession(sessionId)
                                            val result = EvidenceHasher.verifyChain(chain)
                                            integrityVerifiedMsg = if (result is IntegrityResult.Valid) {
                                                "Chain Verified: ${chain.size} Tamper-Evident Blocks Secured"
                                            } else {
                                                "Integrity Error: Hash Mismatch Detected"
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Verify Integrity", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val session = ProtectionSession(
                                                id = sessionId,
                                                startTime = sessionStartTime,
                                                endTime = System.currentTimeMillis(),
                                                status = SessionStatus.COMPLETED,
                                                peakRisk = currentRiskScore,
                                                finalRisk = currentRiskScore,
                                                highestStage = currentStage,
                                                inputSource = "SIMULATED_VIDEO_CALL"
                                            )
                                            val chain = ServiceLocator.evidenceRepository.getEvidenceForSession(sessionId)
                                            val report = IncidentReportExporter.generateReport(
                                                rakshaCallId = "DEMO-USER-001",
                                                session = session,
                                                transcripts = transcriptList,
                                                riskSignals = detectedSignals,
                                                evidenceChain = chain
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "RakshaCall Incident Report: $sessionId")
                                                putExtra(Intent.EXTRA_TEXT, report.formattedText)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Export Incident Report"))
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export Report", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Safety Brake Simulation Dialog (Section 9)
    if (showSafetyBrake) {
        SafetyBrakeDialog(
            riskScore = currentRiskScore,
            detectedTactics = detectedSignals.map { it.tactic.displayName }.distinct(),
            onPauseAndVerify = {
                showSafetyBrake = false
                onOpenVerificationCoach()
            },
            onContactTrustedPerson = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "RakshaCall Demo Simulation Alert: Coercive scam sequence reached critical threshold. Verifying Safety Brake.")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Alert Trusted Person"))
            },
            onDismiss = { showSafetyBrake = false }
        )
    }
}
