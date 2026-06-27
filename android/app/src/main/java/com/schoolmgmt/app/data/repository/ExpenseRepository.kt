package com.schoolmgmt.app.data.repository

import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.entity.ExpenseCategoryEntity
import com.schoolmgmt.app.data.local.entity.ExpenseEntity
import com.schoolmgmt.app.data.local.entity.PaymentMode
import com.schoolmgmt.app.data.local.entity.RecurringExpenseTemplateEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Result of generating a recurring expense for a month — mirrors the backend's response shape. */
data class GenerateExpenseResult(val created: Boolean, val expense: ExpenseEntity)

@Singleton
class ExpenseRepository @Inject constructor(
    private val db: AppDatabase,
) {
    private val expenseCategoryDao = db.expenseCategoryDao()
    private val recurringTemplateDao = db.recurringExpenseTemplateDao()
    private val expenseDao = db.expenseDao()

    fun observeCategories() = expenseCategoryDao.observeAll()
    fun observeActiveTemplates() = recurringTemplateDao.observeActive()

    fun observeExpenses(fromMillis: Long? = null, toMillis: Long? = null, categoryId: String? = null) =
        expenseDao.observeExpenses(fromMillis, toMillis, categoryId)

    suspend fun createCategory(name: String, description: String? = null): ExpenseCategoryEntity {
        val now = System.currentTimeMillis()
        val category = ExpenseCategoryEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            updatedAt = now,
        )
        expenseCategoryDao.upsert(category)
        return category
    }

    suspend fun createExpense(
        expenseCategoryId: String,
        description: String,
        amount: Double,
        mode: PaymentMode,
        recordedById: String,
        referenceNo: String? = null,
        spentAt: Long = System.currentTimeMillis(),
    ): ExpenseEntity {
        require(amount > 0) { "amount must be greater than 0" }

        val now = System.currentTimeMillis()
        val expense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            expenseCategoryId = expenseCategoryId,
            description = description,
            amount = amount,
            spentAt = spentAt,
            mode = mode,
            referenceNo = referenceNo,
            recordedById = recordedById,
            updatedAt = now,
        )
        expenseDao.upsert(expense)
        return expense
    }

    suspend fun createRecurringTemplate(
        expenseCategoryId: String,
        label: String,
        amount: Double,
        dayOfMonth: Int = 1,
    ): RecurringExpenseTemplateEntity {
        require(amount > 0) { "amount must be greater than 0" }

        val now = System.currentTimeMillis()
        val template = RecurringExpenseTemplateEntity(
            id = UUID.randomUUID().toString(),
            expenseCategoryId = expenseCategoryId,
            label = label,
            amount = amount,
            dayOfMonth = dayOfMonth,
            updatedAt = now,
        )
        recurringTemplateDao.upsert(template)
        return template
    }

    /**
     * Generates a real Expense from a recurring template for the given
     * month, IF one doesn't already exist for that period — the LOCAL
     * mirror of expenseController.generateRecurringExpense, which was
     * verified with a real idempotency test on the backend (re-clicking
     * "generate" never double-creates).
     */
    suspend fun generateForMonth(templateId: String, recordedById: String, month: Int, year: Int): GenerateExpenseResult {
        val template = recurringTemplateDao.getById(templateId)
            ?: throw IllegalArgumentException("Recurring template $templateId not found")

        val periodStart = MonthBoundary.startMillis(year, month)
        val periodEnd = MonthBoundary.startMillis(year, month + 1) // exclusive upper bound; verified rollover via MonthBoundary

        val existing = expenseDao.findGeneratedForPeriod(templateId, periodStart, periodEnd)
        if (existing != null) {
            return GenerateExpenseResult(created = false, expense = existing)
        }

        val spentAt = MonthBoundary.dayInMonthMillis(year, month, template.dayOfMonth.coerceAtMost(28))
        val now = System.currentTimeMillis()
        val expense = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            expenseCategoryId = template.expenseCategoryId,
            recurringTemplateId = template.id,
            description = template.label,
            amount = template.amount,
            spentAt = spentAt,
            mode = PaymentMode.BANK_TRANSFER, // sensible default for recurring bills; editable after creation
            recordedById = recordedById,
            updatedAt = now,
        )
        expenseDao.upsert(expense)
        return GenerateExpenseResult(created = true, expense = expense)
    }
}
