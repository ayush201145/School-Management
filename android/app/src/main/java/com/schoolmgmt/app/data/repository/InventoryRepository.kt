package com.schoolmgmt.app.data.repository

import androidx.room.withTransaction
import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.dao.InventoryRow
import com.schoolmgmt.app.data.local.entity.InventoryMovementType
import com.schoolmgmt.app.data.local.entity.InventoryTransactionEntity
import com.schoolmgmt.app.data.local.entity.ItemVariantEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Result of a stock movement — mirrors the backend's applyStockMovement return shape. */
data class StockMovementResult(
    val variant: ItemVariantEntity,
    val transaction: InventoryTransactionEntity,
    val wentNegative: Boolean,
    val shortBy: Int,
)

class InsufficientStockException(message: String) : Exception(message)

private val INCREASING_TYPES = setOf(InventoryMovementType.IN, InventoryMovementType.ADJUSTMENT_IN)

@Singleton
class InventoryRepository @Inject constructor(
    private val db: AppDatabase,
) {
    private val itemVariantDao = db.itemVariantDao()
    private val inventoryTransactionDao = db.inventoryTransactionDao()
    private val itemCategoryDao = db.itemCategoryDao()

    fun observeInventory(categoryId: String? = null, lowStockBelow: Int? = null): Flow<List<InventoryRow>> =
        itemVariantDao.observeInventory(categoryId, lowStockBelow)

    fun observeHistory(variantId: String) = inventoryTransactionDao.observeForVariant(variantId)

    /** Item categories (Books, Uniform - Summer, Uniform - Winter...) — for the purchase flow's category picker. */
    fun observeCategories() = itemCategoryDao.observeAll()

    /** Variants within one category (e.g. every book-set variant, or every uniform size) — for the variant picker. */
    fun observeVariants(categoryId: String) = itemVariantDao.observeByCategory(categoryId)

    /** THE "WHO BOUGHT THIS ITEM" REPORT — who purchased a given item variant. */
    fun observePurchasesForVariant(itemVariantId: String) =
        db.studentItemPurchaseDao().observeByVariant(itemVariantId)

    /**
     * Applies a stock movement and records it in the ledger, atomically.
     * Mirrors backend/src/services/inventoryService.js exactly:
     *
     *  - Sales (OUT) NEVER block, even at 0 or negative stock — schools
     *    may need to sell on backorder. wentNegative/shortBy are
     *    returned so the caller (e.g. the purchase flow) can show a
     *    warning on the bill instead of blocking the sale.
     *  - Manual corrections (ADJUSTMENT_OUT) DO block on insufficient
     *    stock by default, since that usually signals a mistake rather
     *    than a real sale. Pass allowNegativeStock=true to override.
     */
    suspend fun applyStockMovement(
        itemVariantId: String,
        type: InventoryMovementType,
        quantity: Int,
        note: String? = null,
        recordedById: String? = null,
        purchaseId: String? = null,
        allowNegativeStock: Boolean = false,
    ): StockMovementResult = db.withTransaction {
        require(quantity > 0) { "quantity must be greater than 0" }

        val variant = itemVariantDao.getById(itemVariantId)
            ?: throw IllegalArgumentException("Item variant $itemVariantId not found")

        val delta = if (type in INCREASING_TYPES) quantity else -quantity
        val newStock = variant.stockQuantity + delta

        val blockOnNegative = type == InventoryMovementType.ADJUSTMENT_OUT && !allowNegativeStock
        if (newStock < 0 && blockOnNegative) {
            throw InsufficientStockException(
                "Insufficient stock for \"${variant.label}\": have ${variant.stockQuantity}, tried to remove $quantity"
            )
        }

        val now = System.currentTimeMillis()
        itemVariantDao.setStockQuantity(itemVariantId, newStock, now)

        val transactionEntity = InventoryTransactionEntity(
            id = UUID.randomUUID().toString(),
            itemVariantId = itemVariantId,
            type = type,
            quantity = quantity,
            note = note,
            recordedById = recordedById,
            purchaseId = purchaseId,
            createdAt = now,
            updatedAt = now,
        )
        inventoryTransactionDao.upsert(transactionEntity)

        StockMovementResult(
            variant = variant.copy(stockQuantity = newStock, updatedAt = now),
            transaction = transactionEntity,
            wentNegative = newStock < 0,
            shortBy = if (newStock < 0) -newStock else 0,
        )
    }

    suspend fun restock(itemVariantId: String, quantity: Int, note: String?, recordedById: String) =
        applyStockMovement(itemVariantId, InventoryMovementType.IN, quantity, note, recordedById)

    suspend fun adjust(
        itemVariantId: String,
        quantity: Int,
        increase: Boolean,
        note: String,
        recordedById: String,
    ) = applyStockMovement(
        itemVariantId,
        if (increase) InventoryMovementType.ADJUSTMENT_IN else InventoryMovementType.ADJUSTMENT_OUT,
        quantity,
        note,
        recordedById,
    )

    suspend fun updateVariantPrice(itemVariantId: String, price: Double, costPrice: Double? = null) {
        val variant = itemVariantDao.getById(itemVariantId)
            ?: throw IllegalArgumentException("Item variant $itemVariantId not found")
        itemVariantDao.update(
            variant.copy(
                price = price,
                costPrice = costPrice,
                updatedAt = System.currentTimeMillis(),
                syncedAt = null
            )
        )
    }
}
