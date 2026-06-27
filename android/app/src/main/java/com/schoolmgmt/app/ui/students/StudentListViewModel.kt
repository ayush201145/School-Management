package com.schoolmgmt.app.ui.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.entity.StudentEntity
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

@HiltViewModel
class StudentListViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    // flatMapLatest so typing a new character cancels the previous
    // search's Flow collection rather than leaving multiple stale
    // queries running and racing to update the UI.
    private val studentsFlow = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) studentRepository.observeAll() else studentRepository.search(query)
    }

    val uiState: StateFlow<StudentListUiState> = combine(searchQuery, studentsFlow) { query, students ->
        StudentListUiState(searchQuery = query, students = students)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudentListUiState())

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
    }
}
