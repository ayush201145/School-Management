package com.schoolmgmt.app.ui.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.entity.StudentEntity
import com.schoolmgmt.app.data.local.entity.SchoolClassEntity
import com.schoolmgmt.app.data.repository.AcademicRepository
import com.schoolmgmt.app.data.repository.StudentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentListUiState(
    val searchQuery: String = "",
    val students: List<StudentEntity> = emptyList(),
    val classes: List<SchoolClassEntity> = emptyList(),
    val selectedClassId: String? = null,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class StudentListViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedClassId = MutableStateFlow<String?>(null)

    private val selectedYear = academicRepository.observeSelectedYear()

    val classes: StateFlow<List<SchoolClassEntity>> = selectedYear.flatMapLatest { year ->
        if (year == null) {
            academicRepository.observeClasses()
        } else {
            academicRepository.observeClassesForYear(year.id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val studentsFlow = combine(selectedYear, selectedClassId, searchQuery) { year, classId, query ->
        Triple(year, classId, query)
    }.flatMapLatest { (year, classId, query) ->
        val yearId = year?.id ?: ""
        if (classId != null) {
            if (query.isBlank()) {
                studentRepository.observeByClass(classId)
            } else {
                studentRepository.searchByClass(classId, query)
            }
        } else {
            if (query.isBlank()) {
                studentRepository.observeByAcademicYear(yearId)
            } else {
                studentRepository.searchByAcademicYear(yearId, query)
            }
        }
    }

    val uiState: StateFlow<StudentListUiState> = combine(
        searchQuery,
        selectedClassId,
        classes,
        studentsFlow
    ) { query, classId, classesList, students ->
        StudentListUiState(
            searchQuery = query,
            students = students,
            classes = classesList,
            selectedClassId = classId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudentListUiState())

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
    }

    fun selectClass(classId: String?) {
        selectedClassId.value = classId
    }
}
