package com.schoolmgmt.app.ui.teachers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherListViewModel @Inject constructor(
    private val teacherRepository: TeacherRepository,
) : ViewModel() {
    val teachers = teacherRepository.observeAll()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun createTeacher(
        employeeNo: String,
        firstName: String,
        lastName: String,
        phone: String?,
        email: String?,
        address: String?,
        qualification: String?,
        monthlySalaryStr: String,
        onSuccess: () -> Unit,
    ) {
        if (employeeNo.isBlank() || firstName.isBlank() || lastName.isBlank()) {
            _errorMessage.value = "Employee No, first name, and last name are required"
            return
        }
        val monthlySalary = if (monthlySalaryStr.isNotBlank()) {
            val parsed = monthlySalaryStr.toDoubleOrNull()
            if (parsed == null || parsed < 0) {
                _errorMessage.value = "Invalid Salary amount"
                return
            }
            parsed
        } else {
            null
        }

        viewModelScope.launch {
            try {
                teacherRepository.createTeacher(
                    employeeNo = employeeNo.trim(),
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    phone = phone?.trim()?.takeIf { it.isNotBlank() },
                    email = email?.trim()?.takeIf { it.isNotBlank() },
                    address = address?.trim()?.takeIf { it.isNotBlank() },
                    qualification = qualification?.trim()?.takeIf { it.isNotBlank() },
                    monthlySalary = monthlySalary,
                )
                _errorMessage.value = null
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Could not create teacher: ${e.message ?: "please try again"}"
            }
        }
    }
}
