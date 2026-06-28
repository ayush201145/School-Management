package com.schoolmgmt.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.schoolmgmt.app.data.local.entity.StudentEntity
import com.schoolmgmt.app.data.local.entity.WithdrawalReason
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(students: List<StudentEntity>)

    @Update
    suspend fun update(student: StudentEntity)

    /** Soft delete — never hard-delete, see schema notes on Student. */
    @Query("UPDATE students SET isDeleted = 1, isActive = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    /**
     * THE "STUDENT QUIT SCHOOL" ACTION. Distinct from softDelete: the
     * record, and all of the student's fee/payment history, is kept
     * permanently (isDeleted stays 0) — only isActive flips off, with
     * a reason and date recorded. A withdrawn student stops appearing
     * in active rosters/dues reports but their full history remains
     * queryable for school records.
     */
    @Query(
        """
        UPDATE students 
        SET isActive = 0, 
            withdrawalReason = :reason, 
            withdrawnDate = :withdrawnDate, 
            withdrawalNotes = :notes,
            updatedAt = :now 
        WHERE id = :id
        """
    )
    suspend fun withdraw(
        id: String,
        reason: WithdrawalReason,
        withdrawnDate: Long,
        notes: String?,
        now: Long = System.currentTimeMillis(),
    )

    /** Reverses a withdrawal — e.g. the student re-enrolls, or it was marked by mistake. */
    @Query(
        """
        UPDATE students 
        SET isActive = 1, withdrawalReason = NULL, withdrawnDate = NULL, withdrawalNotes = NULL, updatedAt = :now 
        WHERE id = :id
        """
    )
    suspend fun reinstate(id: String, now: Long = System.currentTimeMillis())

    @Query(
        "SELECT * FROM students WHERE isActive = 0 AND withdrawalReason IS NOT NULL AND isDeleted = 0 ORDER BY withdrawnDate DESC"
    )
    fun observeWithdrawn(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): StudentEntity?

    @Query("SELECT * FROM students WHERE isDeleted = 0 ORDER BY firstName ASC")
    fun observeAll(): Flow<List<StudentEntity>>

    @Query(
        """
        SELECT s.* FROM students s
        INNER JOIN sections sec ON sec.id = s.sectionId
        INNER JOIN school_classes c ON c.id = sec.classId
        WHERE s.isDeleted = 0 AND c.academicYearId = :yearId
        ORDER BY s.firstName ASC
        """
    )
    fun observeByAcademicYear(yearId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE sectionId = :sectionId AND isDeleted = 0 ORDER BY firstName ASC")
    fun observeBySection(sectionId: String): Flow<List<StudentEntity>>

    /** One-shot (non-Flow) read of active students in a section — used inside @Transaction blocks like FeeStructureRepository.assignToClass. */
    @Query("SELECT * FROM students WHERE sectionId = :sectionId AND isActive = 1 AND isDeleted = 0")
    suspend fun getActiveInSectionOnce(sectionId: String): List<StudentEntity>

    @Query(
        """
        SELECT * FROM students 
        WHERE isDeleted = 0 
        AND (firstName LIKE '%' || :query || '%' 
             OR lastName LIKE '%' || :query || '%' 
             OR admissionNo LIKE '%' || :query || '%'
             OR guardianPhone LIKE '%' || :query || '%')
        ORDER BY firstName ASC
        """
    )
    fun search(query: String): Flow<List<StudentEntity>>

    @Query(
        """
        SELECT s.* FROM students s
        INNER JOIN sections sec ON sec.id = s.sectionId
        INNER JOIN school_classes c ON c.id = sec.classId
        WHERE s.isDeleted = 0 AND c.academicYearId = :yearId
        AND (s.firstName LIKE '%' || :query || '%' 
             OR s.lastName LIKE '%' || :query || '%' 
             OR s.admissionNo LIKE '%' || :query || '%'
             OR s.guardianPhone LIKE '%' || :query || '%')
        ORDER BY s.firstName ASC
        """
    )
    fun searchByAcademicYear(yearId: String, query: String): Flow<List<StudentEntity>>

    @Query(
        """
        SELECT s.* FROM students s
        INNER JOIN sections sec ON sec.id = s.sectionId
        WHERE s.isDeleted = 0 AND sec.classId = :classId
        ORDER BY s.firstName ASC
        """
    )
    fun observeByClass(classId: String): Flow<List<StudentEntity>>

    @Query(
        """
        SELECT s.* FROM students s
        INNER JOIN sections sec ON sec.id = s.sectionId
        WHERE s.isDeleted = 0 AND sec.classId = :classId
        AND (s.firstName LIKE '%' || :query || '%' 
             OR s.lastName LIKE '%' || :query || '%' 
             OR s.admissionNo LIKE '%' || :query || '%'
             OR s.guardianPhone LIKE '%' || :query || '%')
        ORDER BY s.firstName ASC
        """
    )
    fun searchByClass(classId: String, query: String): Flow<List<StudentEntity>>

    @Query("SELECT admissionNo FROM students")
    suspend fun getAllAdmissionNumbers(): List<String>

    @Query("SELECT * FROM students WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<StudentEntity>

    @Query("UPDATE students SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}
