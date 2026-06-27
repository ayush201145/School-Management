package com.schoolmgmt.app.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.local.dao.TransactionRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val totalAmount = transactions.sumOf { it.payment.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total collected", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "₹${"%.2f".format(totalAmount)}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("${transactions.size} transaction(s)", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (transactions.isEmpty()) {
                Text(
                    "No transactions recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(transactions, key = { it.payment.id }) { row ->
                        TransactionRowCard(row)
                    }
                }
            }
        }
    }
}

// Display-only formatter (device's local timezone is correct here,
// unlike IsoDates which deliberately stays in UTC for wire-format
// consistency with the backend).
private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

@Composable
private fun TransactionRowCard(row: TransactionRow) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        ListItem(
            headlineContent = { Text("${row.studentFirstName} ${row.studentLastName} · ${row.feeDescription}") },
            supportingContent = {
                val formatted = Instant.ofEpochMilli(row.payment.paidAt)
                    .atZone(ZoneId.systemDefault())
                    .format(displayFormatter)
                Text("${row.payment.mode.name} · $formatted")
            },
            trailingContent = {
                Text(
                    "₹${"%.2f".format(row.payment.amount)}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
        )
    }
}
