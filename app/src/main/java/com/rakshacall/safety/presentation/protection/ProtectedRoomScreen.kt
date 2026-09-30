package com.rakshacall.safety.presentation.protection

import androidx.compose.runtime.Composable

/**
 * ProtectedRoomScreen provides genuine WebRTC calling with live speech streaming,
 * multilingual semantic intelligence, real-time risk overlay, and safety brake.
 */
@Composable
fun ProtectedRoomScreen(
    onNavigateBack: () -> Unit,
    onOpenVerificationCoach: () -> Unit
) {
    RealtimeCallScreen(
        onEndCall = onNavigateBack,
        onOpenVerificationCoach = onOpenVerificationCoach
    )
}
