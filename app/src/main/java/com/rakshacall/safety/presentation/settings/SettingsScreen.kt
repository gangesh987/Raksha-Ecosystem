package com.rakshacall.safety.presentation.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.presentation.theme.Slate500
import com.rakshacall.safety.presentation.theme.TealPrimary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val preferences = ServiceLocator.preferences

    val highThreshold by preferences.highThreshold.collectAsState(initial = 60)
    val criticalThreshold by preferences.criticalThreshold.collectAsState(initial = 80)
    val visualEnabled by preferences.visualAnalysisEnabled.collectAsState(initial = true)
    val livenessEnabled by preferences.livenessEnabled.collectAsState(initial = false)
    val retentionDays by preferences.retentionDays.collectAsState(initial = 30)

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
            Text(text = "SETTINGS", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Risk Engine Thresholds", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Safety Brake High Threshold", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "$highThreshold pts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                    }
                    Slider(
                        value = highThreshold.toFloat(),
                        onValueChange = {
                            coroutineScope.launch {
                                preferences.updateThresholds(30, it.toInt(), criticalThreshold)
                                ServiceLocator.riskEngine.highThreshold = it.toInt()
                            }
                        },
                        valueRange = 40f..85f
                    )
                    Text(text = "Trigger level where immediate protective interventions appear.", fontSize = 11.sp, color = Slate500)
                }
            }

            Text(text = "Supporting Sensors", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Visual Camera Analysis", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "Analyze frame lighting and stability as secondary supporting signals.", fontSize = 12.sp, color = Slate500)
                        }
                        Switch(
                            checked = visualEnabled,
                            onCheckedChange = {
                                coroutineScope.launch { preferences.setVisualAnalysisEnabled(it) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Experimental Liveness", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "Checks face consistency without making false deepfake claims.", fontSize = 12.sp, color = Slate500)
                        }
                        Switch(
                            checked = livenessEnabled,
                            onCheckedChange = {
                                coroutineScope.launch { preferences.setLivenessEnabled(it) }
                            }
                        )
                    }
                }
            }

            Text(text = "Quick Navigation", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            SettingsNavRow("Trusted Emergency Contacts", Icons.Default.Contacts) { onNavigateToContacts() }
            SettingsNavRow("Privacy & Data Center", Icons.Default.PrivacyTip) { onNavigateToPrivacy() }

            // About Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "About RakshaCall", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Version 1.0.0 (Production-Grade On-Device Prototype)", fontSize = 12.sp, color = Slate500)
                    Text(text = "Architecture: Clean Architecture • MVVM • Room • DataStore • StateFlow", fontSize = 11.sp, color = Slate500)
                    Text(text = "Engine: Deterministic Local NLP & Tamper-Evident SHA-256 Hash Chains", fontSize = 11.sp, color = Slate500)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsNavRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = TealPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Slate500)
        }
    }
}
