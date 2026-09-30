package com.rakshacall.safety.intelligence.visual

class VisualThreatDetector {

    /**
     * Inspect incoming video frame sample and OCR/metadata to detect visual threat patterns.
     * Invariant: Visual signals are strictly supporting evidence (w_visual <= 0.15)
     * and never independently declare a scam.
     */
    fun analyzeFrame(
        sample: VideoFrameSample,
        detectedTextInFrame: String? = null
    ): VisualThreatSignal? {
        if (!detectedTextInFrame.isNullOrBlank()) {
            val text = detectedTextInFrame.lowercase()

            when {
                text.contains("anydesk") || text.contains("teamviewer") || text.contains("rustdesk") || text.contains("quicksupport") -> {
                    return VisualThreatSignal(
                        type = VisualThreatType.REMOTE_ACCESS_INTERFACE,
                        confidence = 0.95f,
                        timestamp = sample.timestamp,
                        description = "Remote access or screen control software interface detected on display.",
                        riskScoreBonus = 20,
                        source = sample.source
                    )
                }
                text.contains("cbi") || text.contains("police notice") || text.contains("arrest warrant") ||
                        text.contains("digital arrest") || text.contains("supreme court") || text.contains("high court") ||
                        text.contains("cyber crime cell") || text.contains("rbi notice") || text.contains("fir copy") -> {
                    return VisualThreatSignal(
                        type = VisualThreatType.OFFICIAL_SEAL_OR_BADGE,
                        confidence = 0.92f,
                        timestamp = sample.timestamp,
                        description = "Simulated police badge, judicial seal, or official notice displayed.",
                        riskScoreBonus = 18,
                        source = sample.source
                    )
                }
                text.contains("upi pin") || text.contains("enter upi pin") || text.contains("gpay") || text.contains("phonepe") || text.contains("paytm") -> {
                    return VisualThreatSignal(
                        type = VisualThreatType.PAYMENT_APP_DETECTED,
                        confidence = 0.92f,
                        timestamp = sample.timestamp,
                        description = "Payment or UPI PIN entry interface displayed during coercive call.",
                        riskScoreBonus = 18,
                        source = sample.source
                    )
                }
                text.contains("scan qr") || text.contains("qr code") -> {
                    return VisualThreatSignal(
                        type = VisualThreatType.QR_CODE_PROMPT,
                        confidence = 0.88f,
                        timestamp = sample.timestamp,
                        description = "QR Code payment prompt visible on screen.",
                        riskScoreBonus = 15,
                        source = sample.source
                    )
                }
                text.contains("enter otp") || text.contains("one time password") || text.contains("cvv") || text.contains("secret pin") -> {
                    return VisualThreatSignal(
                        type = VisualThreatType.CREDENTIAL_ENTRY_SCREEN,
                        confidence = 0.90f,
                        timestamp = sample.timestamp,
                        description = "Sensitive credential authentication dialog active.",
                        riskScoreBonus = 20,
                        source = sample.source
                    )
                }
                text.contains("document") || text.contains("verification letter") || text.contains("chargesheet") || text.contains("undertaking") -> {
                    return VisualThreatSignal(
                        type = VisualThreatType.DOCUMENT_INSPECTION_SCREEN,
                        confidence = 0.85f,
                        timestamp = sample.timestamp,
                        description = "Official document or legal letterhead presented for intimidation.",
                        riskScoreBonus = 12,
                        source = sample.source
                    )
                }
            }
        }

        // Frame-level geometric and context heuristics when OCR text is not present
        if (sample.source == "screen_share") {
            return VisualThreatSignal(
                type = VisualThreatType.SCREEN_SHARE_DETECTED,
                confidence = 0.85f,
                timestamp = sample.timestamp,
                description = "Active screen sharing stream detected.",
                riskScoreBonus = 15,
                source = sample.source
            )
        }

        return null
    }

    fun createSignal(
        type: VisualThreatType,
        confidence: Float,
        description: String,
        riskScoreBonus: Int = 15,
        source: String = "vision_engine"
    ): VisualThreatSignal {
        return VisualThreatSignal(
            type = type,
            confidence = confidence.coerceIn(0f, 1f),
            description = description,
            riskScoreBonus = riskScoreBonus.coerceIn(0, 25),
            source = source
        )
    }
}
