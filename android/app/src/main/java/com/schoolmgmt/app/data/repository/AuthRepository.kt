package com.schoolmgmt.app.data.repository

import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.AuthPreferences
import com.schoolmgmt.app.data.local.entity.UserEntity
import com.schoolmgmt.app.data.local.entity.UserRole
import com.schoolmgmt.app.data.remote.AuthApi
import com.schoolmgmt.app.data.remote.dto.LoginRequest
import com.schoolmgmt.app.data.remote.dto.UserDto
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginFailedException(message: String) : Exception(message)

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val authPreferences: AuthPreferences,
    private val db: AppDatabase,
) {
    val roleFlow = authPreferences.roleFlow
    val tokenFlow = authPreferences.tokenFlow

    /**
     * Login REQUIRES network — there's no offline-first story for the
     * very first login on a device, since we need the server to issue
     * a JWT. Once logged in, the token is cached in DataStore and the
     * user stays logged in across app restarts without needing network
     * again (see AuthInterceptor, which attaches the cached token to
     * every request automatically).
     */
    suspend fun login(username: String, password: String) {
        val response = try {
            authApi.login(LoginRequest(username, password))
        } catch (e: Exception) {
            throw LoginFailedException("Login failed: ${e.message ?: "check your connection and try again"}")
        }

        authPreferences.saveSession(
            token = response.token,
            userId = response.user.id,
            username = response.user.username,
            role = parseRole(response.user.role),
        )
        cacheUser(response.user)
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        authPreferences.clearSession()
        db.clearAllTables()
    }

    suspend fun getCurrentUserId(): String? = authPreferences.getCurrentUserId()

    suspend fun isLoggedIn(): Boolean = authPreferences.getToken() != null

    private suspend fun cacheUser(user: UserDto) {
        db.userDao().upsert(
            UserEntity(
                id = user.id,
                username = user.username,
                role = parseRole(user.role),
                teacherId = user.teacherId,
            )
        )
    }

    private fun parseRole(roleString: String): UserRole =
        runCatching { UserRole.valueOf(roleString) }.getOrDefault(UserRole.TEACHER)
}
