package com.rakshacall.safety.core.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.rakshacall.safety.domain.engine.FrameMetrics
import com.rakshacall.safety.domain.engine.VisualAnalysisEngine
import com.rakshacall.safety.domain.model.VisualSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * CameraX frame analyzer that processes real camera frames for lighting and stability.
 */
class CameraFrameAnalyzer(
    private val visualEngine: VisualAnalysisEngine,
    private val onSignalComputed: (VisualSignal) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedTimestamp = 0L

    override fun analyze(image: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        // Rate-limit analysis to ~2 frames per second to preserve battery and CPU
        if (currentTimestamp - lastAnalyzedTimestamp >= 500L) {
            val buffer: ByteBuffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            // Calculate luminance average (Y plane in YUV_420_888)
            var sum = 0L
            val step = 16 // Sub-sample for performance
            var count = 0
            for (i in data.indices step step) {
                sum += (data[i].toInt() and 0xFF)
                count++
            }
            val avgLuminance = if (count > 0) (sum.toFloat() / count) / 255.0f else 0.5f

            // Frame variance / contrast
            var varianceSum = 0.0
            val mean = avgLuminance * 255.0
            for (i in data.indices step step) {
                val diff = (data[i].toInt() and 0xFF) - mean
                varianceSum += diff * diff
            }
            val variance = if (count > 0) (varianceSum / count).toFloat() else 0f

            // Face presence proxy: adequate variance and lighting within portrait range
            val plausibleFace = avgLuminance in 0.20f..0.80f && variance > 400f

            val metrics = FrameMetrics(
                luminance = avgLuminance,
                variance = variance,
                faceDetected = plausibleFace,
                faceWidthRatio = if (plausibleFace) 0.35f else 0.0f,
                faceHeightRatio = if (plausibleFace) 0.45f else 0.0f,
                timestamp = currentTimestamp
            )

            val signal = visualEngine.analyzeFrameMetrics(metrics)
            onSignalComputed(signal)
            lastAnalyzedTimestamp = currentTimestamp
        }

        image.close()
    }
}

/**
 * Manages CameraX lifecycle and starts real camera frame streaming only upon explicit user consent.
 */
class CameraManager(
    private val context: Context,
    private val visualEngine: VisualAnalysisEngine
) {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private val _latestVisualSignal = MutableStateFlow<VisualSignal?>(null)
    val latestVisualSignal: StateFlow<VisualSignal?> = _latestVisualSignal.asStateFlow()

    fun startCamera(lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(
                    cameraExecutor,
                    CameraFrameAnalyzer(visualEngine) { signal ->
                        _latestVisualSignal.value = signal
                    }
                )

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
            } catch (e: Exception) {
                _latestVisualSignal.value = null
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            _latestVisualSignal.value = null
        } catch (_: Exception) {}
    }

    fun release() {
        stopCamera()
        cameraExecutor.shutdown()
    }
}
