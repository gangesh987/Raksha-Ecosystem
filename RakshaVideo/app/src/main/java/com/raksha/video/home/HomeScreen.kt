package com.raksha.video.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onSettings: () -> Unit, onNewCall: () -> Unit, onJoinCall: () -> Unit, onSafetyCenter: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Raksha Video") }, actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onNewCall, icon = { Icon(Icons.Default.Add, null) }, text = { Text("New call") }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Real-time video calling", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("Raksha Video now uses a real WebRTC media pipeline. No simulated remote video is used.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNewCall, modifier = Modifier.fillMaxWidth()) { Text("Create a call") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onJoinCall, modifier = Modifier.fillMaxWidth()) { Text("Join with Call ID") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onSafetyCenter, modifier = Modifier.fillMaxWidth()) { Text("Safety Center") }
                }
            }
            Text("Recent calls", style = MaterialTheme.typography.titleMedium)
            OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Text("No completed calls yet"); Text("Your call history can be connected to the backend later.", style = MaterialTheme.typography.bodySmall) } }
        }
    }
}
