package com.rakshacall.safety.intelligence.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

enum class EventSource {
    AUDIO_CAPTURE,
    SPEECH_RECOGNITION,
    CAMERA_X,
    WEBRTC_MEDIA,
    TACTIC_ENGINE,
    STAGE_MACHINE,
    VELOCITY_ENGINE,
    RISK_FUSION,
    SAFETY_BRAKE,
    USER_ACTION,
    BACKEND_WEBSOCKET
}

enum class EventType {
    AUDIO_METRICS_UPDATE,
    TRANSCRIPT_CHUNK,
    VISUAL_CONTEXT_SIGNAL,
    TACTIC_DETECTED,
    STAGE_TRANSITION,
    VELOCITY_UPDATED,
    RISK_EVALUATION,
    SAFETY_BRAKE_TRIGGERED,
    DISAGREEMENT_DETECTED
}

/**
 * Canonical event model for all multimodal observations (Phase 3).
 */
data class MultimodalEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val sequenceNumber: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val source: EventSource,
    val eventType: EventType,
    val payload: Map<String, Any?>,
    val modelVersion: String = "raksha-v2.4-neural"
)

/**
 * Thread-safe ordered event bus providing continuous distribution
 * of real-time safety events to the UI, Risk Engine, and Evidence Ledger.
 */
class MultimodalEventBus(private val sessionId: String) {

    private val sequenceCounter = AtomicLong(0)
    private val _events = MutableSharedFlow<MultimodalEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<MultimodalEvent> = _events.asSharedFlow()

    private val history = mutableListOf<MultimodalEvent>()

    fun emitEvent(
        source: EventSource,
        eventType: EventType,
        payload: Map<String, Any?>
    ): MultimodalEvent {
        val event = MultimodalEvent(
            sessionId = sessionId,
            sequenceNumber = sequenceCounter.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            source = source,
            eventType = eventType,
            payload = payload
        )

        synchronized(history) {
            history.add(event)
            if (history.size > 200) {
                history.removeAt(0)
            }
        }

        _events.tryEmit(event)
        return event
    }

    fun getRecentEvents(limit: Int = 50): List<MultimodalEvent> {
        return synchronized(history) {
            history.takeLast(limit)
        }
    }

    fun clear() {
        synchronized(history) {
            history.clear()
        }
    }
}
