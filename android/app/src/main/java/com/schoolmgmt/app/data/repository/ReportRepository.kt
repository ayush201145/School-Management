package com.schoolmgmt.app.data.repository

import com.schoolmgmt.app.data.local.AppDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * THE MONTHLY FINANCIAL REPORT, computed locally — mirrors
 * backend/src/controllers/reportController.getMonthlyReport exactly,
 * including the arithmetic verified there with a real test:
 *   Net for month = Cash Collected - Expenses - Salaries
 *
 * Deliberately called "Net for the month", NOT "profit" — see the
 * identical naming note on the backend's getMonthlyReport. This is a
 * simple cash-basis subtraction, not an accounting/tax-grade profit
 * figure, and shouldn't be presented as audited or tax-significant.
 */
data class MonthlyReport(
    val month: Int,
    val year: Int,
    val cashCollected: Double,
    val collectedByMode: Map<String, Double>,
    val totalExpenses: Double,
    val expensesByCategory: Map<String, Double>,
    val totalSalaries: Double,
    val netForMonth: Double,
)

@Singleton
class ReportRepository @Inject constructor(
    private val db: AppDatabase,
) {
    private val paymentDao = db.paymentDao()
    private val expenseDao = db.expenseDao()
    private val salaryPaymentDao = db.salaryPaymentDao()

    suspend fun getMonthlyReport(month: Int, year: Int): MonthlyReport {
        require(month in 1..12) { "month must be 1-12" }

        val periodStart = MonthBoundary.startMillis(year, month)
        val periodEnd = MonthBoundary.startMillis(year, month + 1) // exclusive upper bound

        val cashCollected = paymentDao.getTotalCollected(periodStart, periodEnd)
        val collectedByMode = paymentDao.getCollectedByMode(periodStart, periodEnd)
            .associate { it.mode to it.total }

        val totalExpenses = expenseDao.getTotalSpent(periodStart, periodEnd)
        val expensesByCategory = expenseDao.getSpentByCategory(periodStart, periodEnd)
            .associate { it.categoryName to it.total }

        val totalSalaries = salaryPaymentDao.getTotalPaidInRange(periodStart, periodEnd)

        return MonthlyReport(
            month = month,
            year = year,
            cashCollected = cashCollected,
            collectedByMode = collectedByMode,
            totalExpenses = totalExpenses,
            expensesByCategory = expensesByCategory,
            totalSalaries = totalSalaries,
            netForMonth = cashCollected - totalExpenses - totalSalaries,
        )
    }
}
