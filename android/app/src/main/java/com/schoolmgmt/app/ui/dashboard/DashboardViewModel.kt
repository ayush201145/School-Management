package com.schoolmgmt.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.entity.AcademicYearEntity
import com.schoolmgmt.app.data.local.entity.UserRole
import com.schoolmgmt.app.data.repository.AcademicRepository
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
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    /**
     * Role drives which dashboard tiles are visible (e.g. a TEACHER
     * shouldn't see "Fee Structures" or "Inventory"). Defaults to null
     * while loading rather than guessing a role, so the UI can show a
     * loading state instead of briefly flashing the wrong tile set.
     */
    val role: StateFlow<UserRole?> = authRepository.roleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val academicYears: StateFlow<List<AcademicYearEntity>> = academicRepository.observeYears()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedYearId: StateFlow<String?> = academicRepository.selectedYearId

    fun selectYearId(id: String?) {
        academicRepository.selectYearId(id)
    }

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
