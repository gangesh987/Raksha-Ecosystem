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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.rakshacall.safety.data.remote.client.NetworkClient
import com.rakshacall.safety.data.remote.config.AppConfig
import com.rakshacall.safety.data.remote.dto.LoginRequest
import com.rakshacall.safety.data.remote.dto.RegisterRequest
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

enum class AuthMethod {
    PHONE, EMAIL
}

@Composable
fun AuthScreen(onVerificationComplete: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var selectedMethod by remember { mutableStateOf(AuthMethod.PHONE) }

    // Phone state
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf<String?>(null) }
    var generatedRakshaId by remember { mutableStateOf<String?>(null) }
    var isPhoneVerifying by remember { mutableStateOf(false) }

    // Email state
    var isRegisterTab by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isEmailLoading by remember { mutableStateOf(false) }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Server configuration state
    val activeServerUrl by ServiceLocator.preferences.backendUrl.collectAsState(initial = AppConfig.activeBaseUrl)
    var showServerDialog by remember { mutableStateOf(false) }
    var editedServerUrl by remember(activeServerUrl) { mutableStateOf(activeServerUrl) }

    // Dialog for changing server URL
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Configure Backend Server") },
            text = {
                Column {
                    Text(
                        "Set the IP address and port of your PC running the backend (e.g. http://172.17.35.95:8000).",
                        fontSize = 13.sp,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editedServerUrl,
                        onValueChange = { editedServerUrl = it },
                        label = { Text("Server URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editedServerUrl = AppConfig.PUBLIC_CLOUD_BASE_URL },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Public Cloud", fontSize = 11.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { editedServerUrl = AppConfig.PC_LAN_BASE_URL },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("PC LAN", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            ServiceLocator.preferences.setBackendUrl(editedServerUrl)
                            errorMsg = null
                            showServerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to RakshaCall",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose your preferred sign-in method to activate on-device protection.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Slate500)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Method Tabs: Phone vs Email
            TabRow(
                selectedTabIndex = if (selectedMethod == AuthMethod.PHONE) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (selectedMethod == AuthMethod.PHONE) 0 else 1]),
                        color = TealPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedMethod == AuthMethod.PHONE,
                    onClick = {
                        selectedMethod = AuthMethod.PHONE
                        errorMsg = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quick Sign-in (Demo)", fontWeight = if (selectedMethod == AuthMethod.PHONE) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedMethod == AuthMethod.EMAIL,
                    onClick = {
                        selectedMethod = AuthMethod.EMAIL
                        errorMsg = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Email & Password", fontWeight = if (selectedMethod == AuthMethod.EMAIL) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==================== 1. QUICK LOCAL SIGN-IN (DEMO MODE) ====================
            if (selectedMethod == AuthMethod.PHONE) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield",
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Quick Local Sign-in (Demo Mode) — Instant on-device login. Demo authentication — no SMS/OTP is sent.",
                            fontSize = 12.sp,
                            color = Slate500,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter { c -> c.isDigit() }.take(10) },
                    label = { Text("Demo Mobile / Identifier") },
                    placeholder = { Text("10-digit number (e.g. 9876543210)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TealPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (generatedOtp == null) {
                    OutlinedButton(
                        onClick = {
                            if (phoneNumber.length < 10) {
                                errorMsg = "Please enter a valid 10-digit mobile number."
                            } else {
                                errorMsg = null
                                val code = CryptographyHelper.generateLocalOtp()
                                val rId = CryptographyHelper.generateRakshaCallId()
                                generatedOtp = code
                                generatedRakshaId = rId
                                otpCode = code // Auto-fill for convenience
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Generate Demo Access Code", fontWeight = FontWeight.SemiBold, color = TealPrimary)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RiskLowBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "Demo Verification Code (Local — No SMS sent):", fontSize = 12.sp, color = Color(0xFF14532D))
                            Text(
                                text = generatedOtp ?: "",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF14532D)
                            )
                            Text(
                                text = "Assigned RakshaCall ID: ${generatedRakshaId ?: ""}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF14532D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { otpCode = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("6-Digit Demo Code") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }

            // ==================== 2. EMAIL & PASSWORD LOGIN ====================
            if (selectedMethod == AuthMethod.EMAIL) {
                // Server Address Card with quick change
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "Server",
                                tint = TealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Server: $activeServerUrl",
                                fontSize = 11.sp,
                                color = Slate500,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { showServerDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Server",
                                tint = TealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sign In vs Create Account Sub-tabs
                TabRow(
                    selectedTabIndex = if (isRegisterTab) 1 else 0,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[if (isRegisterTab) 1 else 0]),
                            color = TealPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = !isRegisterTab,
                        onClick = {
                            isRegisterTab = false
                            errorMsg = null
                        },
                        text = { Text("Sign In", fontWeight = if (!isRegisterTab) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = isRegisterTab,
                        onClick = {
                            isRegisterTab = true
                            errorMsg = null
                        },
                        text = { Text("Create Account", fontWeight = if (isRegisterTab) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isRegisterTab) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g. Gangesh") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text("Email Address") },
                    placeholder = { Text("e.g. user@gmail.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TealPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    placeholder = { Text(if (isRegisterTab) "Minimum 8 characters" else "Your password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TealPrimary) },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
            }

            // Error display card with automatic fallback suggestions
            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                lineHeight = 17.sp
                            )
                        }

                        if (selectedMethod == AuthMethod.EMAIL) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectedMethod = AuthMethod.PHONE
                                        errorMsg = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                                ) {
                                    Text("Use Demo Sign-in", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { showServerDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Change Server IP", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons at bottom
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(24.dp))

            if (selectedMethod == AuthMethod.PHONE) {
                Button(
                    onClick = {
                        if (phoneNumber.length < 10) {
                            errorMsg = "Please enter a valid 10-digit mobile number."
                            return@Button
                        }
                        if (generatedOtp == null) {
                            val code = CryptographyHelper.generateLocalOtp()
                            val rId = CryptographyHelper.generateRakshaCallId()
                            generatedOtp = code
                            generatedRakshaId = rId
                            otpCode = code
                        }
                        if (otpCode != generatedOtp) {
                            errorMsg = "Verification code does not match."
                            return@Button
                        }

                        isPhoneVerifying = true
                        coroutineScope.launch {
                            val userId = UUID.randomUUID().toString()
                            val rId = generatedRakshaId ?: CryptographyHelper.generateRakshaCallId()
                            val user = User(
                                id = userId,
                                rakshaCallId = rId,
                                phoneNumber = phoneNumber,
                                email = null,
                                authMode = "PHONE_OFFLINE"
                            )
                            // Save locally in Room
                            ServiceLocator.userRepository.saveUser(user)
                            // Save session in DataStore
                            ServiceLocator.preferences.saveAuthSession(userId, rId, phoneNumber)
                            isPhoneVerifying = false
                            onVerificationComplete()
                        }
                    },
                    enabled = phoneNumber.length >= 10 && !isPhoneVerifying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isPhoneVerifying) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (generatedOtp == null) "Generate Demo Code & Sign In" else "Verify & Enter Dashboard (Demo)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                // Email action button
                Button(
                    onClick = {
                        if (email.isBlank() || !email.contains("@")) {
                            errorMsg = "Please enter a valid email address."
                            return@Button
                        }
                        if (password.length < 8) {
                            errorMsg = "Password must be at least 8 characters long."
                            return@Button
                        }
                        if (isRegisterTab && name.isBlank()) {
                            errorMsg = "Please enter your full name."
                            return@Button
                        }

                        errorMsg = null
                        isEmailLoading = true

                        coroutineScope.launch {
                            try {
                                val api = NetworkClient.apiService
                                val response = if (isRegisterTab) {
                                    api.register(RegisterRequest(name = name.trim(), email = email.trim(), password = password))
                                } else {
                                    api.login(LoginRequest(email = email.trim(), password = password))
                                }

                                if (response.isSuccessful && response.body() != null) {
                                    val authData = response.body()!!
                                    val userIdStr = authData.user.id.toString()
                                    val rId = CryptographyHelper.generateRakshaCallId()

                                    val user = User(
                                        id = userIdStr,
                                        rakshaCallId = rId,
                                        phoneNumber = "",
                                        email = authData.user.email,
                                        authMode = if (isRegisterTab) "EMAIL_REGISTER" else "EMAIL_LOGIN"
                                    )
                                    ServiceLocator.userRepository.saveUser(user)

                                    ServiceLocator.preferences.saveAuthSessionWithToken(
                                        token = authData.access_token,
                                        email = authData.user.email,
                                        name = authData.user.name,
                                        userId = userIdStr
                                    )

                                    isEmailLoading = false
                                    onVerificationComplete()
                                } else {
                                    isEmailLoading = false
                                    val code = response.code()
                                    val errStr = response.errorBody()?.string() ?: ""
                                    errorMsg = when (code) {
                                        401 -> "Invalid email or password."
                                        409 -> "Email is already registered. Please switch to Sign In."
                                        422 -> "Validation error. Ensure email is valid and password is at least 8 characters."
                                        else -> "Authentication failed (HTTP $code): ${errStr.take(120)}"
                                    }
                                }
                            } catch (e: Exception) {
                                isEmailLoading = false
                                errorMsg = "Cannot reach server at $activeServerUrl.\n${e.localizedMessage ?: "Connection failed"}"
                            }
                        }
                    },
                    enabled = !isEmailLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isEmailLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (isRegisterTab) "Create Account" else "Sign In",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Switch Method Shortcut
            OutlinedButton(
                onClick = {
                    selectedMethod = if (selectedMethod == AuthMethod.PHONE) AuthMethod.EMAIL else AuthMethod.PHONE
                    errorMsg = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (selectedMethod == AuthMethod.PHONE) "Or Sign In with Email & Password" else "Or Quick Local Sign-in (Demo Mode)",
                    color = TealPrimary
                )
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
