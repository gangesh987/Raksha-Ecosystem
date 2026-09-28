package com.rakshacall.safety.presentation.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.presentation.theme.TealPrimary

data class SafetyGuide(
    val title: String,
    val summary: String,
    val keyPoints: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndSafetyScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val guides = listOf(
        SafetyGuide(
            title = "Digital Arrest Scam Defense",
            summary = "Understand how scammers impersonate law enforcement and how to legally dispute claims.",
            keyPoints = listOf(
                "There is NO legal provision under Indian law (BNS / CrPC) for 'Digital Arrest' via Skype, WhatsApp, or video call.",
                "Real police or CBI officers will never place you under custody via video camera or forbid you from hanging up.",
                "Never share money, transfer funds to 'RBI verification accounts', or stay isolated in a closed room."
            )
        ),
        SafetyGuide(
            title = "OTP & UPI Credential Safety",
            summary = "Crucial principles regarding banking passwords, SMS OTPs, and UPI PINs.",
            keyPoints = listOf(
                "Entering a UPI PIN DEDUCTS money from your bank account. You NEVER enter a PIN to receive money.",
                "Bank representatives and government departments will NEVER ask for your SMS OTP or card CVV.",
                "If pressured for an OTP, disconnect immediately and report the caller."
            )
        ),
        SafetyGuide(
            title = "Remote Access Software Risks",
            summary = "Recognizing coercive installation demands for screen sharing apps.",
            keyPoints = listOf(
                "Scammers request AnyDesk, TeamViewer, RustDesk, or QuickSupport under the guise of 'account verification'.",
                "Once installed, they can view your screen, intercept OTPs, and control your banking apps.",
                "Never install third-party APKs or remote desktop apps on instructions from unknown callers."
            )
        ),
        SafetyGuide(
            title = "Official Cyber Crime Reporting (1930)",
            summary = "Official channels to report financial fraud in India.",
            keyPoints = listOf(
                "National Cyber Crime Helpline: Call 1930 immediately if money has been transferred.",
                "National Cyber Crime Reporting Portal: https://cybercrime.gov.in",
                "Contact your bank immediately to freeze the beneficiary account."
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HELP & SAFETY GUIDES", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFD32F2F))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EMERGENCY CYBER FRAUD HELPLINE", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFD32F2F))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "If you have transferred funds under coercion, immediately dial 1930 to trigger golden-hour financial freeze across Indian banking networks.",
                            fontSize = 12.sp,
                            color = Color(0xFFB71C1C)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CALL 1930 NOW")
                        }
                    }
                }
            }

            items(guides) { guide ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(guide.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(guide.summary, fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))
                        guide.keyPoints.forEach { pt ->
                            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                Text("•", fontWeight = FontWeight.Bold, color = TealPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(pt, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
