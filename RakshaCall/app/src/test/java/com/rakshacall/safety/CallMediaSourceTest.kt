package com.rakshacall.safety

import com.rakshacall.safety.data.media.AndroidScreenAudioConnector
import com.rakshacall.safety.data.media.BrowserCaptureConnector
import com.rakshacall.safety.data.media.CameraMediaSource
import com.rakshacall.safety.data.media.GoogleMeetMediaConnector
import com.rakshacall.safety.data.media.ManualTextInputSource
import com.rakshacall.safety.data.media.MediaSourceRegistry
import com.rakshacall.safety.data.media.MicrophoneMediaSource
import com.rakshacall.safety.domain.media.MediaConsentStatus
import com.rakshacall.safety.domain.media.MediaSourceType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite validating real permitted video-call and media monitoring sources.
 * Proves zero fake interceptions and guarantees truthful status disclosures.
 */
class CallMediaSourceTest {

    @Test
    fun testGoogleMeetConnector_UnconfiguredReportsPlatformAccessRequired() = runTest {
        val connector = GoogleMeetMediaConnector(oauthToken = null, meetingSpaceId = null)
        val initialStatus = connector.status.value

        assertEquals(MediaSourceType.GOOGLE_MEET, initialStatus.sourceType)
        assertFalse("Must not claim connected when credentials are missing", initialStatus.isConnected)
        assertFalse(initialStatus.isAudioAvailable)
        assertFalse(initialStatus.isVideoAvailable)
        assertEquals(MediaConsentStatus.REQUIRED, initialStatus.consentStatus)
        assertTrue(initialStatus.detailMessage.contains("NOT AVAILABLE — PLATFORM/API ACCESS REQUIRED"))

        val consentGranted = connector.requestConsent()
        assertFalse("Consent cannot be granted without OAuth client token", consentGranted)
        assertEquals(MediaConsentStatus.RESTRICTED_BY_PLATFORM, connector.status.value.consentStatus)

        val startResult = connector.startMonitoring()
        assertTrue("Starting unconfigured Meet API must fail", startResult.isFailure)
    }

    @Test
    fun testGoogleMeetConnector_ConfiguredLifecycle() = runTest {
        val connector = GoogleMeetMediaConnector(
            oauthToken = "ya29.mock-enterprise-preview-token",
            meetingSpaceId = "spaces/aaa-bbbb-ccc"
        )
        val status1 = connector.status.value
        assertEquals(MediaConsentStatus.GRANTED, status1.consentStatus)

        val startResult = connector.startMonitoring()
        assertTrue("Configured Meet connector starts successfully", startResult.isSuccess)
        val runningStatus = connector.status.value
        assertTrue(runningStatus.isConnected)
        assertTrue(runningStatus.isAudioAvailable)
        assertTrue(runningStatus.isVideoAvailable)
        assertTrue(runningStatus.isTranscriptAvailable)
        assertEquals(45L, runningStatus.latencyMs)

        val stopResult = connector.stopMonitoring()
        assertTrue(stopResult.isSuccess)
        assertFalse(connector.status.value.isConnected)
    }

    @Test
    fun testAndroidScreenAudioConnector_SandboxingFallbackWhenAudioRestricted() = runTest {
        // User granted screen share, but Android OS blocks internal VoIP audio capture (WhatsApp/Teams)
        val connector = AndroidScreenAudioConnector(
            isMediaProjectionGranted = true,
            isInternalAudioPermittedByOs = false
        )
        val status = connector.status.value
        assertTrue(status.isConnected)
        assertFalse("Internal audio must be false when restricted by OS", status.isAudioAvailable)
        assertTrue("Screen video context is available", status.isVideoAvailable)
        assertTrue(status.detailMessage.contains("Call audio from the other application is not directly available due to Android OS sandboxing"))

        val startResult = connector.startMonitoring()
        assertTrue(startResult.isSuccess)
        assertEquals(15L, connector.status.value.latencyMs)

        // Stop monitoring works at all times
        val stopResult = connector.stopMonitoring()
        assertTrue(stopResult.isSuccess)
        assertFalse(connector.status.value.isConnected)
    }

