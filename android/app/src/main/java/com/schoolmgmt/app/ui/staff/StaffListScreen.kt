package com.schoolmgmt.app.ui.staff

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
import com.schoolmgmt.app.data.local.entity.StaffEntity
import com.schoolmgmt.app.data.local.entity.StaffType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffListScreen(
    onStaffClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: StaffListViewModel = hiltViewModel(),
) {
    val staff by viewModel.staff.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add staff")
            }
        },
    ) { padding ->
        if (staff.isEmpty()) {
            Text(
                "No staff added yet. Tap + to add one.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(padding).padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(staff, key = { it.id }) { member ->
                    StaffRow(member, onClick = { onStaffClick(member.id) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddStaffDialog(onDismiss = { showAddDialog = false }, viewModel = viewModel)
    }
}

@Composable
private fun StaffRow(staff: StaffEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        ListItem(
            headlineContent = { Text(staff.name) },
            supportingContent = {
                Text(staff.type.name + (staff.monthlySalary?.let { " · ₹$it/month" } ?: ""))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStaffDialog(onDismiss: () -> Unit, viewModel: StaffListViewModel) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(StaffType.OTHER) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var salaryText by remember { mutableStateOf("") }

    val uiState by viewModel.addUiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add staff member") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )

                TextButton(onClick = { typeMenuExpanded = true }) {
                    Text("Type: ${selectedType.name}")
                }
                DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                    StaffType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = { selectedType = type; typeMenuExpanded = false },
                        )
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                )

                OutlinedTextField(
                    value = salaryText,
                    onValueChange = { salaryText = it },
                    label = { Text("Monthly salary (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

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
                viewModel.createStaff(
                    name = name,
                    type = selectedType,
                    phone = phone,
                    monthlySalary = salaryText.toDoubleOrNull(),
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
