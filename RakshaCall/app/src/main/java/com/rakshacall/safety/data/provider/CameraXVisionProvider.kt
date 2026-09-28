package com.rakshacall.safety.data.provider

import com.rakshacall.safety.domain.provider.VisionProvider
import com.rakshacall.safety.domain.provider.VisionSignal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CameraXVisionProvider : VisionProvider {

    override val isAvailable: Boolean = true
    override var isPermitted: Boolean = false
        private set

    private val _signals = MutableSharedFlow<VisionSignal>(extraBufferCapacity = 32)
    override val signals: SharedFlow<VisionSignal> = _signals.asSharedFlow()

    private var isActive = false

    fun onPermissionGranted(granted: Boolean) {
        isPermitted = granted
    }

    override fun startVisionAnalysis() {
        if (!isPermitted) return
        isActive = true
    }

    override fun stopVisionAnalysis() {
        isActive = false
    }

    fun emitSignal(faceDetected: Boolean, stability: Float, lighting: Float) {
        if (!isActive) return
        _signals.tryEmit(
            VisionSignal(
                timestamp = System.currentTimeMillis(),
                faceDetected = faceDetected,
                stabilityScore = stability,
                lightingScore = lighting,
                isSupportingOnly = true,
                notes = "Supporting signal only. Coercion in conversation remains primary."
            )
        )
    }
}
