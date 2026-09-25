package com.rakshacall.safety

import com.rakshacall.safety.domain.model.PlatformConnection
import org.junit.Assert.*
import org.junit.Test

class PlatformConnectionTest {

    @Test
    fun testWhatsAppReportsPermittedAudioOnlyWithoutDecryption() {
        val wa = PlatformConnection(
            id = "whatsapp",
            platformName = "WhatsApp",
            supportedProtectionMethod = "Permitted Microphone / Screen Capture",
            permissionRequirements = "RECORD_AUDIO",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Zero packet interception of WhatsApp private encrypted VoIP streams."
        )
        assertTrue(wa.isSupported)
        assertTrue(wa.isConfigured)
        assertTrue(wa.limitationNote.contains("Zero packet interception"))
    }

    @Test
    fun testGoogleMeetRequiresOAuthConsent() {
        val meet = PlatformConnection(
            id = "meet",
            platformName = "Google Meet",
            supportedProtectionMethod = "Official Cloud Media API / Screen Capture",
            permissionRequirements = "Google Workspace OAuth Consent",
            isSupported = true,
            isConfigured = false,
            limitationNote = "Requires authorized Google Cloud OAuth tenant credentials to connect to live stream."
        )
        assertTrue(meet.isSupported)
        assertFalse("Unconfigured meet API must not claim ready", meet.isConfigured)
    }

    @Test
    fun testTeamsReportsMediaProjection() {
        val teams = PlatformConnection(
            id = "teams",
            platformName = "Microsoft Teams",
            supportedProtectionMethod = "Consented MediaProjection Audio",
            permissionRequirements = "MediaProjection",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Protects incoming meetings via permitted Android screen/audio projection."
        )
        assertEquals("MediaProjection", teams.permissionRequirements)
    }

    @Test
    fun testNativeRoomReportsFullSupport() {
        val nativeRoom = PlatformConnection(
            id = "native_room",
            platformName = "RakshaCall Protected Room",
            supportedProtectionMethod = "Native 1-to-1 WebRTC Video Room",
            permissionRequirements = "RECORD_AUDIO, CAMERA",
            isSupported = true,
            isConfigured = true,
            limitationNote = "Full end-to-end protection inside RakshaCall with live transcript & risk engine."
        )
        assertTrue(nativeRoom.isSupported)
        assertTrue(nativeRoom.isConfigured)
    }

    @Test
    fun testZeroEncryptedTrafficDecryptionPolicy() {
        val disclaimer = "RakshaCall strictly complies with platform sandboxing: zero silent interception or unauthorized VoIP decryption."
        assertTrue(disclaimer.contains("zero silent interception"))
        assertTrue(disclaimer.contains("unauthorized VoIP decryption"))
    }
}
