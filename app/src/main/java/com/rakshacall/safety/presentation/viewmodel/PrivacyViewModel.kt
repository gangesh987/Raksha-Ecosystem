package com.rakshacall.safety.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshacall.safety.core.permissions.DetailedPermissionSnapshot
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.ConsentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrivacyUiState(
    val permissions: DetailedPermissionSnapshot? = null,
    val isClearingData: Boolean = false,
    val dataClearedSuccessfully: Boolean = false
)

class PrivacyViewModel : ViewModel() {

    private val permissionManager = ServiceLocator.permissionStateManager
    private val consentManager = ServiceLocator.consentManager
    private val deleteUserDataUseCase = ServiceLocator.deleteUserDataUseCase

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            permissionManager.snapshot.collect { snapshot ->
                _uiState.update { it.copy(permissions = snapshot) }
            }
        }
    }

    fun refreshPermissions() {
        permissionManager.refresh()
    }

    fun toggleConsent(type: ConsentType, grant: Boolean) {
        viewModelScope.launch {
            if (grant) {
                consentManager.grantConsent(type)
            } else {
                consentManager.revokeConsent(type)
            }
        }
    }

    fun wipeAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingData = true) }
            deleteUserDataUseCase()
            consentManager.resetAllConsents()
            _uiState.update { it.copy(isClearingData = false, dataClearedSuccessfully = true) }
            onComplete()
        }
    }
}
