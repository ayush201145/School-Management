package com.schoolmgmt.app.ui.purchases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.local.entity.ItemVariantEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseItemDialog(
    studentId: String,
    onDismiss: () -> Unit,
    onPurchaseComplete: () -> Unit,
    viewModel: PurchaseItemViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.currentSelectedCategoryId.collectAsState()
    val variants by viewModel.variants.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var variantMenuExpanded by remember { mutableStateOf(false) }
    var selectedVariant by remember { mutableStateOf<ItemVariantEntity?>(null) }
    var quantityText by remember { mutableStateOf("1") }

    // If a backorder warning came back from a completed purchase, show
    // it as its own follow-up dialog rather than silently closing —
    // this is the actual UI surface for the "warning when bill is
    // generated" behavior built into PurchaseRepository/InventoryRepository.
    if (uiState.successWarning != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissWarning(); onPurchaseComplete() },
            title = { Text("Purchase recorded") },
            text = { Text(uiState.successWarning ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissWarning(); onPurchaseComplete() }) { Text("OK") }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sell book / uniform") },
        text = {
            Column {
                Text("Category", style = MaterialTheme.typography.labelMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    items(categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = category.id == selectedCategoryId,
                            onClick = {
                                viewModel.selectCategory(category.id)
                                selectedVariant = null
                            },
                            label = { Text(category.name) },
                        )
                    }
                }

                if (selectedCategoryId != null) {
                    TextButton(onClick = { variantMenuExpanded = true }) {
                        Text(selectedVariant?.let { "${it.label} · ₹${it.price}" } ?: "Choose item")
                    }
                    DropdownMenu(expanded = variantMenuExpanded, onDismissRequest = { variantMenuExpanded = false }) {
                        if (variants.isEmpty()) {
                            DropdownMenuItem(text = { Text("No items in this category yet") }, onClick = {})
                        }
                        variants.forEach { variant ->
                            DropdownMenuItem(
                                text = { Text("${variant.label} · ₹${variant.price} · ${variant.stockQuantity} in stock") },
                                onClick = { selectedVariant = variant; variantMenuExpanded = false },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                selectedVariant?.let { variant ->
                    val qty = quantityText.toIntOrNull() ?: 1
                    Text(
                        "Total: ₹${"%.2f".format(variant.price * qty)}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !uiState.isSubmitting && selectedVariant != null,
                onClick = {
                    val variant = selectedVariant ?: return@TextButton
                    val qty = quantityText.toIntOrNull() ?: 1
                    viewModel.purchase(
                        studentId = studentId,
                        itemVariantId = variant.id,
                        quantity = qty,
                        // onSuccess fires only when there's NO warning to
                        // show (see PurchaseItemViewModel.purchase — it
                        // only invokes onSuccess in the no-warning case,
                        // and otherwise just updates uiState so the
                        // `if (uiState.successWarning != null)` branch
                        // above takes over and shows its own dialog with
                        // its own OK button that calls onPurchaseComplete).
                        onSuccess = onPurchaseComplete,
                    )
                },
            ) {
                Text(if (uiState.isSubmitting) "Processing..." else "Complete sale")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
