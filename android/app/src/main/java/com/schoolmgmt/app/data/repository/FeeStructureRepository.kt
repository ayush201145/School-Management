package com.schoolmgmt.app.data.repository

import androidx.room.withTransaction
import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.entity.FeeStructureEntity
import com.schoolmgmt.app.data.local.entity.SchoolClassEntity
import com.schoolmgmt.app.data.local.entity.SectionEntity
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import java.util.UUID
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcademicRepository @Inject constructor(
    private val db: AppDatabase,
) {
    private val academicYearDao = db.academicYearDao()
    private val schoolClassDao = db.schoolClassDao()
    private val sectionDao = db.sectionDao()

    fun observeClasses() = schoolClassDao.observeAll()
    fun observeSections(classId: String) = sectionDao.observeByClass(classId)
    fun observeSectionsWithTeacher(classId: String) = sectionDao.observeByClassWithTeacher(classId)
    fun observeAllSectionsWithClassName() = sectionDao.observeAllWithClassName()
    suspend fun getCurrentYear() = academicYearDao.getCurrent()

    suspend fun createClass(name: String, academicYearId: String): SchoolClassEntity = db.withTransaction {
        val now = System.currentTimeMillis()
        val schoolClass = SchoolClassEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            academicYearId = academicYearId,
            updatedAt = now,
        )
        schoolClassDao.upsert(schoolClass)

        val defaultSection = SectionEntity(
            id = UUID.randomUUID().toString(),
            name = "General",
            classId = schoolClass.id,
            updatedAt = now,
        )
        sectionDao.upsert(defaultSection)

        return@withTransaction schoolClass
    }

    suspend fun createSection(name: String, classId: String, classTeacherId: String? = null): SectionEntity {
        val now = System.currentTimeMillis()
        val section = SectionEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            classId = classId,
            classTeacherId = classTeacherId,
            updatedAt = now,
        )
        sectionDao.upsert(section)
        return section
    }

    suspend fun assignClassTeacher(sectionId: String, teacherId: String?) =
        sectionDao.assignClassTeacher(sectionId, teacherId)

    suspend fun getDefaultSectionForClass(classId: String): String = db.withTransaction {
        val sections = sectionDao.getAllInClassOnce(classId)
        val general = sections.firstOrNull { it.name.equals("General", ignoreCase = true) }
        if (general != null) {
            return@withTransaction general.id
        }
        val first = sections.firstOrNull()
        if (first != null) {
            return@withTransaction first.id
        }
        val now = System.currentTimeMillis()
        val defaultSection = SectionEntity(
            id = UUID.randomUUID().toString(),
            name = "General",
            classId = classId,
            updatedAt = now,
        )
        sectionDao.upsert(defaultSection)
        return@withTransaction defaultSection.id
    }
}

/** Result of a bulk fee assignment — mirrors the backend's assign endpoint response shape. */
data class BulkAssignResult(
    val created: Int,
    val skipped: Int,
    val totalStudentsInScope: Int,
)

/**
 * Handles the bulk fee-assign button (#6) — separate from AcademicRepository
 * since it spans FeeStructure/StudentFee/Student/Section, not just academic
 * structure. Mirrors backend/src/controllers/feeController.assignFeeStructure
 * EXACTLY, including the idempotency guarantee verified by a real test on
 * the backend: re-clicking "assign" never double-bills a student who
 * already has a StudentFee for this exact structure.
 */
