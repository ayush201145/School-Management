package com.schoolmgmt.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.entity.UserRole
import com.schoolmgmt.app.data.repository.AuthRepository
import com.schoolmgmt.app.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    /**
     * Role drives which dashboard tiles are visible (e.g. a TEACHER
     * shouldn't see "Fee Structures" or "Inventory"). Defaults to null
     * while loading rather than guessing a role, so the UI can show a
     * loading state instead of briefly flashing the wrong tile set.
     */
    val role: StateFlow<UserRole?> = authRepository.roleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun syncNow() {
        syncScheduler.syncNow()
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            syncScheduler.cancelAll()
            authRepository.logout()
            onLoggedOut()
        }
    }
}
