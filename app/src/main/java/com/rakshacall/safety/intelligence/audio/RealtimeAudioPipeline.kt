package com.rakshacall.safety.intelligence.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Real-time acoustic metrics extracted from live PCM audio frames (Phase 1).
 */
data class AcousticMetrics(
    val rmsEnergy: Float = 0.0f,
    val zeroCrossingRate: Float = 0.0f,
    val spectralCentroid: Float = 0.0f,
    val spectralFlux: Float = 0.0f,
    val isSpeech: Boolean = false,
    val speechDurationMs: Long = 0L,
    val silenceDurationMs: Long = 0L,
    val speechActivityRate: Float = 0.0f,
    val highFrequencyRatio: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Standardized audio chunk emitted for WebSocket transport and downstream NLP.
 */
data class AudioChunk(
    val sessionId: String,
    val sequenceNumber: Long,
    val timestampStart: Long,
    val timestampEnd: Long,
    val pcmData: ByteArray,
    val metrics: AcousticMetrics,
    val sampleRate: Int = 16000
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioChunk
        return sequenceNumber == other.sequenceNumber && sessionId == other.sessionId
    }

    override fun hashCode(): Int {
        var result = sessionId.hashCode()
        result = 31 * result + sequenceNumber.hashCode()
        return result
    }
}

/**
 * Continuous 16kHz PCM audio capture and acoustic feature extraction pipeline.
 * Extracts VAD, RMS, ZCR, and spectral features on 20ms frames and aggregates
 * them into short inference windows (Phase 1).
 */
