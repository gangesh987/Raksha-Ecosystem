package com.rakshacall.safety.presentation.protection

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rakshacall.safety.domain.media.MediaConsentStatus
import com.rakshacall.safety.domain.media.MediaSourceStatus
import com.rakshacall.safety.domain.media.MediaSourceType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.core.export.IncidentReportExporter
import com.rakshacall.safety.core.notifications.NotificationHelper
import com.rakshacall.safety.core.security.EvidenceHasher
import com.rakshacall.safety.core.speech.SpeechRecognitionManager
import com.rakshacall.safety.core.speech.SpeechState
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.engine.StageTransition
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.ScamTactic
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

@Composable
fun ProtectionScreen(
    onNavigateBack: () -> Unit,
    onOpenVerificationCoach: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeSession by ServiceLocator.sessionRepository.observeActiveSession().collectAsState(initial = null)

    var currentRiskScore by remember { mutableIntStateOf(activeSession?.peakRisk ?: 0) }
    var currentStage by remember { mutableStateOf(activeSession?.highestStage ?: ScamStage.CONTACT) }
    val detectedSignals = remember { mutableStateListOf<RiskSignal>() }
    val transcriptList = remember { mutableStateListOf<TranscriptEvent>() }
    var showSafetyBrake by remember { mutableStateOf(false) }
    var showWhyDialog by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var isListening by remember { mutableStateOf(false) }

    var selectedSourceType by remember { mutableStateOf(MediaSourceType.MICROPHONE) }
    val currentMediaSource = remember(selectedSourceType) {
        ServiceLocator.mediaSourceRegistry.getSource(selectedSourceType)
    }
    val mediaSourceStatus by currentMediaSource.status.collectAsState()

    val velocityEngine = ServiceLocator.velocityEngine
    var currentVelocity by remember { mutableStateOf(velocityEngine.calculateVelocity(emptyList())) }

    val listState = rememberLazyListState()

    // Timer for elapsed seconds
    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            val start = activeSession!!.startTime
            while (true) {
                elapsedSeconds = (System.currentTimeMillis() - start) / 1000L
                delay(1000L)
            }
        }
    }

    // Speech manager for continuous chunk analysis
    val speechManager = remember {
        SpeechRecognitionManager(context) { text, isFinal ->
            if (activeSession != null && text.isNotBlank()) {
                coroutineScope.launch {
                    val event = TranscriptEvent(
                        id = UUID.randomUUID().toString(),
                        sessionId = activeSession!!.id,
                        timestamp = System.currentTimeMillis(),
                        speaker = "CALLER",
                        text = text,
                        confidence = 0.95f
                    )
                    transcriptList.add(event)
                    ServiceLocator.riskRepository.insertTranscriptEvent(event)

                    // Run Risk Engine
                    val newSignals = ServiceLocator.riskEngine.analyzeTranscript(event, detectedSignals)
                    for (sig in newSignals) {
                        detectedSignals.add(sig)
                        ServiceLocator.riskRepository.insertRiskSignal(sig)

                        // Update scam stage
                        val transition = ServiceLocator.scamStageMachine.processSignal(sig)
                        if (transition != null) {
                            currentStage = transition.toStage
                        }

                        // Append tamper-evident SHA-256 evidence event
                        val lastHash = ServiceLocator.evidenceRepository.getLastEvidenceHash(activeSession!!.id)
                        val evEvent = EvidenceHasher.createEvent(
                            eventId = UUID.randomUUID().toString(),
                            sessionId = activeSession!!.id,
                            timestamp = sig.timestamp,
                            eventType = "TACTIC_DETECTED",
                            payloadJson = "{\"tactic\":\"${sig.tactic.name}\",\"weight\":${sig.riskContribution},\"snippet\":\"${sig.evidenceText}\"}",
                            lastKnownHash = lastHash
                        )
                        ServiceLocator.evidenceRepository.appendEvidence(evEvent)
                    }

                    // Recalculate score
                    val newScore = ServiceLocator.riskEngine.calculateCurrentScore(
                        allSignals = detectedSignals,
                        sessionStartTime = activeSession!!.startTime
                    )
                    currentRiskScore = newScore

                    // Save trajectory point
                    ServiceLocator.riskRepository.recordRiskPoint(activeSession!!.id, System.currentTimeMillis(), newScore)

                    // Calculate velocity
                    currentVelocity = velocityEngine.calculateVelocity(detectedSignals)

                    // Update session entity in Room
                    val updatedSession = activeSession!!.copy(
                        peakRisk = maxOf(activeSession!!.peakRisk, newScore),
                        finalRisk = newScore,
                        highestStage = currentStage,
                        totalTacticsDetected = detectedSignals.size
                    )
                    ServiceLocator.sessionRepository.updateSession(updatedSession)

                    // Check Safety Brake trigger
                    if (ServiceLocator.riskEngine.isSafetyBrakeTriggered(newScore, detectedSignals)) {
                        showSafetyBrake = true
                        NotificationHelper.showSafetyBrakeNotification(context, sigName(detectedSignals), newScore)
                    }

                    listState.animateScrollToItem(transcriptList.size)
                }
            }
        }
    }

    val speechState by speechManager.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PROTECTION ACTIVE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = RiskHigh
                )
                val mins = elapsedSeconds / 60
                val secs = elapsedSeconds % 60
                Text(
                    text = "%02d:%02d".format(mins, secs),
                    fontSize = 12.sp,
                    color = Slate500
                )
            }

            IconButton(onClick = { showWhyDialog = true }) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Why", tint = TealPrimary)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Risk & Stage Dashboard Card
            item {
                val level = RiskLevel.fromScore(currentRiskScore)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (level) {
                            RiskLevel.CRITICAL -> RiskCriticalBg
                            RiskLevel.HIGH -> RiskHighBg
                            RiskLevel.MEDIUM -> RiskMediumBg
                            RiskLevel.LOW -> RiskLowBg
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = level.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = when (level) {
                                        RiskLevel.CRITICAL -> RiskCritical
                                        RiskLevel.HIGH -> RiskHigh
                                        RiskLevel.MEDIUM -> RiskMedium
                                        RiskLevel.LOW -> RiskLow
                                    }
                                )
                                Text(
                                    text = "$currentRiskScore / 100",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Velocity Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "VELOCITY", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = currentVelocity.level.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (currentVelocity.level) {
                                            VelocityLevel.HIGH -> RiskHigh
                                            VelocityLevel.MODERATE -> RiskMedium
                                            VelocityLevel.LOW -> RiskLow
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stage progression badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Stage: ", fontSize = 13.sp, color = Slate500)
                            Text(
                                text = currentStage.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (currentVelocity.explanation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = currentVelocity.explanation, fontSize = 11.sp, color = Slate500)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SOURCE: ${mediaSourceStatus.sourceType.displayName.uppercase()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500
                            )
                            Text(
                                text = "AUDIO: ${if (mediaSourceStatus.isAudioAvailable || isListening) "CONNECTED" else "STANDBY"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mediaSourceStatus.isAudioAvailable || isListening) TealPrimary else Slate500
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AI: GROQ / GEMINI HYBRID",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500
                            )
                            Text(
                                text = "LOCAL GUARDRAIL: ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        }
                    }
                }
            }

            // Permitted Call Media Monitoring Source Card
            item {
                MonitoringSourceCard(
                    status = mediaSourceStatus,
                    selectedType = selectedSourceType,
                    onSelectSource = { type ->
                        selectedSourceType = type
                    },
                    onRequestConsent = {
                        coroutineScope.launch {
                            currentMediaSource.requestConsent()
                            currentMediaSource.startMonitoring()
                        }
                    },
                    onDisconnect = {
                        coroutineScope.launch {
                            currentMediaSource.stopMonitoring()
                        }
                    }
                )
            }

            // Microphone Status Controller
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) TealPrimary else Slate500.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isListening) Color.White else Slate500,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isListening) "Listening on Microphone..." else "Microphone Standby",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Android SpeechRecognizer chunks",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isListening) {
                                    speechManager.stopListening()
                                    isListening = false
                                } else {
                                    speechManager.startListening()
                                    isListening = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isListening) RiskHigh else TealPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isListening) "Pause" else "Listen", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Detected Tactics Section
            item {
                Text(text = "Detected Coercive Tactics", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (detectedSignals.isEmpty()) {
                item {
                    Text(
                        text = "No coercive scam tactics detected yet. Waiting for speech...",
                        fontSize = 13.sp,
                        color = Slate500
                    )
                }
            } else {
                items(detectedSignals) { signal ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (signal.tactic.isIrreversibleAction) RiskHighBg else RiskMediumBg
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+${signal.riskContribution} ${signal.tactic.displayName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (signal.tactic.isIrreversibleAction) RiskHigh else RiskMedium
                                )
                                Text(
                                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(signal.timestamp)),
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Snippet: \"${signal.evidenceText}\"",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Live Transcript Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Live Conversation Stream", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            if (transcriptList.isEmpty()) {
                item {
                    Text(
                        text = "Transcript is empty. Speak into the microphone or test in Live Lab.",
                        fontSize = 13.sp,
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
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "${t.speaker}: ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TealPrimary
                        )
                        Text(text = t.text, fontSize = 13.sp)
                    }
                }
            }
        }

        // Action Bar (Stop Protection)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    speechManager.stopListening()
                    coroutineScope.launch {
                        if (activeSession != null) {
                            ServiceLocator.sessionRepository.endSession(
                                sessionId = activeSession!!.id,
                                finalRisk = currentRiskScore,
                                peakRisk = currentRiskScore
                            )
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop Protection Session", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Safety Brake Full-Screen Dialog
    if (showSafetyBrake) {
        SafetyBrakeDialog(
            riskScore = currentRiskScore,
            detectedTactics = detectedSignals.map { it.tactic.displayName }.distinct(),
            onPauseAndVerify = {
                showSafetyBrake = false
                onOpenVerificationCoach()
            },
            onContactTrustedPerson = {
                // Handoff to Android SMS / Share Intent
                val contacts = ServiceLocator.trustedContactRepository
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "RakshaCall Urgent Alert: High-risk coercive scam detected on my active call. Do not transfer funds. Please contact me.")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Alert Trusted Person"))
            },
            onDismiss = { showSafetyBrake = false }
        )
    }

    // Why / Explainable Fusion Dialog
    if (showWhyDialog) {
        AlertDialog(
            onDismissRequest = { showWhyDialog = false },
            title = { Text("Explainable Risk Fusion", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Risk Calculation Breakdown:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "• Primary Signal: Deterministic Conversation NLP (+${detectedSignals.sumOf { it.riskContribution }} pts)", fontSize = 13.sp)
                    Text(text = "• Manipulation Velocity: ${currentVelocity.level.name}", fontSize = 13.sp)
                    Text(text = "• Current Stage: ${currentStage.displayName}", fontSize = 13.sp)
                    Text(text = "• Tamper-Evident SHA-256 blocks: ${detectedSignals.size}", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Principle: Conversation evidence remains primary. Irreversible tactics (payment/credentials) directly trigger Safety Brake.", fontSize = 12.sp, color = Slate500)
                }
            },
            confirmButton = {
                Button(onClick = { showWhyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)) {
                    Text("Understood")
                }
            }
        )
    }
}

private fun sigName(signals: List<RiskSignal>): String {
    return signals.lastOrNull { it.tactic.isIrreversibleAction }?.tactic?.displayName
        ?: signals.lastOrNull()?.tactic?.displayName
        ?: "High-Risk Coercion"
}

@Composable
fun SafetyBrakeDialog(
    riskScore: Int,
    detectedTactics: List<String>,
    onPauseAndVerify: () -> Unit,
    onContactTrustedPerson: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = RiskCritical, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "⚠ HIGH-RISK CONVERSATION", fontWeight = FontWeight.ExtraBold, color = RiskCritical, fontSize = 16.sp)
            }
        },
        text = {
            Column {
                Text(
                    text = "Irreversible coercive pressure detected ($riskScore/100).",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(RiskCriticalBg)
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "• DO NOT TRANSFER MONEY", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = RiskCritical)
                        Text(text = "• DO NOT SHARE OTP / UPI PIN", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = RiskCritical)
                        Text(text = "• DO NOT FOLLOW REMOTE-ACCESS INSTRUCTIONS", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = RiskCritical)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tactics identified: ${detectedTactics.joinToString(", ")}",
                    fontSize = 11.sp,
                    color = Slate500
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onPauseAndVerify,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("PAUSE & VERIFY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onContactTrustedPerson) {
                Text("CONTACT TRUSTED PERSON")
            }
        }
    )
}

