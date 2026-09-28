package com.raksha.video.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.raksha.video.data.AppPreferences
import kotlinx.coroutines.launch

@Composable
fun PermissionsScreen(onContinue: () -> Unit, preferences: AppPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cameraGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var micGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        cameraGranted = result[Manifest.permission.CAMERA] == true || cameraGranted
        micGranted = result[Manifest.permission.RECORD_AUDIO] == true || micGranted
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Call permissions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Raksha Video needs camera and microphone access to make real video calls. Protection telemetry will remain permission-controlled.")
        Spacer(Modifier.height(24.dp))
        PermissionRow("Camera", cameraGranted)
        PermissionRow("Microphone", micGranted)
        Spacer(Modifier.height(24.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (!cameraGranted || !micGranted) {
                    launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                } else {
                    scope.launch { preferences.setOnboardingSeen(); onContinue() }
                }
            }
        ) { Text(if (cameraGranted && micGranted) "Continue" else "Grant permissions") }
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { scope.launch { preferences.setOnboardingSeen(); onContinue() } }
        ) { Text("Continue without protected media permissions") }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(if (granted) "Granted" else "Required", color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    }
}
