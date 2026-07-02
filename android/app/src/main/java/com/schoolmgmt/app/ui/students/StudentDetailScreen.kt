package com.schoolmgmt.app.ui.students

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.local.entity.AcademicYearEntity
import com.schoolmgmt.app.data.local.entity.FeeStatus
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import com.schoolmgmt.app.data.local.entity.WithdrawalReason
import com.schoolmgmt.app.ui.payments.RecordPaymentDialog
import com.schoolmgmt.app.ui.payments.RecordBulkPaymentDialog
import com.schoolmgmt.app.ui.payments.ReceiptOptionsDialog
import com.schoolmgmt.app.ui.purchases.PurchaseItemDialog
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class ReceiptData(
    val receiptNo: String,
    val paidAmount: Double,
    val mode: String,
    val studentName: String,
    val admissionNo: String,
    val className: String,
    val items: List<com.schoolmgmt.app.data.repository.ReceiptItem>,
    val remainingDues: Double,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    studentId: String,
    onBack: () -> Unit,
    viewModel: StudentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val academicYears by viewModel.academicYears.collectAsState()
    
    val scope = rememberCoroutineScope()
    var receiptOptionsData by remember { mutableStateOf<ReceiptData?>(null) }
    
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showMigrateDialog by remember { mutableStateOf(false) }
    var feeForPayment by remember { mutableStateOf<StudentFeeEntity?>(null) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var showBulkPaymentDialog by remember { mutableStateOf(false) }
    var showDefaultConfirmFeeId by remember { mutableStateOf<String?>(null) }

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
                    student.tuitionFee?.let { Text("Custom Tuition Fee: ₹${"%.2f".format(it)}", style = MaterialTheme.typography.bodyMedium) }

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
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = { showWithdrawDialog = true }) {
                                Text("Mark as withdrawn")
                            }
                            TextButton(onClick = { showMigrateDialog = true }) {
                                Text("Migrate/Promote")
                            }
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
                    FeeRow(
                        fee = fee,
                        onClick = { if (fee.status != FeeStatus.PAID && !fee.isDefaulted) feeForPayment = fee },
                        onMarkDefault = { showDefaultConfirmFeeId = fee.id }
                    )
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

    if (showDefaultConfirmFeeId != null) {
        AlertDialog(
            onDismissRequest = { showDefaultConfirmFeeId = null },
            title = { Text("Mark as Defaulted") },
            text = { Text("Are you sure you want to mark this fee as defaulted? This will write off the unpaid balance locally and sync it with the server.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDefaultConfirmFeeId?.let { viewModel.markFeeAsDefaulted(it) }
                        showDefaultConfirmFeeId = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDefaultConfirmFeeId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMigrateDialog && uiState.student != null) {
        MigrateStudentDialog(
            currentTuitionFee = uiState.student?.tuitionFee,
            academicYears = academicYears,
            viewModel = viewModel,
            onDismiss = { showMigrateDialog = false },
            onConfirm = { targetClassId, targetTuitionFee ->
                viewModel.migrateStudent(targetClassId, targetTuitionFee) {
                    showMigrateDialog = false
                }
            }
        )
    }

    feeForPayment?.let { fee ->
        val student = uiState.student
        var remainingBalance by remember(fee.id) { mutableStateOf<Double?>(null) }
        LaunchedEffect(fee.id) {
            remainingBalance = viewModel.getRemainingBalance(fee.id)
        }

        if (remainingBalance != null && student != null) {
            RecordPaymentDialog(
                studentFeeId = fee.id,
                feeDescription = fee.description,
                suggestedAmount = remainingBalance ?: 0.0,
                onDismiss = { feeForPayment = null },
                onPaymentRecorded = { receiptNo, paidAmount, mode ->
                    feeForPayment = null
                    scope.launch {
                        val cName = viewModel.getClassNameForSection(student.sectionId)
                        val totalDues = viewModel.getTotalDuesBalance()
                        receiptOptionsData = ReceiptData(
                            receiptNo = receiptNo,
                            paidAmount = paidAmount,
                            mode = mode,
                            studentName = "${student.firstName} ${student.lastName}",
                            admissionNo = student.admissionNo,
                            className = cName,
                            items = listOf(com.schoolmgmt.app.data.repository.ReceiptItem(fee.description, paidAmount)),
                            remainingDues = totalDues
                        )
                    }
                },
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
        val student = uiState.student
        var totalOutstanding by remember { mutableStateOf<Double?>(null) }
        LaunchedEffect(studentId) {
            totalOutstanding = viewModel.getTotalDuesBalance()
        }

        if (totalOutstanding != null && student != null) {
            RecordBulkPaymentDialog(
                studentId = studentId,
                suggestedAmount = totalOutstanding ?: 0.0,
                onDismiss = { showBulkPaymentDialog = false },
                onPaymentRecorded = { receiptNo, paidAmount, mode, items ->
                    showBulkPaymentDialog = false
                    scope.launch {
                        val cName = viewModel.getClassNameForSection(student.sectionId)
                        val totalDues = viewModel.getTotalDuesBalance()
                        receiptOptionsData = ReceiptData(
                            receiptNo = receiptNo,
                            paidAmount = paidAmount,
                            mode = mode,
                            studentName = "${student.firstName} ${student.lastName}",
                            admissionNo = student.admissionNo,
                            className = cName,
                            items = items,
                            remainingDues = totalDues
                        )
                    }
                }
            )
        }
    }

    receiptOptionsData?.let { data ->
        ReceiptOptionsDialog(
            receiptNo = data.receiptNo,
            studentName = data.studentName,
            admissionNo = data.admissionNo,
            className = data.className,
            items = data.items,
            paidAmount = data.paidAmount,
            mode = data.mode,
            remainingDues = data.remainingDues,
            onDismiss = { receiptOptionsData = null }
        )
    }
}

@Composable
private fun FeeRow(
    fee: StudentFeeEntity, 
    onClick: () -> Unit,
    onMarkDefault: () -> Unit
) {
    Card(
        onClick = { if (!fee.isDefaulted) onClick() },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = if (fee.isDefaulted) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) else CardDefaults.cardColors()
    ) {
        ListItem(
            headlineContent = { Text(fee.description) },
            supportingContent = { 
                val supportText = if (fee.isDefaulted) "Defaulted (Write-off)" else "₹${fee.amount} · ${fee.status.name}"
                Text(supportText)
            },
            trailingContent = {
                if (fee.isDefaulted) {
                    Text("DEFAULTED", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                } else {
                    val color = when (fee.status) {
                        FeeStatus.PAID -> MaterialTheme.colorScheme.primary
                        FeeStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
                        FeeStatus.UNPAID -> MaterialTheme.colorScheme.error
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(fee.status.name, color = color, style = MaterialTheme.typography.labelMedium)
                        if (fee.status != FeeStatus.PAID) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = onMarkDefault) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Mark Default",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
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
    var reason by remember { mutableStateOf(WithdrawalReason.TRANSFERRED) }
    var notes by remember { mutableStateOf("") }
    var reasonExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw student") },
        text = {
            Column {
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = reason.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { reasonExpanded = true }
                    )
                    DropdownMenu(
                        expanded = reasonExpanded,
                        onDismissRequest = { reasonExpanded = false }
                    ) {
                        WithdrawalReason.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    reason = option
                                    reasonExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason, notes.ifBlank { null }) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MigrateStudentDialog(
    currentTuitionFee: Double?,
    academicYears: List<AcademicYearEntity>,
    viewModel: StudentDetailViewModel,
    onDismiss: () -> Unit,
    onConfirm: (classId: String, tuitionFee: Double?) -> Unit,
) {
    var selectedYearId by remember { mutableStateOf<String?>(null) }
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var selectedClassLabel by remember { mutableStateOf("") }
    var tuitionFeeStr by remember { mutableStateOf(currentTuitionFee?.toString() ?: "") }
    
    var yearExpanded by remember { mutableStateOf(false) }
    var classExpanded by remember { mutableStateOf(false) }
    
    val classes by if (selectedYearId == null) {
        flowOf(emptyList())
    } else {
        viewModel.observeClassesForYear(selectedYearId!!)
    }.collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Migrate/Promote Student") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Select target academic year and class. You can optionally specify a custom tuition fee override.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Year selector dropdown
                val selectedYearLabel = academicYears.firstOrNull { it.id == selectedYearId }?.label ?: "Select Academic Year"
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = selectedYearLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Academic Year") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { yearExpanded = true }
                    )
                    DropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        academicYears.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.label) },
                                onClick = {
                                    selectedYearId = year.id
                                    selectedClassId = null
                                    selectedClassLabel = ""
                                    yearExpanded = false
                                }
                            )
                        }
                    }
                }

                // Class selector dropdown
                if (selectedYearId != null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        OutlinedTextField(
                            value = selectedClassLabel.ifBlank { "Select Class" },
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
                            classes.forEach { schoolClass ->
                                DropdownMenuItem(
                                    text = { Text(schoolClass.name) },
                                    onClick = {
                                        selectedClassId = schoolClass.id
                                        selectedClassLabel = schoolClass.name
                                        classExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Custom tuition fee override input field
                OutlinedTextField(
                    value = tuitionFeeStr,
                    onValueChange = { tuitionFeeStr = it },
                    label = { Text("Custom Tuition Fee Override (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedClassId != null,
                onClick = {
                    val parsedFee = tuitionFeeStr.toDoubleOrNull()
                    onConfirm(selectedClassId!!, parsedFee)
                }
            ) {
                Text("Migrate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
