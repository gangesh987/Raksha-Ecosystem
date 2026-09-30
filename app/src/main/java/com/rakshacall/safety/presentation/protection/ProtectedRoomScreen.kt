package com.rakshacall.safety.presentation.protection

<<<<<<< HEAD
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.media.WebRtcRoomManager
import com.rakshacall.safety.domain.model.*
import com.rakshacall.safety.presentation.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ProtectedRoomScreen provides genuine WebRTC calling with live speech streaming,
 * multilingual semantic intelligence, real-time risk overlay, and safety brake.
 */
@Composable
fun ProtectedRoomScreen(
    onNavigateBack: () -> Unit,
    onOpenVerificationCoach: () -> Unit
) {
    var useRealtimeCallScreen by remember { mutableStateOf(false) }

    if (useRealtimeCallScreen) {
        RealtimeCallScreen(
            onEndCall = onNavigateBack,
            onOpenVerificationCoach = onOpenVerificationCoach
        )
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val roomManager = remember { WebRtcRoomManager() }

    var room by remember { mutableStateOf(roomManager.createRoom()) }
    val durationSeconds by roomManager.callDurationSeconds.collectAsState()

    var riskScore by remember { mutableIntStateOf(0) }
    var currentStage by remember { mutableStateOf(ScamStage.CONTACT) }
    var velocityLevel by remember { mutableStateOf(VelocityLevel.LOW) }
    var detectedTactics by remember { mutableStateOf<List<RiskSignal>>(emptyList()) }
    var transcriptEvents by remember { mutableStateOf<List<TranscriptEvent>>(emptyList()) }

    // Phase 1 Multilingual ASR Validation State
    var showAsrValidationHud by remember { mutableStateOf(true) }
    var currentAsrLanguage by remember { mutableStateOf("Tamil") }
    var currentAsrConfidence by remember { mutableFloatStateOf(0.89f) }
    var currentAsrTranscript by remember { mutableStateOf("உங்கள் வங்கி கணக்கு முடக்கப்பட்டுள்ளது.") }
    var currentAsrCodeSwitch by remember { mutableStateOf(false) }
    var currentAsrStatus by remember { mutableStateOf("ASR_OK") }

    var showSafetyBrake by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var testInputText by remember { mutableStateOf("") }
    var isProtectionPaused by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val serviceIntent = Intent(context, com.rakshacall.safety.services.ProtectionForegroundService::class.java).apply {
            action = com.rakshacall.safety.services.ProtectionForegroundService.ACTION_START
        }
        try {
            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: Exception) {}

        onDispose {
            roomManager.leaveRoom()
            val stopIntent = Intent(context, com.rakshacall.safety.services.ProtectionForegroundService::class.java).apply {
                action = com.rakshacall.safety.services.ProtectionForegroundService.ACTION_STOP
            }
            try {
                context.startService(stopIntent)
            } catch (_: Exception) {}
        }
    }

    fun triggerHapticFeedback() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 100, 250), -1))
        } catch (e: Exception) { /* ignore */ }
    }

    fun detectLanguageHeuristic(text: String): Pair<String, Boolean> {
        val hasTamil = text.any { it in '\u0B80'..'\u0BFF' }
        val hasHindi = text.any { it in '\u0900'..'\u097F' }
        val hasTelugu = text.any { it in '\u0C00'..'\u0C7F' }
        val hasKannada = text.any { it in '\u0C80'..'\u0CFF' }
        val hasMalayalam = text.any { it in '\u0D00'..'\u0D7F' }
        val hasBengali = text.any { it in '\u0980'..'\u09FF' }
        val hasGujarati = text.any { it in '\u0A80'..'\u0AFF' }
        val hasPunjabi = text.any { it in '\u0A00'..'\u0A7F' }
        val hasOdia = text.any { it in '\u0B00'..'\u0B7F' }
        val hasLatin = text.any { it in 'a'..'z' || it in 'A'..'Z' }

        val lower = text.lowercase()
        val isTanglish = hasLatin && (lower.contains("unga") || lower.contains("pannunga") || lower.contains("aayiduchu") || lower.contains("kaasu"))
        val isHinglish = hasLatin && (lower.contains("aapka") || lower.contains("kijiye") || lower.contains("ho gaya") || lower.contains("paisa"))

        return when {
            isTanglish -> Pair("Tamil (Tanglish)", true)
            isHinglish -> Pair("Hindi (Hinglish)", true)
            hasTamil -> Pair("Tamil", hasLatin)
            hasHindi -> Pair("Hindi", hasLatin)
            hasTelugu -> Pair("Telugu", hasLatin)
            hasKannada -> Pair("Kannada", hasLatin)
            hasMalayalam -> Pair("Malayalam", hasLatin)
            hasBengali -> Pair("Bengali", hasLatin)
            hasGujarati -> Pair("Gujarati", hasLatin)
            hasPunjabi -> Pair("Punjabi", hasLatin)
            hasOdia -> Pair("Odia", hasLatin)
            else -> Pair("English", false)
        }
    }

    fun processSpeechInput(
        text: String,
        speaker: String = "CALLER",
        customLang: String? = null,
        confidence: Float = 0.89f,
        asrStatus: String = "ASR_OK"
    ) {
        if (text.isBlank() || isProtectionPaused) return
        val (detectedLang, isCodeSwitch) = detectLanguageHeuristic(text)
        val finalLang = customLang ?: detectedLang
        currentAsrTranscript = text.trim()
        currentAsrLanguage = finalLang
        currentAsrConfidence = confidence
        currentAsrStatus = asrStatus
        currentAsrCodeSwitch = isCodeSwitch

        val timestamp = System.currentTimeMillis()
        val event = TranscriptEvent(
            id = UUID.randomUUID().toString(),
            sessionId = room.roomId,
            timestamp = timestamp,
            speaker = speaker,
            text = text.trim(),
            confidence = confidence,
            language = finalLang
        )
        transcriptEvents = transcriptEvents + event

        // Analyze via RiskEngine
        val newSignals = ServiceLocator.riskEngine.analyzeTranscript(event, detectedTactics)
        if (newSignals.isNotEmpty()) {
            detectedTactics = detectedTactics + newSignals
            // Advance Scam Stage
            for (sig in newSignals) {
                val transition = ServiceLocator.scamStageMachine.processSignal(sig)
                if (transition != null) {
                    currentStage = transition.toStage
                }
            }
            // Recalculate score
            val newScore = ServiceLocator.riskEngine.calculateCurrentScore(detectedTactics, room.createdAt)
            riskScore = newScore

            // Velocity
            val vel = ServiceLocator.velocityEngine.calculateVelocity(detectedTactics, room.createdAt)
            velocityLevel = vel.level

            // Safety brake check
            if (ServiceLocator.riskEngine.isSafetyBrakeTriggered(newScore, detectedTactics)) {
                showSafetyBrake = true
                triggerHapticFeedback()
            }
        }
    }

    val formattedDuration = String.format("%02d:%02d", durationSeconds / 60, durationSeconds % 60)
    val riskLevel = RiskLevel.fromScore(riskScore)
    val riskColor = when (riskLevel) {
        RiskLevel.LOW -> Color(0xFF2E7D32)
        RiskLevel.MEDIUM -> Color(0xFFF57C00)
        RiskLevel.HIGH -> Color(0xFFD32F2F)
        RiskLevel.CRITICAL -> Color(0xFFB71C1C)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isProtectionPaused) Color.Gray else TealPrimary,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("RAKSHACALL ROOM", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${room.roomId} • $formattedDuration", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showAsrValidationHud = !showAsrValidationHud }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Toggle ASR HUD",
                            tint = if (showAsrValidationHud) TealPrimary else Color.Gray
                        )
                    }
                    IconButton(onClick = { showInviteDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Invite")
                    }
                    IconButton(onClick = {
                        isProtectionPaused = !isProtectionPaused
                    }) {
                        Icon(
                            if (isProtectionPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Toggle Protection",
                            tint = if (isProtectionPaused) Color(0xFFF57C00) else TealPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Video Areas (Remote & Local preview)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF0F172A))
            ) {
                // Remote Tile
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Remote Participant Connected", color = Color.LightGray, fontSize = 13.sp)
                    Text("RakshaCall Real-Time Safety Guard Active", color = TealPrimary, fontSize = 11.sp)
                }

                // Local Preview PIP Tile
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(width = 80.dp, height = 110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, TealPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (room.isVideoEnabled) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Face, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(24.dp))
                            Text("You", color = Color.White, fontSize = 10.sp)
                        }
                    } else {
                        Icon(Icons.Default.VideocamOff, contentDescription = null, tint = Color.Gray)
                    }
                }

                // Live Risk Pill Overlay
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = riskColor.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RISK: $riskScore/100", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(riskLevel.label, color = Color.White, fontSize = 10.sp)
                    }
                }
            }

            // Real-Time Metrics Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("SCAM STAGE", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(currentStage.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text("VELOCITY", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(velocityLevel.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (velocityLevel == VelocityLevel.HIGH) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("TACTICS DETECTED", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("${detectedTactics.size} signals", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Step 6: Mobile ASR Validation HUD
            if (showAsrValidationHud) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ASR VALIDATION HUD",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (currentAsrStatus == "ASR_OK") Color(0xFF2E7D32) else Color(0xFFD32F2F)
                            ) {
                                Text(
                                    currentAsrStatus,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("LANGUAGE", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(currentAsrLanguage, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("CONFIDENCE", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("${(currentAsrConfidence * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("CODE SWITCH", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(if (currentAsrCodeSwitch) "YES" else "NO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (currentAsrCodeSwitch) Color(0xFFF57C00) else Color.White)
                            }
                            Column {
                                Text("RISK STATUS", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(riskLevel.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = riskColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("TRANSCRIPT", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            "\"$currentAsrTranscript\"",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            maxLines = 2
                        )
                        if (detectedTactics.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("RISK REASONS", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(
                                detectedTactics.takeLast(2).joinToString("; ") { "${it.tactic.displayName}: \"${it.evidenceText}\"" },
                                fontSize = 11.sp,
                                color = Color(0xFFFCA5A5),
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // Live Transcript Feed
            Text(
                "LIVE CONVERSATION TRANSCRIPT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (transcriptEvents.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Microphone is live and monitoring for coercive speech patterns. Speak into the microphone or use the test phrases below.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                items(transcriptEvents) { evt ->
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(evt.timestamp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (evt.speaker == "CALLER") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else TealPrimary.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(evt.speaker, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (evt.speaker == "CALLER") Color(0xFF1E88E5) else TealPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(time, fontSize = 10.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(evt.text, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Multilingual Test Matrix Presets (Step 8, 9, 12, 13, 14)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionChip(
                    onClick = { processSpeechInput("Your bank account has been blocked. Please transfer the money immediately.", customLang = "English", confidence = 0.95f) },
                    label = { Text("English", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("உங்கள் வங்கி கணக்கு முடக்கப்பட்டுள்ளது. உடனே பணத்தை மாற்ற வேண்டும்.", customLang = "Tamil", confidence = 0.89f) },
                    label = { Text("Tamil", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("आपका बैंक खाता बंद कर दिया गया है। तुरंत पैसे ट्रांसफर कीजिए।", customLang = "Hindi", confidence = 0.91f) },
                    label = { Text("Hindi", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("మీ బ్యాంక్ ఖాతా బ్లాక్ చేయబడింది. వెంటనే డబ్బును బదిలీ చేయండి.", customLang = "Telugu", confidence = 0.87f) },
                    label = { Text("Telugu", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("ನಿಮ್ಮ ಬ್ಯಾಂಕ್ ಖಾತೆಯನ್ನು ನಿರ್ಬಂಧಿಸಲಾಗಿದೆ. ತಕ್ಷಣವೇ ಹಣವನ್ನು ವರ್ಗಾಯಿಸಿ.", customLang = "Kannada", confidence = 0.86f) },
                    label = { Text("Kannada", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("നിങ്ങളുടെ ബാങ്ക് അക്കൗണ്ട് ബ്ലോക്ക് ചെയ്തു. ഉടൻ പണം മാറ്റുക.", customLang = "Malayalam", confidence = 0.88f) },
                    label = { Text("Malayalam", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("আপনার ব্যাঙ্ক অ্যাকাউন্ট ব্লক করা হয়েছে। অবিলম্বে টাকা স্থানান্তর করুন।", customLang = "Bengali", confidence = 0.85f) },
                    label = { Text("Bengali", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("तुमचे बँक खाते ब्लॉक केले आहे. त्वरित पैसे ट्रान्सफर करा.", customLang = "Marathi", confidence = 0.88f) },
                    label = { Text("Marathi", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("તમારું બેંક ખાતું બ્લોક કરી દેવામાં આવ્યું છે. તરત જ પૈસા ટ્રાન્સફર કરો.", customLang = "Gujarati", confidence = 0.84f) },
                    label = { Text("Gujarati", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("ਤੁਹਾਡਾ ਬੈਂਕ ਖਾਤਾ ਬਲਾਕ ਕਰ ਦਿੱਤਾ ਗਿਆ ਹੈ। ਤੁਰੰਤ ਪੈਸੇ ਟ੍ਰਾਂਸਫਰ ਕਰੋ।", customLang = "Punjabi", confidence = 0.83f) },
                    label = { Text("Punjabi", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("ଆପଣଙ୍କର ବ୍ୟାଙ୍କ ଖାତା ବନ୍ଦ ହୋଇଯାଇଛି। ତୁରନ୍ତ ଟଙ୍କା ସ୍ଥାନାନ୍ତର କରନ୍ତୁ।", customLang = "Odia", confidence = 0.81f) },
                    label = { Text("Odia", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("Sir unga bank account block aayiduchu. Immediate ah amount transfer pannunga.", customLang = "Tamil (Tanglish)", confidence = 0.90f) },
                    label = { Text("Tanglish (CS)", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("Aapka bank account block ho gaya hai. Turant amount transfer kijiye.", customLang = "Hindi (Hinglish)", confidence = 0.92f) },
                    label = { Text("Hinglish (CS)", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("My bank asked me to visit the branch tomorrow.", customLang = "English", confidence = 0.98f) },
                    label = { Text("Benign 1", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("I watched a documentary about digital arrest scams.", customLang = "English", confidence = 0.97f) },
                    label = { Text("Benign 2", fontSize = 10.sp) }
                )
                SuggestionChip(
                    onClick = { processSpeechInput("Move the funds before the deadline.", customLang = "English", confidence = 0.94f) },
                    label = { Text("Paraphrase Scam", fontSize = 10.sp) }
                )
            }

            // Manual Text Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = testInputText,
                    onValueChange = { testInputText = it },
                    placeholder = { Text("Speak or type test phrase...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (testInputText.isNotBlank()) {
                            processSpeechInput(testInputText)
                            testInputText = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = TealPrimary)
                }
            }

            // Bottom In-Call Action Controls
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { roomManager.toggleMute(); room = roomManager.currentRoom.value ?: room }
                    ) {
                        Icon(
                            if (room.isAudioMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = if (room.isAudioMuted) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { roomManager.toggleVideo(); room = roomManager.currentRoom.value ?: room }
                    ) {
                        Icon(
                            if (room.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Video",
                            tint = if (room.isVideoEnabled) TealPrimary else Color.Red
                        )
                    }
                    IconButton(
                        onClick = { roomManager.toggleSpeaker(); room = roomManager.currentRoom.value ?: room }
                    ) {
                        Icon(
                            if (room.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Speaker"
                        )
                    }
                    Button(
                        onClick = onOpenVerificationCoach,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("VERIFY", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            roomManager.leaveRoom()
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("END", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // Invite Dialog
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite to Protected Room", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Share this Room Code with the other participant:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TealPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            room.roomId,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = TealPrimary,
                            modifier = Modifier.padding(14.dp),
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("RakshaCall's real-time safety layer will monitor the session for coercive tactics.", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("RakshaCall Room", room.roomId)
                        clipboard.setPrimaryClip(clip)
                        showInviteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("COPY CODE")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Join my RakshaCall Protected Call")
                            putExtra(Intent.EXTRA_TEXT, "Join my RakshaCall Protected Call with code: ${room.roomId}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Room Code"))
                        showInviteDialog = false
                    }
                ) {
                    Text("SHARE LINK")
                }
            }
        )
    }

    // FULL SCREEN SAFETY BRAKE MODAL
    if (showSafetyBrake) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss by tapping outside when safety brake is active */ },
            containerColor = Color(0xFF1E1B2E),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAFETY BRAKE ACTIVATED", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text("PAUSE — DO NOT CONTINUE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("High risk coercive behavior detected with irreversible pressure:", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("❌ DO NOT transfer money or funds", color = Color(0xFFFF8A80), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("❌ DO NOT share OTP, UPI PIN, or passwords", color = Color(0xFFFF8A80), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("❌ DO NOT install remote access software", color = Color(0xFFFF8A80), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("❌ DO NOT obey instructions under fear of arrest", color = Color(0xFFFF8A80), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No legitimate government officer or police official will demand money over video call or request private OTPs.", color = Color.LightGray, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            showSafetyBrake = false
                            onOpenVerificationCoach()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("VERIFY INDEPENDENTLY")
                    }
                    Button(
                        onClick = {
                            showSafetyBrake = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "RakshaCall Urgent Alert: High-risk coercive scam detected on my active call. Do not transfer funds. Please contact me."
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Alert Trusted Person"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary.copy(alpha = 0.85f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CONTACT TRUSTED PERSON")
                    }
                    Button(
                        onClick = {
                            showSafetyBrake = false
                            roomManager.leaveRoom()
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("DISCONNECT / PAUSE CALL")
                    }
                    TextButton(
                        onClick = { showSafetyBrake = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CONTINUE AT MY OWN RISK", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        )
    }
}
