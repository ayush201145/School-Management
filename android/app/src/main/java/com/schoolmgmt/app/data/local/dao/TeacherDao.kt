package com.schoolmgmt.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.schoolmgmt.app.data.local.entity.TeacherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(teacher: TeacherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(teachers: List<TeacherEntity>)

    @Update
    suspend fun update(teacher: TeacherEntity)

    @Query("UPDATE teachers SET isDeleted = 1, isActive = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM teachers WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): TeacherEntity?

    @Query("SELECT * FROM teachers WHERE isDeleted = 0 ORDER BY firstName ASC")
    fun observeAll(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers WHERE isActive = 1 AND isDeleted = 0 ORDER BY firstName ASC")
    fun observeActive(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<TeacherEntity>

    @Query("UPDATE teachers SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}
