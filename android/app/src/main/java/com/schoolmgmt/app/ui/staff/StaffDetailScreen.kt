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
import com.schoolmgmt.app.data.local.entity.PaymentMode
import com.schoolmgmt.app.data.local.entity.SalaryPaymentEntity
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDetailScreen(
    staffId: String,
    onBack: () -> Unit,
    viewModel: StaffDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val payments by viewModel.salaryPayments.collectAsState(initial = emptyList())
    var showPaymentDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.staff?.name ?: "Staff") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showPaymentDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Record salary payment")
            }
        },
    ) { padding ->
        val staff = uiState.staff
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (staff != null) {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(staff.type.name, style = MaterialTheme.typography.bodyMedium)
                        staff.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        staff.monthlySalary?.let {
                            Text("Monthly salary: ₹$it", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Text(
                "Salary payments",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (payments.isEmpty()) {
                Text(
                    "No payments recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(payments, key = { it.id }) { payment ->
                        SalaryPaymentRow(payment)
                    }
                }
            }
        }
    }

    if (showPaymentDialog) {
        RecordSalaryPaymentDialog(
            onDismiss = { showPaymentDialog = false },
            onRecorded = { showPaymentDialog = false },
            viewModel = viewModel,
        )
    }
}

private val monthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

@Composable
private fun SalaryPaymentRow(payment: SalaryPaymentEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        ListItem(
            headlineContent = { Text("${monthNames[payment.forMonth - 1]} ${payment.forYear}") },
            supportingContent = { Text(payment.mode.name + (payment.notes?.let { " · $it" } ?: "")) },
            trailingContent = {
                Text(
                    "₹${"%.2f".format(payment.amount)}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordSalaryPaymentDialog(
    onDismiss: () -> Unit,
    onRecorded: () -> Unit,
    viewModel: StaffDetailViewModel,
) {
    val now = Calendar.getInstance()
    var amountText by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf(now.get(Calendar.MONTH) + 1) } // Calendar.MONTH is 0-based
    var selectedYear by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(PaymentMode.CASH) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record salary payment") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )

                TextButton(onClick = { monthMenuExpanded = true }) {
                    Text("For: ${monthNames[selectedMonth - 1]} $selectedYear")
                }
                DropdownMenu(expanded = monthMenuExpanded, onDismissRequest = { monthMenuExpanded = false }) {
                    monthNames.forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = { Text("$name $selectedYear") },
                            onClick = { selectedMonth = index + 1; monthMenuExpanded = false },
                        )
                    }
                }

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

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
            TextButton(
                enabled = !uiState.isSubmittingPayment,
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@TextButton
                    viewModel.recordPayment(
                        amount = amount,
                        forMonth = selectedMonth,
                        forYear = selectedYear,
                        mode = selectedMode,
                        notes = notes,
                        onSuccess = onRecorded,
                    )
                },
            ) {
                Text(if (uiState.isSubmittingPayment) "Saving..." else "Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
