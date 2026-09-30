package com.rakshacall.safety.presentation.protection

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rakshacall.R
import com.rakshacall.safety.domain.model.PlatformConnection
import com.rakshacall.safety.presentation.theme.*

data class ProtectOption(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val mediaType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectACallScreen(
    onNavigateBack: () -> Unit,
    onStartNativeRoom: () -> Unit,
    onStartProtection: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedPlatform by remember { mutableStateOf<PlatformConnection?>(null) }
    var showConsentDialog by remember { mutableStateOf(false) }

    val protectOptions = listOf(
        ProtectOption(stringResource(R.string.protect_video_call), stringResource(R.string.protect_video_desc), Icons.Default.Videocam, "VIDEO"),
        ProtectOption(stringResource(R.string.protect_audio_call), stringResource(R.string.protect_audio_desc), Icons.Default.Phone, "AUDIO"),
        ProtectOption(stringResource(R.string.protect_screen), stringResource(R.string.protect_screen_desc), Icons.Default.ScreenShare, "SCREEN"),
        ProtectOption(stringResource(R.string.protect_microphone), stringResource(R.string.protect_microphone_desc), Icons.Default.Mic, "MICROPHONE"),
        ProtectOption(stringResource(R.string.protect_camera), stringResource(R.string.protect_camera_desc), Icons.Default.CameraAlt, "CAMERA"),
        ProtectOption(stringResource(R.string.protect_text), stringResource(R.string.protect_text_desc), Icons.Default.Chat, "TEXT")
    )

    val platforms = listOf(
        PlatformConnection(
            id = "native_room",
            platformName = "RakshaCall Protected Room",
            supportedProtectionMethod = "Native 1-to-1 WebRTC Video Room",
            permissionRequirements = "Microphone + Camera",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Full end-to-end protection inside RakshaCall with live transcript & risk engine."
        ),
        PlatformConnection(
            id = "whatsapp",
            platformName = "WhatsApp",
            supportedProtectionMethod = "Permitted Microphone / Screen Capture",
            permissionRequirements = "RECORD_AUDIO / MediaProjection",
            isSupported = true,
            isConfigured = true,
            limitationNote = "No private VoIP packet decryption. Operates via permitted Android media capture."
        ),
        PlatformConnection(
            id = "meet",
            platformName = "Google Meet",
            supportedProtectionMethod = "Official Cloud Connector / Screen Capture",
            permissionRequirements = "OAuth / MediaProjection",
            isSupported = true,
            isConfigured = false,
            limitationNote = "Google Meet official Media API requires tenant administrator authorization."
        ),
        PlatformConnection(
            id = "teams",
            platformName = "Microsoft Teams",
            supportedProtectionMethod = "Consented OS MediaProjection",
            permissionRequirements = "MediaProjection",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Protects permitted audio and shared meeting screens."
        ),
        PlatformConnection(
            id = "zoom",
            platformName = "Zoom",
            supportedProtectionMethod = "Consented Microphone / Audio Loopback",
            permissionRequirements = "RECORD_AUDIO",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Audio analysis only through permitted device audio input."
        ),
        PlatformConnection(
            id = "telegram",
            platformName = "Telegram",
            supportedProtectionMethod = "Permitted Local Microphone Capture",
            permissionRequirements = "RECORD_AUDIO",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Analyzes ambient speakerphone speech with explicit user consent."
        ),
        PlatformConnection(
            id = "browser",
            platformName = "Browser Call",
            supportedProtectionMethod = "Browser Audio Bridge",
            permissionRequirements = "Local Network Access",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Monitors web conference audio relayed via the RakshaCall companion bridge."
        ),
        PlatformConnection(
            id = "other",
            platformName = "Other App / Manual Input",
            supportedProtectionMethod = "Direct Manual Input & Real-Time Lab",
            permissionRequirements = "None",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Always available offline fallback for direct text and transcript testing."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.protect_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Hero Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TealPrimary.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.protect_active_layer), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TealPrimary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.protect_active_desc),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text("PROTECTION MODES", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }

            items(protectOptions) { option ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (option.mediaType == "VIDEO") {
                                onStartNativeRoom()
                            } else {
                                onStartProtection(option.mediaType)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(option.icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(option.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("PLATFORM INTEGRATIONS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Transparent platform capabilities. Zero silent interception or unauthorized VoIP tampering.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(platforms) { platform ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPlatform = platform
                            showConsentDialog = true
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(platform.platformName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            val badgeColor = if (platform.id == "native_room") TealPrimary else if (platform.isConfigured) Color(0xFF2E7D32) else Color(0xFFF57C00)
                            val badgeText = if (platform.id == "native_room") "NATIVE ROOM" else if (platform.isConfigured) "PERMITTED" else "OFFICIAL API REQ"
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.15f)
                            ) {
                                Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Method: ${platform.supportedProtectionMethod}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Requirements: ${platform.permissionRequirements}", fontSize = 11.sp, color = Color.Gray)
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

    if (showConsentDialog && selectedPlatform != null) {
        val platform = selectedPlatform!!
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text("Protect Call with ${platform.platformName}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("WHAT RAKSHACALL MONITORS:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Ambient speakerphone audio via microphone (not background cellular line)", fontSize = 12.sp)
                    Text("• Camera video signals for temporal consistency", fontSize = 12.sp)
                    Text("• Live transcript generation for tactic detection", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("PRIVACY GUARANTEE & TELEPHONY BOUNDARY:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TealPrimary)
                    Text("No telephony or call-screening permissions are requested in the manifest. Audio is captured only via permitted microphone (with call placed on speakerphone) or user-consented screen share, analyzed in RAM, and discarded.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConsentDialog = false
                        if (platform.id == "native_room") {
                            onStartNativeRoom()
                        } else {
                            // Launch platform package or web intent
                            if (platform.id == "whatsapp") {
                                val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                                if (intent != null) context.startActivity(intent)
                            }
                            onStartProtection(platform.platformName)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text(stringResource(R.string.protect_consent_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) {
                    Text(stringResource(R.string.protect_consent_cancel))
                }
            }
        )
    }
}
