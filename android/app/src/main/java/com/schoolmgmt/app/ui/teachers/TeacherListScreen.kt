package com.schoolmgmt.app.ui.teachers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherListScreen(
    onBack: () -> Unit,
    viewModel: TeacherListViewModel = hiltViewModel(),
) {
    val teachers by viewModel.teachers.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teachers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Teacher")
            }
        }
    ) { padding ->
        if (teachers.isEmpty()) {
            Text(
                "No teachers yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(padding).padding(16.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(teachers, key = { it.id }) { teacher ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                        ListItem(
                            headlineContent = { Text("${teacher.firstName} ${teacher.lastName}") },
                            supportingContent = {
                                Text("Employee No: ${teacher.employeeNo}" + (teacher.phone?.let { " · $it" } ?: ""))
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTeacherDialog(
            onDismiss = { showAddDialog = false },
            viewModel = viewModel
        )
    }
}

@Composable
fun AddTeacherDialog(
    onDismiss: () -> Unit,
    viewModel: TeacherListViewModel,
) {
    var employeeNo by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var qualification by remember { mutableStateOf("") }
    var monthlySalaryStr by remember { mutableStateOf("") }

    val errorMessage by viewModel.errorMessage.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Teacher") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = employeeNo,
                    onValueChange = { employeeNo = it },
                    label = { Text("Employee No") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (optional)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = qualification,
                    onValueChange = { qualification = it },
                    label = { Text("Qualification (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = monthlySalaryStr,
                    onValueChange = { monthlySalaryStr = it },
                    label = { Text("Monthly Salary (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.createTeacher(
                    employeeNo = employeeNo,
                    firstName = firstName,
                    lastName = lastName,
                    phone = phone,
                    email = email,
                    address = address,
                    qualification = qualification,
                    monthlySalaryStr = monthlySalaryStr,
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
