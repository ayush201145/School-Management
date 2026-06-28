package com.schoolmgmt.app.di

import android.content.Context
import androidx.room.Room
import com.schoolmgmt.app.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            // NOTE: fallbackToDestructiveMigration is intentionally OMITTED.
            // Every future schema version bump needs a real Migration
            // (see MIGRATION_1_2 above as the pattern to follow) once
            // this app has real user data on real devices, or upgrading
            // the app would silently wipe every school's local data.
            .build()
}
