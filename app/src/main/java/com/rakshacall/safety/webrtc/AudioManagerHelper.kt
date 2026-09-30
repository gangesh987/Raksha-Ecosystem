package com.rakshacall.safety.webrtc

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

class AudioManagerHelper(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalMode = audioManager.mode
    private var originalSpeakerphoneOn = audioManager.isSpeakerphoneOn
    private var originalMicrophoneMute = audioManager.isMicrophoneMute

    fun startCallAudio(speakerphone: Boolean = true) {
        originalMode = audioManager.mode
        originalSpeakerphoneOn = audioManager.isSpeakerphoneOn
        originalMicrophoneMute = audioManager.isMicrophoneMute

        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        setSpeakerphone(speakerphone)
    }

    fun setSpeakerphone(on: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (on) {
                val speakerDevice = audioManager.availableCommunicationDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
                if (speakerDevice != null) {
                    audioManager.setCommunicationDevice(speakerDevice)
                }
            } else {
                audioManager.clearCommunicationDevice()
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = on
        }
    }

    fun isSpeakerphoneOn(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn
        }
    }

    fun setMicrophoneMute(muted: Boolean) {
        audioManager.isMicrophoneMute = muted
    }

    fun stopCallAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = originalSpeakerphoneOn
        }
        audioManager.isMicrophoneMute = originalMicrophoneMute
        audioManager.mode = originalMode
    }
}
