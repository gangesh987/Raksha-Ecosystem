package com.rakshacall.safety.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.ScamStage
import com.rakshacall.safety.domain.model.SessionStatus
import com.rakshacall.safety.presentation.evidence.EvidenceDetailScreen
import com.rakshacall.safety.presentation.evidence.EvidenceVaultScreen
import com.rakshacall.safety.presentation.home.*
import com.rakshacall.safety.presentation.intelligence.IntelligenceScreen
import com.rakshacall.safety.presentation.intelligence.ModelCenterScreen
import com.rakshacall.safety.presentation.onboarding.*
import com.rakshacall.safety.presentation.privacy.PrivacyCenterScreen
import com.rakshacall.safety.presentation.protection.*
import com.rakshacall.safety.presentation.settings.SecurityCenterScreen
import com.rakshacall.safety.presentation.settings.SettingsScreen
import com.rakshacall.safety.presentation.theme.TealPrimary
import com.rakshacall.safety.presentation.trustedcontacts.FamilyScreen
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun RakshaCallNavHost() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val isBottomNavVisible = currentScreen is Screen.Home ||
            currentScreen is Screen.ProtectHub ||
            currentScreen is Screen.Calls ||
            currentScreen is Screen.Intelligence ||
            currentScreen is Screen.Evidence ||
            currentScreen is Screen.Family ||
            currentScreen is Screen.Settings

    Scaffold(
        bottomBar = {
            if (isBottomNavVisible) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Home,
                        onClick = { currentScreen = Screen.Home },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("HOME", fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = TealPrimary, indicatorColor = TealPrimary.copy(alpha = 0.15f))
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.ProtectHub,
                        onClick = { currentScreen = Screen.ProtectHub },
                        icon = { Icon(Icons.Default.Security, contentDescription = "Protect") },
                        label = { Text("PROTECT", fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = TealPrimary, indicatorColor = TealPrimary.copy(alpha = 0.15f))
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Calls,
                        onClick = { currentScreen = Screen.Calls },
                        icon = { Icon(Icons.Default.PhoneCallback, contentDescription = "Calls") },
                        label = { Text("CALLS", fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = TealPrimary, indicatorColor = TealPrimary.copy(alpha = 0.15f))
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Intelligence,
                        onClick = { currentScreen = Screen.Intelligence },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = "Intelligence") },
                        label = { Text("INTEL", fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = TealPrimary, indicatorColor = TealPrimary.copy(alpha = 0.15f))
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Evidence,
                        onClick = { currentScreen = Screen.Evidence },
                        icon = { Icon(Icons.Default.FolderShared, contentDescription = "Evidence") },
                        label = { Text("EVIDENCE", fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = TealPrimary, indicatorColor = TealPrimary.copy(alpha = 0.15f))
                    )
                    NavigationBarItem(
                        selected = currentScreen is Screen.Settings,
                        onClick = { currentScreen = Screen.Settings },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                        label = { Text("MORE", fontSize = 9.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = TealPrimary, indicatorColor = TealPrimary.copy(alpha = 0.15f))
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                // Onboarding & Authentication
                is Screen.Splash -> {
                    SplashScreen(
                        onNavigateToHome = { currentScreen = Screen.Home },
                        onNavigateToWelcome = { currentScreen = Screen.Welcome }
                    )
                }

                is Screen.Welcome -> {
                    WelcomeScreen(
                        onContinue = { currentScreen = Screen.PrivacyExplanation },
                        onWatchTour = { currentScreen = Screen.IntroTour }
                    )
                }

                is Screen.IntroTour -> {
                    com.rakshacall.safety.presentation.intro.IntroTourScreen(
                        onNavigateBack = { currentScreen = Screen.Welcome }
                    )
                }

                is Screen.PrivacyExplanation -> {
                    PrivacyExplanationScreen(onContinue = { currentScreen = Screen.Auth })
                }

                is Screen.Auth -> {
                    AuthScreen(onVerificationComplete = { currentScreen = Screen.PermissionCenter })
                }

                is Screen.PermissionCenter -> {
                    PermissionCenterScreen(onContinue = { currentScreen = Screen.Home })
                }

                // Primary Tabs
                is Screen.Home -> {
                    HomeScreen(
                        onStartProtection = {
                            currentScreen = Screen.ProtectHub
                        },
                        onViewActiveProtection = { currentScreen = Screen.Protection },
                        onNavigateToIntelligence = { currentScreen = Screen.Intelligence },
                        onNavigateToEvidence = { currentScreen = Screen.Evidence },
                        onNavigateToContacts = { currentScreen = Screen.Family },
                        onNavigateToPrivacy = { currentScreen = Screen.PrivacyCenter },
                        onNavigateToSettings = { currentScreen = Screen.Settings },
                        onOpenLiveInputLab = { currentScreen = Screen.LiveInputLab },
                        onOpenVideoCallSimulation = { currentScreen = Screen.VideoCallSimulation },
                        onWatchTour = { currentScreen = Screen.IntroTour },
                        onSelectSession = { id ->
                            selectedSessionId = id
                            currentScreen = Screen.SessionDetails(id)
                        }
                    )
                }

                is Screen.ProtectHub -> {
                    ProtectACallScreen(
                        onNavigateBack = { currentScreen = Screen.Home },
                        onStartNativeRoom = { currentScreen = Screen.ProtectedRoom },
                        onStartProtection = { platformName ->
                            coroutineScope.launch {
                                val session = ProtectionSession(
                                    id = UUID.randomUUID().toString(),
                                    startTime = System.currentTimeMillis(),
                                    status = SessionStatus.ACTIVE,
                                    peakRisk = 0,
                                    finalRisk = 0,
                                    highestStage = ScamStage.CONTACT,
                                    inputSource = platformName
                                )
                                ServiceLocator.sessionRepository.createSession(session)
                                currentScreen = Screen.Protection
                            }
                        }
                    )
                }

                is Screen.Calls -> {
                    CallsScreen(
                        onSelectSession = { id ->
                            selectedSessionId = id
                            currentScreen = Screen.SessionDetails(id)
                        },
                        onStartNewProtectedCall = { currentScreen = Screen.ProtectHub }
                    )
                }

                is Screen.Protection -> {
                    ProtectionScreen(
                        onNavigateBack = { currentScreen = Screen.Home },
                        onOpenVerificationCoach = { currentScreen = Screen.VerificationCoach }
                    )
                }

                is Screen.ProtectedRoom -> {
                    ProtectedRoomScreen(
                        onNavigateBack = { currentScreen = Screen.ProtectHub },
                        onOpenVerificationCoach = { currentScreen = Screen.VerificationCoach }
                    )
                }

                is Screen.IncomingCall -> {
                    IncomingCallScreen(
                        onAnswerNormally = { currentScreen = Screen.Home },
                        onProtectThisCall = { currentScreen = Screen.ProtectedRoom },
                        onDecline = { currentScreen = Screen.Home }
                    )
                }

                is Screen.Intelligence -> {
                    IntelligenceScreen()
                }

                is Screen.Evidence -> {
                    EvidenceVaultScreen(
                        onSelectSession = { id ->
                            selectedSessionId = id
                            currentScreen = Screen.SessionDetails(id)
                        }
                    )
                }

                is Screen.Family, is Screen.TrustedContacts -> {
                    FamilyScreen(
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }

                // Extended Hubs & More Screen
                is Screen.Settings -> {
                    MoreHubScreen(
                        onNavigateToConnectedPlatforms = { currentScreen = Screen.ConnectedPlatforms },
                        onNavigateToModelCenter = { currentScreen = Screen.ModelCenter },
                        onNavigateToSecurityCenter = { currentScreen = Screen.SecurityCenter },
                        onNavigateToNotificationCenter = { currentScreen = Screen.NotificationCenter },
                        onNavigateToSafetyAnalytics = { currentScreen = Screen.SafetyAnalytics },
                        onNavigateToHelpAndSafety = { currentScreen = Screen.HelpAndSafety },
                        onNavigateToSimulationLab = { currentScreen = Screen.VideoCallSimulation },
                        onNavigateToPrivacyCenter = { currentScreen = Screen.PrivacyCenter },
                        onNavigateToPermissionCenter = { currentScreen = Screen.PermissionCenter },
                        onNavigateToSettings = {
                            currentScreen = Screen.AppSettings
                        },
                        onNavigateToAbout = { currentScreen = Screen.AboutRakshaCall }
                    )
                }

                is Screen.AppSettings -> {
                    SettingsScreen(
                        onNavigateBack = { currentScreen = Screen.Settings },
                        onNavigateToContacts = { currentScreen = Screen.Family },
                        onNavigateToPrivacy = { currentScreen = Screen.PrivacyCenter },
                        onSignOut = { currentScreen = Screen.Welcome }
                    )
                }

                is Screen.ConnectedPlatforms -> {
                    ConnectedPlatformsScreen(onNavigateBack = { currentScreen = Screen.Settings })
                }

                is Screen.ModelCenter -> {
                    ModelCenterScreen(onNavigateBack = { currentScreen = Screen.Settings })
                }

                is Screen.SecurityCenter -> {
                    SecurityCenterScreen(onNavigateBack = { currentScreen = Screen.Settings })
                }

                is Screen.NotificationCenter -> {
                    NotificationCenterScreen(onNavigateBack = { currentScreen = Screen.Settings })
                }

                is Screen.SafetyAnalytics -> {
                    SafetyAnalyticsScreen(onNavigateBack = { currentScreen = Screen.Settings })
                }

                is Screen.HelpAndSafety -> {
                    HelpAndSafetyScreen(onNavigateBack = { currentScreen = Screen.Settings })
                }

                is Screen.AboutRakshaCall -> {
                    AboutScreen(onNavigateBack = { currentScreen = Screen.Settings })
                }

                is Screen.SessionDetails -> {
                    EvidenceDetailScreen(
                        sessionId = selectedSessionId ?: screen.sessionId,
                        onNavigateBack = { currentScreen = Screen.Evidence }
                    )
                }

                is Screen.PrivacyCenter -> {
                    PrivacyCenterScreen(
                        onNavigateBack = { currentScreen = Screen.Settings },
                        onResetCompleted = { currentScreen = Screen.Welcome }
                    )
                }

                is Screen.LiveInputLab -> {
                    LiveInputLabScreen(
                        onNavigateBack = { currentScreen = Screen.Home },
                        onOpenVerificationCoach = { currentScreen = Screen.VerificationCoach }
                    )
                }

                is Screen.VideoCallSimulation -> {
                    ProtectedRoomScreen(
                        onNavigateBack = { currentScreen = Screen.Home },
                        onOpenVerificationCoach = { currentScreen = Screen.VerificationCoach }
                    )
                }

                is Screen.VerificationCoach -> {
                    VerificationCoachScreen(
                        onNavigateBack = { currentScreen = Screen.Protection }
                    )
                }

                else -> {}
            }
        }
    }
}
