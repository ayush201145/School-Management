package com.schoolmgmt.app.data.remote

import com.schoolmgmt.app.data.remote.dto.LoginRequest
import com.schoolmgmt.app.data.remote.dto.LoginResponse
import com.schoolmgmt.app.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Mirrors backend/src/routes/authRoutes.js */
interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/auth/me")
    suspend fun me(): UserDto
}
