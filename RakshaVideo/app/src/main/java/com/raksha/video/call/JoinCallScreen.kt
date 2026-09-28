package com.raksha.video.call

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun JoinCallScreen(onJoin: (String) -> Unit, onBack: () -> Unit) {
    var callId by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Join a call", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Enter the Call ID shown on the caller's device.")
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(callId, { callId = it.uppercase() }, Modifier.fillMaxWidth(), label = { Text("Call ID") }, singleLine = true)
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onJoin(callId.trim()) }, enabled = callId.trim().length >= 6, modifier = Modifier.fillMaxWidth()) { Text("Join real call") }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
