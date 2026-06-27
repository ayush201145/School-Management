package com.schoolmgmt.app.ui.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.entity.StaffType
import com.schoolmgmt.app.data.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddStaffUiState(val errorMessage: String? = null)

@HiltViewModel
class StaffListViewModel @Inject constructor(
    private val staffRepository: StaffRepository,
) : ViewModel() {
    val staff = staffRepository.observeActive()

    private val _addUiState = MutableStateFlow(AddStaffUiState())
    val addUiState: StateFlow<AddStaffUiState> = _addUiState

    fun createStaff(name: String, type: StaffType, phone: String?, monthlySalary: Double?, onSuccess: () -> Unit) {
        if (name.isBlank()) {
            _addUiState.value = AddStaffUiState(errorMessage = "Name is required")
            return
        }
        viewModelScope.launch {
            try {
                staffRepository.createStaff(
                    name = name.trim(),
                    type = type,
                    phone = phone?.ifBlank { null },
                    monthlySalary = monthlySalary,
                )
                _addUiState.value = AddStaffUiState()
                onSuccess()
            } catch (e: Exception) {
                _addUiState.value = AddStaffUiState(errorMessage = "Could not add staff: ${e.message ?: "please try again"}")
            }
        }
    }
}
