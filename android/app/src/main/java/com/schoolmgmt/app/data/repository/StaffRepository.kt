package com.schoolmgmt.app.data.repository

import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.dao.SalaryStatusRow
import com.schoolmgmt.app.data.local.entity.PaymentMode
import com.schoolmgmt.app.data.local.entity.SalaryPaymentEntity
import com.schoolmgmt.app.data.local.entity.StaffEntity
import com.schoolmgmt.app.data.local.entity.StaffType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaffRepository @Inject constructor(
    private val db: AppDatabase,
) {
    private val staffDao = db.staffDao()
    private val salaryPaymentDao = db.salaryPaymentDao()

    fun observeActive() = staffDao.observeActive()
    suspend fun getById(id: String) = staffDao.getById(id)

    /**
     * teacherId is optional — set it when this staff member IS a
     * teacher, so their salary record links back to their existing
     * Teacher profile instead of duplicating name/contact info.
     */
    suspend fun createStaff(
        name: String,
        type: StaffType,
        teacherId: String? = null,
        phone: String? = null,
        monthlySalary: Double? = null,
    ): StaffEntity {
        val now = System.currentTimeMillis()
        val staff = StaffEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type,
            teacherId = teacherId,
            phone = phone,
            monthlySalary = monthlySalary,
            joiningDate = now,
            updatedAt = now,
        )
        staffDao.upsert(staff)
        return staff
    }

    suspend fun updateStaff(staff: StaffEntity) =
        staffDao.update(staff.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteStaff(id: String) = staffDao.softDelete(id)

    fun observeSalaryPayments(staffId: String) = salaryPaymentDao.observeForStaff(staffId)

    /** "Who hasn't been paid this month" — local mirror of GET /api/salary-status. */
    fun observeSalaryStatus(month: Int, year: Int) = salaryPaymentDao.observeSalaryStatus(month, year)

    suspend fun recordSalaryPayment(
        staffId: String,
        amount: Double,
        forMonth: Int,
        forYear: Int,
        mode: PaymentMode,
        recordedById: String,
        referenceNo: String? = null,
        notes: String? = null,
        paidAt: Long = System.currentTimeMillis(),
    ): SalaryPaymentEntity {
        require(forMonth in 1..12) { "forMonth must be 1-12" }
        require(amount > 0) { "amount must be greater than 0" }

        // Existence check mirrors the backend's identical guard in
        // salaryController.createSalaryPayment — fail clearly now
        // rather than create an orphaned salary payment for a staff
        // record that doesn't exist (e.g. deleted on another device,
        // not yet synced down to this one).
        if (staffDao.getById(staffId) == null) {
            throw IllegalArgumentException("Staff $staffId not found")
        }

        val now = System.currentTimeMillis()
        val payment = SalaryPaymentEntity(
            id = UUID.randomUUID().toString(),
            staffId = staffId,
            amount = amount,
            forMonth = forMonth,
            forYear = forYear,
            paidAt = paidAt,
            mode = mode,
            referenceNo = referenceNo,
            notes = notes,
            recordedById = recordedById,
            updatedAt = now,
        )
        salaryPaymentDao.upsert(payment)
        return payment
    }
}
