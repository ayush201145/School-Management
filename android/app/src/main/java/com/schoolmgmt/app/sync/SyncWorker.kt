package com.schoolmgmt.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.schoolmgmt.app.data.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs SyncRepository.syncNow() in the background. WorkManager (not a
 * plain coroutine or Service) is the right tool here specifically
 * because it survives process death, retries with backoff on failure,
 * and respects battery/network constraints automatically — all things
 * a school office app needs without hand-rolling retry logic.
 *
 * Registered twice (see SyncScheduler): as a periodic job for routine
 * background sync, AND as a one-time expedited job for "sync now" /
 * immediately after login, so the person doesn't have to wait for the
 * next periodic window.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val result = syncRepository.syncNow()
            if (result.errors.isNotEmpty()) {
                // Partial failure (e.g. one batch's network call failed) —
                // retry the whole cycle rather than treating it as a
                // permanent failure. WorkManager's backoff policy (set in
                // SyncScheduler) prevents this from hammering the server.
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            // Network unavailable, server down, etc. — retry rather than
            // fail permanently, since connectivity is exactly the kind of
            // transient condition this whole offline-first design exists
            // to tolerate.
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "sync_periodic"
        const val WORK_NAME_ONE_TIME = "sync_one_time"
    }
}
