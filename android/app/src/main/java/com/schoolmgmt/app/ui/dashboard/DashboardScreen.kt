package com.schoolmgmt.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schoolmgmt.app.data.local.entity.UserRole

private data class DashboardTile(
    val label: String,
    val icon: ImageVector,
    val visibleTo: Set<UserRole>,
    val gradient: Brush,
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
    onNavigateToAttendance: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val role by viewModel.role.collectAsState()
    val academicYears by viewModel.academicYears.collectAsState()
    val selectedYearId by viewModel.selectedYearId.collectAsState()

    val activeYear = academicYears.firstOrNull { it.id == selectedYearId }
        ?: academicYears.firstOrNull { it.isCurrent }
        ?: academicYears.firstOrNull()

    var bannerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        bannerVisible = true
    }

    val allTiles = listOf(
        DashboardTile("Students", Icons.Filled.People, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT, UserRole.TEACHER), Brush.linearGradient(listOf(Color(0xFF3F51B5), Color(0xFF2196F3))), onNavigateToStudents),
        DashboardTile("Teachers", Icons.Filled.Person, setOf(UserRole.ADMIN), Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63))), onNavigateToTeachers),
        DashboardTile("Fee Dues", Icons.Filled.Checklist, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), Brush.linearGradient(listOf(Color(0xFFFF5722), Color(0xFFFF9800))), onNavigateToDues),
        DashboardTile("Transactions", Icons.Filled.Receipt, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), Brush.linearGradient(listOf(Color(0xFF009688), Color(0xFF4CAF50))), onNavigateToTransactions),
        DashboardTile("Inventory", Icons.Filled.Inventory, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), Brush.linearGradient(listOf(Color(0xFF607D8B), Color(0xFF90A4AE))), onNavigateToInventory),
        DashboardTile("Fee Structures", Icons.Filled.AccountBalance, setOf(UserRole.ADMIN), Brush.linearGradient(listOf(Color(0xFF673AB7), Color(0xFF3F51B5))), onNavigateToFeeStructures),
        DashboardTile("Staff", Icons.Filled.Person, setOf(UserRole.ADMIN), Brush.linearGradient(listOf(Color(0xFF795548), Color(0xFFA1887F))), onNavigateToStaff),
        DashboardTile("Expenses", Icons.Filled.Receipt, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFFD81B60))), onNavigateToExpenses),
        DashboardTile("Monthly Report", Icons.Filled.AccountBalance, setOf(UserRole.ADMIN, UserRole.ACCOUNTANT), Brush.linearGradient(listOf(Color(0xFF00ACC1), Color(0xFF00838F))), onNavigateToMonthlyReport),
        DashboardTile("Attendance", Icons.Filled.DateRange, setOf(UserRole.ADMIN, UserRole.TEACHER), Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))), onNavigateToAttendance),
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
            // Welcome School Banner Header
            AnimatedVisibility(
                visible = bannerVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = "Welcome",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.graphicsLayer(scaleX = 1.3f, scaleY = 1.3f)
                        )
                        Column {
                            Text(
                                text = "School Management Hub",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Offline-first sync engine connected to Neon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Academic Year Selector Dropdown
            var dropdownExpanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Card(
        onClick = tile.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(tile.gradient, shape = CardDefaults.shape),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = tile.label,
                tint = Color.White,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = tile.label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
    }
}
