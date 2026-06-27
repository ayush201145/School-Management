package com.schoolmgmt.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class StaffType { TEACHER, ADMIN, ACCOUNTANT_STAFF, PEON, DRIVER, OTHER }
enum class RecurrenceFrequency { MONTHLY }

/** Mirrors `Staff` — generic record covering teachers and non-teaching staff. */
@Entity(
    tableName = "staff",
    indices = [Index(value = ["teacherId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = TeacherEntity::class,
            parentColumns = ["id"],
            childColumns = ["teacherId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class StaffEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: StaffType,
    val teacherId: String? = null, // set only if this staff member IS a teacher
    val phone: String? = null,
    val monthlySalary: Double? = null,
    val joiningDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

/** Mirrors `SalaryPayment` — one disbursement; multiple rows per staff per month are allowed. */
@Entity(
    tableName = "salary_payments",
    indices = [Index(value = ["staffId", "forYear", "forMonth"])],
    foreignKeys = [
        ForeignKey(
            entity = StaffEntity::class,
            parentColumns = ["id"],
            childColumns = ["staffId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class SalaryPaymentEntity(
    @PrimaryKey val id: String,
    val staffId: String,
    val amount: Double,
    val forMonth: Int, // 1-12
    val forYear: Int,
    val paidAt: Long = System.currentTimeMillis(),
    val mode: PaymentMode,
    val referenceNo: String? = null,
    val notes: String? = null,
    val recordedById: String,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

/** Mirrors `ExpenseCategory` — Rent, Utilities, Office Supplies (pages/ink/markers), Maintenance... */
@Entity(
    tableName = "expense_categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class ExpenseCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

/** Mirrors `RecurringExpenseTemplate` — e.g. "Office Rent, ₹15000, every month". */
@Entity(
    tableName = "recurring_expense_templates",
    indices = [Index(value = ["expenseCategoryId"])],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseCategoryId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class RecurringExpenseTemplateEntity(
    @PrimaryKey val id: String,
    val expenseCategoryId: String,
    val label: String,
    val amount: Double,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val dayOfMonth: Int = 1,
    val isActive: Boolean = true,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

/** Mirrors `Expense` — one actual spend, one-off or generated from a RecurringExpenseTemplate. */
@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["expenseCategoryId"]),
        Index(value = ["spentAt"]),
        Index(value = ["recurringTemplateId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["expenseCategoryId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = RecurringExpenseTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringTemplateId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val expenseCategoryId: String,
    val recurringTemplateId: String? = null,
    val description: String,
    val amount: Double,
    val spentAt: Long = System.currentTimeMillis(),
    val mode: PaymentMode,
    val referenceNo: String? = null,
    val recordedById: String,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)
