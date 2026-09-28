package com.raksha.video.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raksha.video.data.AppPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(preferences: AppPreferences, onBack: () -> Unit, onLoggedOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    val darkTheme by preferences.darkTheme.collectAsState(initial = false)
    val email by preferences.userEmail.collectAsState(initial = "")
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Account", style = MaterialTheme.typography.titleLarge)
            Text(email.ifBlank { "Local development session" })
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Dark theme")
                    Text("Use the security-focused dark interface", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = darkTheme, onCheckedChange = { scope.launch { preferences.setDarkTheme(it) } })
            }
            HorizontalDivider()
            Text("Privacy", style = MaterialTheme.typography.titleLarge)
            Text("Camera and microphone permissions are controlled by Android. Monitoring features will be opt-in and connected to Raksha protection sessions in later phases.")
            OutlinedButton(onClick = { scope.launch { preferences.logout(); onLoggedOut() } }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
        }
    }
}