@Singleton
class FeeStructureRepository @Inject constructor(
    private val db: AppDatabase,
) {
    private val feeStructureDao = db.feeStructureDao()
    private val sectionDao = db.sectionDao()
    private val studentDao = db.studentDao()
    private val studentFeeDao = db.studentFeeDao()

    fun observeByClass(classId: String) = feeStructureDao.observeByClass(classId)

    fun observeFeeCategories() = db.feeCategoryDao().observeAll()
    suspend fun getCurrentAcademicYear() = db.academicYearDao().getCurrent()

    suspend fun createFeeStructure(
        feeCategoryId: String,
        classId: String,
        academicYearId: String,
        amount: Double,
        dueDate: Long,
        description: String? = null,
    ): FeeStructureEntity {
        val now = System.currentTimeMillis()
        val structure = FeeStructureEntity(
            id = UUID.randomUUID().toString(),
            feeCategoryId = feeCategoryId,
            classId = classId,
            academicYearId = academicYearId,
            amount = amount,
            dueDate = dueDate,
            description = description,
            updatedAt = now,
        )
        feeStructureDao.upsert(structure)
        return structure
    }

    suspend fun createMonthlyRecurringFeeStructures(
        feeCategoryId: String,
        classId: String,
        academicYearId: String,
        amount: Double,
        baseDueDate: Long,
        descriptionPrefix: String?
    ): List<FeeStructureEntity> = db.withTransaction {
        val year = db.academicYearDao().getById(academicYearId) ?: return@withTransaction emptyList()
        val startLocalDate = Instant.ofEpochMilli(year.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
        val endLocalDate = Instant.ofEpochMilli(year.endDate).atZone(ZoneId.systemDefault()).toLocalDate()
        val baseDate = Instant.ofEpochMilli(baseDueDate).atZone(ZoneId.systemDefault()).toLocalDate()
        
        val structures = mutableListOf<FeeStructureEntity>()
        var current = baseDate
        
        while (!current.isBefore(startLocalDate) && !current.isAfter(endLocalDate)) {
            val monthName = current.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.US)
            val desc = "${descriptionPrefix ?: "Tuition Fee"} - $monthName ${current.year}"
            val dueDateMillis = current.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val structure = FeeStructureEntity(
                id = UUID.randomUUID().toString(),
                feeCategoryId = feeCategoryId,
                classId = classId,
                academicYearId = academicYearId,
                amount = amount,
                dueDate = dueDateMillis,
                description = desc,
                updatedAt = System.currentTimeMillis()
            )
            feeStructureDao.upsert(structure)
            structures.add(structure)
            
            current = current.plusMonths(1)
        }
        return@withTransaction structures
    }

    /**
     * THE BULK-ASSIGN ENGINE (#6), local-first equivalent of
     * POST /api/fee-structures/:id/assign on the backend.
     *
     * sectionId is optional — pass null to bill the WHOLE class, or a
     * specific section id to bill just that section. Safe to call more
     * than once: students who already have a StudentFee for this exact
     * structure are skipped, never double-billed.
     */
    suspend fun assignToClass(
        feeStructureId: String,
        sectionId: String? = null,
    ): BulkAssignResult = db.withTransaction {
        val structure = feeStructureDao.getById(feeStructureId)
            ?: throw IllegalArgumentException("FeeStructure $feeStructureId not found")

        val sections = if (sectionId != null) {
            listOfNotNull(sectionDao.getById(sectionId))
        } else {
            // observeByClass is a Flow; for a one-shot read inside a
            // transaction we need the DAO's underlying query directly.
            // (See note below — exposed via a dedicated suspend query.)
            sectionDao.getAllInClassOnce(structure.classId)
        }
        if (sections.isEmpty()) {
            return@withTransaction BulkAssignResult(created = 0, skipped = 0, totalStudentsInScope = 0)
        }

        val students = sections.flatMap { section ->
            studentDao.getActiveInSectionOnce(section.id)
        }
        if (students.isEmpty()) {
            return@withTransaction BulkAssignResult(created = 0, skipped = 0, totalStudentsInScope = 0)
        }

        val studentIds = students.map { it.id }
        val alreadyBilledIds = studentFeeDao
            .getStudentIdsAlreadyBilledForStructure(feeStructureId, studentIds)
            .toSet()

        val category = db.feeCategoryDao().getById(structure.feeCategoryId)
        val isTuitionCategory = category?.name?.contains("Tuition", ignoreCase = true) ?: false

        val now = System.currentTimeMillis()
        val toCreate = students
            .filter { it.id !in alreadyBilledIds }
            .map { student ->
                val finalAmount = if (isTuitionCategory && student.tuitionFee != null) {
                    student.tuitionFee
                } else {
                    structure.amount
                }
                StudentFeeEntity(
                    id = UUID.randomUUID().toString(),
                    studentId = student.id,
                    feeStructureId = structure.id,
                    description = structure.description ?: "Fee for ${structure.classId}",
                    amount = finalAmount,
                    dueDate = structure.dueDate,
                    updatedAt = now,
                )
            }

        if (toCreate.isNotEmpty()) {
            studentFeeDao.upsertAll(toCreate)
        }

        BulkAssignResult(
            created = toCreate.size,
            skipped = alreadyBilledIds.size,
            totalStudentsInScope = students.size,
        )
    }
}
