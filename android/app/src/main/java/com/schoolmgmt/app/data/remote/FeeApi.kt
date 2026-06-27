package com.schoolmgmt.app.data.remote

import com.schoolmgmt.app.data.remote.dto.AssignFeeStructureRequest
import com.schoolmgmt.app.data.remote.dto.AssignFeeStructureResponse
import com.schoolmgmt.app.data.remote.dto.CreateFeeStructureRequest
import com.schoolmgmt.app.data.remote.dto.CreatePaymentRequest
import com.schoolmgmt.app.data.remote.dto.CreatePaymentResponse
import com.schoolmgmt.app.data.remote.dto.DuesResponse
import com.schoolmgmt.app.data.remote.dto.FeeCategoryDto
import com.schoolmgmt.app.data.remote.dto.FeeStructureDto
import com.schoolmgmt.app.data.remote.dto.PaymentDto
import com.schoolmgmt.app.data.remote.dto.TransactionsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Mirrors backend/src/routes/feeRoutes.js */
interface FeeApi {
    @GET("api/fee-categories")
    suspend fun listFeeCategories(): List<FeeCategoryDto>

    @POST("api/fee-categories")
    suspend fun createFeeCategory(@Body category: FeeCategoryDto): FeeCategoryDto

    @GET("api/fee-structures")
    suspend fun listFeeStructures(
        @Query("classId") classId: String? = null,
        @Query("academicYearId") academicYearId: String? = null,
    ): List<FeeStructureDto>

    @POST("api/fee-structures")
    suspend fun createFeeStructure(@Body request: CreateFeeStructureRequest): FeeStructureDto

    /** THE BULK-ASSIGN BUTTON (#6). Safe to call more than once — see backend idempotency guarantee. */
    @POST("api/fee-structures/{id}/assign")
    suspend fun assignFeeStructure(
        @Path("id") feeStructureId: String,
        @Body request: AssignFeeStructureRequest,
    ): AssignFeeStructureResponse
}

/** Mirrors backend/src/routes/paymentRoutes.js */
interface PaymentApi {
    @POST("api/student-fees/{studentFeeId}/payments")
    suspend fun createPayment(
        @Path("studentFeeId") studentFeeId: String,
        @Body request: CreatePaymentRequest,
    ): CreatePaymentResponse

    @GET("api/student-fees/{studentFeeId}/payments")
    suspend fun listPaymentsForFee(@Path("studentFeeId") studentFeeId: String): List<PaymentDto>

    /** THE TRANSACTION LEDGER (#7). */
    @GET("api/transactions")
    suspend fun listTransactions(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("studentId") studentId: String? = null,
        @Query("mode") mode: String? = null,
    ): TransactionsResponse

    /** THE DUES REPORT (#5). */
    @GET("api/dues")
    suspend fun listDues(
        @Query("sectionId") sectionId: String? = null,
        @Query("classId") classId: String? = null,
        @Query("overdueOnly") overdueOnly: Boolean? = null,
    ): DuesResponse
}
