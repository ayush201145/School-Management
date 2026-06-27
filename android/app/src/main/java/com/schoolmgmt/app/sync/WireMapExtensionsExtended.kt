package com.schoolmgmt.app.sync

import com.schoolmgmt.app.data.local.entity.AttendanceEntity
import com.schoolmgmt.app.data.local.entity.PaymentEntity
import com.schoolmgmt.app.data.local.entity.SchoolClassEntity
import com.schoolmgmt.app.data.local.entity.SectionEntity
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import com.schoolmgmt.app.data.local.entity.StudentItemPurchaseEntity
import com.schoolmgmt.app.data.local.entity.TeacherAttendanceEntity

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
