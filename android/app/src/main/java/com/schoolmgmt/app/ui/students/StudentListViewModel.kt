package com.schoolmgmt.app.ui.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.entity.StudentEntity
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
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class StudentListViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    private val selectedYear = academicRepository.observeSelectedYear()

    private val studentsFlow = combine(selectedYear, searchQuery) { year, query ->
        year to query
    }.flatMapLatest { (year, query) ->
        val yearId = year?.id ?: ""
        if (query.isBlank()) {
            studentRepository.observeByAcademicYear(yearId)
        } else {
            studentRepository.searchByAcademicYear(yearId, query)
        }
    }

    val uiState: StateFlow<StudentListUiState> = combine(searchQuery, studentsFlow) { query, students ->
        StudentListUiState(searchQuery = query, students = students)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudentListUiState())

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
    }
}
