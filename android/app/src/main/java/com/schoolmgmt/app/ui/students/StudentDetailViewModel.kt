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

    // Pulled from the nav graph's "studentId" route argument — see
    // NavGraph.kt's composable(Routes.STUDENT_DETAIL) entry. Reading it
    // via SavedStateHandle (rather than a constructor param passed
    // through hiltViewModel()) is the standard Hilt+Navigation pattern,
    // and survives process death/recreation correctly.
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
}
