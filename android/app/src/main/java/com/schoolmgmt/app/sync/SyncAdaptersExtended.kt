package com.schoolmgmt.app.sync

import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.entity.ItemCategoryEntity
import com.schoolmgmt.app.data.local.entity.ItemVariantEntity
import com.schoolmgmt.app.data.local.entity.InventoryTransactionEntity
import com.schoolmgmt.app.data.local.entity.FeeCategoryEntity
import com.schoolmgmt.app.data.local.entity.FeeStructureEntity
import com.schoolmgmt.app.data.local.entity.ItemType
import com.schoolmgmt.app.data.local.entity.InventoryMovementType
import com.schoolmgmt.app.data.local.entity.AttendanceEntity
import com.schoolmgmt.app.data.local.entity.AttendanceStatus
import com.schoolmgmt.app.data.local.entity.FeeStatus
import com.schoolmgmt.app.data.local.entity.PaymentEntity
import com.schoolmgmt.app.data.local.entity.PaymentMode
import com.schoolmgmt.app.data.local.entity.SchoolClassEntity
import com.schoolmgmt.app.data.local.entity.SectionEntity
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import com.schoolmgmt.app.data.local.entity.StudentItemPurchaseEntity
import com.schoolmgmt.app.data.local.entity.AcademicYearEntity
import com.schoolmgmt.app.data.local.entity.TeacherAttendanceEntity
import com.schoolmgmt.app.data.local.entity.InvoiceSettingsEntity

