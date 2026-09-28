package com.raksha.video.webrtc

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.AudioSource
import org.webrtc.Camera2Enumerator
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack

class WebRtcEngine(
    private val context: Context,
    private val config: WebRtcConfig,
    private val onLocalIceCandidate: (IceCandidate) -> Unit,
    private val onRemoteVideo: (VideoTrack) -> Unit,
    private val onConnectionState: (WebRtcState) -> Unit
) : WebRtcManager {
    private val _state = MutableStateFlow<WebRtcState>(WebRtcState.New)
    override val state: StateFlow<WebRtcState> = _state
    private var factory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localMedia: LocalMediaManager? = null
    private val eglBase = org.webrtc.EglBase.create()

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(org.webrtc.DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(org.webrtc.DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    override fun initialize() {
        localMedia = LocalMediaManager(context, factory, config).also { it.start() }
        val iceServers = config.iceServers.map {
            PeerConnection.IceServer.builder(it.urls)
                .apply { if (it.username != null) setUsername(it.username); if (it.credential != null) setPassword(it.credential) }
                .createIceServer()
        }
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                val mapped = when (state) {
                    PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> WebRtcState.Connected
                    PeerConnection.IceConnectionState.CHECKING -> WebRtcState.Connecting
                    PeerConnection.IceConnectionState.DISCONNECTED -> WebRtcState.Disconnected
                    PeerConnection.IceConnectionState.FAILED -> WebRtcState.Failed
                    PeerConnection.IceConnectionState.CLOSED -> WebRtcState.Closed
                    else -> WebRtcState.New
                }
                _state.value = mapped
                onConnectionState(mapped)
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate) { onLocalIceCandidate(candidate) }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream) { stream.videoTracks.firstOrNull()?.let(onRemoteVideo) }
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
                (receiver.track() as? VideoTrack)?.let(onRemoteVideo)
            }
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onTrack(transceiver: RtpTransceiver?) {
                (transceiver?.receiver?.track() as? VideoTrack)?.let(onRemoteVideo)
            }
        })
        requireNotNull(peerConnection) { "Unable to create PeerConnection" }
        localMedia?.videoTrack?.let { peerConnection!!.addTrack(it) }
        localMedia?.audioTrack?.let { peerConnection!!.addTrack(it) }
    }

    override fun createOffer(onCreated: (SessionDescription) -> Unit) {
        val pc = requireNotNull(peerConnection)
        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(description: SessionDescription?) {
                if (description != null) {
                    pc.setLocalDescription(SimpleSdpObserver(), description)
                    onCreated(description)
                }
            }
        }, MediaConstraints())
    }

    override fun createAnswer(onCreated: (SessionDescription) -> Unit) {
        val pc = requireNotNull(peerConnection)
        pc.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(description: SessionDescription?) {
                if (description != null) {
                    pc.setLocalDescription(SimpleSdpObserver(), description)
                    onCreated(description)
                }
            }
        }, MediaConstraints())
    }

    override fun setRemoteDescription(description: SessionDescription, onDone: () -> Unit) {
        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() { override fun onSetSuccess() { onDone() } }, description)
    }

    override fun addIceCandidate(candidate: IceCandidate) { peerConnection?.addIceCandidate(candidate) }
    override fun enableCamera() { localMedia?.setCameraEnabled(true) }
    override fun disableCamera() { localMedia?.setCameraEnabled(false) }
    override fun muteMicrophone() { localMedia?.setMicrophoneEnabled(false) }
    override fun unmuteMicrophone() { localMedia?.setMicrophoneEnabled(true) }
    override fun switchCamera() { localMedia?.switchCamera() }
    override fun closePeerConnection() { peerConnection?.close(); peerConnection = null }

    override fun release() {
        closePeerConnection()
        localMedia?.stop()
        localMedia = null
        factory.dispose()
        eglBase.release()
    }

    fun localVideoTrack(): VideoTrack? = localMedia?.videoTrack
    fun localVideoEnabled(): Boolean = localMedia?.videoTrack?.enabled() ?: false
    fun localAudioEnabled(): Boolean = localMedia?.audioTrack?.enabled() ?: false

    open class SimpleSdpObserver : org.webrtc.SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}
