package com.raksha.video.intelligence.conversation

import java.util.Locale
import java.util.UUID

class ConversationAnalyzer {
    private val patterns = linkedMapOf(
        "AUTHORITY_CLAIM" to listOf("police", "bank manager", "income tax", "government", "cyber crime", "officer", "official"),
        "URGENCY" to listOf("immediately", "right now", "urgent", "don't wait", "act now"),
        "THREAT" to listOf("arrest", "jail", "legal action", "account will be blocked", "case will be filed"),
        "ISOLATION" to listOf("don't tell", "keep it secret", "don't call anyone", "stay on the line"),
        "FINANCIAL_REQUEST" to listOf("transfer", "send money", "pay fee", "refund", "upi", "bank account", "card details"),
        "OTP_REQUEST" to listOf("otp", "one-time password", "verification code"),
        "PASSWORD_REQUEST" to listOf("password", "passcode", "pin"),
        "CREDENTIAL_REQUEST" to listOf("login", "username", "credentials", "cvv"),
        "REMOTE_ACCESS_REQUEST" to listOf("anydesk", "teamviewer", "remote access", "screen share", "install app"),
        "SECRECY_REQUEST" to listOf("secret", "confidential", "don't tell anyone"),
        "FEAR_LANGUAGE" to listOf("danger", "risk", "you are in trouble", "problem"),
        "IMPERSONATION_SIGNAL" to listOf("calling from", "this is the", "support team", "customer care")
    )

    fun analyze(event: TranscriptEvent): List<ConversationRiskSignal> {
        val text = event.text.lowercase(Locale.US)
        return patterns.mapNotNull { (type, words) ->
            val hits = words.count { text.contains(it) }
            if (hits == 0) null else ConversationRiskSignal(type, (0.55f + 0.1f * hits).coerceAtMost(0.95f), event.timestamp, "on_device_rules", event.eventId)
        }
    }
}
