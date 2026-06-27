package com.schoolmgmt.app.data.repository

import androidx.room.withTransaction
import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.entity.InventoryMovementType
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import com.schoolmgmt.app.data.local.entity.StudentItemPurchaseEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** What the billing screen needs after completing a sale. */
data class PurchaseResult(
    val purchase: StudentItemPurchaseEntity,
    val studentFee: StudentFeeEntity,
    val remainingStock: Int,
    val warning: String?,
)

/**
 * Mirrors POST /api/students/:id/purchases on the backend: selling a
 * book/uniform variant to a student creates the purchase record,
 * decrements stock (never blocking — see InventoryRepository), and
 * bills the student via a new StudentFee, all in ONE atomic transaction.
 */
@Singleton
class PurchaseRepository @Inject constructor(
    private val db: AppDatabase,
    private val inventoryRepository: InventoryRepository,
) {
    private val itemVariantDao = db.itemVariantDao()
    private val studentItemPurchaseDao = db.studentItemPurchaseDao()
    private val studentFeeDao = db.studentFeeDao()

    /** Purchase history for one student, with item details joined in — for the student detail screen. */
    fun observeHistoryForStudent(studentId: String) =
        studentItemPurchaseDao.observePurchaseHistoryForStudent(studentId)

    suspend fun purchaseItem(
        studentId: String,
        itemVariantId: String,
        quantity: Int = 1,
        recordedById: String,
        dueDate: Long = System.currentTimeMillis(),
    ): PurchaseResult = db.withTransaction {
        require(quantity > 0) { "quantity must be greater than 0" }

        val variant = itemVariantDao.getById(itemVariantId)
            ?: throw IllegalArgumentException("Item variant $itemVariantId not found")

        val now = System.currentTimeMillis()
        val purchase = StudentItemPurchaseEntity(
            id = UUID.randomUUID().toString(),
            studentId = studentId,
            itemVariantId = itemVariantId,
            quantity = quantity,
            updatedAt = now,
        )
        studentItemPurchaseDao.upsert(purchase)

        // Stock decrement happens inside the SAME outer transaction.
        // Room's withTransaction() explicitly supports this reentrant
        // case (calling withTransaction from code already running
        // inside a transaction) — it detects that the transaction
        // thread is already active and joins the existing transaction
        // rather than starting an independent one, so this stays fully
        // atomic with the purchase and fee writes below.
        val stockResult = inventoryRepository.applyStockMovement(
            itemVariantId = itemVariantId,
            type = InventoryMovementType.OUT,
            quantity = quantity,
            note = "Sold to student $studentId",
            recordedById = recordedById,
            purchaseId = purchase.id,
        )

        val totalAmount = variant.price * quantity
        val label = buildString {
            append(variant.label)
            if (quantity > 1) append(" x$quantity")
        }
        val studentFee = StudentFeeEntity(
            id = UUID.randomUUID().toString(),
            studentId = studentId,
            purchaseId = purchase.id,
            description = label,
            amount = totalAmount,
            dueDate = dueDate,
            updatedAt = now,
        )
        studentFeeDao.upsert(studentFee)

        val warning = if (stockResult.wentNegative) {
            "Stock for \"${variant.label}\" is now short by ${stockResult.shortBy} unit(s) — this sale was completed on backorder."
        } else null

        PurchaseResult(
            purchase = purchase,
            studentFee = studentFee,
            remainingStock = stockResult.variant.stockQuantity,
            warning = warning,
        )
    }
}