@Composable
fun MonitoringSourceCard(
    status: MediaSourceStatus,
    selectedType: MediaSourceType,
    onSelectSource: (MediaSourceType) -> Unit,
    onRequestConsent: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MONITORING SOURCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Slate500
                )
                // Connection Pill: App NEVER displays CALL MONITORED unless stream is connected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (status.isConnected) TealPrimary.copy(alpha = 0.2f) else Slate500.copy(alpha = 0.15f))
                        .border(1.dp, if (status.isConnected) TealPrimary else Slate500, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (status.isConnected) "STREAM CONNECTED" else "DISCONNECTED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (status.isConnected) TealPrimary else Slate500
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Source Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MediaSourceType.values()) { type ->
                    val isSelected = type == selectedType
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) TealPrimary else MaterialTheme.colorScheme.surface)
                            .clickable { onSelectSource(type) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = type.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Stream Status Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StreamBadge(label = "AUDIO", isAvailable = status.isAudioAvailable)
                StreamBadge(label = "VIDEO", isAvailable = status.isVideoAvailable)
                StreamBadge(label = "TRANSCRIPT", isAvailable = status.isTranscriptAvailable)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics: Latency & Consent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Latency: ${if (status.isConnected) "${status.latencyMs}ms" else "N/A"}",
                    fontSize = 11.sp,
                    color = Slate500
                )
                Text(
                    text = "Consent: ${status.consentStatus.name}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (status.consentStatus) {
                        MediaConsentStatus.GRANTED -> TealPrimary
                        MediaConsentStatus.REQUIRED -> RiskMedium
                        MediaConsentStatus.RESTRICTED_BY_PLATFORM -> RiskHigh
                        MediaConsentStatus.REVOKED -> RiskCritical
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Detail / Platform sandboxing explanation
            Text(
                text = status.detailMessage,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!status.isConnected) {
                    Button(
                        onClick = onRequestConsent,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Connect / Grant Consent", fontSize = 11.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Disconnect Source", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamBadge(label: String, isAvailable: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isAvailable) TealPrimary.copy(alpha = 0.15f) else Slate500.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isAvailable) TealPrimary else Slate500)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label ${if (isAvailable) "YES" else "NO"}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAvailable) TealPrimary else Slate500
        )
    }
}
