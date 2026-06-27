package com.schoolmgmt.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.repository.MonthlyReport
import com.schoolmgmt.app.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class MonthlyReportUiState(
    val month: Int,
    val year: Int,
    val report: MonthlyReport? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val now = Calendar.getInstance()
    private val _uiState = MutableStateFlow(
        MonthlyReportUiState(month = now.get(Calendar.MONTH) + 1, year = now.get(Calendar.YEAR))
    )
    val uiState: StateFlow<MonthlyReportUiState> = _uiState

    init {
        load()
    }

    fun selectMonth(month: Int, year: Int) {
        _uiState.value = _uiState.value.copy(month = month, year = year)
        load()
    }

    private fun load() {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val report = reportRepository.getMonthlyReport(state.month, state.year)
                _uiState.value = _uiState.value.copy(report = report, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Could not load report: ${e.message ?: "please try again"}",
                )
            }
        }
    }
}
