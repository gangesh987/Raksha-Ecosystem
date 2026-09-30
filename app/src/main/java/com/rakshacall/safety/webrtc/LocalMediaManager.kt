package com.rakshacall.safety.webrtc

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnectionFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class LocalMediaManager(
    private val context: Context,
    private val factory: PeerConnectionFactory,
    private val config: WebRtcConfig,
    private val rootEglBase: org.webrtc.EglBase
) {
    private var cameraCapturer: CameraVideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    var videoTrack: VideoTrack? = null
        private set
    var audioTrack: AudioTrack? = null
        private set

    fun start() {
        if (videoTrack != null && audioTrack != null) return

        // 1. Setup Camera Video Capturer if video is enabled
        if (config.isVideoEnabled) {
            runCatching {
                val enumerator = Camera2Enumerator(context)
                val deviceNames = enumerator.deviceNames
                val cameraName = deviceNames.firstOrNull { enumerator.isFrontFacing(it) } ?: deviceNames.firstOrNull()

                if (cameraName != null) {
                    cameraCapturer = enumerator.createCapturer(cameraName, object : CameraVideoCapturer.CameraEventsHandler {
                        override fun onCameraError(errorDescription: String?) {}
                        override fun onCameraDisconnected() {}
                        override fun onCameraFreezed(errorDescription: String?) {}
                        override fun onCameraOpening(cameraName: String?) {}
                        override fun onFirstFrameAvailable() {}
                        override fun onCameraClosed() {}
                    })
                    if (cameraCapturer != null) {
                        videoSource = factory.createVideoSource(cameraCapturer!!.isScreencast)
                        surfaceTextureHelper = SurfaceTextureHelper.create("RakshaCallCamera", rootEglBase.eglBaseContext)
                        cameraCapturer!!.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)
                        cameraCapturer!!.startCapture(config.videoWidth, config.videoHeight, config.videoFps)
                        videoTrack = factory.createVideoTrack("raksha-call-video", videoSource)
                    }
                }
            }
        }

        // 2. Setup Audio Track if audio is enabled
        if (config.isAudioEnabled) {
            runCatching {
                val audioConstraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                }
                audioSource = factory.createAudioSource(audioConstraints)
                audioTrack = factory.createAudioTrack("raksha-call-audio", audioSource)
            }
        }
    }

    fun switchCamera() {
        cameraCapturer?.switchCamera(null)
    }

    fun setCameraEnabled(enabled: Boolean) {
        videoTrack?.setEnabled(enabled)
    }

    fun setMicrophoneEnabled(enabled: Boolean) {
        audioTrack?.setEnabled(enabled)
    }

    fun stop() {
        runCatching { cameraCapturer?.stopCapture() }
        runCatching { cameraCapturer?.dispose() }
        runCatching { videoTrack?.dispose() }
        runCatching { audioTrack?.dispose() }
        runCatching { videoSource?.dispose() }
        runCatching { audioSource?.dispose() }
        runCatching { surfaceTextureHelper?.dispose() }

        cameraCapturer = null
        videoTrack = null
        audioTrack = null
        videoSource = null
        audioSource = null
        surfaceTextureHelper = null
    }
}
