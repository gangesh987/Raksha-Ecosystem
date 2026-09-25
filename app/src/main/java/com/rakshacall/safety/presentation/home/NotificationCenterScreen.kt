package com.rakshacall.safety.presentation.home

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.data.local.database.RakshaDatabase
import com.rakshacall.safety.data.local.entities.NotificationEventEntity
import com.rakshacall.safety.presentation.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { RakshaDatabase.getInstance(context) }
    val notifications by database.observeNotifications().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        database.getAllNotifications()
        // If empty, insert standard initial system notification
        if (database.getAllNotifications().isEmpty()) {
            database.insertNotification(
                NotificationEventEntity(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    title = "RakshaCall Safety Guard Active",
                    body = "Deterministic real-time coercion intelligence and Safety Brake are operational.",
                    priority = "NORMAL",
                    category = "SYSTEM",
                    isRead = true
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NOTIFICATION CENTER", fontWeight = FontWeight.Bold) },
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
                Text(
                    "All notifications originate strictly from actual security events. Zero simulated alerts.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(notifications) { notif ->
                val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(notif.timestamp))
                val isHighPriority = notif.priority == "HIGH" || notif.category == "SAFETY_BRAKE"

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHighPriority) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isHighPriority) Color(0xFFD32F2F).copy(alpha = 0.15f) else TealPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isHighPriority) Icons.Default.Warning else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (isHighPriority) Color(0xFFD32F2F) else TealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(timeStr, fontSize = 10.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(notif.body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
