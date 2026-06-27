package com.schoolmgmt.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.schoolmgmt.app.data.local.entity.InventoryTransactionEntity
import com.schoolmgmt.app.data.local.entity.ItemCategoryEntity
import com.schoolmgmt.app.data.local.entity.ItemVariantEntity
import com.schoolmgmt.app.data.local.entity.StudentItemPurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: ItemCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<ItemCategoryEntity>)

    @Query("SELECT * FROM item_categories WHERE isDeleted = 0 ORDER BY name ASC")
    fun observeAll(): Flow<List<ItemCategoryEntity>>

    @Query("SELECT * FROM item_categories WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<ItemCategoryEntity>

    @Query("UPDATE item_categories SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

/** One row of the remaining-inventory report, with category name joined in for display. */
data class InventoryRow(
    @Embedded val variant: ItemVariantEntity,
    val categoryName: String,
)

@Dao
interface ItemVariantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(variant: ItemVariantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(variants: List<ItemVariantEntity>)

    @Update
    suspend fun update(variant: ItemVariantEntity)

    @Query("SELECT * FROM item_variants WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): ItemVariantEntity?

    @Query("SELECT * FROM item_variants WHERE itemCategoryId = :categoryId AND isDeleted = 0 ORDER BY label ASC")
    fun observeByCategory(categoryId: String): Flow<List<ItemVariantEntity>>

    /**
     * THE REMAINING-INVENTORY REPORT. Optionally filtered to variants
     * below a stock threshold (low-stock alert view).
     */
    @Query(
        """
        SELECT v.*, c.name AS categoryName
        FROM item_variants v
        INNER JOIN item_categories c ON c.id = v.itemCategoryId
        WHERE v.isDeleted = 0 AND v.isActive = 1
          AND (:categoryId IS NULL OR v.itemCategoryId = :categoryId)
          AND (:lowStockBelow IS NULL OR v.stockQuantity < :lowStockBelow)
        ORDER BY c.name ASC, v.label ASC
        """
    )
    fun observeInventory(categoryId: String? = null, lowStockBelow: Int? = null): Flow<List<InventoryRow>>

    /**
     * Directly adjusts stockQuantity. Only ever called from
     * InventoryRepository alongside an InventoryTransaction insert in
     * the SAME database transaction — see InventoryRepository.applyStockMovement().
     * Never call this in isolation or the ledger will drift from the total.
     */
    @Query("UPDATE item_variants SET stockQuantity = :newStock, updatedAt = :now WHERE id = :id")
    suspend fun setStockQuantity(id: String, newStock: Int, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM item_variants WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<ItemVariantEntity>

    @Query("UPDATE item_variants SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

@Dao
interface StudentItemPurchaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(purchase: StudentItemPurchaseEntity)

    @Query("SELECT * FROM student_item_purchases WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): StudentItemPurchaseEntity?

    @Query("SELECT * FROM student_item_purchases WHERE studentId = :studentId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeByStudent(studentId: String): Flow<List<StudentItemPurchaseEntity>>

    /**
     * Student-side purchase history WITH item details joined in (label,
     * category, price) — what the student detail screen's purchase
     * history list renders directly, no separate per-row lookup needed.
     */
    @Query(
        """
        SELECT 
            p.id AS purchaseId,
            p.itemVariantId AS itemVariantId,
            p.quantity AS quantity,
            p.updatedAt AS purchasedAt,
            v.label AS itemLabel,
            v.price AS unitPrice,
            c.name AS categoryName
        FROM student_item_purchases p
        INNER JOIN item_variants v ON v.id = p.itemVariantId
        INNER JOIN item_categories c ON c.id = v.itemCategoryId
        WHERE p.studentId = :studentId AND p.isDeleted = 0
        ORDER BY p.updatedAt DESC
        """
    )
    fun observePurchaseHistoryForStudent(studentId: String): Flow<List<StudentPurchaseHistoryRow>>

    /**
     * THE "WHO BOUGHT THIS ITEM" REPORT — the reverse lookup of
     * observeByStudent. Local mirror of GET /api/item-variants/:id/purchases.
     * Joined with student info directly (no separate per-row lookup needed).
     */
    @Query(
        """
        SELECT 
            p.id AS purchaseId,
            p.studentId AS studentId,
            p.itemVariantId AS itemVariantId,
            p.quantity AS quantity,
            p.updatedAt AS purchasedAt,
            s.firstName AS studentFirstName,
            s.lastName AS studentLastName,
            s.admissionNo AS admissionNo
        FROM student_item_purchases p
        INNER JOIN students s ON s.id = p.studentId
        WHERE p.itemVariantId = :itemVariantId AND p.isDeleted = 0
        ORDER BY p.updatedAt DESC
        """
    )
    fun observeByVariant(itemVariantId: String): Flow<List<PurchaseByVariantRow>>

    @Query("SELECT * FROM student_item_purchases WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<StudentItemPurchaseEntity>

    @Query("UPDATE student_item_purchases SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

/** One row of the student-side purchase history, with item details pre-joined. */
data class StudentPurchaseHistoryRow(
    val purchaseId: String,
    val itemVariantId: String,
    val quantity: Int,
    val purchasedAt: Long,
    val itemLabel: String,
    val unitPrice: Double,
    val categoryName: String,
)

/** One row of the "who bought this item" report. */
data class PurchaseByVariantRow(
    val purchaseId: String,
    val studentId: String,
    val itemVariantId: String,
    val quantity: Int,
    val purchasedAt: Long,
    val studentFirstName: String,
    val studentLastName: String,
    val admissionNo: String,
)

@Dao
interface InventoryTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: InventoryTransactionEntity)

    /** Full movement ledger for one variant — audit trail ("where did the stock go"). */
    @Query("SELECT * FROM inventory_transactions WHERE itemVariantId = :variantId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun observeForVariant(variantId: String): Flow<List<InventoryTransactionEntity>>

    @Query("SELECT * FROM inventory_transactions WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<InventoryTransactionEntity>

    @Query("UPDATE inventory_transactions SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}
