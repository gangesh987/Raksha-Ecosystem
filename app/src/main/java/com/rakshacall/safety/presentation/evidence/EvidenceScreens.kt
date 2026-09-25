package com.rakshacall.safety.presentation.evidence

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.core.export.IncidentReportExporter
import com.rakshacall.safety.core.security.EvidenceHasher
import com.rakshacall.safety.core.security.IntegrityResult
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.EvidenceEvent
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.RiskSignal
import com.rakshacall.safety.domain.model.TranscriptEvent
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
fun EvidenceVaultScreen(
    onSelectSession: (String) -> Unit
) {
    val allSessions by ServiceLocator.sessionRepository.observeAllSessions().collectAsState(initial = emptyList())
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filterLabels = listOf("All", "High Risk", "Payment Demands", "Credential Pressure")
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    val filteredSessions = allSessions.filter { session ->
        val matchesSearch = session.id.contains(searchQuery, ignoreCase = true) ||
                session.highestStage.displayName.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilterIndex) {
            1 -> session.peakRisk >= 60
            2 -> session.highestStage.order >= 5
            3 -> session.highestStage.order >= 5
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "EVIDENCE VAULT", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TealPrimary, letterSpacing = 1.sp)
        Text(text = "Tamper-evident incident timeline with SHA-256 integrity.", fontSize = 12.sp, color = Slate500)

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by session ID or stage...", fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedFilterIndex,
            edgePadding = 0.dp,
            divider = {}
        ) {
            filterLabels.forEachIndexed { index, label ->
                Tab(
                    selected = selectedFilterIndex == index,
                    onClick = { selectedFilterIndex = index },
                    text = { Text(label, fontSize = 12.sp, fontWeight = if (selectedFilterIndex == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Slate500, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "No evidence records found", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "Complete a protection session to generate an evidence log.", color = Slate500, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredSessions) { session ->
                    EvidenceSessionCard(session = session, dateFormat = dateFormat) {
                        onSelectSession(session.id)
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun EvidenceSessionCard(session: ProtectionSession, dateFormat: SimpleDateFormat, onClick: () -> Unit) {
    val level = RiskLevel.fromScore(session.peakRisk)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Incident #${session.id.take(8)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

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

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Date: ${dateFormat.format(Date(session.startTime))}", fontSize = 12.sp, color = Slate500)
            Text(text = "Highest Stage: ${session.highestStage.displayName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = "Tactics Detected: ${session.totalTacticsDetected}", fontSize = 12.sp, color = Slate500)
        }
    }
}

@Composable
fun EvidenceDetailScreen(
    sessionId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var session by remember { mutableStateOf<ProtectionSession?>(null) }
    var transcripts by remember { mutableStateOf<List<TranscriptEvent>>(emptyList()) }
    var riskSignals by remember { mutableStateOf<List<RiskSignal>>(emptyList()) }
    var evidenceChain by remember { mutableStateOf<List<EvidenceEvent>>(emptyList()) }
    var userProfile by remember { mutableStateOf<com.rakshacall.safety.domain.model.User?>(null) }

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    LaunchedEffect(sessionId) {
        session = ServiceLocator.sessionRepository.getSession(sessionId)
        transcripts = ServiceLocator.riskRepository.getTranscripts(sessionId)
        riskSignals = ServiceLocator.riskRepository.getRiskSignals(sessionId)
        evidenceChain = ServiceLocator.evidenceRepository.getEvidenceForSession(sessionId)
        userProfile = ServiceLocator.userRepository.getActiveUser()
    }

    val integrityResult = remember(evidenceChain) { EvidenceHasher.verifyChain(evidenceChain) }
    val isChainValid = integrityResult is IntegrityResult.Valid

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "INCIDENT EVIDENCE LOG", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)
        }

        if (session == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Loading incident details...", color = Slate500)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cryptographic Integrity Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChainValid) RiskLowBg else RiskCriticalBg
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isChainValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isChainValid) RiskLow else RiskCritical,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isChainValid) "Integrity: VALID" else "Integrity: FAILED",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isChainValid) RiskLow else RiskCritical
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isChainValid)
                                    "Tamper-evident incident timeline verified across ${evidenceChain.size} cryptographic block hashes."
                                else
                                    "Hash chain mismatch detected. Record may have been modified or deleted.",
                                fontSize = 12.sp,
                                color = if (isChainValid) Color(0xFF14532D) else Color(0xFF7F1D1D)
                            )
                        }
                    }
                }

                // Session Meta
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "Session ID: ${session!!.id}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Recorded: ${fullDateFormat.format(Date(session!!.startTime))}", fontSize = 12.sp, color = Slate500)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Peak Risk: ${session!!.peakRisk}/100 • Stage: ${session!!.highestStage.displayName}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Timeline of Actual Detected Events
                item {
                    Text(text = "Tamper-Evident Event Timeline", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (riskSignals.isEmpty()) {
                    item {
                        Text(text = "No coercive tactics recorded in this session.", fontSize = 13.sp, color = Slate500)
                    }
                } else {
                    items(riskSignals) { sig ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = timeFormat.format(Date(sig.timestamp)),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = TealPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "+${sig.riskContribution} ${sig.tactic.displayName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Evidence: \"${sig.evidenceText}\"",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }
                }

                // Full Transcript
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Recorded Speech Transcript (${transcripts.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                if (transcripts.isEmpty()) {
                    item {
                        Text(text = "No transcript dialogue captured.", fontSize = 12.sp, color = Slate500)
                    }
                } else {
                    items(transcripts) { t ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                text = "${timeFormat.format(Date(t.timestamp))} ",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate500
                            )
                            Text(
                                text = "${t.speaker}: ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TealPrimary
                            )
                            Text(text = t.text, fontSize = 12.sp)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }

            // Export & Share Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val report = IncidentReportExporter.generateReport(
                            rakshaCallId = userProfile?.rakshaCallId ?: "RC-UNKNOWN",
                            session = session!!,
                            transcripts = transcripts,
                            riskSignals = riskSignals,
                            evidenceChain = evidenceChain
                        )
                        IncidentReportExporter.shareReport(context, report.formattedText)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export & Share")
                }
            }
        }
    }
}
