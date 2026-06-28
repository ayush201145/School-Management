package com.schoolmgmt.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.entity.PaymentMode
import com.schoolmgmt.app.data.repository.AcademicRepository
import com.schoolmgmt.app.data.repository.AuthRepository
import com.schoolmgmt.app.data.repository.ExpenseRepository
import com.schoolmgmt.app.data.repository.GenerateExpenseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ExpensesUiState(
    val errorMessage: String? = null,
    val lastGenerateResult: GenerateExpenseResult? = null,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val authRepository: AuthRepository,
    private val academicRepository: AcademicRepository,
) : ViewModel() {

    val categories = expenseRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val templates = expenseRepository.observeActiveTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = academicRepository.observeSelectedYear()
        .flatMapLatest { year ->
            if (year == null) {
                expenseRepository.observeExpenses()
            } else {
                expenseRepository.observeExpenses(
                    fromMillis = year.startDate,
                    toMillis = year.endDate
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState

    fun createExpense(
        expenseCategoryId: String,
        description: String,
        amount: Double,
        mode: PaymentMode,
        onSuccess: () -> Unit,
    ) {
        if (description.isBlank() || amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Description and a valid amount are required")
            return
        }
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId() ?: throw IllegalStateException("Not logged in")
                expenseRepository.createExpense(
                    expenseCategoryId = expenseCategoryId,
                    description = description.trim(),
                    amount = amount,
                    mode = mode,
                    recordedById = userId,
                )
                _uiState.value = _uiState.value.copy(errorMessage = null)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Could not add expense: ${e.message ?: "please try again"}")
            }
        }
    }

    /** Generates this month's expense from a recurring template — safe to tap more than once (idempotent). */
    fun generateThisMonth(templateId: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId() ?: throw IllegalStateException("Not logged in")
                val now = Calendar.getInstance()
                val result = expenseRepository.generateForMonth(
                    templateId = templateId,
                    recordedById = userId,
                    month = now.get(Calendar.MONTH) + 1,
                    year = now.get(Calendar.YEAR),
                )
                _uiState.value = _uiState.value.copy(lastGenerateResult = result)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Could not generate expense: ${e.message ?: "please try again"}")
            }
        }
    }

    fun dismissGenerateResult() {
        _uiState.value = _uiState.value.copy(lastGenerateResult = null)
    }
}
