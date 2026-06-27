package com.schoolmgmt.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import com.schoolmgmt.app.data.local.entity.AcademicYearEntity
import com.schoolmgmt.app.data.local.entity.SchoolClassEntity
import com.schoolmgmt.app.data.local.entity.SectionEntity
import com.schoolmgmt.app.data.local.entity.TeacherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicYearDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(year: AcademicYearEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(years: List<AcademicYearEntity>)

    @Query("SELECT * FROM academic_years WHERE isDeleted = 0 ORDER BY startDate DESC")
    fun observeAll(): Flow<List<AcademicYearEntity>>

    @Query("SELECT * FROM academic_years WHERE isCurrent = 1 AND isDeleted = 0 LIMIT 1")
    suspend fun getCurrent(): AcademicYearEntity?

    @Query("SELECT * FROM academic_years WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<AcademicYearEntity>

    @Query("UPDATE academic_years SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

@Dao
interface SchoolClassDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schoolClass: SchoolClassEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(classes: List<SchoolClassEntity>)

    @Query("SELECT * FROM school_classes WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): SchoolClassEntity?

    @Query("SELECT * FROM school_classes WHERE isDeleted = 0 ORDER BY name ASC")
    fun observeAll(): Flow<List<SchoolClassEntity>>

    @Query("SELECT * FROM school_classes WHERE academicYearId = :yearId AND isDeleted = 0 ORDER BY name ASC")
    fun observeByYear(yearId: String): Flow<List<SchoolClassEntity>>

    @Query("SELECT * FROM school_classes WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<SchoolClassEntity>

    @Query("UPDATE school_classes SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

/**
 * Flat picker-friendly result: one section, labeled with its class
 * name. Distinct from SectionWithTeacher (which embeds the full
 * SectionEntity + Teacher relation) — this is intentionally minimal
 * for dropdown/picker UI that just needs an id + display label.
 */
data class SectionWithClassName(
    val id: String,
    val sectionName: String,
    val className: String,
)

/**
 * A Section together with its class teacher's name — used for display
 * screens (e.g. "Class 5 - Section A — Class Teacher: Mrs. Sharma")
 * without a separate lookup. Not an @Entity; query-result-only, same
 * pattern as Room's official UserWithPosts example.
 */
data class SectionWithTeacher(
    @Embedded val section: SectionEntity,
    @Relation(parentColumn = "classTeacherId", entityColumn = "id")
    val classTeacher: TeacherEntity?
)

@Dao
interface SectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(section: SectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sections: List<SectionEntity>)

    @Query("UPDATE sections SET classTeacherId = :teacherId, updatedAt = :now WHERE id = :sectionId")
    suspend fun assignClassTeacher(sectionId: String, teacherId: String?, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM sections WHERE classId = :classId AND isDeleted = 0 ORDER BY name ASC")
    fun observeByClass(classId: String): Flow<List<SectionEntity>>

    /** One-shot (non-Flow) read — used inside @Transaction blocks like FeeStructureRepository.assignToClass. */
    @Query("SELECT * FROM sections WHERE classId = :classId AND isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllInClassOnce(classId: String): List<SectionEntity>

    /**
     * Flat list of EVERY section across all classes, with the class
     * name joined in for display (e.g. "Class 5 - Section A") — used
     * by pickers like the add-student dialog where the person needs to
     * choose a section without first drilling into a specific class.
     */
    @Query(
        """
        SELECT sections.id AS id, sections.name AS sectionName, school_classes.name AS className
        FROM sections
        INNER JOIN school_classes ON school_classes.id = sections.classId
        WHERE sections.isDeleted = 0 AND school_classes.isDeleted = 0
        ORDER BY school_classes.name ASC, sections.name ASC
        """
    )
    fun observeAllWithClassName(): Flow<List<SectionWithClassName>>

    @androidx.room.Transaction
    @Query("SELECT * FROM sections WHERE classId = :classId AND isDeleted = 0 ORDER BY name ASC")
    fun observeByClassWithTeacher(classId: String): Flow<List<SectionWithTeacher>>

    @Query("SELECT * FROM sections WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): SectionEntity?

    @Query("SELECT * FROM sections WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<SectionEntity>

    @Query("UPDATE sections SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}
