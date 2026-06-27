package com.schoolmgmt.app.data.remote

import com.schoolmgmt.app.data.remote.dto.AdjustStockRequest
import com.schoolmgmt.app.data.remote.dto.CreatePurchaseRequest
import com.schoolmgmt.app.data.remote.dto.InventoryItemDto
import com.schoolmgmt.app.data.remote.dto.InventoryResponse
import com.schoolmgmt.app.data.remote.dto.ItemCategoryDto
import com.schoolmgmt.app.data.remote.dto.ItemVariantDto
import com.schoolmgmt.app.data.remote.dto.PurchaseDto
import com.schoolmgmt.app.data.remote.dto.PurchaseResponse
import com.schoolmgmt.app.data.remote.dto.RestockRequest
import com.schoolmgmt.app.data.remote.dto.UpdateItemVariantRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Mirrors backend/src/routes/itemRoutes.js */
interface ItemApi {
    @GET("api/item-categories")
    suspend fun listItemCategories(): List<ItemCategoryDto>

    @POST("api/item-categories")
    suspend fun createItemCategory(@Body category: ItemCategoryDto): ItemCategoryDto

    @GET("api/item-variants")
    suspend fun listItemVariants(
        @Query("itemCategoryId") itemCategoryId: String? = null,
        @Query("classId") classId: String? = null,
    ): List<ItemVariantDto>

    @POST("api/item-variants")
    suspend fun createItemVariant(@Body variant: ItemVariantDto): ItemVariantDto

    @PATCH("api/item-variants/{id}")
    suspend fun updateItemVariant(@Path("id") id: String, @Body request: UpdateItemVariantRequest): ItemVariantDto

    /** Sale never blocks even at 0 stock — see PurchaseResponse.warning. */
    @POST("api/students/{studentId}/purchases")
    suspend fun purchaseItem(
        @Path("studentId") studentId: String,
        @Body request: CreatePurchaseRequest,
    ): PurchaseResponse

    @GET("api/students/{studentId}/purchases")
    suspend fun listPurchasesForStudent(@Path("studentId") studentId: String): List<PurchaseDto>

    @POST("api/item-variants/{id}/restock")
    suspend fun restockItemVariant(@Path("id") id: String, @Body request: RestockRequest): InventoryItemDto

    @POST("api/item-variants/{id}/adjust")
    suspend fun adjustItemVariantStock(@Path("id") id: String, @Body request: AdjustStockRequest): InventoryItemDto

    /** THE REMAINING-INVENTORY REPORT. */
    @GET("api/inventory")
    suspend fun listInventory(
        @Query("itemCategoryId") itemCategoryId: String? = null,
        @Query("lowStockBelow") lowStockBelow: Int? = null,
    ): InventoryResponse
}
