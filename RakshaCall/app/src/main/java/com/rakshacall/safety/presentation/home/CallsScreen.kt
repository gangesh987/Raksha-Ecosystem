package com.rakshacall.safety.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.data.local.database.RakshaDatabase
import com.rakshacall.safety.data.local.entities.ProtectionSessionEntity
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.presentation.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    onSelectSession: (String) -> Unit,
    onStartNewProtectedCall: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { RakshaDatabase.getInstance(context) }
    val sessions by database.observeSessions().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        database.getAllSessions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PROTECTED CALLS", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartNewProtectedCall,
                containerColor = TealPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddIcCall, contentDescription = "Start Call")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Analytics Summary Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${sessions.size}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TealPrimary)
                            Text("Total Sessions", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val highRiskCount = sessions.count { it.peakRisk >= 60 }
                            Text("$highRiskCount", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = if (highRiskCount > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32))
                            Text("High Risk Blocked", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val totalTactics = sessions.sumOf { it.totalTacticsDetected }
                            Text("$totalTactics", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Tactics Deflected", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            item {
                Text("CALL HISTORY & ACTIVE SESSIONS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }

            if (sessions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No protected calls yet", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Use Protect A Call or create a Protected Room to start.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(sessions) { session ->
                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(session.startTime))
                    val riskLevel = RiskLevel.fromScore(session.peakRisk)
                    val badgeColor = when (riskLevel) {
                        RiskLevel.LOW -> Color(0xFF2E7D32)
                        RiskLevel.MEDIUM -> Color(0xFFF57C00)
                        RiskLevel.HIGH -> Color(0xFFD32F2F)
                        RiskLevel.CRITICAL -> Color(0xFFB71C1C)
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSession(session.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = badgeColor, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Session ${session.id.take(8)}...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Source: ${session.inputSource} • $dateStr", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Highest Stage: ${session.highestStage}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "${session.peakRisk}/100",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(riskLevel.label, fontSize = 10.sp, color = badgeColor, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}
