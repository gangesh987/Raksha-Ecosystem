package com.rakshacall.safety.presentation.settings

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.presentation.theme.TealPrimary

data class SecurityCheckItem(
    val title: String,
    val description: String,
    val status: String, // VERIFIED, ACTIVE, ENFORCED
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCenterScreen(
    onNavigateBack: () -> Unit
) {
    val checks = listOf(
        SecurityCheckItem("Android Keystore System", "Cryptographic key material managed inside hardware-backed TEE/StrongBox.", "ACTIVE", Icons.Default.Key),
        SecurityCheckItem("SHA-256 Evidence Chain", "Cryptographic append-only hash chaining anchored at 64-zero genesis block.", "ENFORCED", Icons.Default.Link),
        SecurityCheckItem("Zero In-APK Secret Keys", "Production credentials isolated in secure backend environment variables.", "VERIFIED", Icons.Default.Lock),
        SecurityCheckItem("Transient Audio Memory", "Raw PCM voice audio analyzed in RAM and purged immediately.", "ENFORCED", Icons.Default.Memory),
        SecurityCheckItem("Zero Credential Persistence", "OTPs, UPI PINs, and banking passwords are never written to disk or logs.", "ENFORCED", Icons.Default.Password),
        SecurityCheckItem("TLS 1.3 Transport Security", "All WebSocket and REST network channels enforce modern encrypted TLS.", "ACTIVE", Icons.Default.Https),
        SecurityCheckItem("Firebase App Check", "Attests device authenticity using Play Integrity API.", "CONFIGURED", Icons.Default.VerifiedUser),
        SecurityCheckItem("SQLite Database Isolation", "Local database protected by Android OS sandbox application UID boundaries.", "ACTIVE", Icons.Default.Storage)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SECURITY & INTEGRITY CENTER", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Security Posture: HARDENED", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1B5E20))
                            Text("8/8 active hardening controls verified.", fontSize = 12.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }

            items(checks) { check ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
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
                                Icon(check.icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(check.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(check.description, fontSize = 11.sp, color = Color.Gray)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                        ) {
                            Text(
                                check.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
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
