package com.schoolmgmt.app.ui.reports

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
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.repository.MonthlyReport
import java.util.Calendar

private val monthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    onBack: () -> Unit,
    viewModel: MonthlyReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var monthMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TextButton(
                onClick = { monthMenuExpanded = true },
                modifier = Modifier.padding(16.dp),
            ) {
                Text("${monthNames[uiState.month - 1]} ${uiState.year}")
            }
            DropdownMenu(expanded = monthMenuExpanded, onDismissRequest = { monthMenuExpanded = false }) {
                // Last 12 months from now, most recent first — covers
                // the realistic range an office would actually look
                // back over without needing a full date-range picker.
                val now = Calendar.getInstance()
                for (i in 0 until 12) {
                    val cal = now.clone() as Calendar
                    cal.add(Calendar.MONTH, -i)
                    val m = cal.get(Calendar.MONTH) + 1
                    val y = cal.get(Calendar.YEAR)
                    DropdownMenuItem(
                        text = { Text("${monthNames[m - 1]} $y") },
                        onClick = { viewModel.selectMonth(m, y); monthMenuExpanded = false },
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Text(
                    uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }

            uiState.report?.let { report ->
                ReportContent(report)
            }
        }
    }
}

@Composable
private fun ReportContent(report: MonthlyReport) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                SummaryLine("Cash collected", report.cashCollected, MaterialTheme.colorScheme.primary)
                SummaryLine("Expenses", -report.totalExpenses, MaterialTheme.colorScheme.error)
                SummaryLine("Salaries", -report.totalSalaries, MaterialTheme.colorScheme.error)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SummaryLine(
                    "Net for the month",
                    report.netForMonth,
                    if (report.netForMonth >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    emphasize = true,
                )
                Text(
                    // Deliberately not calling this "profit" in the UI —
                    // see the identical naming note on the backend and
                    // ReportRepository: this is a simple cash-basis
                    // subtraction, not an accounting/tax-grade figure.
                    "Cash collected minus expenses and salaries this month.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        if (report.collectedByMode.isNotEmpty()) {
            Text(
                "Collected by mode",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            BreakdownList(report.collectedByMode)
        }

        if (report.expensesByCategory.isNotEmpty()) {
            Text(
                "Expenses by category",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            BreakdownList(report.expensesByCategory)
        }
    }
}

@Composable
private fun SummaryLine(label: String, amount: Double, color: Color, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
        Text(
            "₹${"%.2f".format(amount)}",
            color = color,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BreakdownList(breakdown: Map<String, Double>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items(breakdown.entries.toList()) { (label, amount) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text("₹${"%.2f".format(amount)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
