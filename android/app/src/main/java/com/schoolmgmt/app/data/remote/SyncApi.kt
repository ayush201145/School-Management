package com.schoolmgmt.app.data.remote

import com.schoolmgmt.app.data.remote.dto.ResolveConflictRequest
import com.schoolmgmt.app.data.remote.dto.SyncConflictDto
import com.schoolmgmt.app.data.remote.dto.SyncPullResponse
import com.schoolmgmt.app.data.remote.dto.SyncPushRequest
import com.schoolmgmt.app.data.remote.dto.SyncPushResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Mirrors backend/src/routes/syncRoutes.js */
interface SyncApi {
    @POST("api/sync/push")
    suspend fun push(@Body request: SyncPushRequest): SyncPushResponse

    /** Always pass the serverTime from the PREVIOUS pull response, never the device's own clock. */
    @GET("api/sync/pull")
    suspend fun pull(@Query("since") since: String?): SyncPullResponse

    @GET("api/sync/conflicts")
    suspend fun listConflicts(@Query("status") status: String? = null): List<SyncConflictDto>

    @POST("api/sync/conflicts/{id}/resolve")
    suspend fun resolveConflict(
        @Path("id") conflictId: String,
        @Body request: ResolveConflictRequest,
    ): SyncConflictDto
}
