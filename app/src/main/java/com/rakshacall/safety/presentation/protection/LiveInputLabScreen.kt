package com.rakshacall.safety.presentation.protection

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.core.security.EvidenceHasher
import com.rakshacall.safety.core.speech.SpeechRecognitionManager
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.SessionStatus
import com.rakshacall.safety.domain.model.TranscriptEvent
import com.rakshacall.safety.domain.model.VelocityLevel
import com.rakshacall.safety.presentation.theme.RiskCritical
import com.rakshacall.safety.presentation.theme.RiskCriticalBg
import com.rakshacall.safety.presentation.theme.RiskHigh
import com.rakshacall.safety.presentation.theme.RiskHighBg
import com.rakshacall.safety.presentation.theme.RiskLow
import com.rakshacall.safety.presentation.theme.RiskLowBg
import com.rakshacall.safety.presentation.theme.RiskMedium
import com.rakshacall.safety.presentation.theme.RiskMediumBg
import com.rakshacall.safety.presentation.theme.Slate500
import com.rakshacall.safety.presentation.theme.TealPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun LiveInputLabScreen(
    onNavigateBack: () -> Unit,
    onOpenVerificationCoach: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sessionId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var sessionStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var currentRiskScore by remember { mutableIntStateOf(0) }
    var currentStage by remember { mutableStateOf(ScamStage.CONTACT) }
    val detectedSignals = remember { mutableStateListOf<RiskSignal>() }
    val transcriptList = remember { mutableStateListOf<TranscriptEvent>() }
    var customText by remember { mutableStateOf("") }
    var isMicActive by remember { mutableStateOf(false) }
    var showSafetyBrake by remember { mutableStateOf(false) }

    val velocityEngine = ServiceLocator.velocityEngine
    var velocityCalc by remember { mutableStateOf(velocityEngine.calculateVelocity(emptyList())) }

    fun processInputText(text: String) {
        if (text.isBlank()) return

        coroutineScope.launch {
            val event = TranscriptEvent(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                speaker = "CALLER",
                text = text.trim()
            )
            transcriptList.add(event)
            ServiceLocator.riskRepository.insertTranscriptEvent(event)

            // 1. Run local deterministic RiskEngine
            val newSignals = ServiceLocator.riskEngine.analyzeTranscript(event, detectedSignals)
            for (sig in newSignals) {
                detectedSignals.add(sig)
                ServiceLocator.riskRepository.insertRiskSignal(sig)

                // 2. Scam stage transition
                val transition = ServiceLocator.scamStageMachine.processSignal(sig)
                if (transition != null) {
                    currentStage = transition.toStage
                }

                // 3. Append SHA-256 evidence block
                val lastHash = ServiceLocator.evidenceRepository.getLastEvidenceHash(sessionId)
                val ev = EvidenceHasher.createEvent(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    timestamp = sig.timestamp,
                    eventType = "TACTIC_DETECTED",
                    payloadJson = "{\"tactic\":\"${sig.tactic.name}\",\"weight\":${sig.riskContribution},\"evidence\":\"${sig.evidenceText}\"}",
                    lastKnownHash = lastHash
                )
                ServiceLocator.evidenceRepository.appendEvidence(ev)
            }

            // 4. Recalculate risk score
            val newScore = ServiceLocator.riskEngine.calculateCurrentScore(detectedSignals, sessionStartTime)
            currentRiskScore = newScore

            // 5. Persist risk trajectory point in Room
            ServiceLocator.riskRepository.recordRiskPoint(sessionId, System.currentTimeMillis(), newScore)

            // 6. Recalculate manipulation velocity
            velocityCalc = velocityEngine.calculateVelocity(detectedSignals)

            // 7. Update ProtectionSession in Room
            val session = ProtectionSession(
                id = sessionId,
                startTime = sessionStartTime,
                status = SessionStatus.ACTIVE,
                peakRisk = newScore,
                finalRisk = newScore,
                highestStage = currentStage,
                inputSource = "LIVE_LAB",
                totalTacticsDetected = detectedSignals.size,
                isDemoSession = true
            )
            ServiceLocator.sessionRepository.createSession(session)

            // 8. Trigger Safety Brake if high-risk + irreversible action
            if (ServiceLocator.riskEngine.isSafetyBrakeTriggered(newScore, detectedSignals)) {
                showSafetyBrake = true
            }
        }
    }

    val speechManager = remember {
        SpeechRecognitionManager(context) { recognized, isFinal ->
            if (isFinal && recognized.isNotBlank()) {
                processInputText(recognized)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(text = "LIVE INPUT LAB", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)
                    Text(text = "Interactive Speech & Tactic Verification", fontSize = 11.sp, color = Slate500)
                }
            }

            IconButton(onClick = {
                // Reset lab session
                sessionId = UUID.randomUUID().toString()
                sessionStartTime = System.currentTimeMillis()
                currentRiskScore = 0
                currentStage = ScamStage.CONTACT
                detectedSignals.clear()
                transcriptList.clear()
                ServiceLocator.scamStageMachine.reset()
                velocityCalc = velocityEngine.calculateVelocity(emptyList())
            }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Slate500)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Status Card
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
                                    fontSize = 12.sp,
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

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "STAGE", fontSize = 10.sp, color = Slate500, fontWeight = FontWeight.Bold)
                                Text(
                                    text = currentStage.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Velocity: ${velocityCalc.level.name}",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }
                }
            }

            // Real Microphone Speech Input Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isMicActive) RiskHigh else TealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isMicActive) "Microphone Recording Active" else "Speech Recognition Ready",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Speak live to test on-device NLP",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isMicActive) {
                                    speechManager.stopListening()
                                    isMicActive = false
                                } else {
                                    speechManager.startListening()
                                    isMicActive = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isMicActive) RiskHigh else TealPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isMicActive) "Stop" else "Speak", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Fast Test Utterances for Presentation
            item {
                Text(text = "Quick Presenter Phrases (Tap to Test)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DemoPhraseChip("1. Authority: \"I am calling from Mumbai Police Cyber Crime Cell.\"") {
                        processInputText("I am calling from Mumbai Police Cyber Crime Cell.")
                    }
                    DemoPhraseChip("2. Criminal Allegation: \"Your Aadhaar is involved in illegal money laundering.\"") {
                        processInputText("Your Aadhaar is involved in illegal money laundering.")
                    }
                    DemoPhraseChip("3. Isolation: \"Do not disconnect the call. Stay in a quiet closed room.\"") {
                        processInputText("Do not disconnect the call. Stay in a quiet closed room.")
                    }
                    DemoPhraseChip("4. Payment Pressure: \"Transfer ₹50,000 immediately to RBI verification account.\"") {
                        processInputText("Transfer ₹50,000 immediately to RBI verification account.")
                    }
                    DemoPhraseChip("5. OTP Pressure: \"Give me your OTP right now to stop arrest warrant.\"") {
                        processInputText("Give me your OTP right now to stop arrest warrant.")
                    }
                }
            }

            // Custom Text Input
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it },
                        label = { Text("Type custom speech...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (customText.isNotBlank()) {
                                processInputText(customText)
                                customText = ""
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(TealPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }

            // Detected Tactics Stream
            item {
                Text(text = "Real Detected Signals (${detectedSignals.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            if (detectedSignals.isEmpty()) {
                item {
                    Text(text = "No tactics detected yet. Speak or select a phrase.", fontSize = 12.sp, color = Slate500)
                }
            } else {
                items(detectedSignals) { sig ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (sig.tactic.isIrreversibleAction) RiskHighBg else RiskMediumBg
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "+${sig.riskContribution} ${sig.tactic.displayName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (sig.tactic.isIrreversibleAction) RiskHigh else RiskMedium
                                )
                                Text(
                                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sig.timestamp)),
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                            Text(text = "Evidence: \"${sig.evidenceText}\"", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showSafetyBrake) {
        SafetyBrakeDialog(
            riskScore = currentRiskScore,
            detectedTactics = detectedSignals.map { it.tactic.displayName }.distinct(),
            onPauseAndVerify = {
                showSafetyBrake = false
                onOpenVerificationCoach()
            },
            onContactTrustedPerson = {
                showSafetyBrake = false
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "RakshaCall Alert: Coercive scam pressure detected on my call. Please verify before any money transfer.")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Alert Contact"))
            },
            onDismiss = { showSafetyBrake = false }
        )
    }
}

@Composable
private fun DemoPhraseChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
