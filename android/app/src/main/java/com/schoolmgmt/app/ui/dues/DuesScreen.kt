package com.schoolmgmt.app.ui.dues

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
import com.schoolmgmt.app.data.local.dao.DueRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuesScreen(
    onBack: () -> Unit,
    viewModel: DuesViewModel = hiltViewModel(),
) {
    val dues by viewModel.dues.collectAsState(initial = emptyList())
    val totalOutstanding = dues.sumOf { it.amount - it.discount - it.paidAmount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fee Dues") },
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
                    Text("Total outstanding", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "₹${"%.2f".format(totalOutstanding)}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text("${dues.size} fee(s) pending", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (dues.isEmpty()) {
                Text(
                    "No dues — everything is paid up.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(dues, key = { it.feeId }) { due ->
                        DueRowCard(due)
                    }
                }
            }
        }
    }
}

@Composable
private fun DueRowCard(due: DueRow) {
    val balance = due.amount - due.discount - due.paidAmount
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        ListItem(
            headlineContent = { Text("${due.studentFirstName} ${due.studentLastName}") },
            supportingContent = {
                Text("${due.description} · Admission No: ${due.admissionNo}" + (due.guardianPhone?.let { " · $it" } ?: ""))
            },
            trailingContent = {
                Text(
                    "₹${"%.2f".format(balance)}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
        )
    }
}
