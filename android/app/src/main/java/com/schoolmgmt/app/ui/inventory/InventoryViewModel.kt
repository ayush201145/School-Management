package com.schoolmgmt.app.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schoolmgmt.app.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import kotlinx.coroutines.launch

private const val LOW_STOCK_THRESHOLD = 10

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
) : ViewModel() {

    private val lowStockOnly = MutableStateFlow(false)
    val isLowStockOnly: StateFlow<Boolean> = lowStockOnly

    val items = lowStockOnly.flatMapLatest { onlyLow ->
        inventoryRepository.observeInventory(lowStockBelow = if (onlyLow) LOW_STOCK_THRESHOLD else null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleLowStockOnly() {
        lowStockOnly.value = !lowStockOnly.value
    }

    fun restock(itemVariantId: String, quantity: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                inventoryRepository.restock(
                    itemVariantId = itemVariantId,
                    quantity = quantity,
                    note = "Restocked from App UI",
                    recordedById = "SYSTEM"
                )
                onSuccess()
            } catch (e: Exception) {
                // simple swallow or log, the repository handles validation
            }
        }
    }

    fun updatePrice(itemVariantId: String, price: Double, costPrice: Double?, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                inventoryRepository.updateVariantPrice(itemVariantId, price, costPrice)
                onSuccess()
            } catch (e: Exception) {
                // simple swallow or log
            }
        }
    }
}
