package com.schoolmgmt.app.ui.payments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.local.entity.PaymentMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    studentFeeId: String,
    feeDescription: String,
    suggestedAmount: Double,
    onDismiss: () -> Unit,
    onPaymentRecorded: () -> Unit,
    viewModel: RecordPaymentViewModel = hiltViewModel(),
) {
    var amountText by remember { mutableStateOf(if (suggestedAmount > 0) "%.2f".format(suggestedAmount) else "") }
    var localError by remember { mutableStateOf<String?>(null) }
    var selectedMode by remember { mutableStateOf(PaymentMode.CASH) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    var referenceNo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment") },
        text = {
            Column {
                Text(feeDescription, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))

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

                // Reference number is most useful for non-cash modes
                // (UPI transaction id, cheque number, bank ref) — still
                // optional for CASH in case a receipt number is tracked.
                OutlinedTextField(
                    value = referenceNo,
                    onValueChange = { referenceNo = it },
                    label = { Text("Reference no. (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (localError != null) {
                    Text(
                        text = localError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
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
                enabled = !uiState.isSubmitting,
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null) {
                        localError = "Enter a valid amount"
                        return@TextButton
                    }
                    localError = null
                    viewModel.recordPayment(
                        studentFeeId = studentFeeId,
                        amount = amount,
                        mode = selectedMode,
                        referenceNo = referenceNo,
                        notes = notes,
                        onSuccess = onPaymentRecorded,
                    )
                },
            ) {
                Text(if (uiState.isSubmitting) "Saving..." else "Record payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
