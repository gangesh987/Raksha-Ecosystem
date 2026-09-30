package com.rakshacall.safety.presentation.home

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rakshacall.R
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.SessionStatus
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: com.rakshacall.safety.presentation.viewmodel.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onStartProtection: () -> Unit,
    onViewActiveProtection: () -> Unit,
    onNavigateToIntelligence: () -> Unit,
    onNavigateToEvidence: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenLiveInputLab: () -> Unit,
    onOpenVideoCallSimulation: () -> Unit,
    onWatchTour: () -> Unit = {},
    onSelectSession: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeUser = uiState.activeUser
    val activeSession = uiState.activeSession
    val allSessions = uiState.recentSessions
    val contacts = uiState.contacts
    val isSessionActive = uiState.isSessionActive
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "RakshaCall App Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "RAKSHA CALL",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = TealPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeUser?.rakshaCallId?.let { "ID: $it" } ?: "Personal Safety Layer",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${uiState.syncStatus.name}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onNavigateToPrivacy) {
                        Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = "Privacy", tint = Slate500)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Slate500)
                    }
                }
            }
        }

        // Active State or Ready Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSessionActive) {
                        val level = RiskLevel.fromScore(activeSession?.peakRisk ?: 0)
                        when (level) {
                            RiskLevel.CRITICAL -> RiskCriticalBg
                            RiskLevel.HIGH -> RiskHighBg
                            RiskLevel.MEDIUM -> RiskMediumBg
                            RiskLevel.LOW -> RiskLowBg
                        }
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isSessionActive) RiskHigh else RiskLow)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSessionActive) "PROTECTION ACTIVE" else "PROTECTION READY",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                color = if (isSessionActive) RiskHigh else RiskLow
                            )
                        }

                        if (isSessionActive) {
                            Text(
                                text = "Peak: ${activeSession?.peakRisk ?: 0}/100",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isSessionActive) {
                        Text(
                            text = "Current Scam Stage: ${activeSession?.highestStage?.displayName ?: "Contact"}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Analyzing live speech and tactics on-device.",
                            fontSize = 13.sp,
                            color = Slate500
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onViewActiveProtection,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Open Live Protection Console", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Section 15: Home Dashboard Status
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("Current Risk", fontSize = 11.sp, color = Slate500)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("LOW (0/100)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RiskLow)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("Last Detection", fontSize = 11.sp, color = Slate500)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("None", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("Trusted Contact", fontSize = 11.sp, color = Slate500)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            if (contacts.isNotEmpty()) "Configured (${contacts.size})" else "Not Configured",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (contacts.isNotEmpty()) TealPrimary else Slate500
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("Evidence", fontSize = 11.sp, color = Slate500)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Ready (${allSessions.size} sealed)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Primary and Secondary Actions (Section 15)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onStartProtection,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.home_start_protection), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onOpenVideoCallSimulation,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp), tint = TealPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.call_title), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Real-Time WebRTC Protected Call Room
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVideoCallSimulation() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TealPrimary.copy(alpha = 0.12f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Real-Time WebRTC Safety Call",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(RiskHighBg)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text("DEMO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RiskHigh)
                                }
                            }
                            Text(
                                text = "Interactive 3D scam sequence: Authority → Fear → Isolation → Payment → OTP",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TealPrimary
                    )
                }
            }
        }

        // Hero Story Tour (Section 1: Detect → Warn → Verify → Connect → Preserve)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWatchTour() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Watch Hero Experience", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                text = "11-Step Architecture Tour (Detect → Warn → Verify → Connect → Preserve)",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Slate500)
                }
            }
        }

        // Real Stats Grid (Zero Mock Data)
        item {
            Text(text = "Security Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Recorded Sessions",
                    value = allSessions.size.toString(),
                    emptyNote = if (allSessions.isEmpty()) "No sessions yet" else null
                )
                val highRiskCount = allSessions.count { it.peakRisk >= 60 }
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "High-Risk Incidents",
                    value = highRiskCount.toString(),
                    emptyNote = if (allSessions.isEmpty()) "No incidents" else null
                )
            }
        }

        // Trusted Contacts Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToContacts() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Contacts, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Trusted Contacts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = if (contacts.isEmpty()) "No trusted contacts configured" else "${contacts.size} emergency contacts ready",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                    }
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Slate500)
                }
            }
        }

        // Real Recent Sessions List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Recent Sessions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (allSessions.isNotEmpty()) {
                    Text(
                        text = "View All",
                        color = TealPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToEvidence() }
                    )
                }
            }
        }

        if (allSessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = Slate500, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No protection sessions yet", fontWeight = FontWeight.Medium, color = Slate500, fontSize = 14.sp)
                        Text(text = "Start a session to monitor your calls.", color = Slate500, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(allSessions.take(3)) { session ->
                SessionItemCard(session = session, dateFormat = dateFormat, onClick = { onSelectSession(session.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String, emptyNote: String?) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = label, fontSize = 12.sp, color = Slate500)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (emptyNote != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = emptyNote, fontSize = 11.sp, color = Slate500)
            }
        }
    }
}

@Composable
fun SessionItemCard(session: ProtectionSession, dateFormat: SimpleDateFormat, onClick: () -> Unit) {
    val level = RiskLevel.fromScore(session.peakRisk)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            Column {
                Text(
                    text = "Session #${session.id.take(8)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${dateFormat.format(Date(session.startTime))} • Stage: ${session.highestStage.displayName}",
                    fontSize = 12.sp,
                    color = Slate500
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when (level) {
                            RiskLevel.CRITICAL -> RiskCriticalBg
                            RiskLevel.HIGH -> RiskHighBg
                            RiskLevel.MEDIUM -> RiskMediumBg
                            RiskLevel.LOW -> RiskLowBg
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${session.peakRisk} / 100",
                    color = when (level) {
                        RiskLevel.CRITICAL -> RiskCritical
                        RiskLevel.HIGH -> RiskHigh
                        RiskLevel.MEDIUM -> RiskMedium
                        RiskLevel.LOW -> RiskLow
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
