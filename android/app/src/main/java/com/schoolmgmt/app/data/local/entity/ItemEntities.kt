package com.schoolmgmt.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ItemType { BOOK, UNIFORM_SUMMER, UNIFORM_WINTER, OTHER }
enum class InventoryMovementType { IN, OUT, ADJUSTMENT_IN, ADJUSTMENT_OUT }

/** Mirrors `ItemCategory` — Books, Uniform - Summer (Regular), Uniform - Summer (PT), Uniform - Winter. */
@Entity(
    tableName = "item_categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class ItemCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: ItemType,
    val description: String? = null,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

/**
 * Mirrors `ItemVariant` — a priced, stocked variant of an item category.
 * Books: classId set (e.g. "UKG Book Set"). Uniforms: size set (e.g. "Size 24").
 */
@Entity(
    tableName = "item_variants",
    indices = [
        Index(value = ["itemCategoryId"]),
        Index(value = ["classId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ItemCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemCategoryId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = SchoolClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class ItemVariantEntity(
    @PrimaryKey val id: String,
    val itemCategoryId: String,
    val label: String,             // "UKG Book Set", "Size 24"
    val classId: String? = null,   // set for book variants
    val size: String? = null,      // set for uniform variants ("20".."34")
    val price: Double,
    val costPrice: Double? = null,
    val stockQuantity: Int = 0,
    val isActive: Boolean = true,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

/** Mirrors `StudentItemPurchase` — a student buying a specific item variant. */
@Entity(
    tableName = "student_item_purchases",
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["itemVariantId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = ItemVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemVariantId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class StudentItemPurchaseEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val itemVariantId: String,
    val quantity: Int = 1,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

/**
 * Mirrors `InventoryTransaction` — the audit ledger for every stock
 * movement (restock, sale, manual correction). stockQuantity on
 * ItemVariantEntity is the running total; this table explains how it
 * got there. Never write stockQuantity directly — always go through
 * the same applyStockMovement-equivalent logic used by the repository
 * layer, mirroring the backend's inventoryService.js.
 */
@Entity(
    tableName = "inventory_transactions",
    indices = [
        Index(value = ["itemVariantId"]),
        Index(value = ["createdAt"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = ItemVariantEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemVariantId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class InventoryTransactionEntity(
    @PrimaryKey val id: String,
    val itemVariantId: String,
    val type: InventoryMovementType,
    val quantity: Int,              // always positive; direction comes from `type`
    val note: String? = null,
    val recordedById: String? = null,
    val purchaseId: String? = null, // set when type=OUT and auto-created by a sale
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)