class RealtimeAudioPipeline(
    private val sessionId: String,
    private val sampleRate: Int = 16000,
    private val frameSizeMs: Int = 20 // 20ms frame = 320 samples @ 16kHz
) {
    private val samplesPerFrame = (sampleRate * frameSizeMs) / 1000 // 320 samples
    private val bytesPerFrame = samplesPerFrame * 2 // 16-bit PCM = 2 bytes/sample

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _acousticFlow = MutableStateFlow(AcousticMetrics())
    val acousticFlow: StateFlow<AcousticMetrics> = _acousticFlow.asStateFlow()
    val acousticMetrics: StateFlow<AcousticMetrics> get() = acousticFlow

    private val _chunkFlow = MutableSharedFlow<AudioChunk>(extraBufferCapacity = 32)
    val chunkFlow: SharedFlow<AudioChunk> = _chunkFlow.asSharedFlow()
    val audioChunks: SharedFlow<AudioChunk> get() = chunkFlow

    @Volatile
    private var isRunning = false
    private var sequenceNumber = 0L

    // Adaptive noise floor tracking
    private var backgroundEnergyFloor = 0.005f
    private var speechFramesCount = 0L
    private var totalFramesCount = 0L
    private var consecutiveSpeechMs = 0L
    private var consecutiveSilenceMs = 0L
    private var prevFftMags: FloatArray? = null

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRunning) return true

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = maxOf(minBufferSize, bytesPerFrame * 10)

        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return false
            }

            record.startRecording()
            audioRecord = record
            isRunning = true

            startProcessingLoop()
            true
        } catch (e: Exception) {
            isRunning = false
            false
        }
    }

    private fun startProcessingLoop() {
        recordingJob = scope.launch {
            val shortBuffer = ShortArray(samplesPerFrame)
            val byteBuffer = ByteArray(bytesPerFrame)
            var frameStartTime = System.currentTimeMillis()

            // Aggregation buffer for 500ms chunk emission
            val chunkSamples = sampleRate / 2 // 8000 samples = 500ms
            val chunkBuffer = ByteArray(chunkSamples * 2)
            var chunkBytesFilled = 0
            var chunkStartTs = System.currentTimeMillis()

            while (isActive && isRunning) {
                val record = audioRecord ?: break
                val readCount = record.read(shortBuffer, 0, samplesPerFrame)

                if (readCount > 0) {
                    val now = System.currentTimeMillis()

                    // Convert shorts to bytes
                    for (i in 0 until readCount) {
                        val s = shortBuffer[i]
                        byteBuffer[i * 2] = (s.toInt() and 0xFF).toByte()
                        byteBuffer[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
                    }

                    // Compute acoustic features for this 20ms frame
                    val metrics = computeFrameMetrics(shortBuffer, readCount, now)
                    _acousticFlow.value = metrics

                    // Accumulate into bounded chunk window
                    val toCopy = minOf(readCount * 2, chunkBuffer.size - chunkBytesFilled)
                    System.arraycopy(byteBuffer, 0, chunkBuffer, chunkBytesFilled, toCopy)
                    chunkBytesFilled += toCopy

                    if (chunkBytesFilled >= chunkBuffer.size) {
                        sequenceNumber++
                        val chunkCopy = chunkBuffer.copyOf()
                        _chunkFlow.tryEmit(
                            AudioChunk(
                                sessionId = sessionId,
                                sequenceNumber = sequenceNumber,
                                timestampStart = chunkStartTs,
                                timestampEnd = now,
                                pcmData = chunkCopy,
                                metrics = metrics,
                                sampleRate = sampleRate
                            )
                        )
                        chunkBytesFilled = 0
                        chunkStartTs = now
                    }

                    frameStartTime = now
                } else {
                    delay(10)
                }
            }
        }
    }

    private fun computeFrameMetrics(
        samples: ShortArray,
        count: Int,
        timestamp: Long
    ): AcousticMetrics {
        if (count == 0) return AcousticMetrics(timestamp = timestamp)

        totalFramesCount++

        // 1. RMS Energy
        var sumSquares = 0.0
        var zeroCrossings = 0
        var prevSign = samples[0] >= 0

        for (i in 0 until count) {
            val s = samples[i] / 32768.0f
            sumSquares += s * s

            val currentSign = samples[i] >= 0
            if (currentSign != prevSign) {
                zeroCrossings++
                prevSign = currentSign
            }
        }

        val rms = sqrt(sumSquares / count).toFloat()
        val zcr = zeroCrossings.toFloat() / count

        // 2. Adaptive background noise floor tracking
        if (rms < backgroundEnergyFloor * 1.5f) {
            backgroundEnergyFloor = 0.95f * backgroundEnergyFloor + 0.05f * rms
        }

        // 3. Voice Activity Detection (VAD)
        val isSpeech = rms > (backgroundEnergyFloor * 2.2f).coerceAtLeast(0.015f) && zcr in 0.02f..0.65f

        if (isSpeech) {
            speechFramesCount++
            consecutiveSpeechMs += frameSizeMs
            consecutiveSilenceMs = 0L
        } else {
            consecutiveSilenceMs += frameSizeMs
            consecutiveSpeechMs = 0L
        }

        val speechActivityRate = if (totalFramesCount > 0) {
            speechFramesCount.toFloat() / totalFramesCount
        } else 0.0f

        // 4. Approximate Spectral Centroid & Flux
        val (spectralCentroid, spectralFlux, highFreqRatio) = computeSpectralFeatures(samples, count)

        return AcousticMetrics(
            rmsEnergy = rms,
            zeroCrossingRate = zcr,
            spectralCentroid = spectralCentroid,
            spectralFlux = spectralFlux,
            isSpeech = isSpeech,
            speechDurationMs = consecutiveSpeechMs,
            silenceDurationMs = consecutiveSilenceMs,
            speechActivityRate = speechActivityRate,
            highFrequencyRatio = highFreqRatio,
            timestamp = timestamp
        )
    }

    private fun computeSpectralFeatures(
        samples: ShortArray,
        count: Int
    ): Triple<Float, Float, Float> {
        val n = minOf(count, 256)
        var weightedSum = 0.0f
        var totalMag = 0.0f
        var highFreqMag = 0.0f
        var flux = 0.0f

        val currentMags = FloatArray(n / 2)
        val prev = prevFftMags

        for (k in 0 until n / 2) {
            // Simple Discrete Fourier Transform bin approximation
            var real = 0.0f
            var imag = 0.0f
            for (t in 0 until n) {
                val angle = (2.0 * Math.PI * k * t / n).toFloat()
                val sampleVal = samples[t] / 32768.0f
                real += sampleVal * kotlin.math.cos(angle)
                imag -= sampleVal * kotlin.math.sin(angle)
            }
            val mag = sqrt(real * real + imag * imag)
            currentMags[k] = mag

            val freq = (k * sampleRate) / n.toFloat()
            weightedSum += freq * mag
            totalMag += mag

            if (freq > 2500.0f) {
                highFreqMag += mag
            }

            if (prev != null && k < prev.size) {
                val diff = mag - prev[k]
                if (diff > 0) flux += diff
            }
        }

        prevFftMags = currentMags

        val centroid = if (totalMag > 1e-6f) weightedSum / totalMag else 0.0f
        val highRatio = if (totalMag > 1e-6f) highFreqMag / totalMag else 0.0f

        return Triple(centroid, flux, highRatio)
    }

    fun stop() {
        isRunning = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun startCapture(): Boolean = start()
    fun stopCapture() = stop()
}
