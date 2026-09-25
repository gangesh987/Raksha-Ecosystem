package com.rakshacall.safety

import com.rakshacall.safety.domain.media.WebRtcRoomManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebRtcRoomManagerTest {

    private lateinit var roomManager: WebRtcRoomManager

    @Before
    fun setUp() {
        roomManager = WebRtcRoomManager()
    }

    @Test
    fun testRoomCreationGeneratesValidRcCode() {
        val room = roomManager.createRoom(hostName = "TestUser")
        assertNotNull(room)
        assertTrue(room.roomId.startsWith("RC-"))
        assertEquals(9, room.roomId.length) // RC- + 6 digits
        assertEquals("CONNECTED", room.status)
        assertEquals("TestUser", room.hostName)
    }

    @Test
    fun testRoomInitialStates() {
        val room = roomManager.createRoom()
        assertFalse("Audio should not be muted initially", room.isAudioMuted)
        assertTrue("Video should be enabled initially", room.isVideoEnabled)
        assertTrue("Speaker should be on initially", room.isSpeakerOn)
        assertEquals("EXCELLENT", room.connectionQuality)
    }

    @Test
    fun testMuteToggle() {
        roomManager.createRoom()
        roomManager.toggleMute()
        assertTrue(roomManager.currentRoom.value!!.isAudioMuted)
        roomManager.toggleMute()
        assertFalse(roomManager.currentRoom.value!!.isAudioMuted)
    }

    @Test
    fun testVideoToggle() {
        roomManager.createRoom()
        roomManager.toggleVideo()
        assertFalse(roomManager.currentRoom.value!!.isVideoEnabled)
        roomManager.toggleVideo()
        assertTrue(roomManager.currentRoom.value!!.isVideoEnabled)
    }

    @Test
    fun testSpeakerToggle() {
        roomManager.createRoom()
        roomManager.toggleSpeaker()
        assertFalse(roomManager.currentRoom.value!!.isSpeakerOn)
        roomManager.toggleSpeaker()
        assertTrue(roomManager.currentRoom.value!!.isSpeakerOn)
    }

    @Test
    fun testConnectionQualityUpdate() {
        roomManager.createRoom()
        roomManager.updateConnectionQuality("FAIR")
        assertEquals("FAIR", roomManager.currentRoom.value!!.connectionQuality)
    }

    @Test
    fun testLeaveRoomResetsDurationAndState() {
        roomManager.createRoom()
        assertNotNull(roomManager.currentRoom.value)
        roomManager.leaveRoom()
        assertNull(roomManager.currentRoom.value)
        assertEquals(0L, roomManager.callDurationSeconds.value)
    }
}
