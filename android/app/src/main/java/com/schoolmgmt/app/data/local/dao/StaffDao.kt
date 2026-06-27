package com.schoolmgmt.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.schoolmgmt.app.data.local.entity.SalaryPaymentEntity
import com.schoolmgmt.app.data.local.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(staff: StaffEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(staff: List<StaffEntity>)

    @Update
    suspend fun update(staff: StaffEntity)

    @Query("SELECT * FROM staff WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE isActive = 1 AND isDeleted = 0 ORDER BY name ASC")
    fun observeActive(): Flow<List<StaffEntity>>

    @Query("UPDATE staff SET isDeleted = 1, isActive = 0, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM staff WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<StaffEntity>

    @Query("UPDATE staff SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

/** One row of the "who's been paid this month" status report. */
data class SalaryStatusRow(
    val staffId: String,
    val name: String,
    val monthlySalary: Double?,
    val paidThisMonth: Double,
)

@Dao
interface SalaryPaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: SalaryPaymentEntity)

    @Query("SELECT * FROM salary_payments WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): SalaryPaymentEntity?

    @Query(
        "SELECT * FROM salary_payments WHERE staffId = :staffId AND isDeleted = 0 ORDER BY forYear DESC, forMonth DESC"
    )
    fun observeForStaff(staffId: String): Flow<List<SalaryPaymentEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM salary_payments 
        WHERE staffId = :staffId AND forMonth = :month AND forYear = :year AND isDeleted = 0
        """
    )
    suspend fun getTotalPaidForMonth(staffId: String, month: Int, year: Int): Double

    /**
     * THE "WHO HASN'T BEEN PAID THIS MONTH" REPORT, computed locally —
     * mirrors GET /api/salary-status on the backend. Joins every active
     * staff member with however much they've been paid for the given
     * month/year (0 if nothing yet).
     */
    @Query(
        """
        SELECT 
            s.id AS staffId,
            s.name AS name,
            s.monthlySalary AS monthlySalary,
            COALESCE((
                SELECT SUM(sp.amount) FROM salary_payments sp 
                WHERE sp.staffId = s.id AND sp.forMonth = :month AND sp.forYear = :year AND sp.isDeleted = 0
            ), 0) AS paidThisMonth
        FROM staff s
        WHERE s.isActive = 1 AND s.isDeleted = 0
        ORDER BY s.name ASC
        """
    )
    fun observeSalaryStatus(month: Int, year: Int): Flow<List<SalaryStatusRow>>

    @Query("SELECT * FROM salary_payments WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<SalaryPaymentEntity>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM salary_payments 
        WHERE isDeleted = 0 AND paidAt >= :fromMillis AND paidAt < :toMillisExclusive
        """
    )
    suspend fun getTotalPaidInRange(fromMillis: Long, toMillisExclusive: Long): Double

    @Query("UPDATE salary_payments SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}
