package com.schoolmgmt.app.ui.feestructures

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.local.entity.FeeStructureEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeeStructuresScreen(
    onBack: () -> Unit,
    viewModel: FeeStructuresViewModel = hiltViewModel(),
) {
    val classes by viewModel.classes.collectAsState()
    val selectedClassId by viewModel.currentSelectedClassId.collectAsState()
    val structures by viewModel.structures.collectAsState()
    val assignResult by viewModel.lastAssignResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fee Structures") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(classes, key = { it.id }) { schoolClass ->
                    FilterChip(
                        selected = schoolClass.id == selectedClassId,
                        onClick = { viewModel.selectClass(schoolClass.id) },
                        label = { Text(schoolClass.name) },
                    )
                }
            }

            if (selectedClassId == null) {
                Text(
                    "Pick a class above to see its fee structures.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else if (structures.isEmpty()) {
                Text(
                    "No fee structures defined for this class yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(structures, key = { it.id }) { structure ->
                        FeeStructureRow(
                            structure = structure,
                            onAssign = { viewModel.assignToClass(structure.id) },
                        )
                    }
                }
            }
        }
    }

    if (assignResult != null) {
        val result = assignResult!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissAssignResult() },
            title = { Text("Fee assigned") },
            text = {
                Text(
                    "Billed ${result.created} student(s)." +
                        if (result.skipped > 0) " ${result.skipped} already had this fee and were skipped." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAssignResult() }) { Text("OK") }
            },
        )
    }
}

@Composable
private fun FeeStructureRow(structure: FeeStructureEntity, onAssign: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            ListItem(
                headlineContent = { Text(structure.description ?: "Fee") },
                supportingContent = { Text("₹${structure.amount}") },
            )
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Button(onClick = onAssign) {
                    Text("Assign to all students in class")
                }
            }
        }
    }
}
