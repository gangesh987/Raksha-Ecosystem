package com.rakshacall.safety.presentation.onboarding

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.core.permissions.PermissionHelper
import com.rakshacall.safety.core.security.CryptographyHelper
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.User
import com.rakshacall.safety.presentation.theme.Navy900
import com.rakshacall.safety.presentation.theme.RiskLow
import com.rakshacall.safety.presentation.theme.RiskLowBg
import com.rakshacall.safety.presentation.theme.RiskMedium
import com.rakshacall.safety.presentation.theme.RiskMediumBg
import com.rakshacall.safety.presentation.theme.Slate500
import com.rakshacall.safety.presentation.theme.TealPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit, onNavigateToWelcome: () -> Unit) {
    val isLoggedIn by ServiceLocator.preferences.isLoggedIn.collectAsState(initial = null)

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn != null) {
            delay(800)
            if (isLoggedIn == true) {
                onNavigateToHome()
            } else {
                onNavigateToWelcome()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(TealPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Shield",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "RAKSHACALL",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "See the risk before the pressure becomes irreversible.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TealPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Privacy-Conscious AI Safety Layer",
                style = MaterialTheme.typography.bodyMedium.copy(color = Slate500)
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                color = TealPrimary,
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp
            )
        }
    }
}

@Composable
fun WelcomeScreen(onContinue: () -> Unit, onWatchTour: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(TealPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security",
                    tint = TealPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Digital-Arrest & Coercion Protection",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "RakshaCall is an on-device safety layer that recognizes authority impersonation, extortion, isolation tactics, and urgent payment demands in real time.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Slate500,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Bullet features
            FeatureRow(
                icon = Icons.Default.Security,
                title = "Local Risk Engine",
                subtitle = "Detects coercive tactics locally without transmitting private calls to external servers."
            )
            Spacer(modifier = Modifier.height(16.dp))
            FeatureRow(
                icon = Icons.Default.Warning,
                title = "Immediate Safety Brake",
                subtitle = "Intervenes before irreversible money transfers, OTP disclosure, or AnyDesk installations."
            )
            Spacer(modifier = Modifier.height(16.dp))
            FeatureRow(
                icon = Icons.Default.CheckCircle,
                title = "Tamper-Evident Incident Log",
                subtitle = "Appends verifiable SHA-256 evidence hashes for independent verification and reporting."
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onWatchTour,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Watch 12s Intro Tour", fontWeight = FontWeight.Medium, color = TealPrimary)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Review Privacy Model", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}


@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TealPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = TealPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = Slate500))
        }
    }
}

@Composable
fun PrivacyExplanationScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Privacy & Consent Architecture",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "RakshaCall operates under strict transparent privacy principles:",
                style = MaterialTheme.typography.bodyMedium.copy(color = Slate500)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. No Secret Call Interception",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Android strictly prevents third-party apps from silently intercepting encrypted WhatsApp, Telegram, or phone audio. RakshaCall relies on permitted microphone input during active sessions or user-provided transcripts.",
                        fontSize = 13.sp,
                        color = Slate500,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "2. Ephemeral Audio Processing",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Audio chunks are analyzed on-device for recognized speech and immediately discarded. Raw audio recordings are NEVER stored by default.",
                        fontSize = 13.sp,
                        color = Slate500,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "3. Zero Fake Integrations",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All scores, graphs, and evidence timelines reflect actual live session state. If no data exists, the application honestly displays an empty state.",
                        fontSize = 13.sp,
                        color = Slate500,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("I Understand & Agree", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun AuthScreen(onVerificationComplete: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf<String?>(null) }
    var generatedRakshaId by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Prototype Verification",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Local-first authentication. No external SMS provider is pretended.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Slate500)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Honest Prototype Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RiskMediumBg)
                    .border(1.dp, RiskMedium.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Notice: Because this is an on-device prototype without a production cloud gateway, verification codes are generated locally for testing.",
                    fontSize = 12.sp,
                    color = Color(0xFF78350F),
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it.filter { char -> char.isDigit() }.take(10) },
                label = { Text("Mobile Number") },
                placeholder = { Text("e.g. 9876543210") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (generatedOtp == null) {
                OutlinedButton(
                    onClick = {
                        if (phoneNumber.length < 10) {
                            errorMsg = "Please enter a valid 10-digit mobile number"
                        } else {
                            errorMsg = null
                            val code = CryptographyHelper.generateLocalOtp()
                            val rId = CryptographyHelper.generateRakshaCallId()
                            generatedOtp = code
                            generatedRakshaId = rId
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Generate Local Verification Code")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        errorMsg = "DEVELOPMENT CONFIGURATION REQUIRED: Google Sign-In requires active google-services.json and SHA-1 certificate configuration."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sign in with Google (OAuth2)")
                }
            } else {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RiskLowBg),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Local Verification Code:", fontSize = 12.sp, color = Color(0xFF14532D))
                        Text(
                            text = generatedOtp ?: "",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF14532D)
                        )
                        Text(
                            text = "Assigned RakshaCall ID: ${generatedRakshaId ?: ""}",
                            fontSize = 12.sp,
                            color = Color(0xFF14532D)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Enter 6-digit Code") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }

        Button(
            onClick = {
                if (generatedOtp != null && otpCode == generatedOtp) {
                    isVerifying = true
                    coroutineScope.launch {
                        val userId = UUID.randomUUID().toString()
                        val rId = generatedRakshaId ?: CryptographyHelper.generateRakshaCallId()
                        val user = User(
                            id = userId,
                            rakshaCallId = rId,
                            phoneNumber = phoneNumber
                        )
                        // Save in Room
                        ServiceLocator.userRepository.saveUser(user)
                        // Save in DataStore
                        ServiceLocator.preferences.saveAuthSession(userId, rId, phoneNumber)
                        onVerificationComplete()
                    }
                } else {
                    errorMsg = "Verification code does not match."
                }
            },
            enabled = generatedOtp != null && otpCode.length == 6 && !isVerifying,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isVerifying) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("Verify & Continue", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun PermissionCenterScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var permissionsList by remember { mutableStateOf(PermissionHelper.getAllPermissionsStatus(context)) }

    // Launcher for multiple permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionsList = PermissionHelper.getAllPermissionsStatus(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Permission Center",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Actual Android system permission status. No simulated states.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Slate500)
            )

            Spacer(modifier = Modifier.height(24.dp))

            for (perm in permissionsList) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = when (perm.title) {
                                    "Microphone" -> Icons.Default.Mic
                                    "Camera (Optional)" -> Icons.Default.Videocam
                                    else -> Icons.Default.Notifications
                                }
                                Icon(imageVector = icon, contentDescription = perm.title, tint = TealPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = perm.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            // Real status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (perm.isGranted) RiskLowBg else RiskMediumBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (perm.isGranted) "Granted" else "Not Granted",
                                    color = if (perm.isGranted) RiskLow else RiskMedium,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = perm.description, fontSize = 13.sp, color = Slate500, lineHeight = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val needed = permissionsList
                        .filter { !it.isGranted }
                        .map { it.permission }
                        .toTypedArray()
                    if (needed.isNotEmpty()) {
                        permissionLauncher.launch(needed)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Request Android Permissions")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Proceed to Home", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}
