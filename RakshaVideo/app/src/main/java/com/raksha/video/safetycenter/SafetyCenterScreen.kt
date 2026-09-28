package com.raksha.video.safetycenter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raksha.video.trusted.TrustedContact
import com.raksha.video.trusted.TrustedContactManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCenterScreen(onBack: () -> Unit) {
    val manager = remember { TrustedContactManager() }
    val contacts by manager.contacts.collectAsState()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety Center") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { p ->
        LazyColumn(
            modifier = Modifier.padding(p).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Trusted Contacts", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Alerts require explicit confirmation and never include raw media by default.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        manager.add(name, phone.ifBlank { null }, null, null)
                        name = ""
                        phone = ""
                    }
                ) {
                    Text("Add contact")
                }
            }
            items(contacts) { c: TrustedContact ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.displayName)
                            Text(c.phoneNumber ?: "No phone", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { manager.toggle(c.id) }) {
                            Text(if (c.enabled) "Enabled" else "Disabled")
                        }
                        TextButton(onClick = { manager.remove(c.id) }) {
                            Text("Remove")
                        }
                    }
                }
            }
            item {
                HorizontalDivider()
                Text("Privacy Controls", style = MaterialTheme.typography.titleLarge)
                Text("Raw audio, raw video and raw frames are not stored by default.")
                Text("Evidence contains structured signals, risk events and user actions.")
            }
        }
    }
}
