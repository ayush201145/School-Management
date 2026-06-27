package com.schoolmgmt.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.schoolmgmt.app.data.local.entity.AttendanceEntity
import com.schoolmgmt.app.data.local.entity.SyncConflictEntity
import com.schoolmgmt.app.data.local.entity.TeacherAttendanceEntity
import com.schoolmgmt.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<AttendanceEntity>)

    @Query("SELECT * FROM attendance WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE sectionId = :sectionId AND date = :date AND isDeleted = 0")
    fun observeForSectionAndDate(sectionId: String, date: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId AND isDeleted = 0 ORDER BY date DESC")
    fun observeForStudent(studentId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<AttendanceEntity>

    @Query("UPDATE attendance SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

@Dao
interface TeacherAttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attendance: TeacherAttendanceEntity)

    @Query("SELECT * FROM teacher_attendance WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): TeacherAttendanceEntity?

    @Query("SELECT * FROM teacher_attendance WHERE teacherId = :teacherId AND isDeleted = 0 ORDER BY date DESC")
    fun observeForTeacher(teacherId: String): Flow<List<TeacherAttendanceEntity>>

    @Query("SELECT * FROM teacher_attendance WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<TeacherAttendanceEntity>

    @Query("UPDATE teacher_attendance SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE isDeleted = 0 ORDER BY username ASC")
    fun observeAll(): Flow<List<UserEntity>>
}

/**
 * Conflicts are pulled DOWN from the server for review — they don't
 * follow the normal push/getUnsyncedChanges pattern (see notes on
 * SyncConflictEntity), so this DAO is intentionally simpler.
 */
@Dao
interface SyncConflictDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(conflicts: List<SyncConflictEntity>)

    @Query("SELECT * FROM sync_conflicts WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun observePending(): Flow<List<SyncConflictEntity>>

    @Query("DELETE FROM sync_conflicts WHERE id = :id")
    suspend fun delete(id: String)
}
