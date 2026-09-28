plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.raksha.video"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.raksha.video"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    val backendHttpUrl = providers.gradleProperty("RAKSHA_BACKEND_URL")
        .orElse(System.getenv("RAKSHA_BACKEND_URL") ?: "https://poor-keys-like.loca.lt")
        .get()
    val backendWsUrl = providers.gradleProperty("RAKSHA_SIGNALING_WS_URL")
        .orElse(System.getenv("RAKSHA_SIGNALING_WS_URL") ?: "wss://poor-keys-like.loca.lt/ws/signaling")
        .get()
    val apiToken = providers.gradleProperty("RAKSHA_API_TOKEN")
        .orElse(System.getenv("RAKSHA_API_TOKEN") ?: "demo-token-raksha-video")
        .get()
    buildTypes.getByName("debug") {
        buildConfigField("String", "BACKEND_HTTP_URL", "\"${backendHttpUrl.replace("\"", "\\\"")}\"")
        buildConfigField("String", "SIGNALING_WS_URL", "\"${backendWsUrl.replace("\"", "\\\"")}\"")
        buildConfigField("String", "API_TOKEN", "\"${apiToken.replace("\"", "\\\"")}\"")
    }
    buildTypes.getByName("release") {
        buildConfigField("String", "BACKEND_HTTP_URL", "\"${backendHttpUrl.replace("\"", "\\\"")}\"")
        buildConfigField("String", "SIGNALING_WS_URL", "\"${backendWsUrl.replace("\"", "\\\"")}\"")
        buildConfigField("String", "API_TOKEN", "\"${apiToken.replace("\"", "\\\"")}\"")
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Modern WebRTC media (Maven Central)
    implementation("io.getstream:stream-webrtc-android:1.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
