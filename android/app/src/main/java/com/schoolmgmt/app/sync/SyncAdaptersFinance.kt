package com.schoolmgmt.app.sync

import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.entity.ExpenseCategoryEntity
import com.schoolmgmt.app.data.local.entity.ExpenseEntity
import com.schoolmgmt.app.data.local.entity.PaymentMode
import com.schoolmgmt.app.data.local.entity.RecurrenceFrequency
import com.schoolmgmt.app.data.local.entity.RecurringExpenseTemplateEntity
import com.schoolmgmt.app.data.local.entity.SalaryPaymentEntity
import com.schoolmgmt.app.data.local.entity.StaffEntity
import com.schoolmgmt.app.data.local.entity.StaffType

object StaffSyncAdapter : SyncTableAdapter {
    override val tableName = "Staff"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.staffDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.staffDao().getById(id)
        val merged = StaffEntity(
            id = id,
            name = (fields["name"] as? String) ?: existing?.name ?: "",
            type = (fields["type"] as? String)?.let {
                runCatching { StaffType.valueOf(it) }.getOrNull()
            } ?: existing?.type ?: StaffType.OTHER,
            teacherId = (fields["teacherId"] as? String) ?: existing?.teacherId,
            phone = (fields["phone"] as? String) ?: existing?.phone,
            monthlySalary = (fields["monthlySalary"] as? Double) ?: existing?.monthlySalary,
            joiningDate = (fields["joiningDate"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.joiningDate ?: updatedAtMillis,
            isActive = (fields["isActive"] as? Boolean) ?: existing?.isActive ?: true,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.staffDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.staffDao().markSynced(id, syncedAt)
}

object SalaryPaymentSyncAdapter : SyncTableAdapter {
    override val tableName = "SalaryPayment"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.salaryPaymentDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.salaryPaymentDao().getById(id)
        val merged = SalaryPaymentEntity(
            id = id,
            staffId = (fields["staffId"] as? String) ?: existing?.staffId ?: "",
            amount = (fields["amount"] as? Double) ?: existing?.amount ?: 0.0,
            forMonth = (fields["forMonth"] as? Double)?.toInt() ?: existing?.forMonth ?: 1,
            forYear = (fields["forYear"] as? Double)?.toInt() ?: existing?.forYear ?: 1970,
            paidAt = (fields["paidAt"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.paidAt ?: updatedAtMillis,
            mode = (fields["mode"] as? String)?.let {
                runCatching { PaymentMode.valueOf(it) }.getOrNull()
            } ?: existing?.mode ?: PaymentMode.OTHER,
            referenceNo = (fields["referenceNo"] as? String) ?: existing?.referenceNo,
            notes = (fields["notes"] as? String) ?: existing?.notes,
            recordedById = (fields["recordedById"] as? String) ?: existing?.recordedById ?: "",
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.salaryPaymentDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.salaryPaymentDao().markSynced(id, syncedAt)
}

object ExpenseCategorySyncAdapter : SyncTableAdapter {
    override val tableName = "ExpenseCategory"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.expenseCategoryDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        // ExpenseCategoryDao has no getById (categories are simple
        // reference data, same as FeeCategoryDao before it) — name and
        // description are always sent together by the backend's create
        // endpoint, so there's no realistic partial-field pull payload
        // to guard against here the way StaffEntity needs to.
        val merged = ExpenseCategoryEntity(
            id = id,
            name = (fields["name"] as? String) ?: "",
            description = fields["description"] as? String,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.expenseCategoryDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.expenseCategoryDao().markSynced(id, syncedAt)
}

object RecurringExpenseTemplateSyncAdapter : SyncTableAdapter {
    override val tableName = "RecurringExpenseTemplate"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.recurringExpenseTemplateDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.recurringExpenseTemplateDao().getById(id)
        val merged = RecurringExpenseTemplateEntity(
            id = id,
            expenseCategoryId = (fields["expenseCategoryId"] as? String) ?: existing?.expenseCategoryId ?: "",
            label = (fields["label"] as? String) ?: existing?.label ?: "",
            amount = (fields["amount"] as? Double) ?: existing?.amount ?: 0.0,
            frequency = (fields["frequency"] as? String)?.let {
                runCatching { RecurrenceFrequency.valueOf(it) }.getOrNull()
            } ?: existing?.frequency ?: RecurrenceFrequency.MONTHLY,
            dayOfMonth = (fields["dayOfMonth"] as? Double)?.toInt() ?: existing?.dayOfMonth ?: 1,
            isActive = (fields["isActive"] as? Boolean) ?: existing?.isActive ?: true,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.recurringExpenseTemplateDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) =
        db.recurringExpenseTemplateDao().markSynced(id, syncedAt)
}

object ExpenseSyncAdapter : SyncTableAdapter {
    override val tableName = "Expense"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.expenseDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        // ExpenseDao also has no getById — same reasoning as
        // ExpenseCategorySyncAdapter; every field is always sent
        // together by the backend's create/generate endpoints.
        val merged = ExpenseEntity(
            id = id,
            expenseCategoryId = (fields["expenseCategoryId"] as? String) ?: "",
            recurringTemplateId = fields["recurringTemplateId"] as? String,
            description = (fields["description"] as? String) ?: "",
            amount = (fields["amount"] as? Double) ?: 0.0,
            spentAt = (fields["spentAt"] as? String)?.let(IsoDates::parseToMillis) ?: updatedAtMillis,
            mode = (fields["mode"] as? String)?.let {
                runCatching { PaymentMode.valueOf(it) }.getOrNull()
            } ?: PaymentMode.OTHER,
            referenceNo = fields["referenceNo"] as? String,
            recordedById = (fields["recordedById"] as? String) ?: "",
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.expenseDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.expenseDao().markSynced(id, syncedAt)
}
