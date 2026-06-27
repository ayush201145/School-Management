package com.schoolmgmt.app.ui.staff

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.entity.PaymentMode
import com.schoolmgmt.app.data.local.entity.StaffEntity
import com.schoolmgmt.app.data.repository.AuthRepository
import com.schoolmgmt.app.data.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StaffDetailUiState(
    val staff: StaffEntity? = null,
    val isSubmittingPayment: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class StaffDetailViewModel @Inject constructor(
    private val staffRepository: StaffRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val staffId: String = checkNotNull(savedStateHandle["staffId"])

    private val _uiState = MutableStateFlow(StaffDetailUiState())
    val uiState: StateFlow<StaffDetailUiState> = _uiState

    val salaryPayments = staffRepository.observeSalaryPayments(staffId)

    init {
        viewModelScope.launch {
            staffRepository.getById(staffId)?.let { staff ->
                _uiState.value = _uiState.value.copy(staff = staff)
            }
        }
    }

    fun recordPayment(amount: Double, forMonth: Int, forYear: Int, mode: PaymentMode, notes: String?, onSuccess: () -> Unit) {
        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter an amount greater than 0")
            return
        }

        _uiState.value = _uiState.value.copy(isSubmittingPayment = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId() ?: throw IllegalStateException("Not logged in")
                staffRepository.recordSalaryPayment(
                    staffId = staffId,
                    amount = amount,
                    forMonth = forMonth,
                    forYear = forYear,
                    mode = mode,
                    recordedById = userId,
                    notes = notes?.ifBlank { null },
                )
                _uiState.value = _uiState.value.copy(isSubmittingPayment = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmittingPayment = false,
                    errorMessage = "Could not record payment: ${e.message ?: "please try again"}",
                )
            }
        }
    }
}
