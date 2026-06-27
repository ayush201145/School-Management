package com.schoolmgmt.app.data.repository

import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.entity.TeacherEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import androidx.room.withTransaction
import com.schoolmgmt.app.data.local.entity.StaffEntity
import com.schoolmgmt.app.data.local.entity.StaffType

@Singleton
class TeacherRepository @Inject constructor(
    private val db: AppDatabase,
) {
    private val teacherDao = db.teacherDao()

    fun observeAll() = teacherDao.observeAll()
    fun observeActive() = teacherDao.observeActive()
    suspend fun getById(id: String) = teacherDao.getById(id)

    suspend fun createTeacher(
        employeeNo: String,
        firstName: String,
        lastName: String,
        phone: String? = null,
        email: String? = null,
        address: String? = null,
        qualification: String? = null,
        monthlySalary: Double? = null,
    ): TeacherEntity = db.withTransaction {
        val now = System.currentTimeMillis()
        val teacher = TeacherEntity(
            id = UUID.randomUUID().toString(),
            employeeNo = employeeNo,
            firstName = firstName,
            lastName = lastName,
            phone = phone,
            email = email,
            address = address,
            qualification = qualification,
            joiningDate = now,
            updatedAt = now,
        )
        teacherDao.upsert(teacher)

        val staff = StaffEntity(
            id = UUID.randomUUID().toString(),
            name = "$firstName $lastName",
            type = StaffType.TEACHER,
            teacherId = teacher.id,
            phone = phone,
            monthlySalary = monthlySalary,
            joiningDate = now,
            updatedAt = now,
        )
        db.staffDao().upsert(staff)

        return@withTransaction teacher
    }

    suspend fun updateTeacher(teacher: TeacherEntity) =
        teacherDao.update(teacher.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteTeacher(id: String) = teacherDao.softDelete(id)
}
