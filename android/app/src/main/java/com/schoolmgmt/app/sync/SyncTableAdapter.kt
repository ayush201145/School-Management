package com.schoolmgmt.app.sync

import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.entity.PaymentEntity
import com.schoolmgmt.app.data.local.entity.SectionEntity
import com.schoolmgmt.app.data.local.entity.StudentEntity
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import com.schoolmgmt.app.data.local.entity.TeacherEntity
import com.schoolmgmt.app.data.local.entity.WithdrawalReason

/**
 * One entry per syncable table — mirrors backend/src/sync/syncRegistry.js
 * exactly (same table names, same writable-field sets). Each entry
 * knows how to:
 *  - read which LOCAL rows need pushing (getUnsynced)
 *  - convert a local entity to the generic wire Map (toWireMap)
 *  - apply an incoming wire Map from a pull onto the local table (applyFromServer)
 *  - mark a row as synced after a successful push (markSynced)
 *
 * Adding a new syncable entity means adding ONE entry here — the actual
 * push/pull loop in SyncRepository is generic and doesn't change.
 */
interface SyncTableAdapter {
    val tableName: String
    suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> // id -> wire map
    suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean)
    suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long)
}

object StudentSyncAdapter : SyncTableAdapter {
    override val tableName = "Student"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.studentDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.studentDao().getById(id)
        val merged = StudentEntity(
            id = id,
            admissionNo = (fields["admissionNo"] as? String) ?: existing?.admissionNo ?: "",
            firstName = (fields["firstName"] as? String) ?: existing?.firstName ?: "",
            lastName = (fields["lastName"] as? String) ?: existing?.lastName ?: "",
            dateOfBirth = (fields["dateOfBirth"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.dateOfBirth,
            gender = (fields["gender"] as? String) ?: existing?.gender,
            guardianName = (fields["guardianName"] as? String) ?: existing?.guardianName,
            guardianPhone = (fields["guardianPhone"] as? String) ?: existing?.guardianPhone,
            guardianEmail = (fields["guardianEmail"] as? String) ?: existing?.guardianEmail,
            address = (fields["address"] as? String) ?: existing?.address,
            fatherPhone = (fields["fatherPhone"] as? String) ?: existing?.fatherPhone,
            motherPhone = (fields["motherPhone"] as? String) ?: existing?.motherPhone,
            whatsappPhone = (fields["whatsappPhone"] as? String) ?: existing?.whatsappPhone,
            tuitionFee = (fields["tuitionFee"] as? Double) ?: existing?.tuitionFee,
            sectionId = (fields["sectionId"] as? String) ?: existing?.sectionId ?: "",
            admissionDate = (fields["admissionDate"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.admissionDate ?: updatedAtMillis,
            isActive = (fields["isActive"] as? Boolean) ?: existing?.isActive ?: true,
            withdrawalReason = (fields["withdrawalReason"] as? String)?.let {
                runCatching { WithdrawalReason.valueOf(it) }.getOrNull()
            } ?: existing?.withdrawalReason,
            withdrawnDate = (fields["withdrawnDate"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.withdrawnDate,
            withdrawalNotes = (fields["withdrawalNotes"] as? String) ?: existing?.withdrawalNotes,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis, // came FROM the server, so it's synced as of now
        )
        db.studentDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.studentDao().markSynced(id, syncedAt)
}

object TeacherSyncAdapter : SyncTableAdapter {
    override val tableName = "Teacher"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.teacherDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.teacherDao().getById(id)
        val merged = TeacherEntity(
            id = id,
            employeeNo = (fields["employeeNo"] as? String) ?: existing?.employeeNo ?: "",
            firstName = (fields["firstName"] as? String) ?: existing?.firstName ?: "",
            lastName = (fields["lastName"] as? String) ?: existing?.lastName ?: "",
            phone = (fields["phone"] as? String) ?: existing?.phone,
            email = (fields["email"] as? String) ?: existing?.email,
            address = (fields["address"] as? String) ?: existing?.address,
            qualification = (fields["qualification"] as? String) ?: existing?.qualification,
            joiningDate = (fields["joiningDate"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.joiningDate ?: updatedAtMillis,
            isActive = (fields["isActive"] as? Boolean) ?: existing?.isActive ?: true,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.teacherDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.teacherDao().markSynced(id, syncedAt)
}

/**
 * The registry — order matters slightly for PULL (parents before
 * children avoids a brief foreign-key dangling reference mid-sync,
 * though Room's FKs here use NO_ACTION so it's not strictly enforced;
 * still good hygiene). Push order doesn't matter since the server
 * validates independently per change.
 */
val SYNC_REGISTRY: List<SyncTableAdapter> = listOf(
    // Parents before children — see ordering note above.
    TeacherSyncAdapter,
    SchoolClassSyncAdapter,
    SectionSyncAdapter,
    StudentSyncAdapter,
    StudentFeeSyncAdapter,
    PaymentSyncAdapter,
    ItemCategorySyncAdapter,
    ItemVariantSyncAdapter,
    StudentItemPurchaseSyncAdapter,
    InventoryTransactionSyncAdapter,
    AttendanceSyncAdapter,
    TeacherAttendanceSyncAdapter,
    StaffSyncAdapter,
    SalaryPaymentSyncAdapter,
    ExpenseCategorySyncAdapter,
    RecurringExpenseTemplateSyncAdapter,
    ExpenseSyncAdapter,
)
