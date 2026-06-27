package com.schoolmgmt.app.data.remote.dto

import com.squareup.moshi.JsonClass

/** Mirrors backend/src/controllers/studentController.js request/response shapes. */
@JsonClass(generateAdapter = true)
data class StudentDto(
    val id: String,
    val admissionNo: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String? = null, // ISO 8601
    val gender: String? = null,
    val guardianName: String? = null,
    val guardianPhone: String? = null,
    val guardianEmail: String? = null,
    val address: String? = null,
    val sectionId: String,
    val admissionDate: String? = null,
    val isActive: Boolean = true,
    val withdrawalReason: String? = null,
    val withdrawnDate: String? = null,
    val withdrawalNotes: String? = null,
)

@JsonClass(generateAdapter = true)
data class WithdrawStudentRequest(
    val reason: String, // "TRANSFERRED" | "GRADUATED" | "EXPELLED" | "OTHER"
    val notes: String? = null,
    val withdrawnDate: String? = null,
)

/**
 * All fields nullable; the backend's PATCH handler (studentController.updateStudent)
 * only applies fields that are present/non-null, leaving everything
 * else untouched — same partial-update semantics either way.
 */
@JsonClass(generateAdapter = true)
data class UpdateStudentRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val guardianName: String? = null,
    val guardianPhone: String? = null,
    val guardianEmail: String? = null,
    val address: String? = null,
    val sectionId: String? = null,
    val isActive: Boolean? = null,
)

/** Mirrors backend/src/controllers/teacherController.js. */
@JsonClass(generateAdapter = true)
data class TeacherDto(
    val id: String,
    val employeeNo: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val qualification: String? = null,
    val joiningDate: String? = null,
    val isActive: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class UpdateTeacherRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val qualification: String? = null,
    val isActive: Boolean? = null,
)
