package com.rakshacall.safety.presentation.intelligence

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.RiskLevel
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.presentation.theme.RiskCritical
import com.rakshacall.safety.presentation.theme.RiskHigh
import com.rakshacall.safety.presentation.theme.RiskLow
import com.rakshacall.safety.presentation.theme.RiskMedium
import com.rakshacall.safety.presentation.theme.Slate300
import com.rakshacall.safety.presentation.theme.Slate500
import com.rakshacall.safety.presentation.theme.TealPrimary

@Composable
fun IntelligenceScreen() {
    val rawSessions by ServiceLocator.sessionRepository.observeAllSessions().collectAsState(initial = emptyList())
    val allSessions = rawSessions.filter { !it.isDemoSession }
    val activeSession by ServiceLocator.sessionRepository.observeActiveSession().collectAsState(initial = null)

    val targetSessionId = activeSession?.id ?: allSessions.firstOrNull()?.id
    val riskPoints by if (targetSessionId != null) {
        ServiceLocator.riskRepository.observeRiskPoints(targetSessionId).collectAsState(initial = emptyList())
    } else {
        remember { androidx.compose.runtime.mutableStateOf(emptyList<Pair<Long, Int>>()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "RISK INTELLIGENCE", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TealPrimary, letterSpacing = 1.sp)
        Text(text = "Derived exclusively from real on-device events.", fontSize = 12.sp, color = Slate500)

        Spacer(modifier = Modifier.height(16.dp))

        if (allSessions.isEmpty()) {
            // Strictly honest empty state (No mock data)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = Slate500, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "No analytics available yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Analytics appear after your first real protection session.", color = Slate500, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Key Metrics
                item {
                    val totalSessions = allSessions.size
                    val highRiskCount = allSessions.count { it.peakRisk >= 60 }
                    val avgRisk = allSessions.map { it.finalRisk }.average().toInt()
                    val peakRecorded = allSessions.maxOfOrNull { it.peakRisk } ?: 0

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IntelligenceMetricCard(modifier = Modifier.weight(1f), title = "Total Sessions", value = "$totalSessions")
                        IntelligenceMetricCard(modifier = Modifier.weight(1f), title = "High Risk", value = "$highRiskCount")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IntelligenceMetricCard(modifier = Modifier.weight(1f), title = "Average Risk", value = "$avgRisk/100")
                        IntelligenceMetricCard(modifier = Modifier.weight(1f), title = "Peak Recorded", value = "$peakRecorded/100")
                    }
                }

                // Real-Time Risk Trajectory Graph
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Risk Trajectory", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = if (targetSessionId != null) "Session #${targetSessionId.take(6)}" else "",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Real-time risk curve over actual session duration.", fontSize = 11.sp, color = Slate500)

                            Spacer(modifier = Modifier.height(16.dp))

                            if (riskPoints.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "No risk points recorded for this session yet.", fontSize = 12.sp, color = Slate500)
                                }
                            } else {
                                RiskTrajectoryCanvas(
                                    points = riskPoints,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                )
                            }
                        }
                    }
                }

                // Scam Stage Distribution
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Highest Scam Stage Reached", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            val stageCounts = allSessions.groupBy { it.highestStage }
                            for (stage in ScamStage.entries) {
                                val count = stageCounts[stage]?.size ?: 0
                                if (count > 0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = stage.displayName, fontSize = 13.sp)
                                        Text(text = "$count session(s)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun IntelligenceMetricCard(modifier: Modifier, title: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 11.sp, color = Slate500)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Custom Canvas drawing actual real-time (timestamp, riskScore) trajectory points.
 */
@Composable
fun RiskTrajectoryCanvas(
    points: List<Pair<Long, Int>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Draw horizontal threshold grid lines (30, 60, 80)
        val yLow = height - (30f / 100f * height)
        val yHigh = height - (60f / 100f * height)
        val yCrit = height - (80f / 100f * height)

        drawLine(Color(0xFFCBD5E1), Offset(0f, yLow), Offset(width, yLow), strokeWidth = 1f)
        drawLine(Color(0xFFFEF08A), Offset(0f, yHigh), Offset(width, yHigh), strokeWidth = 1f)
        drawLine(Color(0xFFFECACA), Offset(0f, yCrit), Offset(width, yCrit), strokeWidth = 1f)

        if (points.size < 2) {
            // Draw single point if only 1 exists
            val single = points.first()
            val x = width / 2
            val y = height - (single.second.toFloat() / 100f * height)
            drawCircle(TealPrimary, radius = 6f, center = Offset(x, y))
            return@Canvas
        }

        val minTime = points.first().first
        val maxTime = points.last().first
        val timeSpan = (maxTime - minTime).coerceAtLeast(1L).toFloat()

        val path = Path()

        for ((index, pt) in points.withIndex()) {
            val progress = (pt.first - minTime).toFloat() / timeSpan
            val x = progress * width
            val y = height - (pt.second.toFloat() / 100f * height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            // Draw event point circle
            drawCircle(
                color = if (pt.second >= 60) RiskHigh else TealPrimary,
                radius = 4f,
                center = Offset(x, y)
            )
        }

        drawPath(
            path = path,
            color = TealPrimary,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}
