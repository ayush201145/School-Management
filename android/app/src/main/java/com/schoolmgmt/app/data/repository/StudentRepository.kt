package com.schoolmgmt.app.data.repository

import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.entity.StudentEntity
import com.schoolmgmt.app.data.local.entity.WithdrawalReason
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepository @Inject constructor(
    private val db: AppDatabase,
) {
    private val studentDao = db.studentDao()

    fun observeAll() = studentDao.observeAll()
    fun observeByAcademicYear(yearId: String) = studentDao.observeByAcademicYear(yearId)
    fun observeBySection(sectionId: String) = studentDao.observeBySection(sectionId)
    fun search(query: String) = studentDao.search(query)
    fun searchByAcademicYear(yearId: String, query: String) = studentDao.searchByAcademicYear(yearId, query)
    suspend fun getById(id: String) = studentDao.getById(id)

    /**
     * Creates a student LOCALLY first — this is the core offline-first
     * pattern. The id is generated on-device with UUID (matching the
     * backend's @default(uuid()) id strategy), so the row is fully
     * usable immediately and carries a permanent, globally-unique id
     * before it has ever talked to the server. The sync engine picks
     * this row up later via getUnsyncedChanges() and pushes it.
     */
    suspend fun createStudent(
        admissionNo: String?,
        firstName: String,
        lastName: String,
        sectionId: String,
        dateOfBirth: Long? = null,
        gender: String? = null,
        guardianName: String? = null,
        guardianPhone: String? = null,
        guardianEmail: String? = null,
        address: String? = null,
        fatherPhone: String? = null,
        motherPhone: String? = null,
        whatsappPhone: String? = null,
        tuitionFee: Double? = null,
    ): StudentEntity {
        val finalAdmissionNo = if (admissionNo.isNullOrBlank()) {
            val allNos = studentDao.getAllAdmissionNumbers()
            var maxNo = 0
            for (no in allNos) {
                val num = no.toIntOrNull()
                if (num != null && num > maxNo) {
                    maxNo = num
                }
            }
            (maxNo + 1).toString()
        } else {
            admissionNo
        }

        val now = System.currentTimeMillis()
        val student = StudentEntity(
            id = UUID.randomUUID().toString(),
            admissionNo = finalAdmissionNo,
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            gender = gender,
            guardianName = guardianName,
            guardianPhone = guardianPhone,
            guardianEmail = guardianEmail,
            address = address,
            fatherPhone = fatherPhone,
            motherPhone = motherPhone,
            whatsappPhone = whatsappPhone,
            tuitionFee = tuitionFee,
            sectionId = sectionId,
            admissionDate = now,
            updatedAt = now,
        )
        studentDao.upsert(student)
        return student
    }

    suspend fun updateStudent(student: StudentEntity) =
        studentDao.update(student.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteStudent(id: String) = studentDao.softDelete(id)

    /**
     * THE "MARK STUDENT AS QUIT SCHOOL" ACTION. Use this — not
     * deleteStudent — when a student has genuinely left (transferred,
     * graduated, expelled, etc.). The student's record and full
     * fee/payment history are preserved permanently; they just stop
     * appearing in active rosters and dues reports. Reversible via
     * reinstateStudent if marked by mistake or the student re-enrolls.
     */
    suspend fun withdrawStudent(
        id: String,
        reason: WithdrawalReason,
        notes: String? = null,
        withdrawnDate: Long = System.currentTimeMillis(),
    ) = studentDao.withdraw(id, reason, withdrawnDate, notes)

    suspend fun reinstateStudent(id: String) = studentDao.reinstate(id)

    fun observeWithdrawn() = studentDao.observeWithdrawn()
}
