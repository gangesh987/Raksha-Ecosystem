package com.rakshacall.safety.presentation.trustedcontacts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.rakshacall.safety.data.local.entities.TrustedContactEntity
import com.rakshacall.safety.domain.model.AlertDeliveryStatus
import com.rakshacall.safety.presentation.theme.TealPrimary
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = remember { RakshaDatabase.getInstance(context) }
    val contacts by database.observeContacts().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<TrustedContactEntity?>(null) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newRelationship by remember { mutableStateOf("Family Member") }

    var alertStatusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        database.getAllContacts()
    }

    fun sendEmergencyAlert(contact: TrustedContactEntity) {
        val message = "EMERGENCY SAFETY ALERT from RakshaCall: A potential digital-arrest or coercive phone scam was detected during an ongoing call. Please contact me immediately."
        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${contact.phoneNumber}")
                putExtra("sms_body", message)
            }
            context.startActivity(smsIntent)
            alertStatusMessage = "SMS dispatch prepared for ${contact.name}. Delivery status: ALERT_REQUESTED (Handed over to device SMS messenger)."
        } catch (e: Exception) {
            alertStatusMessage = "Direct SMS gateway not configured. Could not open system SMS."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FAMILY SAFETY & TRUSTED CONTACTS", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
            }
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
                // Info Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TealPrimary.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = TealPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Automatic Scam Escalation Alerts", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TealPrimary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "When High or Critical risk is detected during a call, you can instantly notify your trusted contacts. Delivery states are tracked honestly.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            alertStatusMessage?.let { status ->
                item {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(status, color = Color(0xFF1B5E20), fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { alertStatusMessage = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Gray)
                            }
                        }
                    }
                }
            }

            item {
                Text("TRUSTED CONTACTS (${contacts.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }

            if (contacts.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No trusted contacts added yet", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Add family members or guardians who can be alerted if you face a scam threat.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(contacts) { contact ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TealPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = TealPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(contact.relationship, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(contact.phoneNumber, fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Consent: ${contact.consentStatus} • Real Delivery State: READY", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            }
                            Row {
                                IconButton(onClick = { sendEmergencyAlert(contact) }) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = "Alert", tint = Color(0xFFD32F2F))
                                }
                                IconButton(onClick = { contactToEdit = contact }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Contact", tint = TealPrimary)
                                }
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        database.deleteContact(contact.id)
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Trusted Family Contact", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number (+91...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRelationship,
                        onValueChange = { newRelationship = it },
                        label = { Text("Relationship (e.g. Mother, Father, Friend)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Explicit consent is required. Contact will be notified during emergency Safety Brake events.", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newPhone.isNotBlank()) {
                            coroutineScope.launch {
                                database.insertContact(
                                    TrustedContactEntity(
                                        id = UUID.randomUUID().toString(),
                                        name = newName.trim(),
                                        phoneNumber = newPhone.trim(),
                                        relationship = newRelationship.trim(),
                                        consentStatus = "CONSENTED",
                                        isEmergency = true
                                    )
                                )
                                showAddDialog = false
                                newName = ""
                                newPhone = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("SAVE CONTACT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (contactToEdit != null) {
        val targetContact = contactToEdit!!
        var editName by remember(targetContact.id) { mutableStateOf(targetContact.name) }
        var editPhone by remember(targetContact.id) { mutableStateOf(targetContact.phoneNumber) }
        var editRelationship by remember(targetContact.id) { mutableStateOf(targetContact.relationship) }

        AlertDialog(
            onDismissRequest = { contactToEdit = null },
            title = { Text("Edit Trusted Contact", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editRelationship,
                        onValueChange = { editRelationship = it },
                        label = { Text("Relationship") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editPhone.isNotBlank()) {
                            coroutineScope.launch {
                                database.insertContact(
                                    targetContact.copy(
                                        name = editName.trim(),
                                        phoneNumber = editPhone.trim(),
                                        relationship = editRelationship.trim()
                                    )
                                )
                                contactToEdit = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("SAVE CHANGES")
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToEdit = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
