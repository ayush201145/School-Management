package com.schoolmgmt.app.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.entity.PaymentMode
import com.schoolmgmt.app.data.repository.AuthRepository
import com.schoolmgmt.app.data.repository.FeeRepository
import com.schoolmgmt.app.data.repository.OverpaymentException
import com.schoolmgmt.app.data.repository.ReceiptItem
import com.schoolmgmt.app.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordPaymentUiState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class RecordPaymentViewModel @Inject constructor(
    private val feeRepository: FeeRepository,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordPaymentUiState())
    val uiState: StateFlow<RecordPaymentUiState> = _uiState

    fun recordPayment(
        studentFeeId: String,
        amount: Double,
        mode: PaymentMode,
        referenceNo: String?,
        notes: String?,
        onSuccess: () -> Unit,
    ) {
        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter an amount greater than 0")
            return
        }

        _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                    ?: throw IllegalStateException("Not logged in")

                feeRepository.recordPayment(
                    studentFeeId = studentFeeId,
                    amount = amount,
                    mode = mode,
                    recordedById = userId,
                    referenceNo = referenceNo?.ifBlank { null },
                    notes = notes?.ifBlank { null },
                )

                // Recorded locally already (offline-first) — nudge a sync
                // so it reaches the server promptly rather than waiting
                // for the next periodic window.
                syncScheduler.syncNow()

                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onSuccess()
            } catch (e: OverpaymentException) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = e.message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "Could not record payment: ${e.message ?: "please try again"}",
                )
            }
        }
    }

    fun recordBulkPayment(
        studentId: String,
        amount: Double,
        mode: PaymentMode,
        referenceNo: String?,
        notes: String?,
        onSuccess: (List<ReceiptItem>) -> Unit,
    ) {
        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter an amount greater than 0")
            return
        }

        _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                    ?: throw IllegalStateException("Not logged in")

                val result = feeRepository.recordBulkPayment(
                    studentId = studentId,
                    totalAmount = amount,
                    mode = mode,
                    recordedById = userId,
                    referenceNo = referenceNo?.ifBlank { null },
                    notes = notes?.ifBlank { null },
                )

                syncScheduler.syncNow()

                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onSuccess(result.items)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "Could not record bulk payment: ${e.message ?: "please try again"}",
                )
            }
        }
    }
}
