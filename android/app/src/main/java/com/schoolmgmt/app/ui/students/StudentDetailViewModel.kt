package com.schoolmgmt.app.ui.students

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.entity.StudentEntity
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import com.schoolmgmt.app.data.local.entity.WithdrawalReason
import com.schoolmgmt.app.data.repository.FeeRepository
import com.schoolmgmt.app.data.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.schoolmgmt.app.data.local.entity.FeeStatus

data class StudentDetailUiState(
    val student: StudentEntity? = null,
    val fees: List<StudentFeeEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val feeRepository: FeeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val studentId: String = checkNotNull(savedStateHandle["studentId"])

    private val _uiState = MutableStateFlow(StudentDetailUiState())
    val uiState: StateFlow<StudentDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            studentRepository.getById(studentId)?.let { student ->
                _uiState.value = _uiState.value.copy(student = student, isLoading = false)
            }
        }
        viewModelScope.launch {
            feeRepository.observeFeesForStudent(studentId).collect { fees ->
                _uiState.value = _uiState.value.copy(fees = fees)
            }
        }
    }

    fun withdraw(reason: WithdrawalReason, notes: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            studentRepository.withdrawStudent(studentId, reason, notes)
            studentRepository.getById(studentId)?.let { student ->
                _uiState.value = _uiState.value.copy(student = student)
            }
            onDone()
        }
    }

    fun reinstate() {
        viewModelScope.launch {
            studentRepository.reinstateStudent(studentId)
            studentRepository.getById(studentId)?.let { student ->
                _uiState.value = _uiState.value.copy(student = student)
            }
        }
    }

    suspend fun getRemainingBalance(studentFeeId: String): Double =
        feeRepository.getRemainingBalance(studentFeeId)

    suspend fun getTotalDuesBalance(): Double {
        var balance = 0.0
        for (fee in uiState.value.fees) {
            if (fee.status != FeeStatus.PAID) {
                balance += feeRepository.getRemainingBalance(fee.id)
            }
        }
        return balance
    }
}
