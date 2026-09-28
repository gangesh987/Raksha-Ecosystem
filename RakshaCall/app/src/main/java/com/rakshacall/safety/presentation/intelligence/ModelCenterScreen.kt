package com.rakshacall.safety.presentation.intelligence

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.presentation.theme.TealPrimary

data class AIModelInfo(
    val category: String,
    val modelName: String,
    val executionTarget: String,
    val status: String, // ACTIVE, AVAILABLE, NOT CONFIGURED, PROPOSED
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelCenterScreen(
    onNavigateBack: () -> Unit
) {
    val models = listOf(
        // Speech AI
        AIModelInfo("Speech AI", "Android On-Device SpeechRecognizer", "Local Android Framework", "ACTIVE", "Real-time timestamped speech transcription from local microphone buffer."),
        AIModelInfo("Speech AI", "OpenAI Whisper (Adapter)", "PyTorch / ONNX Runtime", "AVAILABLE ADAPTER", "Offline multilingual speech recognition adapter."),
        AIModelInfo("Speech AI", "Wav2Vec 2.0 / HuBERT", "ONNX Mobile", "PROPOSED MODEL", "Proposed acoustic feature extractor for vocal stress & emotional coercion."),

        // NLP & Conversation Intelligence
        AIModelInfo("NLP Intelligence", "RakshaCall Deterministic Regex & Pattern Guardrail", "Zero-Latency CPU", "ACTIVE", "Deterministic high-speed keyword & tactic pattern matching for OTP, arrest threats, and fund demands."),
        AIModelInfo("NLP Intelligence", "Groq Cloud LLaMA 3.3 70B", "Groq Cloud API", "ACTIVE", "Secondary conversational reasoning tier with fast low-latency inference."),
        AIModelInfo("NLP Intelligence", "Google Gemini 1.5 Flash", "Google Generative AI API", "ACTIVE", "Cloud NLP provider for complex multi-turn scam manipulation analysis."),
        AIModelInfo("NLP Intelligence", "DeBERTa-v3 / MuRIL", "HuggingFace Local Adapter", "AVAILABLE ADAPTER", "Indian multilingual language model adapter for regional language coercion detection."),

        // Computer Vision
        AIModelInfo("Computer Vision", "CameraX Video Frame Analyzer", "Android Jetpack CameraX", "ACTIVE", "Live video frame ingestion and temporal consistency verification."),
        AIModelInfo("Computer Vision", "MediaPipe Face Detection", "Google MediaPipe TFLite", "AVAILABLE ADAPTER", "Facial landmark and stability evaluation adapter for supporting visual signals."),
        AIModelInfo("Computer Vision", "VideoMAE / TimeSformer", "Deep Learning Model", "PROPOSED MODEL", "Proposed deepfake and synthetic temporal artifact detection network."),

        // Multimodal Fusion
        AIModelInfo("Multimodal Fusion", "RakshaCall Deterministic Risk Fusion Engine", "On-Device Kotlin", "ACTIVE", "Fuses conversation (primary) and visual (supporting) signals with explainable reasoning and disagreement detection.")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI & MODEL CENTER", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TealPrimary.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Model Integrity Disclosure", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TealPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "In strict accordance with engineering transparency, only models that actually execute in the running app are marked ACTIVE. Proposed research models are explicitly labelled.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(models) { model ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.category.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(model.modelName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            val badgeColor = when (model.status) {
                                "ACTIVE" -> Color(0xFF2E7D32)
                                "AVAILABLE ADAPTER" -> Color(0xFF1976D2)
                                else -> Color(0xFF757575)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    model.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Target: ${model.executionTarget}", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(model.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