object AcademicYearSyncAdapter : SyncTableAdapter {
    override val tableName = "AcademicYear"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.academicYearDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.academicYearDao().getById(id)
        val merged = AcademicYearEntity(
            id = id,
            label = (fields["label"] as? String) ?: existing?.label ?: "",
            startDate = (fields["startDate"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.startDate ?: updatedAtMillis,
            endDate = (fields["endDate"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.endDate ?: updatedAtMillis,
            isCurrent = (fields["isCurrent"] as? Boolean) ?: existing?.isCurrent ?: false,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.academicYearDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.academicYearDao().markSynced(id, syncedAt)
}

object SectionSyncAdapter : SyncTableAdapter {
    override val tableName = "Section"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.sectionDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.sectionDao().getById(id)
        val merged = SectionEntity(
            id = id,
            name = (fields["name"] as? String) ?: existing?.name ?: "",
            classId = (fields["classId"] as? String) ?: existing?.classId ?: "",
            classTeacherId = (fields["classTeacherId"] as? String) ?: existing?.classTeacherId,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.sectionDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.sectionDao().markSynced(id, syncedAt)
}

object SchoolClassSyncAdapter : SyncTableAdapter {
    override val tableName = "SchoolClass"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.schoolClassDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.schoolClassDao().getById(id)
        val merged = SchoolClassEntity(
            id = id,
            name = (fields["name"] as? String) ?: existing?.name ?: "",
            academicYearId = (fields["academicYearId"] as? String) ?: existing?.academicYearId ?: "",
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.schoolClassDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.schoolClassDao().markSynced(id, syncedAt)
}

object StudentFeeSyncAdapter : SyncTableAdapter {
    override val tableName = "StudentFee"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.studentFeeDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.studentFeeDao().getById(id)
        val merged = StudentFeeEntity(
            id = id,
            studentId = (fields["studentId"] as? String) ?: existing?.studentId ?: "",
            feeStructureId = (fields["feeStructureId"] as? String) ?: existing?.feeStructureId,
            purchaseId = (fields["purchaseId"] as? String) ?: existing?.purchaseId,
            description = (fields["description"] as? String) ?: existing?.description ?: "",
            // numeric values from JSON arrive as Double (Moshi's default number
            // representation), so this cast must be Double, not Int/Long.
            amount = (fields["amount"] as? Double) ?: existing?.amount ?: 0.0,
            dueDate = (fields["dueDate"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.dueDate ?: updatedAtMillis,
            discount = (fields["discount"] as? Double) ?: existing?.discount ?: 0.0,
            status = (fields["status"] as? String)?.let {
                runCatching { FeeStatus.valueOf(it) }.getOrNull()
            } ?: existing?.status ?: FeeStatus.UNPAID,
            isDefaulted = (fields["isDefaulted"] as? Boolean) ?: existing?.isDefaulted ?: false,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.studentFeeDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.studentFeeDao().markSynced(id, syncedAt)
}

object PaymentSyncAdapter : SyncTableAdapter {
    override val tableName = "Payment"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.paymentDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.paymentDao().getById(id)
        val merged = PaymentEntity(
            id = id,
            studentFeeId = (fields["studentFeeId"] as? String) ?: existing?.studentFeeId ?: "",
            amount = (fields["amount"] as? Double) ?: existing?.amount ?: 0.0,
            mode = (fields["mode"] as? String)?.let {
                runCatching { PaymentMode.valueOf(it) }.getOrNull()
            } ?: existing?.mode ?: PaymentMode.OTHER,
            referenceNo = (fields["referenceNo"] as? String) ?: existing?.referenceNo,
            paidAt = (fields["paidAt"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.paidAt ?: updatedAtMillis,
            recordedById = (fields["recordedById"] as? String) ?: existing?.recordedById ?: "",
            notes = (fields["notes"] as? String) ?: existing?.notes,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.paymentDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.paymentDao().markSynced(id, syncedAt)
}

object StudentItemPurchaseSyncAdapter : SyncTableAdapter {
    override val tableName = "StudentItemPurchase"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.studentItemPurchaseDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.studentItemPurchaseDao().getById(id)
        val merged = StudentItemPurchaseEntity(
            id = id,
            studentId = (fields["studentId"] as? String) ?: existing?.studentId ?: "",
            itemVariantId = (fields["itemVariantId"] as? String) ?: existing?.itemVariantId ?: "",
            // JSON numbers are Double by default; quantity is a whole
            // unit count, so round-trip through Int explicitly.
            quantity = (fields["quantity"] as? Double)?.toInt() ?: existing?.quantity ?: 1,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.studentItemPurchaseDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.studentItemPurchaseDao().markSynced(id, syncedAt)
}

object AttendanceSyncAdapter : SyncTableAdapter {
    override val tableName = "Attendance"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.attendanceDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.attendanceDao().getById(id)
        val merged = AttendanceEntity(
            id = id,
            studentId = (fields["studentId"] as? String) ?: existing?.studentId ?: "",
            sectionId = (fields["sectionId"] as? String) ?: existing?.sectionId ?: "",
            date = (fields["date"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.date ?: updatedAtMillis,
            status = (fields["status"] as? String)?.let {
                runCatching { AttendanceStatus.valueOf(it) }.getOrNull()
            } ?: existing?.status ?: AttendanceStatus.ABSENT,
            markedById = (fields["markedById"] as? String) ?: existing?.markedById,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.attendanceDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.attendanceDao().markSynced(id, syncedAt)
}

object TeacherAttendanceSyncAdapter : SyncTableAdapter {
    override val tableName = "TeacherAttendance"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.teacherAttendanceDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.teacherAttendanceDao().getById(id)
        val merged = TeacherAttendanceEntity(
            id = id,
            teacherId = (fields["teacherId"] as? String) ?: existing?.teacherId ?: "",
            date = (fields["date"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.date ?: updatedAtMillis,
            status = (fields["status"] as? String)?.let {
                runCatching { AttendanceStatus.valueOf(it) }.getOrNull()
            } ?: existing?.status ?: AttendanceStatus.ABSENT,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.teacherAttendanceDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.teacherAttendanceDao().markSynced(id, syncedAt)
}

object ItemCategorySyncAdapter : SyncTableAdapter {
    override val tableName = "ItemCategory"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.itemCategoryDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.itemCategoryDao().getById(id)
        val merged = ItemCategoryEntity(
            id = id,
            name = (fields["name"] as? String) ?: existing?.name ?: "",
            type = (fields["type"] as? String)?.let {
                runCatching { ItemType.valueOf(it) }.getOrNull()
            } ?: existing?.type ?: ItemType.OTHER,
            description = (fields["description"] as? String) ?: existing?.description,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.itemCategoryDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.itemCategoryDao().markSynced(id, syncedAt)
}

object ItemVariantSyncAdapter : SyncTableAdapter {
    override val tableName = "ItemVariant"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.itemVariantDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.itemVariantDao().getById(id)
        val merged = ItemVariantEntity(
            id = id,
            itemCategoryId = (fields["itemCategoryId"] as? String) ?: existing?.itemCategoryId ?: "",
            label = (fields["label"] as? String) ?: existing?.label ?: "",
            classId = (fields["classId"] as? String) ?: existing?.classId,
            size = (fields["size"] as? String) ?: existing?.size,
            price = (fields["price"] as? Double) ?: existing?.price ?: 0.0,
            costPrice = (fields["costPrice"] as? Double) ?: existing?.costPrice,
            stockQuantity = (fields["stockQuantity"] as? Double)?.toInt() ?: existing?.stockQuantity ?: 0,
            isActive = (fields["isActive"] as? Boolean) ?: existing?.isActive ?: true,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.itemVariantDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.itemVariantDao().markSynced(id, syncedAt)
}

object InventoryTransactionSyncAdapter : SyncTableAdapter {
    override val tableName = "InventoryTransaction"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.inventoryTransactionDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.inventoryTransactionDao().getById(id)
        val merged = InventoryTransactionEntity(
            id = id,
            itemVariantId = (fields["itemVariantId"] as? String) ?: existing?.itemVariantId ?: "",
            type = (fields["type"] as? String)?.let {
                runCatching { InventoryMovementType.valueOf(it) }.getOrNull()
            } ?: existing?.type ?: InventoryMovementType.IN,
            quantity = (fields["quantity"] as? Double)?.toInt() ?: existing?.quantity ?: 0,
            note = (fields["note"] as? String) ?: existing?.note,
            recordedById = (fields["recordedById"] as? String) ?: existing?.recordedById,
            purchaseId = (fields["purchaseId"] as? String) ?: existing?.purchaseId,
            createdAt = (fields["createdAt"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.createdAt ?: updatedAtMillis,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.inventoryTransactionDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.inventoryTransactionDao().markSynced(id, syncedAt)
}

object FeeCategorySyncAdapter : SyncTableAdapter {
    override val tableName = "FeeCategory"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.feeCategoryDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.feeCategoryDao().getById(id)
        val merged = FeeCategoryEntity(
            id = id,
            name = (fields["name"] as? String) ?: existing?.name ?: "",
            description = (fields["description"] as? String) ?: existing?.description,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.feeCategoryDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.feeCategoryDao().markSynced(id, syncedAt)
}

object FeeStructureSyncAdapter : SyncTableAdapter {
    override val tableName = "FeeStructure"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.feeStructureDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.feeStructureDao().getById(id)
        val merged = FeeStructureEntity(
            id = id,
            feeCategoryId = (fields["feeCategoryId"] as? String) ?: existing?.feeCategoryId ?: "",
            classId = (fields["classId"] as? String) ?: existing?.classId ?: "",
            academicYearId = (fields["academicYearId"] as? String) ?: existing?.academicYearId ?: "",
            amount = (fields["amount"] as? Double) ?: existing?.amount ?: 0.0,
            dueDate = (fields["dueDate"] as? String)?.let(IsoDates::parseToMillis) ?: existing?.dueDate ?: updatedAtMillis,
            description = (fields["description"] as? String) ?: existing?.description,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.feeStructureDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.feeStructureDao().markSynced(id, syncedAt)
}

object InvoiceSettingsSyncAdapter : SyncTableAdapter {
    override val tableName = "InvoiceSettings"

    override suspend fun getUnsynced(db: AppDatabase): List<Pair<String, Map<String, Any?>>> =
        db.invoiceSettingsDao().getUnsyncedChanges().map { it.id to it.toWireMap() }

    override suspend fun applyFromServer(db: AppDatabase, id: String, fields: Map<String, Any?>, updatedAtMillis: Long, isDeleted: Boolean) {
        val existing = db.invoiceSettingsDao().getSettings()
        val merged = InvoiceSettingsEntity(
            id = id,
            schoolName = (fields["schoolName"] as? String) ?: existing?.schoolName ?: "ABC Public School",
            address = (fields["address"] as? String) ?: existing?.address,
            phone = (fields["phone"] as? String) ?: existing?.phone,
            email = (fields["email"] as? String) ?: existing?.email,
            footerNote = (fields["footerNote"] as? String) ?: existing?.footerNote,
            thermalWidth = (fields["thermalWidth"] as? Number)?.toInt() ?: existing?.thermalWidth ?: 576,
            marginSize = (fields["marginSize"] as? Number)?.toInt() ?: existing?.marginSize ?: 20,
            headerFontSize = (fields["headerFontSize"] as? Number)?.toInt() ?: existing?.headerFontSize ?: 28,
            bodyFontSize = (fields["bodyFontSize"] as? Number)?.toInt() ?: existing?.bodyFontSize ?: 14,
            updatedAt = updatedAtMillis,
            isDeleted = isDeleted,
            syncedAt = updatedAtMillis,
        )
        db.invoiceSettingsDao().upsert(merged)
    }

    override suspend fun markSynced(db: AppDatabase, id: String, syncedAt: Long) = db.invoiceSettingsDao().markSynced(id, syncedAt)
}
