package com.schoolmgmt.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Implementing Configuration.Provider and supplying HiltWorkerFactory
 * here is REQUIRED for @HiltWorker-annotated workers (SyncWorker) to
 * actually be constructible at runtime. Without this, WorkManager
 * falls back to its default WorkerFactory, which doesn't know how to
 * supply SyncWorker's injected SyncRepository dependency, and the app
 * crashes with NoSuchMethodException the first time SyncWorker runs —
 * this is a real runtime failure, not something the compiler catches.
 *
 * This must be paired with disabling WorkManager's default
 * androidx.startup initializer in AndroidManifest.xml (see the
 * <provider tools:node="remove"> block there) — both pieces are
 * required together.
 */
@HiltAndroidApp
class SchoolManagementApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
