package com.rakshacall.safety.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.AlertDeliveryStatus
import com.rakshacall.safety.domain.model.TrustedContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class TrustedContactsUiState(
    val contacts: List<TrustedContact> = emptyList(),
    val lastDeliveryStatus: AlertDeliveryStatus? = null
)

class TrustedContactsViewModel : ViewModel() {

    private val contactRepository = ServiceLocator.trustedContactRepository
    private val alertUseCase = ServiceLocator.alertTrustedContactUseCase

    private val _uiState = MutableStateFlow(TrustedContactsUiState())
    val uiState: StateFlow<TrustedContactsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            contactRepository.observeContacts().collect { list ->
                _uiState.update { it.copy(contacts = list) }
            }
        }
    }

    fun addContact(name: String, phoneNumber: String, relationship: String) {
        if (name.isBlank() || phoneNumber.isBlank()) return
        viewModelScope.launch {
            val contact = TrustedContact(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                phoneNumber = phoneNumber.trim(),
                relationship = relationship.trim()
            )
            contactRepository.addContact(contact)
        }
    }

    fun deleteContact(id: String) {
        viewModelScope.launch {
            contactRepository.deleteContact(id)
        }
    }

    fun sendTestAlert(contact: TrustedContact, sessionId: String = "TEST") {
        viewModelScope.launch {
            val status = alertUseCase(
                contact = contact,
                sessionId = sessionId,
                riskScore = 85,
                detectedTactics = listOf("Authority Impersonation", "Payment Demand")
            )
            _uiState.update { it.copy(lastDeliveryStatus = status) }
        }
    }
}
