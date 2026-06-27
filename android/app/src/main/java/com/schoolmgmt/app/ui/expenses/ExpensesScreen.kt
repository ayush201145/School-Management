package com.schoolmgmt.app.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.local.dao.ExpenseWithCategory
import com.schoolmgmt.app.data.local.entity.ExpenseCategoryEntity
import com.schoolmgmt.app.data.local.entity.PaymentMode
import com.schoolmgmt.app.data.local.entity.RecurringExpenseTemplateEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onBack: () -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val totalThisList = expenses.sumOf { it.expense.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total recorded", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "₹${"%.2f".format(totalThisList)}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (templates.isNotEmpty()) {
                Text(
                    "Recurring expenses",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                templates.forEach { template ->
                    RecurringTemplateRow(template, onGenerate = { viewModel.generateThisMonth(template.id) })
                }
            }

            Text(
                "All expenses",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (expenses.isEmpty()) {
                Text(
                    "No expenses recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(expenses, key = { it.expense.id }) { row ->
                        ExpenseRow(row)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(categories = categories, onDismiss = { showAddDialog = false }, viewModel = viewModel)
    }

    if (uiState.lastGenerateResult != null) {
        val result = uiState.lastGenerateResult!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissGenerateResult() },
            title = { Text(if (result.created) "Expense generated" else "Already generated") },
            text = {
                Text(
                    if (result.created) "Created: ${result.expense.description} — ₹${result.expense.amount}"
                    else "${result.expense.description} was already generated for this month."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissGenerateResult() }) { Text("OK") }
            },
        )
    }
}

@Composable
private fun RecurringTemplateRow(template: RecurringExpenseTemplateEntity, onGenerate: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        ListItem(
            headlineContent = { Text(template.label) },
            supportingContent = { Text("₹${template.amount} · day ${template.dayOfMonth} of month") },
            trailingContent = {
                TextButton(onClick = onGenerate) { Text("Generate") }
            },
        )
    }
}

@Composable
private fun ExpenseRow(row: ExpenseWithCategory) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        ListItem(
            headlineContent = { Text(row.expense.description) },
            supportingContent = { Text("${row.categoryName} · ${row.expense.mode.name}") },
            trailingContent = {
                Text(
                    "₹${"%.2f".format(row.expense.amount)}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(
    categories: List<ExpenseCategoryEntity>,
    onDismiss: () -> Unit,
    viewModel: ExpensesViewModel,
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExpenseCategoryEntity?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(PaymentMode.CASH) }
    var modeMenuExpanded by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expense") },
        text = {
            Column {
                TextButton(onClick = { categoryMenuExpanded = true }) {
                    Text(selectedCategory?.name ?: "Choose category")
                }
                DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                    if (categories.isEmpty()) {
                        DropdownMenuItem(text = { Text("No categories yet") }, onClick = {})
                    }
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = { selectedCategory = category; categoryMenuExpanded = false },
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (e.g. Pages, Ink, Markers)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )

                TextButton(onClick = { modeMenuExpanded = true }) {
                    Text("Mode: ${selectedMode.name}")
                }
                DropdownMenu(expanded = modeMenuExpanded, onDismissRequest = { modeMenuExpanded = false }) {
                    PaymentMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.name) },
                            onClick = { selectedMode = mode; modeMenuExpanded = false },
                        )
                    }
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
            TextButton(onClick = {
                val category = selectedCategory ?: return@TextButton
                val amount = amountText.toDoubleOrNull() ?: return@TextButton
                viewModel.createExpense(
                    expenseCategoryId = category.id,
                    description = description,
                    amount = amount,
                    mode = selectedMode,
                    onSuccess = onDismiss,
                )
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
