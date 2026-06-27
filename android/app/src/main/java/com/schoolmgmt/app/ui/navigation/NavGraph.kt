package com.schoolmgmt.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.schoolmgmt.app.ui.auth.LoginScreen
import com.schoolmgmt.app.ui.dashboard.DashboardScreen
import com.schoolmgmt.app.ui.dues.DuesScreen
import com.schoolmgmt.app.ui.expenses.ExpensesScreen
import com.schoolmgmt.app.ui.feestructures.FeeStructuresScreen
import com.schoolmgmt.app.ui.inventory.InventoryScreen
import com.schoolmgmt.app.ui.reports.MonthlyReportScreen
import com.schoolmgmt.app.ui.staff.StaffDetailScreen
import com.schoolmgmt.app.ui.staff.StaffListScreen
import com.schoolmgmt.app.ui.students.StudentDetailScreen
import com.schoolmgmt.app.ui.students.StudentListScreen
import com.schoolmgmt.app.ui.teachers.TeacherListScreen
import com.schoolmgmt.app.ui.transactions.TransactionsScreen

/** Centralized route definitions — avoids magic strings scattered across screens. */
object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val STUDENT_LIST = "students"
    const val STUDENT_DETAIL = "students/{studentId}"
    const val TEACHER_LIST = "teachers"
    const val DUES_REPORT = "dues"
    const val TRANSACTIONS = "transactions"
    const val INVENTORY = "inventory"
    const val FEE_STRUCTURES = "fee-structures"
    const val STAFF_LIST = "staff"
    const val STAFF_DETAIL = "staff/{staffId}"
    const val EXPENSES = "expenses"
    const val MONTHLY_REPORT = "monthly-report"

    fun studentDetail(studentId: String) = "students/$studentId"
    fun staffDetail(staffId: String) = "staff/$staffId"
}

@Composable
fun SchoolManagementNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        // Clear login off the back stack — pressing back
                        // from the dashboard should exit the app, not
                        // return to the login screen.
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToStudents = { navController.navigate(Routes.STUDENT_LIST) },
                onNavigateToTeachers = { navController.navigate(Routes.TEACHER_LIST) },
                onNavigateToDues = { navController.navigate(Routes.DUES_REPORT) },
                onNavigateToTransactions = { navController.navigate(Routes.TRANSACTIONS) },
                onNavigateToInventory = { navController.navigate(Routes.INVENTORY) },
                onNavigateToFeeStructures = { navController.navigate(Routes.FEE_STRUCTURES) },
                onNavigateToStaff = { navController.navigate(Routes.STAFF_LIST) },
                onNavigateToExpenses = { navController.navigate(Routes.EXPENSES) },
                onNavigateToMonthlyReport = { navController.navigate(Routes.MONTHLY_REPORT) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true } // clear entire back stack on logout
                    }
                },
            )
        }

        composable(Routes.STUDENT_LIST) {
            StudentListScreen(
                onStudentClick = { studentId ->
                    navController.navigate(Routes.studentDetail(studentId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.STUDENT_DETAIL) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: return@composable
            StudentDetailScreen(
                studentId = studentId,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.TEACHER_LIST) {
            TeacherListScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.DUES_REPORT) {
            DuesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.TRANSACTIONS) {
            TransactionsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.INVENTORY) {
            InventoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.FEE_STRUCTURES) {
            FeeStructuresScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.STAFF_LIST) {
            StaffListScreen(
                onStaffClick = { staffId -> navController.navigate(Routes.staffDetail(staffId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.STAFF_DETAIL) { backStackEntry ->
            val staffId = backStackEntry.arguments?.getString("staffId") ?: return@composable
            StaffDetailScreen(staffId = staffId, onBack = { navController.popBackStack() })
        }

        composable(Routes.EXPENSES) {
            ExpensesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.MONTHLY_REPORT) {
            MonthlyReportScreen(onBack = { navController.popBackStack() })
        }
    }
}
