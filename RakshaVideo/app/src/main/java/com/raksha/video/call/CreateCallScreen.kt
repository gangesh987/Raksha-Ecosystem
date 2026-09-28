package com.raksha.video.call

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CreateCallScreen(onCallCreated: (String) -> Unit, onJoinExisting: () -> Unit, onBack: () -> Unit, vm: CallViewModel = viewModel()) {
    var callId by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Start a protected video call", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text("Create a real WebRTC room, then give the Call ID to the second device.")
        Spacer(Modifier.height(24.dp))
        if (callId == null) {
            Button(onClick = { vm.createCall(); val s = vm.callState.value as CallState.Created; callId = s.callId }, modifier = Modifier.fillMaxWidth()) { Text("Create call") }
        } else {
            Text("Call ID", style = MaterialTheme.typography.labelLarge)
            Text(callId!!, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text("Enter this ID on Device B.")
            Spacer(Modifier.height(18.dp))
            Button(onClick = { onCallCreated(callId!!) }, modifier = Modifier.fillMaxWidth()) { Text("Open caller screen") }
        }
        TextButton(onClick = onJoinExisting, modifier = Modifier.fillMaxWidth()) { Text("Join an existing call") }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
