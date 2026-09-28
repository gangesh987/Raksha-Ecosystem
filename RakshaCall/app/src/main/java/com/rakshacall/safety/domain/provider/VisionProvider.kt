package com.rakshacall.safety.domain.provider

import kotlinx.coroutines.flow.Flow

data class VisionSignal(
    val timestamp: Long,
    val faceDetected: Boolean,
    val stabilityScore: Float,
    val lightingScore: Float,
    val isSupportingOnly: Boolean = true,
    val notes: String = "Supporting visual signal only"
)

interface VisionProvider {
    val isAvailable: Boolean
    val isPermitted: Boolean
    val signals: Flow<VisionSignal>
    fun startVisionAnalysis()
    fun stopVisionAnalysis()
}
