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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.presentation.theme.TealPrimary

data class MoreHubItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreHubScreen(
    onNavigateToConnectedPlatforms: () -> Unit,
    onNavigateToModelCenter: () -> Unit,
    onNavigateToSecurityCenter: () -> Unit,
    onNavigateToNotificationCenter: () -> Unit,
    onNavigateToSafetyAnalytics: () -> Unit,
    onNavigateToHelpAndSafety: () -> Unit,
    onNavigateToSimulationLab: () -> Unit,
    onNavigateToPrivacyCenter: () -> Unit,
    onNavigateToPermissionCenter: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val hubItems = listOf(
        MoreHubItem("Connected Platforms", "Manage WhatsApp, Google Meet, Teams & Browser bridges", Icons.Default.Hub, onNavigateToConnectedPlatforms),
        MoreHubItem("AI & Model Center", "Inspect on-device NLP, Groq, Gemini & model statuses", Icons.Default.Psychology, onNavigateToModelCenter),
        MoreHubItem("Security Center", "Android Keystore, Database encryption & TLS posture", Icons.Default.Security, onNavigateToSecurityCenter),
        MoreHubItem("Notification Center", "Scam stage alerts, safety brake events & notifications", Icons.Default.Notifications, onNavigateToNotificationCenter),
        MoreHubItem("Safety Analytics", "Real session risk metrics, duration & deflected threats", Icons.Default.QueryStats, onNavigateToSafetyAnalytics),
        MoreHubItem("Simulation Lab", "Replay controlled scam scenarios without polluting data", Icons.Default.Science, onNavigateToSimulationLab),
        MoreHubItem("Help & Safety Guides", "Digital arrest defense playbook & police reporting guides", Icons.Default.MenuBook, onNavigateToHelpAndSafety),
        MoreHubItem("Privacy Center", "Audit data capture, raw audio policy & export records", Icons.Default.PrivacyTip, onNavigateToPrivacyCenter),
        MoreHubItem("Permission Center", "Check Microphone, Camera & Notification grants", Icons.Default.VerifiedUser, onNavigateToPermissionCenter),
        MoreHubItem("Settings", "Threshold configurations, model fallbacks & preferences", Icons.Default.Settings, onNavigateToSettings),
        MoreHubItem("About RakshaCall", "Production architecture, version 2.0 & engineering team", Icons.Default.Info, onNavigateToAbout)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MORE & SECURITY HUBS", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Personal Cyber-Safety OS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "RakshaCall operates with zero fake states and zero unauthorized background decryption.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(hubItems) { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { item.onClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TealPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(item.icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(item.subtitle, fontSize = 11.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}
