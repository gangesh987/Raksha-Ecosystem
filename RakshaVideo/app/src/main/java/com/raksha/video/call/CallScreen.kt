package com.raksha.video.call

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raksha.video.protection.*
import com.raksha.video.protection.RiskLevel
import com.raksha.video.intelligence.visual.FrameSampler
import com.raksha.video.safety.SafetyViewModel
import kotlinx.coroutines.flow.collectLatest
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun CallScreen(callId: String, caller: Boolean, onEnded: () -> Unit, vm: CallViewModel = viewModel()) {
    val state by vm.callState.collectAsState()
    val local by vm.localVideo.collectAsState()
    val remote by vm.remoteVideo.collectAsState()
    val mic by vm.micEnabled.collectAsState()
    val camera by vm.cameraEnabled.collectAsState()
    val connection by vm.connectionLabel.collectAsState()
    val protectionVm: ProtectionViewModel = viewModel(key = "protection-$callId")
    val safetyVm: SafetyViewModel = viewModel(key = "safety-$callId")
    val safetyState by safetyVm.state.collectAsState()
    val safetyDecision by safetyVm.decision.collectAsState()
    var showBrake by remember { mutableStateOf(false) }
    val protectionState by protectionVm.state.collectAsState()
    val latestRisk by protectionVm.latestRisk.collectAsState()
    var showConsent by remember { mutableStateOf(true) }
    var showRisk by remember { mutableStateOf(false) }
    val frameSampler = remember { FrameSampler("remote_video", 1000L) }
    LaunchedEffect(callId) { vm.join(callId, caller) }
    LaunchedEffect(Unit) { protectionVm.send(ProtectionEvent("call_started", callId)) }
    LaunchedEffect(latestRisk) { latestRisk?.let { safetyVm.onRisk(it); if (it.level == RiskLevel.HIGH || it.level == RiskLevel.CRITICAL) showRisk = true } }
    DisposableEffect(remote) {
        remote?.addSink(frameSampler)
        onDispose { remote?.removeSink(frameSampler) }
    }
    LaunchedEffect(frameSampler) {
        frameSampler.samples.collectLatest { sample ->
            protectionVm.submitVisualSignal("FRAME_SAMPLE", 1.0f, mapOf("width" to sample.width, "height" to sample.height, "rotation" to sample.rotation, "source" to sample.source))
        }
    }
    DisposableEffect(Unit) { onDispose { remote?.removeSink(frameSampler); if (state !is CallState.Ended) vm.endCall() } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        remote?.let { track -> VideoRenderer(track, Modifier.fillMaxSize()) } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (state is CallState.Waiting) "Waiting for the other participant…" else "Connecting…", color = Color.White)
        }
        local?.let { track -> VideoRenderer(track, Modifier.align(Alignment.TopEnd).padding(16.dp).size(120.dp, 180.dp).clip(MaterialTheme.shapes.medium)) }
        Column(Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Text("Raksha Video", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("$connection  •  $callId", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            val protectionLabel = when (protectionState) {
                ProtectionState.MONITORING, ProtectionState.CONNECTED -> "🛡 Protected"
                ProtectionState.CONNECTING -> "🛡 Protection connecting…"
                ProtectionState.DISCONNECTED, ProtectionState.ERROR -> "⚠ Protection unavailable"
                else -> "🛡 Protection off"
            }
            AssistChip(onClick = { showConsent = true }, label = { Text(protectionLabel) })
            if (latestRisk != null) { Spacer(Modifier.height(4.dp)); AssistChip(onClick={showBrake=true}, label={Text("🛡 Safety Brake • ${safetyState.name}")}) }
            latestRisk?.let { risk -> if (showRisk) { Spacer(Modifier.height(8.dp)); Card { Column(Modifier.padding(12.dp)) { Text("⚠ Potentially Risky Conversation", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold); Text("Risk level: ${risk.level}"); risk.confidence?.let { Text("Analysis confidence: ${"%.0f".format(it * 100)}%", style = MaterialTheme.typography.bodySmall) }; risk.stage?.let { Text("Conversation stage: $it", style = MaterialTheme.typography.bodySmall) }; if (risk.reasons.isNotEmpty()) Text(risk.reasons.joinToString(" • "), style = MaterialTheme.typography.bodySmall); TextButton(onClick = { showRisk = false }) { Text("Close") } } } } }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = vm::toggleMic) { Icon(if (mic) Icons.Default.Mic else Icons.Default.MicOff, null) }
            FilledIconButton(onClick = vm::toggleCamera) { Icon(if (camera) Icons.Default.Videocam else Icons.Default.VideocamOff, null) }
            FilledIconButton(onClick = vm::switchCamera) { Icon(Icons.Default.Cameraswitch, null) }
            IconButton(onClick = { vm.endCall(); onEnded() }, modifier = Modifier.size(58.dp).background(MaterialTheme.colorScheme.error, CircleShape)) { Icon(Icons.Default.CallEnd, "End call", tint = Color.White) }
        }
    }
    if (showBrake) { AlertDialog(onDismissRequest={showBrake=false}, title={Text("🛡 Safety Brake")}, text={Column{Text("Risk signals detected. Choose an action; the call will not be ended silently."); safetyDecision?.reasons?.take(3)?.forEach{Text("• $it")}}}, confirmButton={TextButton(onClick={safetyVm::pause;showBrake=false}){Text("Pause & Review")}}, dismissButton={Row{TextButton(onClick={showBrake=false}){Text("Continue")}; TextButton(onClick={safetyVm::review;showBrake=false}){Text("Review")}}}) }
    if (showConsent) { ProtectionConsentDialog(onEnable = { consent -> showConsent = false; protectionVm.enable(callId, consent) }, onContinueWithout = { showConsent = false }) }
    LaunchedEffect(state) { if (state is CallState.Connected) protectionVm.send(ProtectionEvent("call_connected", callId)); if (state is CallState.Ended) { protectionVm.disable(); onEnded() } }
}

@Composable
private fun VideoRenderer(track: VideoTrack, modifier: Modifier) {
    val context = LocalContext.current
    val egl = remember { EglBase.create() }
    val renderer = remember { SurfaceViewRenderer(context) }
    DisposableEffect(track) {
        renderer.init(egl.eglBaseContext, null)
        renderer.setEnableHardwareScaler(true)
        renderer.setMirror(true)
        track.addSink(renderer)
        onDispose { track.removeSink(renderer); renderer.release(); egl.release() }
    }
    AndroidView(factory = { renderer }, modifier = modifier) { it.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) }
}
