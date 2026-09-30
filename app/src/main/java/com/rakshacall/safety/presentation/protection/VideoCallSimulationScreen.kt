package com.rakshacall.safety.presentation.protection

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.TranscriptEvent
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
 * Prototype demo scenario definition for evaluators and judges.
 */
data class DemoScenario(
    val id: String,
    val title: String,
    val language: String,
    val turns: List<String>
)

val PROTOTYPE_SCENARIOS = listOf(
    DemoScenario(
        id = "police_money",
        title = "1. Police + Money Scam",
        language = "English",
        turns = listOf(
            "Hello, this is Inspector Vikram from Central Cyber Crime Cell.",
            "Your Aadhaar is involved in international money laundering and drugs parcel.",
            "This is urgent! Transfer ₹50,000 immediately to verify your clean account."
        )
    ),
    DemoScenario(
        id = "bank_otp",
        title = "2. Bank + OTP Scam",
        language = "English",
        turns = listOf(
            "Good afternoon, I am calling from RBI security division.",
            "Suspicious debit of ₹75,000 detected on your account right now.",
            "Tell me OTP right now to unfreeze your bank account!"
        )
    ),
    DemoScenario(
        id = "courier_payment",
        title = "3. Courier + Payment Scam",
        language = "English",
        turns = listOf(
            "FedEx Customs delivery cell here. A contraband parcel in your name was seized.",
            "Customs clearance fee of ₹25,000 is required right now to avoid police detention."
        )
    ),
    DemoScenario(
        id = "govt_threat",
        title = "4. Government + Threat Scam",
        language = "English",
        turns = listOf(
            "This is Enforcement Directorate officer calling regarding non-bailable warrant.",
            "You are under digital arrest. Police team is being dispatched to your house."
        )
    ),
    DemoScenario(
        id = "remote_access",
        title = "5. Remote Access Scam",
        language = "English",
        turns = listOf(
            "Technical support executive calling. Your device has been compromised.",
            "Please install AnyDesk immediately and share your screen to secure your funds."
        )
    ),
    DemoScenario(
        id = "investment_scam",
        title = "6. Investment Scam",
        language = "English",
        turns = listOf(
            "Exclusive wealth opportunity! Double your money in 7 days guaranteed.",
            "Guaranteed profit 100% risk free. Deposit ₹10,000 right now to claim reward."
        )
    ),
    DemoScenario(
        id = "benign_conv",
        title = "7. Benign Conversation",
        language = "English",
        turns = listOf(
            "Hey, I am near the police station right now.",
            "Can you transfer money to my mother for groceries?",
            "Also my college assignment is urgent, need to submit today."
        )
    ),
    DemoScenario(
        id = "tamil_scam",
        title = "8. Tamil Scam",
        language = "Tamil",
        turns = listOf(
            "நான் போலீஸ்ல இருந்து பேசுறேன்.",
            "உங்க ஆதார் மேல கேஸ் இருக்கு. இது அவசரம்.",
            "உடனே பணம் அனுப்புங்க இல்லன்னா கைது செய்வோம்."
        )
    ),
    DemoScenario(
        id = "tanglish_scam",
        title = "9. Tanglish Scam",
        language = "Tanglish",
        turns = listOf(
            "Naan police la irundhu pesuren. Idhu romba urgent.",
            "Unga account suspend aaga pogudhu. Udane money send pannunga."
        )
    ),
    DemoScenario(
        id = "hindi_scam",
        title = "10. Hindi Scam",
        language = "Hindi",
        turns = listOf(
            "Main police se bol raha hoon. Yeh bahut urgent hai.",
            "Aapke naam par arrest warrant nikla hai. Abhi paise bhejo."
        )
    ),
    DemoScenario(
        id = "hinglish_scam",
        title = "11. Hinglish Scam",
        language = "Hinglish",
        turns = listOf(
            "Main police department se bol raha hoon, bahut urgent hai.",
            "Account block ho jayega, so immediately money send karo."
        )
    ),
    DemoScenario(
        id = "telugu_scam",
        title = "12. Telugu Scam",
        language = "Telugu",
        turns = listOf(
            "Nenu police nundi matladutunnanu. Idi urgent.",
            "Ventane dabbu pampinchandi lekapothe arrest chestham."
        )
    ),
    DemoScenario(
        id = "code_switch_scam",
        title = "13. Multilingual Code-Switch Scam",
        language = "Code-Switch",
        turns = listOf(
            "Sir naan police department la irundhu pesuren, this is urgent.",
            "Main cyber cell se bol raha hoon, account suspend ho jayega.",
            "Immediately money transfer pannunga to clearance escrow account."
        )
    )
)

