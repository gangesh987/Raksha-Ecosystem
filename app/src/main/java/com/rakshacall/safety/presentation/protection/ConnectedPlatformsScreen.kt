package com.rakshacall.safety.presentation.protection

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.domain.model.PlatformConnection
import com.rakshacall.safety.presentation.theme.TealPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedPlatformsScreen(
    onNavigateBack: () -> Unit
) {
    val platforms = listOf(
        PlatformConnection(
            id = "native",
            platformName = "RakshaCall Protected Rooms",
            supportedProtectionMethod = "Native 1-to-1 WebRTC Video Room",
            permissionRequirements = "RECORD_AUDIO, CAMERA",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Full end-to-end encryption with real-time on-device NLP & live transcript."
        ),
        PlatformConnection(
            id = "whatsapp",
            platformName = "WhatsApp",
            supportedProtectionMethod = "Permitted Microphone / Screen Capture",
            permissionRequirements = "RECORD_AUDIO",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Strict compliance: zero interception of WhatsApp private encrypted VoIP streams."
        ),
        PlatformConnection(
            id = "meet",
            platformName = "Google Meet",
            supportedProtectionMethod = "Official Cloud Media API / Screen Capture",
            permissionRequirements = "Google Workspace OAuth Consent",
            isSupported = true,
            isConfigured = false,
            limitationNote = "Requires authorized Google Cloud OAuth tenant credentials to connect to live stream."
        ),
        PlatformConnection(
            id = "teams",
            platformName = "Microsoft Teams",
            supportedProtectionMethod = "Consented MediaProjection Audio",
            permissionRequirements = "MediaProjection",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Protects incoming meetings via permitted Android screen/audio projection."
        ),
        PlatformConnection(
            id = "zoom",
            platformName = "Zoom",
            supportedProtectionMethod = "Local Microphone Audio Capture",
            permissionRequirements = "RECORD_AUDIO",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Analyzes audio from loudspeaker with explicit on-device permission."
        ),
        PlatformConnection(
            id = "telegram",
            platformName = "Telegram",
            supportedProtectionMethod = "Local Microphone Audio Capture",
            permissionRequirements = "RECORD_AUDIO",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Captures permitted ambient speech for real-time coercion detection."
        ),
        PlatformConnection(
            id = "browser",
            platformName = "Browser Web Conference",
            supportedProtectionMethod = "Companion Web Audio Relay Bridge",
            permissionRequirements = "Local Network Access",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Relays browser tab audio to the RakshaCall analysis engine."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CONNECTED PLATFORMS", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
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
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TealPrimary.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Platform Sandboxing Compliance", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TealPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Android OS prohibits background packet interception and decryption of other apps' private VoIP streams. RakshaCall strictly respects this security model, analyzing only permitted media inputs with user consent.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(platforms) { platform ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(platform.platformName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            val color = if (platform.isConfigured) Color(0xFF2E7D32) else Color(0xFFF57C00)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = color.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    if (platform.isConfigured) "READY" else "CONFIG REQUIRED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Protection Method: ${platform.supportedProtectionMethod}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Permissions: ${platform.permissionRequirements}", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(platform.limitationNote, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
