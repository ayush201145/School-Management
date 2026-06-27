package com.schoolmgmt.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
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

    // All tiles defined once with their role visibility, then filtered —
    // adding a new tile means adding one entry here, not branching logic
    // scattered through the layout.
    val allTiles = listOf(
        DashboardTile("Students", Icons.Filled.People, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT, UserRole.TEACHER), onNavigateToStudents),
        DashboardTile("Teachers", Icons.Filled.Person, setOf(UserRole.ADMIN), onNavigateToTeachers),
        DashboardTile("Fee Dues", Icons.Filled.Checklist, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), onNavigateToDues),
        DashboardTile("Transactions", Icons.Filled.Receipt, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), onNavigateToTransactions),
        DashboardTile("Inventory", Icons.Filled.Inventory, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), onNavigateToInventory),
        DashboardTile("Fee Structures", Icons.Filled.AccountBalance, setOf(UserRole.ADMIN), onNavigateToFeeStructures),
        // Reusing already-verified icons rather than introducing new,
        // unverified Material icon names (Person for staff, Receipt
        // for expenses, AccountBalance for the report — same icons as
        // above, distinguishable by label since this app prioritizes
        // function over icon variety).
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
            // Loading the cached role from DataStore — brief, but real;
            // showing nothing rather than a wrong tile set avoids a
            // flash of incorrect content.
            return@Scaffold
        }

        val visibleTiles = allTiles.filter { role in it.visibleTo }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(visibleTiles) { tile ->
                DashboardTileCard(tile)
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
