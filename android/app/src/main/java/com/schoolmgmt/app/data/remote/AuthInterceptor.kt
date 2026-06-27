package com.schoolmgmt.app.data.remote

import com.schoolmgmt.app.data.local.AuthPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Attaches "Authorization: Bearer <token>" to every request, reading
 * the cached token from DataStore. Login/bootstrap-admin requests
 * don't need this (no token exists yet before login), but sending a
 * null/missing header on those is harmless — the backend's requireAuth
 * middleware isn't applied to those specific routes anyway.
 *
 * runBlocking here is intentional: OkHttp's Interceptor.intercept() is
 * a synchronous callback by design — OkHttp calls it from its own
 * background thread pool (never the main thread) and blocks that
 * thread waiting for intercept() to return, so there's no other way to
 * read an async value here. This is safe specifically because
 * DataStore's Flow.first() completes on its own internal dispatcher
 * rather than needing the calling (now-blocked) thread back to make
 * progress — so there's no self-deadlock, just a brief, bounded block
 * of one OkHttp worker thread.
 */
class AuthInterceptor @Inject constructor(
    private val authPreferences: AuthPreferences,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authPreferences.getToken() }
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
