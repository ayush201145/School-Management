package com.schoolmgmt.app.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.local.dao.InventoryRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val lowStockOnly by viewModel.isLowStockOnly.collectAsState()
    var selectedRow by remember { mutableStateOf<InventoryRow?>(null) }
    var selectedCategory by remember { mutableStateOf("Books") }

    val filteredItems = items.filter { row ->
        val name = row.categoryName.lowercase()
        val mappedCategory = when {
            name.contains("book") -> "Books"
            name.contains("summer (regular)") || name.contains("summer regular") -> "Summer Uniform (Regular)"
            name.contains("pt") || name.contains("sports") || name.contains("pt/sports") -> "PT / Sports"
            name.contains("winter") -> "Winter Uniform"
            else -> row.categoryName
        }
        val targetCategory = when (selectedCategory) {
            "Books" -> "Books"
            "Summer Uniforms" -> "Summer Uniform (Regular)"
            "PT / Sports" -> "PT / Sports"
            "Winter Uniforms" -> "Winter Uniform"
            else -> "Books"
        }
        mappedCategory == targetCategory
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Category Buttons (2x2 Grid)
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryGridCard(
                        title = "Books",
                        isSelected = selectedCategory == "Books",
                        onClick = { selectedCategory = "Books" },
                        modifier = Modifier.weight(1f)
                    )
                    CategoryGridCard(
                        title = "Summer Uniforms",
                        isSelected = selectedCategory == "Summer Uniforms",
                        onClick = { selectedCategory = "Summer Uniforms" },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryGridCard(
                        title = "PT / Sports",
                        isSelected = selectedCategory == "PT / Sports",
                        onClick = { selectedCategory = "PT / Sports" },
                        modifier = Modifier.weight(1f)
                    )
                    CategoryGridCard(
                        title = "Winter Uniforms",
                        isSelected = selectedCategory == "Winter Uniforms",
                        onClick = { selectedCategory = "Winter Uniforms" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = lowStockOnly,
                    onClick = { viewModel.toggleLowStockOnly() },
                    label = { Text("Low stock only (< 10)") },
                )
            }

            if (filteredItems.isEmpty()) {
                Text(
                    if (lowStockOnly) "Nothing is low on stock in this category." else "No items in this category yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filteredItems, key = { it.variant.id }) { row ->
                        InventoryRowCard(row, onClick = { selectedRow = row })
                    }
                }
            }
        }
    }

    selectedRow?.let { row ->
        InventoryActionDialog(
            row = row,
            onDismiss = { selectedRow = null },
            onRestock = { qty -> viewModel.restock(row.variant.id, qty) },
            onUpdatePrice = { price -> viewModel.updatePrice(row.variant.id, price) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryGridCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InventoryRowCard(row: InventoryRow, onClick: () -> Unit) {
    val isLow = row.variant.stockQuantity < 10
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 2.dp)
        .clickable { onClick() }
    ) {
        ListItem(
            headlineContent = { Text("${row.categoryName} — ${row.variant.label}") },
            supportingContent = { Text("₹${row.variant.price}") },
            trailingContent = {
                Text(
                    "${row.variant.stockQuantity} in stock",
                    color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
        )
    }
}

@Composable
fun InventoryActionDialog(
    row: InventoryRow,
    onDismiss: () -> Unit,
    onRestock: (Int) -> Unit,
    onUpdatePrice: (Double) -> Unit,
) {
    var restockQty by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf(row.variant.price.toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update ${row.categoryName} - ${row.variant.label}") },
        text = {
            Column {
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Price (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = restockQty,
                    onValueChange = { restockQty = it },
                    label = { Text("Add Stock (Quantity)") },
                    placeholder = { Text("e.g. 50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newPrice = priceStr.toDoubleOrNull()
                if (newPrice == null || newPrice < 0) {
                    errorMsg = "Invalid price"
                    return@TextButton
                }

                val qty = if (restockQty.isNotBlank()) {
                    val parsed = restockQty.toIntOrNull()
                    if (parsed == null || parsed <= 0) {
                        errorMsg = "Invalid restock quantity"
                        return@TextButton
                    }
                    parsed
                } else {
                    0
                }

                if (newPrice != row.variant.price) {
                    onUpdatePrice(newPrice)
                }
                if (qty > 0) {
                    onRestock(qty)
                }
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
