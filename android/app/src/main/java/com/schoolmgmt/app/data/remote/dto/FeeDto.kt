package com.schoolmgmt.app.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeeCategoryDto(
    val id: String,
    val name: String,
    val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class FeeStructureDto(
    val id: String,
    val feeCategoryId: String,
    val classId: String,
    val academicYearId: String,
    val amount: Double,
    val dueDate: String,
    val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateFeeStructureRequest(
    val feeCategoryId: String,
    val classId: String,
    val academicYearId: String,
    val amount: Double,
    val dueDate: String,
    val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class AssignFeeStructureRequest(
    val sectionId: String? = null,
)

@JsonClass(generateAdapter = true)
data class AssignFeeStructureResponse(
    val created: Int,
    val skipped: Int,
    val totalStudentsInScope: Int,
    val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class StudentFeeDto(
    val id: String,
    val studentId: String,
    val feeStructureId: String? = null,
    val purchaseId: String? = null,
    val description: String,
    val amount: Double,
    val dueDate: String,
    val discount: Double = 0.0,
    val status: String, // "UNPAID" | "PARTIAL" | "PAID"
)

@JsonClass(generateAdapter = true)
data class CreatePaymentRequest(
    val amount: Double,
    val mode: String, // "CASH" | "UPI" | "CHEQUE" | "BANK_TRANSFER" | "CARD" | "OTHER"
    val referenceNo: String? = null,
    val notes: String? = null,
    val paidAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class PaymentDto(
    val id: String,
    val studentFeeId: String,
    val amount: Double,
    val mode: String,
    val referenceNo: String? = null,
    val paidAt: String,
    val recordedById: String,
    val notes: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreatePaymentResponse(
    val payment: PaymentDto,
    val studentFee: StudentFeeDto,
)

@JsonClass(generateAdapter = true)
data class DuesResponse(
    val count: Int,
    val totalOutstanding: Double,
    val dues: List<DueDto>,
)

@JsonClass(generateAdapter = true)
data class DueDto(
    val id: String,
    val description: String,
    val dueDate: String,
    val status: String,
    val amount: Double,
    val paid: Double,
    val balance: Double,
    val student: StudentSummaryDto,
)

@JsonClass(generateAdapter = true)
data class StudentSummaryDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val admissionNo: String,
    val guardianPhone: String? = null,
)

@JsonClass(generateAdapter = true)
data class TransactionsResponse(
    val count: Int,
    val totalAmount: Double,
    val transactions: List<PaymentDto>,
)
