package com.schoolmgmt.app.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ItemCategoryDto(
    val id: String,
    val name: String,
    val type: String, // "BOOK" | "UNIFORM_SUMMER" | "UNIFORM_WINTER" | "OTHER"
    val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class ItemVariantDto(
    val id: String,
    val itemCategoryId: String,
    val label: String,
    val classId: String? = null,
    val size: String? = null,
    val price: Double,
    val costPrice: Double? = null,
    val stockQuantity: Int = 0,
    val isActive: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class UpdateItemVariantRequest(
    val label: String? = null,
    val price: Double? = null,
    val costPrice: Double? = null,
    val isActive: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class CreatePurchaseRequest(
    val itemVariantId: String,
    val quantity: Int = 1,
    val dueDate: String? = null,
)

@JsonClass(generateAdapter = true)
data class PurchaseResponse(
    val purchase: PurchaseDto,
    val studentFee: StudentFeeDto,
    val remainingStock: Int,
    val warning: String? = null,
)

@JsonClass(generateAdapter = true)
data class PurchaseDto(
    val id: String,
    val studentId: String,
    val itemVariantId: String,
    val quantity: Int,
)

@JsonClass(generateAdapter = true)
data class RestockRequest(
    val quantity: Int,
    val note: String? = null,
)

@JsonClass(generateAdapter = true)
data class AdjustStockRequest(
    val quantity: Int,
    val direction: String, // "increase" | "decrease"
    val note: String,
)

@JsonClass(generateAdapter = true)
data class InventoryResponse(
    val count: Int,
    val items: List<InventoryItemDto>,
)

@JsonClass(generateAdapter = true)
data class InventoryItemDto(
    val id: String,
    val category: String,
    val label: String,
    val size: String? = null,
    val `class`: String? = null, // "class" is a Kotlin keyword, hence backticks
    val price: Double,
    val stockQuantity: Int,
)
