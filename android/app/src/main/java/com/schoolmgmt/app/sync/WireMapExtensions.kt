package com.schoolmgmt.app.sync

import com.schoolmgmt.app.data.local.entity.StudentEntity
import com.schoolmgmt.app.data.local.entity.TeacherEntity

/**
 * Converts a StudentEntity to the wire Map shape the backend's
 * syncRegistry.js expects for "Student" (see writableFields there) —
 * enum-like and date fields are converted to their JSON-primitive
 * representation explicitly (see SyncDto.kt's documented constraint:
 * Moshi only accepts String/Double/Boolean/List/Map/null in a
 * Map<String, Any?>, never raw enums or platform date types).
 */
fun StudentEntity.toWireMap(): Map<String, Any?> = mapOf(
    "admissionNo" to admissionNo,
    "rollNo" to rollNo,
    "firstName" to firstName,
    "lastName" to lastName,
    "dateOfBirth" to dateOfBirth?.let(IsoDates::toIsoString),
    "gender" to gender,
    "guardianName" to guardianName,
    "guardianPhone" to guardianPhone,
    "guardianEmail" to guardianEmail,
    "address" to address,
    "fatherPhone" to fatherPhone,
    "motherPhone" to motherPhone,
    "whatsappPhone" to whatsappPhone,
    "tuitionFee" to tuitionFee,
    "sectionId" to sectionId,
    "admissionDate" to IsoDates.toIsoString(admissionDate),
    "isActive" to isActive,
    "withdrawalReason" to withdrawalReason?.name,
    "withdrawnDate" to withdrawnDate?.let(IsoDates::toIsoString),
    "withdrawalNotes" to withdrawalNotes,
)

fun TeacherEntity.toWireMap(): Map<String, Any?> = mapOf(
    "employeeNo" to employeeNo,
    "firstName" to firstName,
    "lastName" to lastName,
    "phone" to phone,
    "email" to email,
    "address" to address,
    "qualification" to qualification,
    "joiningDate" to IsoDates.toIsoString(joiningDate),
    "isActive" to isActive,
)
