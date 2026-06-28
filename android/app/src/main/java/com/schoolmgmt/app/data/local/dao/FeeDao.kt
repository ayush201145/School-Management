package com.schoolmgmt.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.schoolmgmt.app.data.local.entity.FeeCategoryEntity
import com.schoolmgmt.app.data.local.entity.FeeStructureEntity
import com.schoolmgmt.app.data.local.entity.PaymentEntity
import com.schoolmgmt.app.data.local.entity.StudentEntity
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeeCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: FeeCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<FeeCategoryEntity>)

    @Query("SELECT * FROM fee_categories WHERE isDeleted = 0 ORDER BY name ASC")
    fun observeAll(): Flow<List<FeeCategoryEntity>>

    @Query("SELECT * FROM fee_categories WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): FeeCategoryEntity?

    @Query("SELECT * FROM fee_categories WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<FeeCategoryEntity>

    @Query("UPDATE fee_categories SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

@Dao
interface FeeStructureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(structure: FeeStructureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(structures: List<FeeStructureEntity>)

    @Query("SELECT * FROM fee_structures WHERE classId = :classId AND isDeleted = 0 ORDER BY dueDate DESC")
    fun observeByClass(classId: String): Flow<List<FeeStructureEntity>>

    @Query("SELECT * FROM fee_structures WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): FeeStructureEntity?

    @Query("SELECT * FROM fee_structures WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<FeeStructureEntity>

    @Query("UPDATE fee_structures SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

/** A due row joined with the student's display info — what the dues report screen renders directly. */
data class DueRow(
    val feeId: String,
    val description: String,
    val dueDate: Long,
    val status: String,
    val amount: Double,
    val discount: Double,
    val paidAmount: Double,
    val studentId: String,
    val studentFirstName: String,
    val studentLastName: String,
    val admissionNo: String,
    val guardianPhone: String?,
)

@Dao
interface StudentFeeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(fee: StudentFeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(fees: List<StudentFeeEntity>)

    @Update
    suspend fun update(fee: StudentFeeEntity)

    @Query("SELECT * FROM student_fees WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): StudentFeeEntity?

    @Query("SELECT * FROM student_fees WHERE studentId = :studentId AND isDeleted = 0 ORDER BY dueDate DESC")
    fun observeByStudent(studentId: String): Flow<List<StudentFeeEntity>>

    @Query("SELECT * FROM student_fees WHERE studentId = :studentId AND status != 'PAID' AND isDeleted = 0 ORDER BY dueDate ASC")
    suspend fun getUnpaidForStudentOnce(studentId: String): List<StudentFeeEntity>

    /**
     * THE IDEMPOTENCY CHECK for bulk-assign. Returns which of the given
     * student ids ALREADY have a (non-deleted) StudentFee tied to this
     * exact FeeStructure, so FeeStructureRepository.assignToClass can
     * skip them and never double-bill on a repeat click. Mirrors the
     * backend's identical check in feeController.assignFeeStructure,
     * which was verified with a real idempotency test.
     */
    @Query(
        """
        SELECT studentId FROM student_fees 
        WHERE feeStructureId = :feeStructureId 
        AND isDeleted = 0 
        AND studentId IN (:studentIds)
        """
    )
    suspend fun getStudentIdsAlreadyBilledForStructure(feeStructureId: String, studentIds: List<String>): List<String>

    // NOTE: status recalculation (UNPAID/PARTIAL/PAID) after a payment is
    // recorded is NOT done here. Room DAOs can only coordinate calls to
    // their OWN methods inside a @Transaction-annotated function — they
    // can't safely depend on a different DAO (PaymentDao) or an injected
    // lambda. That orchestration logic lives in FeeRepository instead,
    // using a database-level @Transaction method that can see both
    // StudentFeeDao and PaymentDao. See FeeRepository.recordPayment().

    /**
     * THE DUES REPORT (#5). Every unpaid/partial fee joined with student
     * info, with the actually-paid amount computed inline so the UI can
     * show a running balance without a second query per row.
     */
    @Query(
        """
        SELECT 
            sf.id AS feeId,
            sf.description AS description,
            sf.dueDate AS dueDate,
            sf.status AS status,
            sf.amount AS amount,
            sf.discount AS discount,
            COALESCE((
                SELECT SUM(p.amount) FROM payments p 
                WHERE p.studentFeeId = sf.id AND p.isDeleted = 0
            ), 0) AS paidAmount,
            s.id AS studentId,
            s.firstName AS studentFirstName,
            s.lastName AS studentLastName,
            s.admissionNo AS admissionNo,
            s.guardianPhone AS guardianPhone
        FROM student_fees sf
        INNER JOIN students s ON s.id = sf.studentId
        WHERE sf.isDeleted = 0 
          AND sf.status IN ('UNPAID', 'PARTIAL')
          AND (:sectionId IS NULL OR s.sectionId = :sectionId)
          AND (:overdueOnly = 0 OR sf.dueDate < :nowMillis)
        ORDER BY sf.dueDate ASC
        """
    )
    fun observeDues(
        sectionId: String? = null,
        overdueOnly: Boolean = false,
        nowMillis: Long = System.currentTimeMillis(),
    ): Flow<List<DueRow>>

    @Query("SELECT * FROM student_fees WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<StudentFeeEntity>

    @Query("UPDATE student_fees SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}

/** One payment mode's total within a date range — used by the monthly report's by-mode breakdown. */
data class ModeTotal(val mode: String, val total: Double)

/** One row of the transaction ledger (#7), with student name pre-joined for display. */
data class TransactionRow(
    @Embedded val payment: PaymentEntity,
    val studentFirstName: String,
    val studentLastName: String,
    val admissionNo: String,
    val feeDescription: String,
)

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(payments: List<PaymentEntity>)

    @Query("SELECT * FROM payments WHERE id = :id AND isDeleted = 0")
    suspend fun getById(id: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE studentFeeId = :studentFeeId AND isDeleted = 0 ORDER BY paidAt DESC")
    fun observeForFee(studentFeeId: String): Flow<List<PaymentEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM payments 
        WHERE studentFeeId = :studentFeeId AND isDeleted = 0
        """
    )
    suspend fun getTotalPaidForFee(studentFeeId: String): Double

    /**
     * THE FULL TRANSACTION LEDGER (#7), with student + fee context
     * joined in, filterable by date range — mirrors GET /api/transactions.
     */
    @Query(
        """
        SELECT 
            p.*,
            s.firstName AS studentFirstName,
            s.lastName AS studentLastName,
            s.admissionNo AS admissionNo,
            sf.description AS feeDescription
        FROM payments p
        INNER JOIN student_fees sf ON sf.id = p.studentFeeId
        INNER JOIN students s ON s.id = sf.studentId
        WHERE p.isDeleted = 0
          AND (:fromMillis IS NULL OR p.paidAt >= :fromMillis)
          AND (:toMillis IS NULL OR p.paidAt <= :toMillis)
          AND (:studentId IS NULL OR s.id = :studentId)
        ORDER BY p.paidAt DESC
        """
    )
    fun observeTransactions(
        fromMillis: Long? = null,
        toMillis: Long? = null,
        studentId: String? = null,
    ): Flow<List<TransactionRow>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM payments 
        WHERE isDeleted = 0 AND paidAt >= :fromMillis AND paidAt < :toMillisExclusive
        """
    )
    suspend fun getTotalCollected(fromMillis: Long, toMillisExclusive: Long): Double

    /** Same date-range sum, broken down by payment mode — for reconciling cash vs bank at month end. */
    @Query(
        """
        SELECT mode, COALESCE(SUM(amount), 0) as total FROM payments 
        WHERE isDeleted = 0 AND paidAt >= :fromMillis AND paidAt < :toMillisExclusive
        GROUP BY mode
        """
    )
    suspend fun getCollectedByMode(fromMillis: Long, toMillisExclusive: Long): List<ModeTotal>

    @Query("SELECT * FROM payments WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedChanges(): List<PaymentEntity>

    @Query("UPDATE payments SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Long)
}
