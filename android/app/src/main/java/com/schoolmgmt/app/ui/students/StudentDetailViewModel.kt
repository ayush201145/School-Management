package com.schoolmgmt.app.ui.students

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.entity.StudentEntity
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import com.schoolmgmt.app.data.local.entity.WithdrawalReason
import com.schoolmgmt.app.data.repository.FeeRepository
import com.schoolmgmt.app.data.repository.StudentRepository
import com.schoolmgmt.app.data.repository.AcademicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.schoolmgmt.app.data.local.entity.FeeStatus
import com.schoolmgmt.app.data.local.entity.AcademicYearEntity

import kotlinx.coroutines.flow.first

data class StudentDetailUiState(
    val student: StudentEntity? = null,
    val fees: List<StudentFeeEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val feeRepository: FeeRepository,
    private val academicRepository: AcademicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val studentId: String = checkNotNull(savedStateHandle["studentId"])

    private val _uiState = MutableStateFlow(StudentDetailUiState())
    val uiState: StateFlow<StudentDetailUiState> = _uiState

    val academicYears: StateFlow<List<AcademicYearEntity>> = academicRepository.observeYears()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun observeClassesForYear(yearId: String) = academicRepository.observeClassesForYear(yearId)

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

    fun migrateStudent(classId: String, newTuitionFee: Double?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val student = uiState.value.student ?: return@launch
            try {
                val sectionId = academicRepository.getDefaultSectionForClass(classId)
                val updated = student.copy(
                    sectionId = sectionId,
                    tuitionFee = newTuitionFee,
                    updatedAt = System.currentTimeMillis()
                )
                studentRepository.updateStudent(updated)
                // Reload student info in UI
                _uiState.value = _uiState.value.copy(student = updated)
                onSuccess()
            } catch (e: Exception) {
                // Handle or log error
            }
        }
    }

    suspend fun getRemainingBalance(studentFeeId: String): Double =
        feeRepository.getRemainingBalance(studentFeeId)

    suspend fun getClassNameForSection(sectionId: String): String {
        val list = academicRepository.observeAllSectionsWithClassName().first()
        return list.find { it.id == sectionId }?.let { "${it.className} - ${it.sectionName}" } ?: "General"
    }

    suspend fun getTotalDuesBalance(): Double {
        var balance = 0.0
        for (fee in uiState.value.fees) {
            if (fee.status != FeeStatus.PAID && !fee.isDefaulted) {
                balance += feeRepository.getRemainingBalance(fee.id)
            }
        }
        return balance
    }

    fun markFeeAsDefaulted(feeId: String) {
        viewModelScope.launch {
            feeRepository.markFeeAsDefaulted(feeId)
        }
    }
}
