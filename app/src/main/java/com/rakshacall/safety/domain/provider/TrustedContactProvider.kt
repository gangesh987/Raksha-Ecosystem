package com.rakshacall.safety.domain.provider

import com.rakshacall.safety.domain.model.AlertDeliveryStatus

data class ContactAlertRequest(
    val contactName: String,
    val phoneNumber: String,
    val rakshaCallId: String,
    val sessionId: String,
    val riskScore: Int,
    val alertMessage: String
)

data class ContactAlertResult(
    val status: AlertDeliveryStatus,
    val providerName: String,
    val messageSid: String? = null,
    val note: String
)

interface TrustedContactProvider {
    val providerName: String
    val isDirectSmsConfigured: Boolean
    suspend fun dispatchEmergencyAlert(request: ContactAlertRequest): ContactAlertResult
}
