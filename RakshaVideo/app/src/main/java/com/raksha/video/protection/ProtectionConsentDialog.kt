package com.raksha.video.protection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun ProtectionConsentDialog(onEnable: (ConsentState) -> Unit, onContinueWithout: () -> Unit) {
    var conversation by remember { mutableStateOf(true) }; var visual by remember { mutableStateOf(true) }; var risk by remember { mutableStateOf(true) }; var trusted by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = {}, title = { Text("🛡 Raksha Protection") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Raksha can analyze this call to help identify potentially risky or coercive conversation patterns.")
        Text("Protection may process conversation signals, visual safety signals and risk indicators.", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(conversation, { conversation = it }); Text("Conversation Analysis") }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(visual, { visual = it }); Text("Visual Analysis") }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(risk, { risk = it }); Text("Risk Detection") }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(trusted, { trusted = it }); Text("Trusted Contact Alerts") }
    } }, confirmButton = { Button(onClick = { onEnable(ConsentState(conversation, visual, risk, trusted)) }) { Text("Enable Protection") } }, dismissButton = { TextButton(onClick = onContinueWithout) { Text("Continue Without Protection") } })
}
