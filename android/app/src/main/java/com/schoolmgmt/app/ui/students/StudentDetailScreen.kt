package com.schoolmgmt.app.ui.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.local.entity.FeeStatus
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import com.schoolmgmt.app.data.local.entity.WithdrawalReason
import com.schoolmgmt.app.ui.payments.RecordPaymentDialog
import com.schoolmgmt.app.ui.payments.RecordBulkPaymentDialog
import com.schoolmgmt.app.ui.purchases.PurchaseItemDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    studentId: String,
    onBack: () -> Unit,
    viewModel: StudentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var feeForPayment by remember { mutableStateOf<StudentFeeEntity?>(null) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showBulkPaymentDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.student?.let { "${it.firstName} ${it.lastName}" } ?: "Student") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val student = uiState.student
        if (student == null) {
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Admission No: ${student.admissionNo}", style = MaterialTheme.typography.bodyMedium)
                    student.guardianName?.let { Text("Guardian: $it", style = MaterialTheme.typography.bodyMedium) }
                    student.guardianPhone?.let { Text("Phone: $it", style = MaterialTheme.typography.bodyMedium) }

                    if (!student.isActive && student.withdrawalReason != null) {
                        Text(
                            text = "Withdrawn — ${student.withdrawalReason.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        TextButton(onClick = { viewModel.reinstate() }) {
                            Text("Reinstate student")
                        }
                    } else {
                        TextButton(onClick = { showWithdrawDialog = true }) {
                            Text("Mark as withdrawn")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Fees", style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(onClick = { showBulkPaymentDialog = true }) {
                        Text("Pay Multiple Fees")
                    }
                    TextButton(onClick = { showPurchaseDialog = true }) {
                        Text("Sell book / uniform")
                    }
                }
            }
            LazyColumn {
                items(uiState.fees, key = { it.id }) { fee ->
                    FeeRow(fee, onClick = { if (fee.status != FeeStatus.PAID) feeForPayment = fee })
                }
            }
        }
    }

    if (showWithdrawDialog) {
        WithdrawDialog(
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { reason, notes ->
                viewModel.withdraw(reason, notes) { showWithdrawDialog = false }
            },
        )
    }

    feeForPayment?.let { fee ->
        var remainingBalance by remember(fee.id) { mutableStateOf<Double?>(null) }
        LaunchedEffect(fee.id) {
            remainingBalance = viewModel.getRemainingBalance(fee.id)
        }

        // Wait for the real remaining-balance lookup before showing the
        // dialog, rather than briefly flashing a wrong suggested amount
        // (the overpayment guard would still catch a bad value either
        // way, but a correct default avoids the person needing to
        // notice and fix it themselves on every partial payment).
        if (remainingBalance != null) {
            RecordPaymentDialog(
                studentFeeId = fee.id,
                feeDescription = fee.description,
                suggestedAmount = remainingBalance ?: 0.0,
                onDismiss = { feeForPayment = null },
                onPaymentRecorded = { feeForPayment = null },
            )
        }
    }

    if (showPurchaseDialog) {
        PurchaseItemDialog(
            studentId = studentId,
            onDismiss = { showPurchaseDialog = false },
            onPurchaseComplete = { showPurchaseDialog = false },
        )
    }

    if (showBulkPaymentDialog) {
        var totalOutstanding by remember { mutableStateOf<Double?>(null) }
        LaunchedEffect(studentId) {
            totalOutstanding = viewModel.getTotalDuesBalance()
        }

        if (totalOutstanding != null) {
            RecordBulkPaymentDialog(
                studentId = studentId,
                suggestedAmount = totalOutstanding ?: 0.0,
                onDismiss = { showBulkPaymentDialog = false },
                onPaymentRecorded = { showBulkPaymentDialog = false }
            )
        }
    }
}

@Composable
private fun FeeRow(fee: StudentFeeEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        ListItem(
            headlineContent = { Text(fee.description) },
            supportingContent = { Text("₹${fee.amount} · ${fee.status.name}") },
            trailingContent = {
                val color = when (fee.status) {
                    FeeStatus.PAID -> MaterialTheme.colorScheme.primary
                    FeeStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
                    FeeStatus.UNPAID -> MaterialTheme.colorScheme.error
                }
                Text(fee.status.name, color = color, style = MaterialTheme.typography.labelMedium)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithdrawDialog(
    onDismiss: () -> Unit,
    onConfirm: (WithdrawalReason, String?) -> Unit,
) {
    var selectedReason by remember { mutableStateOf(WithdrawalReason.TRANSFERRED) }
    var notes by remember { mutableStateOf("") }
    var reasonMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark student as withdrawn") },
        text = {
            Column {
                Text(
                    "This keeps the student's full fee and payment history. " +
                        "They'll stop appearing in active rosters and dues reports.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                TextButton(onClick = { reasonMenuExpanded = true }) {
                    Text("Reason: ${selectedReason.name}")
                }
                DropdownMenu(expanded = reasonMenuExpanded, onDismissRequest = { reasonMenuExpanded = false }) {
                    WithdrawalReason.entries.forEach { reason ->
                        DropdownMenuItem(
                            text = { Text(reason.name) },
                            onClick = { selectedReason = reason; reasonMenuExpanded = false },
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedReason, notes.ifBlank { null }) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
