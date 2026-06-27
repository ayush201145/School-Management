package com.schoolmgmt.app.data.remote

import com.schoolmgmt.app.data.remote.dto.StudentDto
import com.schoolmgmt.app.data.remote.dto.TeacherDto
import com.schoolmgmt.app.data.remote.dto.UpdateStudentRequest
import com.schoolmgmt.app.data.remote.dto.UpdateTeacherRequest
import com.schoolmgmt.app.data.remote.dto.WithdrawStudentRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Mirrors backend/src/routes/studentRoutes.js */
interface StudentApi {
    @GET("api/students")
    suspend fun listStudents(
        @Query("search") search: String? = null,
        @Query("sectionId") sectionId: String? = null,
        @Query("isActive") isActive: Boolean? = null,
    ): List<StudentDto>

    @GET("api/students/{id}")
    suspend fun getStudent(@Path("id") id: String): StudentDto

    @POST("api/students")
    suspend fun createStudent(@Body student: StudentDto): StudentDto

    // Typed request DTO instead of a raw Map<String, Any?> — avoids
    // Moshi's documented refusal to serialize java.*/android.* platform
    // types without a custom adapter, and is more type-safe besides.
    // Every field is nullable; only non-null fields are meant to be
    // changed (see UpdateStudentRequest doc comment).
    @PATCH("api/students/{id}")
    suspend fun updateStudent(@Path("id") id: String, @Body request: UpdateStudentRequest): StudentDto

    @POST("api/students/{id}/withdraw")
    suspend fun withdrawStudent(@Path("id") id: String, @Body request: WithdrawStudentRequest): StudentDto

    @POST("api/students/{id}/reinstate")
    suspend fun reinstateStudent(@Path("id") id: String): StudentDto
}

/** Mirrors backend/src/routes/teacherRoutes.js */
interface TeacherApi {
    @GET("api/teachers")
    suspend fun listTeachers(
        @Query("search") search: String? = null,
        @Query("isActive") isActive: Boolean? = null,
    ): List<TeacherDto>

    @GET("api/teachers/{id}")
    suspend fun getTeacher(@Path("id") id: String): TeacherDto

    @POST("api/teachers")
    suspend fun createTeacher(@Body teacher: TeacherDto): TeacherDto

    @PATCH("api/teachers/{id}")
    suspend fun updateTeacher(@Path("id") id: String, @Body request: UpdateTeacherRequest): TeacherDto
}
