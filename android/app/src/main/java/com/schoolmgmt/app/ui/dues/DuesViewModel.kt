package com.schoolmgmt.app.ui.dues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.dao.DueRow
import com.schoolmgmt.app.data.repository.AcademicRepository
import com.schoolmgmt.app.data.repository.FeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class DuesSortOption(val label: String) {
    DUE_DATE_ASC("Due Date (Oldest First)"),
    DUE_DATE_DESC("Due Date (Newest First)"),
    AMOUNT_DESC("Outstanding Amount (Highest First)"),
    AMOUNT_ASC("Outstanding Amount (Lowest First)"),
    NAME_ASC("Student Name (A-Z)")
}

data class AggregatedDueRow(
    val studentId: String,
    val studentName: String,
    val admissionNo: String,
    val className: String,
    val totalOutstanding: Double,
    val oldestDueDate: Long,
    val duesCount: Int
)

@HiltViewModel
class DuesViewModel @Inject constructor(
    private val feeRepository: FeeRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    val classes = academicRepository.observeClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedClassId = MutableStateFlow<String?>(null)
    val sortOption = MutableStateFlow(DuesSortOption.DUE_DATE_ASC)

    private val rawDues = feeRepository.observeDues()

    val uiState: StateFlow<List<AggregatedDueRow>> = combine(
        rawDues,
        selectedClassId,
        sortOption
    ) { dues, classId, sortOpt ->
        // 1. Filter by class if selected
        val filtered = if (classId == null) {
            dues
        } else {
            dues.filter { it.classId == classId }
        }

        // 2. Group by student
        val aggregated = filtered.groupBy { it.studentId }.map { (studentId, studentDues) ->
            val first = studentDues.first()
            val totalOut = studentDues.sumOf { it.amount - it.discount - it.paidAmount }
            val oldestDue = studentDues.minOf { it.dueDate }
            AggregatedDueRow(
                studentId = studentId,
                studentName = "${first.studentFirstName} ${first.studentLastName}",
                admissionNo = first.admissionNo,
                className = first.className,
                totalOutstanding = totalOut,
                oldestDueDate = oldestDue,
                duesCount = studentDues.size
            )
        }

        // 3. Sort based on selection
        when (sortOpt) {
            DuesSortOption.DUE_DATE_ASC -> aggregated.sortedBy { it.oldestDueDate }
            DuesSortOption.DUE_DATE_DESC -> aggregated.sortedByDescending { it.oldestDueDate }
            DuesSortOption.AMOUNT_DESC -> aggregated.sortedByDescending { it.totalOutstanding }
            DuesSortOption.AMOUNT_ASC -> aggregated.sortedBy { it.totalOutstanding }
            DuesSortOption.NAME_ASC -> aggregated.sortedBy { it.studentName }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectClass(classId: String?) {
        selectedClassId.value = classId
    }

    fun selectSortOption(option: DuesSortOption) {
        sortOption.value = option
    }
}
