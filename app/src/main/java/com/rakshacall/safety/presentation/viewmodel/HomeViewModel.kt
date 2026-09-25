package com.rakshacall.safety.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakshacall.safety.data.sync.SyncQueueStatus
import com.rakshacall.safety.di.ServiceLocator
import com.rakshacall.safety.domain.model.ProtectionSession
import com.rakshacall.safety.domain.model.TrustedContact
import com.rakshacall.safety.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val activeUser: User? = null,
    val activeSession: ProtectionSession? = null,
    val recentSessions: List<ProtectionSession> = emptyList(),
    val contacts: List<TrustedContact> = emptyList(),
    val syncStatus: SyncQueueStatus = SyncQueueStatus.OFFLINE_ONLY,
    val isSessionActive: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val userRepository = ServiceLocator.userRepository
    private val sessionRepository = ServiceLocator.sessionRepository
    private val contactRepository = ServiceLocator.trustedContactRepository
    private val syncManager = ServiceLocator.syncManager
    private val startProtectionUseCase = ServiceLocator.startProtectionSessionUseCase

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.observeActiveUser(),
        sessionRepository.observeActiveSession(),
        sessionRepository.observeAllSessions(),
        contactRepository.observeContacts(),
        syncManager.syncStatus
    ) { user, activeSession, allSessions, contacts, syncStatus ->
        // Filter out demo sessions from personal activity cards
        val userSessions = allSessions.filter { !it.isDemoSession }
        HomeUiState(
            activeUser = user,
            activeSession = activeSession,
            recentSessions = userSessions.take(5),
            contacts = contacts,
            syncStatus = syncStatus,
            isSessionActive = (activeSession != null && activeSession.status.name == "ACTIVE")
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun startProtection(onSuccess: (ProtectionSession) -> Unit) {
        viewModelScope.launch {
            val session = startProtectionUseCase(isDemoSession = false)
            onSuccess(session)
        }
    }
}
