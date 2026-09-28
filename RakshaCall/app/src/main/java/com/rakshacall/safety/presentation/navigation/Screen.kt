package com.rakshacall.safety.presentation.navigation

sealed class Screen(val route: String, val title: String) {
    // 1. Onboarding & Authentication
    data object Splash : Screen("splash", "RakshaCall")
    data object Welcome : Screen("welcome", "Welcome")
    data object PrivacyExplanation : Screen("privacy_explanation", "Privacy First")
    data object Auth : Screen("auth", "Authentication")
    data object PermissionCenter : Screen("permission_center", "Permission Center")
    data object IntroTour : Screen("intro_tour", "Product Tour")

    // 2. Primary Navigation Bar Destinations
    data object Home : Screen("home", "Home")
    data object ProtectHub : Screen("protect_hub", "Protect A Call")
    data object Calls : Screen("calls", "Call History")
    data object Intelligence : Screen("intelligence", "Risk Intelligence")
    data object Evidence : Screen("evidence", "Evidence Vault")
    data object Family : Screen("family", "Family Safety")

    // 3. Core Protection & Calling Rooms
    data object Protection : Screen("protection", "Live Protection")
    data object ProtectedRoom : Screen("protected_room", "Protected Room")
    data object IncomingCall : Screen("incoming_call", "Incoming Call Screener")
    data object VerificationCoach : Screen("verification_coach", "Verification Coach")
    data object LiveInputLab : Screen("live_input_lab", "Live Input Lab")
    data object VideoCallSimulation : Screen("video_call_simulation", "Simulation Lab")

    // 4. Detailed & Incident Centers
    data class SessionDetails(val sessionId: String) : Screen("session_details/$sessionId", "Session Incident")
    data class IncidentReportView(val sessionId: String) : Screen("incident_report/$sessionId", "Incident Report")

    // 5. Extended Hubs & Configuration
    data object ConnectedPlatforms : Screen("connected_platforms", "Connected Platforms")
    data object ModelCenter : Screen("model_center", "AI Model Center")
    data object SecurityCenter : Screen("security_center", "Security Center")
    data object NotificationCenter : Screen("notification_center", "Notification Center")
    data object SafetyAnalytics : Screen("safety_analytics", "Safety Analytics")
    data object HelpAndSafety : Screen("help_and_safety", "Help & Safety Guides")
    data object TrustedContacts : Screen("trusted_contacts", "Trusted Contacts")
    data object PrivacyCenter : Screen("privacy_center", "Privacy Center")
    data object Settings : Screen("settings", "Settings")
    data object AppSettings : Screen("app_settings", "Settings & Configuration")
    data object AboutRakshaCall : Screen("about_rakshacall", "About RakshaCall")
}
