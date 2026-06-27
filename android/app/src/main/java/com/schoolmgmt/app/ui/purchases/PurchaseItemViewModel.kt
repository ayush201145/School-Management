package com.schoolmgmt.app.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.local.entity.ItemCategoryEntity
import com.schoolmgmt.app.data.local.entity.ItemVariantEntity
import com.schoolmgmt.app.data.repository.AuthRepository
import com.schoolmgmt.app.data.repository.InventoryRepository
import com.schoolmgmt.app.data.repository.PurchaseRepository
import com.schoolmgmt.app.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseUiState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    /** Set after a successful purchase if stock went negative — the "warning at bill time" feature. */
    val successWarning: String? = null,
)

@HiltViewModel
class PurchaseItemViewModel @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
    private val inventoryRepository: InventoryRepository,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    val categories: StateFlow<List<ItemCategoryEntity>> = inventoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    val currentSelectedCategoryId: StateFlow<String?> = selectedCategoryId

    val variants: StateFlow<List<ItemVariantEntity>> = selectedCategoryId.flatMapLatest { categoryId ->
        if (categoryId == null) flowOf(emptyList()) else inventoryRepository.observeVariants(categoryId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState

    fun selectCategory(categoryId: String) {
        selectedCategoryId.value = categoryId
    }

    fun purchase(studentId: String, itemVariantId: String, quantity: Int, onSuccess: () -> Unit) {
        if (quantity <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Quantity must be at least 1")
            return
        }

        _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null, successWarning = null)
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                    ?: throw IllegalStateException("Not logged in")

                val result = purchaseRepository.purchaseItem(
                    studentId = studentId,
                    itemVariantId = itemVariantId,
                    quantity = quantity,
                    recordedById = userId,
                )

                syncScheduler.syncNow()

                _uiState.value = _uiState.value.copy(isSubmitting = false, successWarning = result.warning)

                // Only auto-close (via onSuccess) when there's nothing
                // the person needs to see. When there IS a warning, we
                // deliberately do NOT call onSuccess here — the warning
                // dialog (driven by successWarning in uiState) takes
                // over, and ITS OK button is what eventually calls
                // onPurchaseComplete via dismissWarning(). This is the
                // real, fresh result, not a stale Compose snapshot, so
                // this decision is safe to make right here.
                if (result.warning == null) {
                    onSuccess()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "Could not complete purchase: ${e.message ?: "please try again"}",
                )
            }
        }
    }

    fun dismissWarning() {
        _uiState.value = _uiState.value.copy(successWarning = null)
    }
}
