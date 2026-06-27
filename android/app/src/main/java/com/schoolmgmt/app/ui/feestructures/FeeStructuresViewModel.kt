package com.schoolmgmt.app.ui.feestructures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.repository.AcademicRepository
import com.schoolmgmt.app.data.repository.BulkAssignResult
import com.schoolmgmt.app.data.repository.FeeStructureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeeStructuresViewModel @Inject constructor(
    private val feeStructureRepository: FeeStructureRepository,
    academicRepository: AcademicRepository,
) : ViewModel() {

    val classes = academicRepository.observeClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedClassId = MutableStateFlow<String?>(null)
    val currentSelectedClassId: StateFlow<String?> = selectedClassId

    val structures = selectedClassId.flatMapLatest { classId ->
        if (classId == null) flowOf(emptyList())
        else feeStructureRepository.observeByClass(classId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastAssignResult = MutableStateFlow<BulkAssignResult?>(null)
    val lastAssignResult: StateFlow<BulkAssignResult?> = _lastAssignResult

    fun selectClass(classId: String) {
        selectedClassId.value = classId
    }

    /** THE BULK-ASSIGN BUTTON (#6), finally with a UI on top of it. */
    fun assignToClass(feeStructureId: String) {
        viewModelScope.launch {
            val result = feeStructureRepository.assignToClass(feeStructureId)
            _lastAssignResult.value = result
        }
    }

    fun dismissAssignResult() {
        _lastAssignResult.value = null
    }
}