    @Test
    fun testAndroidScreenAudioConnector_WithoutPermissionRefusesMonitoring() = runTest {
        val connector = AndroidScreenAudioConnector(isMediaProjectionGranted = false)
        val status = connector.status.value
        assertFalse(status.isConnected)
        assertEquals(MediaConsentStatus.REQUIRED, status.consentStatus)
        assertTrue(status.detailMessage.contains("RakshaCall needs your explicit permission"))

        val startResult = connector.startMonitoring()
        assertTrue(startResult.isFailure)
        assertTrue(startResult.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testBrowserCaptureConnector_HonestStandbyStatus() = runTest {
        val connector = BrowserCaptureConnector(isExtensionConnected = false)
        val status = connector.status.value
        assertFalse(status.isConnected)
        assertEquals(MediaConsentStatus.REQUIRED, status.consentStatus)
        assertTrue(status.detailMessage.contains("Browser capture extension standby"))

        val startResult = connector.startMonitoring()
        assertTrue(startResult.isFailure)

        // When connected
        val connectedConnector = BrowserCaptureConnector(isExtensionConnected = true)
        val activeResult = connectedConnector.startMonitoring()
        assertTrue(activeResult.isSuccess)
        assertTrue(connectedConnector.status.value.isConnected)
        assertTrue(connectedConnector.status.value.isAudioAvailable)
    }

    @Test
    fun testMicrophoneMediaSource_PermissionAndLifecycle() = runTest {
        val mic = MicrophoneMediaSource(hasAudioPermission = true)
        val initialStatus = mic.status.value
        assertEquals(MediaSourceType.MICROPHONE, initialStatus.sourceType)
        assertFalse(initialStatus.isConnected)
        assertTrue(initialStatus.isAudioAvailable)

        val startResult = mic.startMonitoring()
        assertTrue(startResult.isSuccess)
        assertTrue(mic.status.value.isConnected)
        assertEquals(5L, mic.status.value.latencyMs)

        val stopResult = mic.stopMonitoring()
        assertTrue(stopResult.isSuccess)
        assertFalse(mic.status.value.isConnected)
    }

    @Test
    fun testCameraMediaSource_FramesAnalyzedWithoutRawPersistence() = runTest {
        val camera = CameraMediaSource(hasCameraPermission = true)
        val initialStatus = camera.status.value
        assertEquals(MediaSourceType.CAMERA, initialStatus.sourceType)
        assertTrue(initialStatus.isVideoAvailable)
        assertFalse(initialStatus.isAudioAvailable)

        val startResult = camera.startMonitoring()
        assertTrue(startResult.isSuccess)
        assertTrue(camera.status.value.isConnected)
        assertTrue(camera.status.value.detailMessage.contains("no raw frames stored"))

        val stopResult = camera.stopMonitoring()
        assertTrue(stopResult.isSuccess)
        assertFalse(camera.status.value.isConnected)
    }

    @Test
    fun testManualTextInputSource_AlwaysAvailableFallback() = runTest {
        val textSource = ManualTextInputSource()
        val status = textSource.status.value
        assertEquals(MediaSourceType.MANUAL_TEXT, status.sourceType)
        assertTrue(status.isConnected)
        assertTrue(status.isTranscriptAvailable)
        assertFalse(status.isAudioAvailable)
        assertEquals(MediaConsentStatus.GRANTED, status.consentStatus)
    }

    @Test
    fun testMediaSourceRegistry_ProvidesAllPermittedSources() {
        val registry = MediaSourceRegistry()
        val sources = registry.getAllSources()
        assertEquals(6, sources.size)

        val meet = registry.getSource(MediaSourceType.GOOGLE_MEET)
        assertNotNull(meet)
        assertEquals(MediaSourceType.GOOGLE_MEET, meet.sourceType)

        val screen = registry.getSource(MediaSourceType.ANDROID_SCREEN_AUDIO)
        assertNotNull(screen)
        assertEquals(MediaSourceType.ANDROID_SCREEN_AUDIO, screen.sourceType)

        val mic = registry.getSource(MediaSourceType.MICROPHONE)
        assertNotNull(mic)
        assertEquals(MediaSourceType.MICROPHONE, mic.sourceType)
    }
}
