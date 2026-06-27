package com.schoolmgmt.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules SyncWorker. Two distinct work requests, per the doc
 * comment on SyncWorker:
 *  - PERIODIC: routine background sync, runs roughly every 30 minutes
 *    whenever there's network. 30 min is a reasonable default for a
 *    school office app (not so frequent it drains battery/data, not so
 *    rare that offline edits sit unsynced all day) — adjust freely.
 *  - ONE-TIME (expedited): triggered right after login or a manual
 *    "sync now" tap, so the person doesn't wait for the next periodic
 *    window just to see their own recent changes reflected.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        // KEEP, not REPLACE: re-calling this on every app launch (e.g.
        // from Application.onCreate) shouldn't reset an already-scheduled
        // periodic job's timing.
        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Call right after a successful login, or from a manual "sync now" action. */
    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        // REPLACE here (unlike periodic): a fresh manual "sync now" tap
        // should supersede any already-queued/running one-time sync,
        // not queue up a second one behind it.
        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME_ONE_TIME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME_PERIODIC)
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME_ONE_TIME)
    }
}
