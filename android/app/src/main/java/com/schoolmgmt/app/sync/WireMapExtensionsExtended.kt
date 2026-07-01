package com.schoolmgmt.app.sync

import com.schoolmgmt.app.data.local.entity.AttendanceEntity
import com.schoolmgmt.app.data.local.entity.PaymentEntity
import com.schoolmgmt.app.data.local.entity.SchoolClassEntity
import com.schoolmgmt.app.data.local.entity.SectionEntity
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import com.schoolmgmt.app.data.local.entity.StudentItemPurchaseEntity
import com.schoolmgmt.app.data.local.entity.AcademicYearEntity
import com.schoolmgmt.app.data.local.entity.TeacherAttendanceEntity
import com.schoolmgmt.app.data.local.entity.InvoiceSettingsEntity

fun AcademicYearEntity.toWireMap(): Map<String, Any?> = mapOf(
    "label" to label,
    "startDate" to IsoDates.toIsoString(startDate),
    "endDate" to IsoDates.toIsoString(endDate),
    "isCurrent" to isCurrent,
)

fun com.schoolmgmt.app.data.local.entity.FeeCategoryEntity.toWireMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "description" to description,
)

fun com.schoolmgmt.app.data.local.entity.FeeStructureEntity.toWireMap(): Map<String, Any?> = mapOf(
    "feeCategoryId" to feeCategoryId,
    "classId" to classId,
    "academicYearId" to academicYearId,
    "amount" to amount,
    "dueDate" to IsoDates.toIsoString(dueDate),
    "description" to description,
)

fun SectionEntity.toWireMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "classId" to classId,
    "classTeacherId" to classTeacherId,
)

fun SchoolClassEntity.toWireMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "academicYearId" to academicYearId,
)

fun StudentFeeEntity.toWireMap(): Map<String, Any?> = mapOf(
    "studentId" to studentId,
    "feeStructureId" to feeStructureId,
    "purchaseId" to purchaseId,
    "description" to description,
    "amount" to amount,
    "dueDate" to IsoDates.toIsoString(dueDate),
    "discount" to discount,
    "status" to status.name,
)

fun PaymentEntity.toWireMap(): Map<String, Any?> = mapOf(
    "studentFeeId" to studentFeeId,
    "amount" to amount,
    "mode" to mode.name,
    "referenceNo" to referenceNo,
    "paidAt" to IsoDates.toIsoString(paidAt),
    "recordedById" to recordedById,
    "notes" to notes,
)

fun StudentItemPurchaseEntity.toWireMap(): Map<String, Any?> = mapOf(
    "studentId" to studentId,
    "itemVariantId" to itemVariantId,
    "quantity" to quantity,
)

fun AttendanceEntity.toWireMap(): Map<String, Any?> = mapOf(
    "studentId" to studentId,
    "sectionId" to sectionId,
    "date" to IsoDates.toIsoString(date),
    "status" to status.name,
    "markedById" to markedById,
)

fun TeacherAttendanceEntity.toWireMap(): Map<String, Any?> = mapOf(
    "teacherId" to teacherId,
    "date" to IsoDates.toIsoString(date),
    "status" to status.name,
)

fun com.schoolmgmt.app.data.local.entity.ItemCategoryEntity.toWireMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "type" to type.name,
    "description" to description,
)

fun com.schoolmgmt.app.data.local.entity.ItemVariantEntity.toWireMap(): Map<String, Any?> = mapOf(
    "itemCategoryId" to itemCategoryId,
    "label" to label,
    "classId" to classId,
    "size" to size,
    "price" to price,
    "costPrice" to costPrice,
    "stockQuantity" to stockQuantity,
    "isActive" to isActive,
)

fun com.schoolmgmt.app.data.local.entity.InventoryTransactionEntity.toWireMap(): Map<String, Any?> = mapOf(
    "itemVariantId" to itemVariantId,
    "type" to type.name,
    "quantity" to quantity,
    "note" to note,
    "recordedById" to recordedById,
    "purchaseId" to purchaseId,
)

fun InvoiceSettingsEntity.toWireMap(): Map<String, Any?> = mapOf(
    "schoolName" to schoolName,
    "address" to address,
    "phone" to phone,
    "email" to email,
    "footerNote" to footerNote,
    "thermalWidth" to thermalWidth,
    "marginSize" to marginSize,
    "headerFontSize" to headerFontSize,
    "bodyFontSize" to bodyFontSize,
)
