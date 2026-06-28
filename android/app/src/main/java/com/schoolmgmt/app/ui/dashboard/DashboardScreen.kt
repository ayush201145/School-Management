package com.schoolmgmt.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.local.entity.UserRole

private data class DashboardTile(
    val label: String,
    val icon: ImageVector,
    val visibleTo: Set<UserRole>,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToStudents: () -> Unit,
    onNavigateToTeachers: () -> Unit,
    onNavigateToDues: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToFeeStructures: () -> Unit,
    onNavigateToStaff: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToMonthlyReport: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val role by viewModel.role.collectAsState()
    val academicYears by viewModel.academicYears.collectAsState()
    val selectedYearId by viewModel.selectedYearId.collectAsState()

    val activeYear = academicYears.firstOrNull { it.id == selectedYearId }
        ?: academicYears.firstOrNull { it.isCurrent }
        ?: academicYears.firstOrNull()

    val allTiles = listOf(
        DashboardTile("Students", Icons.Filled.People, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT, UserRole.TEACHER), onNavigateToStudents),
        DashboardTile("Teachers", Icons.Filled.Person, setOf(UserRole.ADMIN), onNavigateToTeachers),
        DashboardTile("Fee Dues", Icons.Filled.Checklist, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), onNavigateToDues),
        DashboardTile("Transactions", Icons.Filled.Receipt, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), onNavigateToTransactions),
        DashboardTile("Inventory", Icons.Filled.Inventory, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), onNavigateToInventory),
        DashboardTile("Fee Structures", Icons.Filled.AccountBalance, setOf(UserRole.ADMIN), onNavigateToFeeStructures),
        DashboardTile("Staff", Icons.Filled.Person, setOf(UserRole.ADMIN), onNavigateToStaff),
        DashboardTile("Expenses", Icons.Filled.Receipt, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), onNavigateToExpenses),
        DashboardTile("Monthly Report", Icons.Filled.AccountBalance, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), onNavigateToMonthlyReport),
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { viewModel.syncNow() }) {
                        Icon(Icons.Filled.Sync, contentDescription = "Sync now")
                    }
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log out")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
            )
        },
    ) { padding ->
        if (role == null) {
            return@Scaffold
        }

        val visibleTiles = allTiles.filter { role in it.visibleTo }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Academic Year Selector Dropdown
            var dropdownExpanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { dropdownExpanded = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Active Academic Year", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = activeYear?.label ?: "Loading...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box {
                        IconButton(onClick = { dropdownExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.ExpandMore,
                                contentDescription = "Select Academic Year"
                            )
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            academicYears.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year.label + if (year.isCurrent) " (Current)" else "") },
                                    onClick = {
                                        viewModel.selectYearId(year.id)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visibleTiles) { tile ->
                    DashboardTileCard(tile)
                }
            }
        }
    }
}

@Composable
private fun DashboardTileCard(tile: DashboardTile) {
    Card(
        onClick = tile.onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = tile.label,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(text = tile.label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
