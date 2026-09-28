from pathlib import Path
p=Path('/mnt/data/phase6_work')
# Android build config
f=p/'app/build.gradle.kts'
s=f.read_text()
s=s.replace('        release {\n            isMinifyEnabled = false', '        release {\n            isMinifyEnabled = true')
s=s.replace('    buildFeatures { compose = true }', '''    buildFeatures {\n        compose = true\n        buildConfig = true\n    }\n\n    val backendHttpUrl = providers.gradleProperty("RAKSHA_BACKEND_URL")\n        .orElse(System.getenv("RAKSHA_BACKEND_URL") ?: "")\n        .get()\n    val backendWsUrl = providers.gradleProperty("RAKSHA_SIGNALING_WS_URL")\n        .orElse(System.getenv("RAKSHA_SIGNALING_WS_URL") ?: "")\n        .get()\n    val apiToken = providers.gradleProperty("RAKSHA_API_TOKEN")\n        .orElse(System.getenv("RAKSHA_API_TOKEN") ?: "")\n        .get()\n    buildTypes.getByName("debug") {\n        buildConfigField("String", "BACKEND_HTTP_URL", "\\\"${backendHttpUrl.replace("\\\"", "\\\\\\\"")}\\\"")\n        buildConfigField("String", "SIGNALING_WS_URL", "\\\"${backendWsUrl.replace("\\\"", "\\\\\\\"")}\\\"")\n        buildConfigField("String", "API_TOKEN", "\\\"${apiToken.replace("\\\"", "\\\\\\\"")}\\\"")\n    }\n    buildTypes.getByName("release") {\n        buildConfigField("String", "BACKEND_HTTP_URL", "\\\"${backendHttpUrl.replace("\\\"", "\\\\\\\"")}\\\"")\n        buildConfigField("String", "SIGNALING_WS_URL", "\\\"${backendWsUrl.replace("\\\"", "\\\\\\\"")}\\\"")\n        buildConfigField("String", "API_TOKEN", "\\\"${apiToken.replace("\\\"", "\\\\\\\"")}\\\"")\n    }''')
f.write_text(s)

# Add configuration helper
(p/'app/src/main/java/com/raksha/video/core/security/ProductionConfig.kt').write_text('''package com.raksha.video.core.security\n\nimport com.raksha.video.BuildConfig\n\nobject ProductionConfig {\n    val backendHttpUrl: String = BuildConfig.BACKEND_HTTP_URL.trimEnd('/')\n    val signalingWsUrl: String = BuildConfig.SIGNALING_WS_URL\n    val apiToken: String = BuildConfig.API_TOKEN\n\n    fun validate(): Result<Unit> {\n        if (backendHttpUrl.isBlank() || signalingWsUrl.isBlank()) {\n            return Result.failure(IllegalStateException("Raksha backend endpoints are not configured"))\n        }\n        if (!backendHttpUrl.startsWith("https://")) {\n            return Result.failure(IllegalStateException("Production backend must use HTTPS"))\n        }\n        if (!signalingWsUrl.startsWith("wss://")) {\n            return Result.failure(IllegalStateException("Production signaling must use WSS"))\n        }\n        if (apiToken.isBlank()) return Result.failure(IllegalStateException("Raksha API token is not configured"))\n        return Result.success(Unit)\n    }\n}\n''')

# CallViewModel config
f=p/'app/src/main/java/com/raksha/video/call/CallViewModel.kt'; s=f.read_text()
s=s.replace('import com.raksha.video.network.SignalingClient', 'import com.raksha.video.network.SignalingClient\nimport com.raksha.video.core.security.ProductionConfig')
s=s.replace('    private val signalingUrl: String = "ws://10.0.2.2:8000/ws/call"\n', '')
s=s.replace('        setupWebRtc()\n        signaling = SignalingClient(signalingUrl, "dev-token", object : SignalingClient.Listener {', '        val configResult = ProductionConfig.validate()\n        if (configResult.isFailure) { _callState.value = CallState.Failed(configResult.exceptionOrNull()?.message ?: "Backend configuration missing"); return }\n        setupWebRtc()\n        signaling = SignalingClient(ProductionConfig.signalingWsUrl, ProductionConfig.apiToken, object : SignalingClient.Listener {')
s=s.replace('WebRtcConfig(iceServers = listOf(IceServerConfig(listOf("stun:stun.l.google.com:19302"))))', 'WebRtcConfig(iceServers = listOf(IceServerConfig(listOf("stun:stun.l.google.com:19302"))))')
f.write_text(s)

# Protection and safety clients
f=p/'app/src/main/java/com/raksha/video/protection/RakshaProtectionClient.kt'; s=f.read_text()
s=s.replace('import kotlinx.coroutines.CoroutineScope', 'import com.raksha.video.core.security.ProductionConfig\nimport kotlinx.coroutines.CoroutineScope')
s=s.replace('class RakshaProtectionClient(private val apiBaseUrl: String = "http://10.0.2.2:8000", private val token: String = "dev-token",', 'class RakshaProtectionClient(private val apiBaseUrl: String = ProductionConfig.backendHttpUrl, private val token: String = ProductionConfig.apiToken,')
s=s.replace('        if (!consent.riskDetection', '        ProductionConfig.validate().getOrElse { _state.value = ProtectionState.ERROR; return Result.failure(it) }\n        if (!consent.riskDetection')
f.write_text(s)

f=p/'app/src/main/java/com/raksha/video/safety/SafetyClient.kt'; s=f.read_text()
s=s.replace('import okhttp3.*', 'import com.raksha.video.core.security.ProductionConfig\nimport okhttp3.*')
s=s.replace('class SafetyClient(private val baseUrl:String="http://10.0.2.2:8000", private val token:String="dev-token") {', 'class SafetyClient(private val baseUrl:String=ProductionConfig.backendHttpUrl, private val token:String=ProductionConfig.apiToken) {')
s=s.replace('put("timestamp",System.currentTimeMillis())', 'put("timestamp",System.currentTimeMillis()); put("idempotency_key",java.util.UUID.randomUUID().toString())')
f.write_text(s)

# Better client validation
f=p/'app/src/main/java/com/raksha/video/network/SignalingClient.kt'; s=f.read_text()
s=s.replace('    fun connect() {\n        val request', '    fun connect() {\n        require(serverUrl.startsWith("wss://") || serverUrl.startsWith("ws://")) { "Invalid signaling URL" }\n        require(token.isNotBlank()) { "API token is not configured" }\n        val request')
f.write_text(s)

# Env/docs
(p/'release.properties.example').write_text('''# Supply these through Gradle properties (-P...) or environment variables.\nRAKSHA_BACKEND_URL=https://api.example.com\nRAKSHA_SIGNALING_WS_URL=wss://api.example.com/ws/call\nRAKSHA_API_TOKEN=REPLACE_WITH_DEPLOYMENT_SECRET\n''')
(p/'SIGNING_GUIDE.md').write_text('''# Android Release Signing\n\n1. Create a release keystore outside the repository.\n2. Store keystore credentials in your CI secret store or local Gradle user properties.\n3. Never commit `.jks`, passwords, or signing keys.\n4. Configure the `signingConfigs` block only in the deployment environment.\n5. Verify the release artifact with `apksigner verify --verbose`.\n\nThis repository intentionally contains no signing secrets.\n''')
