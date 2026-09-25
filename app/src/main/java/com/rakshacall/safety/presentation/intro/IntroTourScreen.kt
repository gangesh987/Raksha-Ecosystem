package com.rakshacall.safety.presentation.intro

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.presentation.theme.Navy900
import com.rakshacall.safety.presentation.theme.RiskCritical
import com.rakshacall.safety.presentation.theme.RiskHigh
import com.rakshacall.safety.presentation.theme.RiskLow
import com.rakshacall.safety.presentation.theme.RiskMedium
import com.rakshacall.safety.presentation.theme.Slate500
import com.rakshacall.safety.presentation.theme.TealPrimary
import kotlinx.coroutines.delay

data class IntroStep(
    val index: Int,
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val riskScore: Int,
    val stage: String,
    val color: Color
)

val INTRO_STEPS = listOf(
    IntroStep(
        index = 1,
        title = "1. Incoming Digital-Arrest Call",
        description = "Unknown video call initiates. Caller claims to be an emergency government entity.",
        icon = Icons.Default.Call,
        riskScore = 5,
        stage = "CONTACT",
        color = RiskLow
    ),
    IntroStep(
        index = 2,
        title = "2. Authority Impersonation",
        description = "\"Calling from Mumbai Police Cyber Crime Cell. Do not question official authority.\"",
        icon = Icons.Default.Shield,
        riskScore = 20,
        stage = "AUTHORITY",
        color = RiskLow
    ),
    IntroStep(
        index = 3,
        title = "3. Fear & Criminal Accusation",
        description = "\"Your Aadhaar is implicated in an illegal parcel and money-laundering warrant.\"",
        icon = Icons.Default.Warning,
        riskScore = 40,
        stage = "FEAR",
        color = RiskMedium
    ),
    IntroStep(
        index = 4,
        title = "4. Coercive Isolation",
        description = "\"Do not disconnect. Go into a private room. Do not inform your family or lawyer.\"",
        icon = Icons.Default.Lock,
        riskScore = 58,
        stage = "ISOLATION",
        color = RiskMedium
    ),
    IntroStep(
        index = 5,
        title = "5. Irreversible Payment Demand",
        description = "\"Transfer ₹50,000 immediately into the RBI temporary clearance escrow account.\"",
        icon = Icons.Default.AccountBalance,
        riskScore = 78,
        stage = "PAYMENT_CREDENTIAL",
        color = RiskHigh
    ),
    IntroStep(
        index = 6,
        title = "6. Risk Meter Surge",
        description = "Cumulative multi-tactic NLP and manipulation velocity spike past HIGH threshold.",
        icon = Icons.Default.Speed,
        riskScore = 85,
        stage = "ESCALATION",
        color = RiskCritical
    ),
    IntroStep(
        index = 7,
        title = "7. 🛑 SAFETY BRAKE ACTIVATES",
        description = "Full-screen intervention halts the victim before irreversible financial action.",
        icon = Icons.Default.StopCircle,
        riskScore = 85,
        stage = "ESCALATION",
        color = RiskCritical
    ),
    IntroStep(
        index = 8,
        title = "8. Independent Verification Coach",
        description = "Guides user to disconnect and independently dial official cybercrime channels (1930 / cybercrime.gov.in).",
        icon = Icons.Default.VerifiedUser,
        riskScore = 85,
        stage = "VERIFY",
        color = TealPrimary
    ),
    IntroStep(
        index = 9,
        title = "9. Trusted Contact Escalation",
        description = "Prepares honest emergency alert handoff to designated family/friends.",
        icon = Icons.Default.Call,
        riskScore = 85,
        stage = "CONNECT",
        color = TealPrimary
    ),
    IntroStep(
        index = 10,
        title = "10. Append-Only Evidence Vault",
        description = "Cryptographically seals transcript and signals into a tamper-evident SHA-256 hash chain.",
        icon = Icons.Default.Security,
        riskScore = 85,
        stage = "PRESERVE",
        color = TealPrimary
    ),
    IntroStep(
        index = 11,
        title = "RakshaCall Safety Platform",
        description = "\"See the risk. Stop the pressure. Stay protected.\"",
        icon = Icons.Default.CheckCircle,
        riskScore = 0,
        stage = "PROTECTED",
        color = TealPrimary
    )
)

@Composable
fun IntroTourScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var isAutoPlaying by remember { mutableStateOf(true) }

    LaunchedEffect(isAutoPlaying, currentStepIndex) {
        if (isAutoPlaying) {
            delay(2200)
            if (currentStepIndex < INTRO_STEPS.size - 1) {
                currentStepIndex++
            } else {
                isAutoPlaying = false
            }
        }
    }

    val step = INTRO_STEPS[currentStepIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy900)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "PRODUCT TOUR (12s INTRO)",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            )
            IconButton(onClick = { isAutoPlaying = !isAutoPlaying }) {
                Icon(
                    imageVector = if (isAutoPlaying) Icons.Default.StopCircle else Icons.Default.PlayArrow,
                    contentDescription = if (isAutoPlaying) "Pause" else "Play",
                    tint = Color.White
                )
            }
        }

        // Progress indicator
        LinearProgressIndicator(
            progress = { (currentStepIndex + 1).toFloat() / INTRO_STEPS.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = TealPrimary,
            trackColor = Color.White.copy(alpha = 0.15f)
        )

        // Center Animated Content
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "introStepTransition"
        ) { targetStep ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(targetStep.color.copy(alpha = 0.18f))
                        .border(2.dp, targetStep.color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = targetStep.icon,
                        contentDescription = targetStep.title,
                        tint = targetStep.color,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = targetStep.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = targetStep.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Real telemetry card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SIMULATED RISK", fontSize = 11.sp, color = Slate500)
                            Text(
                                "${targetStep.riskScore} / 100",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = targetStep.color
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SCAM STAGE", fontSize = 11.sp, color = Slate500)
                            Text(
                                targetStep.stage,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentStepIndex > 0) {
                            currentStepIndex--
                            isAutoPlaying = false
                        }
                    },
                    enabled = currentStepIndex > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Previous")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (currentStepIndex < INTRO_STEPS.size - 1) {
                            currentStepIndex++
                            isAutoPlaying = false
                        } else {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text(if (currentStepIndex == INTRO_STEPS.size - 1) "Finish Tour" else "Next")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Video asset: media/RakshaCall_Intro_12s.mp4",
                fontSize = 11.sp,
                color = Slate500,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
