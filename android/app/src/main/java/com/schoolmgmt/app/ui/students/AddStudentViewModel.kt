package com.schoolmgmt.app.ui.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.repository.AcademicRepository
import com.schoolmgmt.app.data.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClassOption(val id: String, val label: String)

@HiltViewModel
class AddStudentViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    val classOptions: StateFlow<List<ClassOption>> = academicRepository
        .observeClasses()
        .map { classes -> classes.map { ClassOption(it.id, it.name) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun createStudent(
        admissionNo: String,
        firstName: String,
        lastName: String,
        guardianPhone: String?,
        classId: String?,
        address: String?,
        fatherPhone: String?,
        motherPhone: String?,
        whatsappPhone: String?,
        tuitionFeeStr: String,
        onSuccess: () -> Unit,
    ) {
        if (firstName.isBlank() || lastName.isBlank()) {
            _errorMessage.value = "First name and last name are required"
            return
        }
        if (classId == null) {
            _errorMessage.value = "Please select a class"
            return
        }
        val tuitionFee = if (tuitionFeeStr.isNotBlank()) {
            val parsed = tuitionFeeStr.toDoubleOrNull()
            if (parsed == null || parsed < 0) {
                _errorMessage.value = "Invalid Tuition Fee amount"
                return
            }
            parsed
        } else {
            null
        }

        viewModelScope.launch {
            try {
                val sectionId = academicRepository.getDefaultSectionForClass(classId)
                studentRepository.createStudent(
                    admissionNo = if (admissionNo.isBlank()) null else admissionNo.trim(),
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    sectionId = sectionId,
                    guardianPhone = guardianPhone?.trim()?.takeIf { it.isNotBlank() },
                    address = address?.trim()?.takeIf { it.isNotBlank() },
                    fatherPhone = fatherPhone?.trim()?.takeIf { it.isNotBlank() },
                    motherPhone = motherPhone?.trim()?.takeIf { it.isNotBlank() },
                    whatsappPhone = whatsappPhone?.trim()?.takeIf { it.isNotBlank() },
                    tuitionFee = tuitionFee,
                )
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Could not add student: ${e.message ?: "please try again"}"
            }
        }
    }
}
