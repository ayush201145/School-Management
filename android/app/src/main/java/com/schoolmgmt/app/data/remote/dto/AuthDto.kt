package com.schoolmgmt.app.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val username: String,
    val role: String, // "ADMIN" | "ACCOUNTANT" | "TEACHER"
    val teacherId: String? = null,
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val token: String,
    val user: UserDto,
)
