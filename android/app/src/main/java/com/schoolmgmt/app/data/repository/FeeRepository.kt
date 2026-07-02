package com.schoolmgmt.app.data.repository

import androidx.room.withTransaction
import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.dao.DueRow
import com.schoolmgmt.app.data.local.dao.TransactionRow
import com.schoolmgmt.app.data.local.entity.FeeStatus
import com.schoolmgmt.app.data.local.entity.PaymentEntity
import com.schoolmgmt.app.data.local.entity.PaymentMode
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thrown when a payment would exceed the remaining balance on a fee —
 * the LOCAL mirror of the 400 error the backend's paymentController.js
 * returns for the same situation. Keeping the same business rule on
 * both sides means the user gets the same feedback whether they're
 * online or offline.
 */
class OverpaymentException(message: String) : Exception(message)

@Singleton
class FeeRepository @Inject constructor(
    private val db: AppDatabase,
) {
    private val studentFeeDao = db.studentFeeDao()
    private val paymentDao = db.paymentDao()
    private val invoiceSettingsDao = db.invoiceSettingsDao()
    private val feeCategoryDao = db.feeCategoryDao()

    fun observeInvoiceSettings(): Flow<com.schoolmgmt.app.data.local.entity.InvoiceSettingsEntity?> =
        invoiceSettingsDao.observeSettings()

    suspend fun getInvoiceSettings(): com.schoolmgmt.app.data.local.entity.InvoiceSettingsEntity? =
        invoiceSettingsDao.getSettings()

    fun observeFeeCategories(): Flow<List<com.schoolmgmt.app.data.local.entity.FeeCategoryEntity>> =
        feeCategoryDao.observeAll()

    suspend fun createFeeCategory(name: String, description: String? = null) {
        val cat = com.schoolmgmt.app.data.local.entity.FeeCategoryEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            updatedAt = System.currentTimeMillis()
        )
        feeCategoryDao.upsert(cat)
    }

    suspend fun updateFeeCategory(id: String, name: String, description: String? = null) {
        val existing = feeCategoryDao.getById(id) ?: return
        val updated = existing.copy(
            name = name,
            description = description,
            updatedAt = System.currentTimeMillis()
        )
        feeCategoryDao.upsert(updated)
    }

    suspend fun deleteFeeCategory(id: String) {
        feeCategoryDao.deleteCategory(id, System.currentTimeMillis())
    }

    fun observeDues(sectionId: String? = null, overdueOnly: Boolean = false): Flow<List<DueRow>> =
        studentFeeDao.observeDues(sectionId = sectionId, overdueOnly = overdueOnly)

    fun observeTransactions(
        fromMillis: Long? = null,
        toMillis: Long? = null,
        studentId: String? = null,
    ): Flow<List<TransactionRow>> =
        paymentDao.observeTransactions(fromMillis, toMillis, studentId)

    fun observeFeesForStudent(studentId: String) = studentFeeDao.observeByStudent(studentId)
    fun observePaymentsForFee(studentFeeId: String) = paymentDao.observeForFee(studentFeeId)

    /** Remaining balance on a fee — (amount - discount) minus whatever's already been paid. */
    suspend fun getRemainingBalance(studentFeeId: String): Double {
        val fee = studentFeeDao.getById(studentFeeId) ?: return 0.0
        val alreadyPaid = paymentDao.getTotalPaidForFee(studentFeeId)
        return (fee.amount - fee.discount - alreadyPaid).coerceAtLeast(0.0)
    }

    suspend fun markFeeAsDefaulted(studentFeeId: String) {
        val existing = studentFeeDao.getById(studentFeeId) ?: return
        val updated = existing.copy(
            isDefaulted = true,
            updatedAt = System.currentTimeMillis(),
            syncedAt = null
        )
        studentFeeDao.upsert(updated)
    }

    /**
     * Records a payment against a StudentFee and recalculates its
     * status, ATOMICALLY — both writes happen in one Room transaction
     * (db.withTransaction), so a crash or process death between the two
     * steps can never leave a payment recorded with a stale status, or
     * vice versa. This is the local mirror of the backend's
     * feeStatusService.recalculateStudentFeeStatus, called from
     * paymentController.createPayment.
     *
     * recordedById should be the locally-cached current user's id (see
     * AuthRepository) — payments recorded offline still need an owner
     * for audit purposes, same as the backend's Payment.recordedById.
     */
    suspend fun recordPayment(
        studentFeeId: String,
        amount: Double,
        mode: PaymentMode,
        recordedById: String,
        referenceNo: String? = null,
        notes: String? = null,
        paidAt: Long = System.currentTimeMillis(),
    ): PaymentEntity = db.withTransaction {
        val fee = studentFeeDao.getById(studentFeeId)
            ?: throw IllegalArgumentException("StudentFee $studentFeeId not found")

        val alreadyPaid = paymentDao.getTotalPaidForFee(studentFeeId)
        val payable = fee.amount - fee.discount
        val remaining = payable - alreadyPaid

        // Same overpayment guard as the backend — reject rather than
        // silently allow a negative balance, even while offline.
        if (amount > remaining + 0.01) {
            throw OverpaymentException(
                "Payment of $amount exceeds remaining balance of ${"%.2f".format(remaining)} for this fee"
            )
        }

        val now = System.currentTimeMillis()
        val payment = PaymentEntity(
            id = UUID.randomUUID().toString(),
            studentFeeId = studentFeeId,
            amount = amount,
            mode = mode,
            referenceNo = referenceNo,
            paidAt = paidAt,
            recordedById = recordedById,
            notes = notes,
            updatedAt = now,
        )
        paymentDao.upsert(payment)

        val newTotalPaid = alreadyPaid + amount
        val newStatus = when {
            newTotalPaid <= 0.0 -> FeeStatus.UNPAID
            newTotalPaid >= payable -> FeeStatus.PAID
            else -> FeeStatus.PARTIAL
        }
        if (newStatus != fee.status) {
            studentFeeDao.update(fee.copy(status = newStatus, updatedAt = now))
        }

        payment
    }

    suspend fun recordBulkPayment(
        studentId: String,
        totalAmount: Double,
        mode: PaymentMode,
        recordedById: String,
        referenceNo: String? = null,
        notes: String? = null,
        paidAt: Long = System.currentTimeMillis()
    ): BulkPaymentResult = db.withTransaction {
        require(totalAmount > 0) { "Amount must be greater than 0" }
        
        val unpaidFees = studentFeeDao.getUnpaidForStudentOnce(studentId)
        if (unpaidFees.isEmpty()) {
            return@withTransaction BulkPaymentResult(emptyList(), emptyList())
        }
        
        var remainingPayment = totalAmount
        val paymentsCreated = mutableListOf<PaymentEntity>()
        val itemsPaid = mutableListOf<ReceiptItem>()
        
        for (fee in unpaidFees) {
            if (remainingPayment <= 0.0) break
            
            val alreadyPaid = paymentDao.getTotalPaidForFee(fee.id)
            val payable = fee.amount - fee.discount
            val remainingFeeBalance = (payable - alreadyPaid).coerceAtLeast(0.0)
            
            if (remainingFeeBalance <= 0.0) continue
            
            val allocation = minOf(remainingFeeBalance, remainingPayment)
            val now = System.currentTimeMillis()
            val payment = PaymentEntity(
                id = UUID.randomUUID().toString(),
                studentFeeId = fee.id,
                amount = allocation,
                mode = mode,
                referenceNo = referenceNo,
                paidAt = paidAt,
                recordedById = recordedById,
                notes = notes,
                updatedAt = now,
            )
            paymentDao.upsert(payment)
            paymentsCreated.add(payment)
            itemsPaid.add(ReceiptItem(fee.description, allocation))
            
            val newTotalPaid = alreadyPaid + allocation
            val newStatus = when {
                newTotalPaid <= 0.0 -> FeeStatus.UNPAID
                newTotalPaid >= payable -> FeeStatus.PAID
                else -> FeeStatus.PARTIAL
            }
            if (newStatus != fee.status) {
                studentFeeDao.update(fee.copy(status = newStatus, updatedAt = now))
            }
            
            remainingPayment -= allocation
        }
        
        return@withTransaction BulkPaymentResult(paymentsCreated, itemsPaid)
    }

    /** Creates an ad-hoc or structure-derived StudentFee row (used by bulk-assign, see FeeStructureRepository). */
    suspend fun createStudentFee(fee: StudentFeeEntity) = studentFeeDao.upsert(fee)
}

data class ReceiptItem(val description: String, val amount: Double)
data class BulkPaymentResult(val payments: List<PaymentEntity>, val items: List<ReceiptItem>)
