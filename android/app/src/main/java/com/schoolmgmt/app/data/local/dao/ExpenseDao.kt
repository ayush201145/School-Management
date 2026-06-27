package com.schoolmgmt.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.schoolmgmt.app.data.local.entity.ExpenseCategoryEntity
import com.schoolmgmt.app.data.local.entity.ExpenseEntity
import com.schoolmgmt.app.data.local.entity.RecurringExpenseTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: ExpenseCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<ExpenseCategoryEntity>)

    @Query("SELECT * FROM expense_categories WHERE isDeleted = 0 ORDER BY name ASC")
    fun observeAll(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<ExpenseCategoryEntity>

    @Query("UPDATE expense_categories SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

@Dao
interface RecurringExpenseTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: RecurringExpenseTemplateEntity)

    @Query("SELECT * FROM recurring_expense_templates WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): RecurringExpenseTemplateEntity?

    @Query("SELECT * FROM recurring_expense_templates WHERE isActive = 1 AND isDeleted = 0 ORDER BY label ASC")
    fun observeActive(): Flow<List<RecurringExpenseTemplateEntity>>

    @Query("SELECT * FROM recurring_expense_templates WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<RecurringExpenseTemplateEntity>

    @Query("UPDATE recurring_expense_templates SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

/** One expense category's total within a date range — for the monthly report's by-category breakdown. */
data class CategoryTotal(val categoryName: String, val total: Double)

/** One expense row joined with its category name, for list display. */
data class ExpenseWithCategory(
    @Embedded val expense: ExpenseEntity,
    val categoryName: String,
)

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: ExpenseEntity)

    /**
     * Checks whether a recurring template already generated an expense
     * for the given month/year — the LOCAL mirror of the backend's
     * idempotency check in expenseController.generateRecurringExpense
     * (verified there with a real test: re-generating never double-creates).
     */
    @Query(
        """
        SELECT * FROM expenses 
        WHERE recurringTemplateId = :templateId 
        AND spentAt >= :periodStartMillis AND spentAt < :periodEndMillis 
        AND isDeleted = 0
        LIMIT 1
        """
    )
    suspend fun findGeneratedForPeriod(templateId: String, periodStartMillis: Long, periodEndMillis: Long): ExpenseEntity?

    @Query(
        """
        SELECT e.*, c.name AS categoryName
        FROM expenses e
        INNER JOIN expense_categories c ON c.id = e.expenseCategoryId
        WHERE e.isDeleted = 0
          AND (:fromMillis IS NULL OR e.spentAt >= :fromMillis)
          AND (:toMillis IS NULL OR e.spentAt <= :toMillis)
          AND (:categoryId IS NULL OR e.expenseCategoryId = :categoryId)
        ORDER BY e.spentAt DESC
        """
    )
    fun observeExpenses(
        fromMillis: Long? = null,
        toMillis: Long? = null,
        categoryId: String? = null,
    ): Flow<List<ExpenseWithCategory>>

    @Query("SELECT * FROM expenses WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<ExpenseEntity>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM expenses 
        WHERE isDeleted = 0 AND spentAt >= :fromMillis AND spentAt < :toMillisExclusive
        """
    )
    suspend fun getTotalSpent(fromMillis: Long, toMillisExclusive: Long): Double

    /** Date-range sum broken down by category name — "where did the money go" for the monthly report. */
    @Query(
        """
        SELECT c.name as categoryName, COALESCE(SUM(e.amount), 0) as total
        FROM expenses e
        INNER JOIN expense_categories c ON c.id = e.expenseCategoryId
        WHERE e.isDeleted = 0 AND e.spentAt >= :fromMillis AND e.spentAt < :toMillisExclusive
        GROUP BY c.name
        """
    )
    suspend fun getSpentByCategory(fromMillis: Long, toMillisExclusive: Long): List<CategoryTotal>

    @Query("UPDATE expenses SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}