/**
 * Video Call Simulation Screen.
 * Demonstrates the RakshaCall real-time multilingual risk engine and protection actions.
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

    var selectedScenarioIndex by remember { mutableIntStateOf(0) }
    var currentStepIndex by remember { mutableIntStateOf(-1) }
    var isAutoPlaying by remember { mutableStateOf(false) }
    var callDurationSeconds by remember { mutableIntStateOf(0) }

    // Risk State
    var currentRiskScore by remember { mutableIntStateOf(0) }
    var previousRiskScore by remember { mutableIntStateOf(0) }
    var riskTrend by remember { mutableStateOf("→ Stable") }
    var currentLanguage by remember { mutableStateOf("English") }
    var languageConfidence by remember { mutableStateOf(0.92f) }

    val detectedSignals = remember { mutableStateListOf<RiskSignal>() }
    val transcriptList = remember { mutableStateListOf<TranscriptEvent>() }

    // Audio & Protection Controls
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var isCallPaused by remember { mutableStateOf(false) }
    var showProtectionDialog by remember { mutableStateOf(false) }

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

    fun injectDialogueTurn(turnText: String) {
        coroutineScope.launch {
            val event = TranscriptEvent(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                speaker = "CALLER",
                text = turnText
            )
            transcriptList.add(event)

            // Detect language & confidence
            val (lang, conf) = ServiceLocator.languageAwareTacticEngine.detectLanguage(turnText)
            currentLanguage = lang
            languageConfidence = conf

            // Run production Risk Engine
            val newSignals = ServiceLocator.riskEngine.analyzeTranscript(event, detectedSignals)
            for (sig in newSignals) {
                if (detectedSignals.none { it.tactic == sig.tactic && it.evidenceText == sig.evidenceText }) {
                    detectedSignals.add(sig)
                }
            }

            // Calculate transparent score
            previousRiskScore = currentRiskScore
            val newScore = ServiceLocator.riskEngine.calculateCurrentScore(
                allSignals = detectedSignals,
                sessionStartTime = sessionStartTime
            )
            currentRiskScore = newScore

            // Compute Trend
            riskTrend = when {
                newScore > previousRiskScore -> "↑ Increasing"
                newScore < previousRiskScore -> "↓ Decreasing"
                else -> "→ Stable"
            }

            // Check if intervention threshold crossed
            if (newScore >= 60 && !isCallPaused) {
                showProtectionDialog = true
                isAutoPlaying = false
            }

            if (transcriptList.isNotEmpty()) {
                listState.animateScrollToItem(transcriptList.size - 1)
            }
        }
    }

    fun resetScenario(newIndex: Int) {
        selectedScenarioIndex = newIndex
        currentStepIndex = -1
        isAutoPlaying = false
        callDurationSeconds = 0
        currentRiskScore = 0
        previousRiskScore = 0
        riskTrend = "→ Stable"
        detectedSignals.clear()
        transcriptList.clear()
        showProtectionDialog = false
        isCallPaused = false
        sessionId = "DEMO-SIM-" + UUID.randomUUID().toString().take(8)
    }

    // Auto-play loop for current scenario
    val activeScenario = PROTOTYPE_SCENARIOS[selectedScenarioIndex]
    LaunchedEffect(isAutoPlaying, currentStepIndex) {
        if (isAutoPlaying && currentStepIndex < activeScenario.turns.size - 1) {
            delay(3200L)
            currentStepIndex++
            injectDialogueTurn(activeScenario.turns[currentStepIndex])
        } else if (currentStepIndex >= activeScenario.turns.size - 1) {
            isAutoPlaying = false
        }
    }

    // Remote video pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha"
    )

    val currentRiskLevel = RiskLevel.fromScore(currentRiskScore)
    val riskColor = when (currentRiskLevel) {
        RiskLevel.CRITICAL -> RiskCritical
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> RiskMedium
        RiskLevel.LOW -> RiskLow
    }
    val riskBg = when (currentRiskLevel) {
        RiskLevel.CRITICAL -> RiskCriticalBg
        RiskLevel.HIGH -> RiskHighBg
        RiskLevel.MEDIUM -> RiskMediumBg
        RiskLevel.LOW -> RiskLowBg
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // ─── TOP BANNER: CLEARLY LABELED DEMO / SIMULATED CALL ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "RAKSHACALL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "DEMO VIDEO CALL",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF334155))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SIMULATED CALL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        }

        // ─── SCENARIO SELECTOR ───
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131D33))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(PROTOTYPE_SCENARIOS) { index, scenario ->
                val isSelected = selectedScenarioIndex == index
                FilterChip(
                    selected = isSelected,
                    onClick = { resetScenario(index) },
                    label = {
                        Text(
                            text = scenario.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TealPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFF94A3B8)
                    )
                )
            }
        }

        // ─── REMOTE VIDEO: SIMULATED CALLER ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color(0xFF1E293B))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isCallPaused) Color(0xFF64748B) else Color(0xFFDC2626).copy(alpha = pulseAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Caller",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Unknown Caller",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isCallPaused) "CALL PAUSED FOR SAFETY" else "Connected • 00:${callDurationSeconds.toString().padStart(2, '0')}",
                    fontSize = 11.sp,
                    color = if (isCallPaused) Color(0xFFF59E0B) else Color(0xFF94A3B8)
                )
            }
        }

        // ─── LIVE PROTECTION HUD ───
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE PROTECTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TealPrimary,
                        letterSpacing = 1.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Language: $currentLanguage",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${(languageConfidence * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Risk Score Box
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Risk Score: ",
                            fontSize = 14.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "$currentRiskScore/100",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = riskColor
                        )
                    }

                    // Risk Level Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(riskBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentRiskLevel.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = riskColor
                        )
                    }

                    // Trend Indicator
                    Text(
                        text = riskTrend,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            riskTrend.contains("↑") -> RiskCritical
                            riskTrend.contains("↓") -> RiskLow
                            else -> Color(0xFF94A3B8)
                        }
                    )
                }

                // Detected Tactics and Evidence
                if (detectedSignals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Detected Tactics & Evidence:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    detectedSignals.takeLast(3).forEach { sig ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⚠ ", fontSize = 12.sp, color = RiskHigh)
                            Text(
                                text = sig.tactic.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "\"${sig.evidenceText}\"",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No coercion tactics detected in speech.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        // ─── LIVE TRANSCRIPT ───
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE TRANSCRIPT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )

                    // Step Controller / Auto-play
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                if (currentStepIndex < activeScenario.turns.size - 1) {
                                    currentStepIndex++
                                    injectDialogueTurn(activeScenario.turns[currentStepIndex])
                                }
                            },
                            enabled = currentStepIndex < activeScenario.turns.size - 1,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(text = "Next Turn", fontSize = 11.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { isAutoPlaying = !isAutoPlaying },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isAutoPlaying) Color(0xFFDC2626) else TealPrimary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (isAutoPlaying) "Pause Auto" else "Auto Play",
                                fontSize = 11.sp,
                                color = if (isAutoPlaying) Color.White else Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (transcriptList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Tap 'Next Turn' or 'Auto Play' to stream transcript into Risk Engine",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transcriptList) { turn ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "CALLER",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TealPrimary
                                        )
                                        Text(
                                            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(turn.timestamp)),
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = turn.text,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── CALL CONTROLS: [ MUTE ], [ SPEAKER ], [ END CALL ], [ PROTECT ] ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B1120))
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color(0xFFDC2626) else Color(0xFF334155))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = Color.White
                        )
                    }
                    Text(text = "MUTE", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                }

                // Speaker
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) TealPrimary else Color(0xFF334155))
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Speaker",
                            tint = if (isSpeakerOn) Color.Black else Color.White
                        )
                    }
                    Text(text = "SPEAKER", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                }

                // End Call
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White
                        )
                    }
                    Text(text = "END CALL", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }

                // Protect Action
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showProtectionDialog = true },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (currentRiskScore >= 60) RiskCritical else TealPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Protect",
                            tint = if (currentRiskScore >= 60) Color.White else Color.Black
                        )
                    }
                    Text(text = "PROTECT", fontSize = 10.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ─── PROTECTION ACTION DIALOG ───
    if (showProtectionDialog) {
        AlertDialog(
            onDismissRequest = { showProtectionDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = RiskCritical,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "POTENTIAL SCAM DETECTED",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Multiple manipulation tactics detected in caller's speech.",
                        fontSize = 13.sp,
                        color = Color(0xFFE2E8F0)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Risk Level: ${currentRiskLevel.label} (${currentRiskScore}/100)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choose a protection action. The call will NOT be disconnected automatically.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isCallPaused = !isCallPaused
                            showProtectionDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = if (isCallPaused) "RESUME CALL" else "PAUSE CALL", fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    Button(
                        onClick = {
                            showProtectionDialog = false
                            onOpenVerificationCoach()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "VERIFY CALLER", fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    OutlinedButton(
                        onClick = {
                            showProtectionDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "ALERT TRUSTED CONTACT", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showProtectionDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "CONTINUE", color = Color.White)
                    }
                }
            },
            dismissButton = null
        )
    }
}
