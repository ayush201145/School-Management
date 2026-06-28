package com.schoolmgmt.app.ui.feestructures

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.local.entity.FeeStructureEntity

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.schoolmgmt.app.data.local.entity.FeeCategoryEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeStructuresScreen(
    onBack: () -> Unit,
    viewModel: FeeStructuresViewModel = hiltViewModel(),
) {
    val classes by viewModel.classes.collectAsState()
    val selectedClassId by viewModel.currentSelectedClassId.collectAsState()
    val structures by viewModel.structures.collectAsState()
    val feeCategories by viewModel.feeCategories.collectAsState()
    val assignResult by viewModel.lastAssignResult.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var operationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fee Structures") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            if (selectedClassId != null) {
                FloatingActionButton(onClick = {
                    operationError = null
                    showAddDialog = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Fee Structure")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(16.dp)) {
                val chunks = classes.chunked(3)
                chunks.forEach { rowClasses ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowClasses.forEach { schoolClass ->
                            val isSelected = schoolClass.id == selectedClassId
                            Button(
                                onClick = { viewModel.selectClass(schoolClass.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = schoolClass.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (rowClasses.size < 3) {
                            repeat(3 - rowClasses.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (selectedClassId == null) {
                Text(
                    "Pick a class above to see its fee structures.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                operationError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (structures.isEmpty()) {
                    Text(
                        "No fee structures defined for this class yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(structures, key = { it.id }) { structure ->
                            FeeStructureRow(
                                structure = structure,
                                onAssign = { viewModel.assignToClass(structure.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFeeStructureDialog(
            categories = feeCategories,
            onDismiss = { showAddDialog = false },
            onConfirm = { catId, amount, dueDate, desc, recur ->
                viewModel.createFeeStructure(
                    feeCategoryId = catId,
                    amount = amount,
                    dueDate = dueDate,
                    description = desc,
                    recurMonthly = recur,
                    onSuccess = {
                        showAddDialog = false
                    },
                    onError = {
                        operationError = it
                        showAddDialog = false
                    }
                )
            }
        )
    }

    if (assignResult != null) {
        val result = assignResult!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissAssignResult() },
            title = { Text("Fee assigned") },
            text = {
                Text(
                    "Billed ${result.created} student(s)." +
                        if (result.skipped > 0) " ${result.skipped} already had this fee and were skipped." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAssignResult() }) { Text("OK") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeeStructureDialog(
    categories: List<FeeCategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: String, amount: Double, dueDate: Long, description: String?, recurMonthly: Boolean) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<FeeCategoryEntity?>(categories.firstOrNull()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDateStr by remember {
        mutableStateOf(LocalDate.now().plusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
    }
    var recurMonthly by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Fee Structure") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (categories.isEmpty()) {
                    Text("No fee categories found. Please sync the app first to pull categories from the server.", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedCategory?.name ?: "Select Category")
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategory = category
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDateStr,
                    onValueChange = { dueDateStr = it },
                    label = { Text("Due Date (YYYY-MM-DD)") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = recurMonthly,
                        onCheckedChange = { recurMonthly = it }
                    )
                    Text(
                        text = "Recur Monthly (for the academic year)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val category = selectedCategory
                    if (category == null) {
                        errorMsg = "Please select a category"
                        return@TextButton
                    }
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMsg = "Please enter a valid amount greater than 0"
                        return@TextButton
                    }
                    val parsedDate = runCatching {
                        LocalDate.parse(dueDateStr)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    }.getOrNull()
                    if (parsedDate == null) {
                        errorMsg = "Invalid date format. Use YYYY-MM-DD"
                        return@TextButton
                    }

                    onConfirm(
                        category.id,
                        amount,
                        parsedDate,
                        description.trim().takeIf { it.isNotBlank() },
                        recurMonthly
                    )
                },
                enabled = categories.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FeeStructureRow(structure: FeeStructureEntity, onAssign: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            ListItem(
                headlineContent = { Text(structure.description ?: "Fee") },
                supportingContent = { Text("₹${structure.amount}") },
            )
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Button(onClick = onAssign) {
                    Text("Assign to all students in class")
                }
            }
        }
    }
}
