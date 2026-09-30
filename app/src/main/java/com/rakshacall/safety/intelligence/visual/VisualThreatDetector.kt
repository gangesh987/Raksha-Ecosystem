package com.rakshacall.safety.intelligence.visual

class VisualThreatDetector {

    /**
     * Inspect incoming video frame sample or OCR/metadata to detect visual threat patterns.
     */
    fun analyzeFrame(
        sample: VideoFrameSample,
        detectedTextInFrame: String? = null
    ): VisualThreatSignal? {
        if (detectedTextInFrame.isNullOrBlank()) {
            return null
        }

        val text = detectedTextInFrame.lowercase()

        return when {
            text.contains("anydesk") || text.contains("teamviewer") || text.contains("rustdesk") || text.contains("quicksupport") -> {
                VisualThreatSignal(
                    type = VisualThreatType.REMOTE_ACCESS_INTERFACE,
                    confidence = 0.95f,
                    description = "Remote access or screen control software interface detected on display.",
                    riskScoreBonus = 25
                )
            }
            text.contains("upi pin") || text.contains("enter upi pin") || text.contains("gpay") || text.contains("phonepe") || text.contains("paytm") -> {
                VisualThreatSignal(
                    type = VisualThreatType.PAYMENT_APP_DETECTED,
                    confidence = 0.92f,
                    description = "Payment or UPI PIN entry interface displayed during coercive call.",
                    riskScoreBonus = 20
                )
            }
            text.contains("scan qr") || text.contains("qr code") -> {
                VisualThreatSignal(
                    type = VisualThreatType.QR_CODE_PROMPT,
                    confidence = 0.88f,
                    description = "QR Code payment prompt visible on screen.",
                    riskScoreBonus = 15
                )
            }
            text.contains("enter otp") || text.contains("one time password") || text.contains("cvv") -> {
                VisualThreatSignal(
                    type = VisualThreatType.CREDENTIAL_ENTRY_SCREEN,
                    confidence = 0.90f,
                    description = "Sensitive credential authentication dialog active.",
                    riskScoreBonus = 25
                )
            }
            else -> null
        }
    }
}
