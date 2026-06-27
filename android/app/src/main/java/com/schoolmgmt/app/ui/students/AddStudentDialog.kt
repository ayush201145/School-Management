package com.schoolmgmt.app.ui.students

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentDialog(
    onDismiss: () -> Unit,
    viewModel: AddStudentViewModel = hiltViewModel(),
) {
    var admissionNo by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var guardianPhone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var fatherPhone by remember { mutableStateOf("") }
    var motherPhone by remember { mutableStateOf("") }
    var whatsappPhone by remember { mutableStateOf("") }
    var tuitionFeeStr by remember { mutableStateOf("") }

    var classExpanded by remember { mutableStateOf(false) }
    var selectedClassLabel by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf<String?>(null) }

    val classes by viewModel.classOptions.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add student") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = admissionNo,
                    onValueChange = { admissionNo = it },
                    label = { Text("Admission No (Optional)") },
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
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = guardianPhone,
                    onValueChange = { guardianPhone = it },
                    label = { Text("Guardian phone (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = fatherPhone,
                    onValueChange = { fatherPhone = it },
                    label = { Text("Father's phone (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = motherPhone,
                    onValueChange = { motherPhone = it },
                    label = { Text("Mother's phone (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = whatsappPhone,
                    onValueChange = { whatsappPhone = it },
                    label = { Text("WhatsApp phone (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
                OutlinedTextField(
                    value = tuitionFeeStr,
                    onValueChange = { tuitionFeeStr = it },
                    label = { Text("Tuition Fee (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )

                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = selectedClassLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Class") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { classExpanded = true }
                    )
                    DropdownMenu(
                        expanded = classExpanded,
                        onDismissRequest = { classExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        classes.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    selectedClassId = option.id
                                    selectedClassLabel = option.label
                                    classExpanded = false
                                },
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.createStudent(
                    admissionNo = admissionNo,
                    firstName = firstName,
                    lastName = lastName,
                    guardianPhone = guardianPhone.ifBlank { null },
                    classId = selectedClassId,
                    address = address.ifBlank { null },
                    fatherPhone = fatherPhone.ifBlank { null },
                    motherPhone = motherPhone.ifBlank { null },
                    whatsappPhone = whatsappPhone.ifBlank { null },
                    tuitionFeeStr = tuitionFeeStr,
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
