package com.rakshacall.safety.webrtc

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*

class WebRtcEngine(
    private val context: Context,
    private val config: WebRtcConfig,
    private val onLocalIceCandidate: (IceCandidate) -> Unit,
    private val onRemoteVideo: (VideoTrack) -> Unit,
    private val onConnectionStateChange: (WebRtcState) -> Unit
) : WebRtcManager {

    private val _state = MutableStateFlow<WebRtcState>(WebRtcState.New)
    override val state: StateFlow<WebRtcState> = _state.asStateFlow()

    private val eglBase: EglBase = EglBase.create()
    private var factory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localMedia: LocalMediaManager? = null

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    override fun initialize() {
        localMedia = LocalMediaManager(context, factory, config, eglBase).also { it.start() }

        val iceServers = config.iceServers.map { server ->
            PeerConnection.IceServer.builder(server.urls).apply {
                if (server.username != null) setUsername(server.username)
                if (server.credential != null) setPassword(server.credential)
            }.createIceServer()
        }

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                val mapped = when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> WebRtcState.Connected
                    PeerConnection.IceConnectionState.CHECKING -> WebRtcState.Connecting
                    PeerConnection.IceConnectionState.DISCONNECTED -> WebRtcState.Disconnected
                    PeerConnection.IceConnectionState.FAILED -> WebRtcState.Failed
                    PeerConnection.IceConnectionState.CLOSED -> WebRtcState.Closed
                    else -> WebRtcState.New
                }
                _state.value = mapped
                onConnectionStateChange(mapped)
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}

            override fun onIceCandidate(candidate: IceCandidate) {
                onLocalIceCandidate(candidate)
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onAddStream(stream: MediaStream) {
                stream.videoTracks.firstOrNull()?.let(onRemoteVideo)
            }

            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}

            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
                (receiver.track() as? VideoTrack)?.let(onRemoteVideo)
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                (transceiver?.receiver?.track() as? VideoTrack)?.let(onRemoteVideo)
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {}
            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
        })

        requireNotNull(peerConnection) { "Unable to instantiate PeerConnection" }

        // Attach local tracks
        localMedia?.videoTrack?.let { peerConnection?.addTrack(it) }
        localMedia?.audioTrack?.let { peerConnection?.addTrack(it) }
    }

    override fun createOffer(onCreated: (SessionDescription) -> Unit) {
        val pc = requireNotNull(peerConnection) { "PeerConnection not initialized" }
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
        val pc = requireNotNull(peerConnection) { "PeerConnection not initialized" }
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
        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                onDone()
            }
        }, description)
    }

    override fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    override fun enableCamera() {
        localMedia?.setCameraEnabled(true)
    }

    override fun disableCamera() {
        localMedia?.setCameraEnabled(false)
    }

    override fun muteMicrophone() {
        localMedia?.setMicrophoneEnabled(false)
    }

    override fun unmuteMicrophone() {
        localMedia?.setMicrophoneEnabled(true)
    }

    override fun switchCamera() {
        localMedia?.switchCamera()
    }

    override fun closePeerConnection() {
        runCatching { peerConnection?.close() }
        peerConnection = null
    }

    override fun release() {
        closePeerConnection()
        localMedia?.stop()
        localMedia = null
        factory.dispose()
        eglBase.release()
    }

    override fun localVideoTrack(): VideoTrack? = localMedia?.videoTrack
    override fun isCameraEnabled(): Boolean = localMedia?.videoTrack?.enabled() ?: false
    override fun isMicEnabled(): Boolean = localMedia?.audioTrack?.enabled() ?: false

    fun getEglBaseContext(): EglBase.Context = eglBase.eglBaseContext

    open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }
}
