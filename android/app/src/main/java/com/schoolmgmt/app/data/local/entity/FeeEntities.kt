package com.schoolmgmt.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FeeStatus { UNPAID, PARTIAL, PAID }
enum class PaymentMode { CASH, UPI, CHEQUE, BANK_TRANSFER, CARD, OTHER }

/** Mirrors `FeeCategory` — Tuition, Exam, Transport, Library... */
@Entity(
    tableName = "fee_categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class FeeCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

/** Mirrors `FeeStructure` — the class-level fee definition used by the bulk-assign button. */
@Entity(
    tableName = "fee_structures",
    indices = [
        Index(value = ["classId", "academicYearId"]),
        Index(value = ["feeCategoryId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = FeeCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["feeCategoryId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = SchoolClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AcademicYearEntity::class,
            parentColumns = ["id"],
            childColumns = ["academicYearId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class FeeStructureEntity(
    @PrimaryKey val id: String,
    val feeCategoryId: String,
    val classId: String,
    val academicYearId: String,
    val amount: Double,           // money stored as Double client-side; server uses Decimal
    val dueDate: Long,
    val description: String? = null,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

/**
 * Mirrors `StudentFee` — a fee actually billed to one student. May
 * trace back to a FeeStructure (bulk-assigned class fee), a
 * StudentItemPurchase (book/uniform sale), or neither (ad-hoc charge).
 */
@Entity(
    tableName = "student_fees",
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["status"]),
        Index(value = ["dueDate"]),
        Index(value = ["purchaseId"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class StudentFeeEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val feeStructureId: String? = null,
    val purchaseId: String? = null,
    val description: String,
    val amount: Double,
    val dueDate: Long,
    val discount: Double = 0.0,
    val status: FeeStatus = FeeStatus.UNPAID,
    val isDefaulted: Boolean = false,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

/** Mirrors `Payment` — one transaction against a StudentFee. */
@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["studentFeeId"]),
        Index(value = ["paidAt"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = StudentFeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentFeeId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val studentFeeId: String,
    val amount: Double,
    val mode: PaymentMode,
    val referenceNo: String? = null,
    val paidAt: Long = System.currentTimeMillis(),
    val recordedById: String,
    val notes: String? = null,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

@Entity(tableName = "invoice_settings")
data class InvoiceSettingsEntity(
    @PrimaryKey val id: String,
    val schoolName: String,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val footerNote: String? = null,
    val thermalWidth: Int = 576,
    val marginSize: Int = 20,
    val headerFontSize: Int = 28,
    val bodyFontSize: Int = 14,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)
