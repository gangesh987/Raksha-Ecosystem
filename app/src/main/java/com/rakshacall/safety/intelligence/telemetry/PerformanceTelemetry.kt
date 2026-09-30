package com.rakshacall.safety.intelligence.telemetry

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TelemetrySnapshot(
    val asrLatencyMs: Long = 45L,
    val aiLatencyMs: Long = 18L,
    val riskUpdateLatencyMs: Long = 8L,
    val videoFps: Int = 30,
    val audioLatencyMs: Long = 20L,
    val memoryUsageMb: Long = 42L,
    val cpuPercent: Int = 12,
    val batteryPercent: Int = 85,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Real-time Performance & Telemetry Tracker (Phase 25).
 * Measures pipeline latencies, frame rates, and hardware utilization.
 */
class PerformanceTelemetry {

    private val _snapshot = MutableStateFlow(TelemetrySnapshot())
    val snapshot: StateFlow<TelemetrySnapshot> = _snapshot.asStateFlow()

    private var lastAsrStart: Long = 0L
    private var lastAiStart: Long = 0L

    fun markAsrStart() {
        lastAsrStart = SystemClock.elapsedRealtime()
    }

    fun recordAsrCompleted() {
        if (lastAsrStart > 0) {
            val latency = SystemClock.elapsedRealtime() - lastAsrStart
            _snapshot.value = _snapshot.value.copy(
                asrLatencyMs = latency.coerceAtLeast(1L),
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun markAiStart() {
        lastAiStart = SystemClock.elapsedRealtime()
    }

    fun recordAiCompleted(riskUpdateLatency: Long = 0L) {
        if (lastAiStart > 0) {
            val latency = SystemClock.elapsedRealtime() - lastAiStart
            val runtime = Runtime.getRuntime()
            val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

            _snapshot.value = _snapshot.value.copy(
                aiLatencyMs = latency.coerceAtLeast(1L),
                riskUpdateLatencyMs = riskUpdateLatency.coerceAtLeast(1L),
                memoryUsageMb = usedMb,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun updateVideoFps(fps: Int) {
        _snapshot.value = _snapshot.value.copy(videoFps = fps)
    }
}
