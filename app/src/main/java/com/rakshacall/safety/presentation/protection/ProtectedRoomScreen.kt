package com.rakshacall.safety.presentation.protection

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.rakshacall.R
import com.rakshacall.safety.presentation.theme.*

/**
 * ProtectedRoomScreen provides genuine WebRTC calling with live speech streaming,
 * multilingual semantic intelligence, real-time risk overlay, and safety brake.
 * Includes graceful runtime permission handling so users are never force-closed.
 */
@Composable
fun ProtectedRoomScreen(
    onNavigateBack: () -> Unit,
    onOpenVerificationCoach: () -> Unit
) {
    val context = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var hasMic by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var userBypassedPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCamera = permissions[Manifest.permission.CAMERA] ?: hasCamera
        hasMic = permissions[Manifest.permission.RECORD_AUDIO] ?: hasMic
        userBypassedPermissions = true
    }

    if (hasCamera && hasMic || userBypassedPermissions) {
        RealtimeCallScreen(
            onEndCall = onNavigateBack,
            onOpenVerificationCoach = onOpenVerificationCoach
        )
    } else {
        // Permission Pre-flight Clearance Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0B1120), Color(0xFF131D3F), Color(0xFF1C2B59))
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        shape = CircleShape,
                        color = TealPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.call_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.live_protection),
                        fontSize = 13.sp,
                        color = TealLight,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(28.dp))

                    // Permission Feature Cards
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.protect_camera), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text(stringResource(R.string.protect_camera_desc), color = Slate300, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.protect_microphone), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text(stringResource(R.string.protect_microphone_desc), color = Slate300, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TealPrimary.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.protect_consent_privacy),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Bottom Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            val perms = mutableListOf<String>()
                            if (!hasCamera) perms.add(Manifest.permission.CAMERA)
                            if (!hasMic) perms.add(Manifest.permission.RECORD_AUDIO)
                            if (perms.isNotEmpty()) {
                                permissionLauncher.launch(perms.toTypedArray())
                            } else {
                                userBypassedPermissions = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_start), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { userBypassedPermissions = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(stringResource(R.string.btn_continue), fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.btn_back), color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
