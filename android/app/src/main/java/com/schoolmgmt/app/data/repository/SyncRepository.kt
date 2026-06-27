package com.schoolmgmt.app.data.repository

import com.schoolmgmt.app.data.local.AppDatabase
import com.schoolmgmt.app.data.local.AuthPreferences
import com.schoolmgmt.app.data.remote.SyncApi
import com.schoolmgmt.app.data.remote.dto.SyncChangeDto
import com.schoolmgmt.app.data.remote.dto.SyncPushRequest
import com.schoolmgmt.app.sync.IsoDates
import com.schoolmgmt.app.sync.SYNC_REGISTRY
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of one full sync cycle — surfaced to the UI (e.g. a small "synced 12 changes" toast or sync-status screen). */
data class SyncResult(
    val pushedCount: Int,
    val pushConflictCount: Int,
    val pulledCount: Int,
    val errors: List<String>,
)

/**
 * Drives one full sync cycle: PUSH everything locally unsynced, THEN
 * PULL everything changed on the server since the last successful
 * pull. Push-before-pull matters: if we pulled first, a local edit
 * still queued for push could get silently overwritten by a pull
 * response for the same row a moment later.
 *
 * This class is intentionally registry-driven (SYNC_REGISTRY) rather
 * than having one hand-written push/pull block per entity — adding a
 * new syncable table means adding one adapter to the registry, not
 * touching this orchestration logic at all.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val syncApi: SyncApi,
    private val db: AppDatabase,
    private val authPreferences: AuthPreferences,
) {
    /**
     * Runs one full push-then-pull cycle. Designed to be called from
     * SyncWorker (WorkManager) but is plain suspend so it's just as
     * easy to trigger manually (e.g. a "sync now" button) or in tests.
     */
    suspend fun syncNow(): SyncResult {
        val pushResult = push()
        val pulledCount = pull()
        return SyncResult(
            pushedCount = pushResult.first,
            pushConflictCount = pushResult.second,
            pulledCount = pulledCount,
            errors = pushResult.third,
        )
    }

    /**
     * PUSH: collects every unsynced row across every registered table,
     * sends them in one batch (chunked to the backend's 500-per-batch
     * limit — see paymentRoutes push validation), and marks each
     * successfully-applied row as synced locally. Conflicted rows are
     * intentionally left UNSYNCED (syncedAt stays null/stale) so they
     * get retried on the next push after an admin resolves the
     * conflict server-side — we never silently drop a local change.
     */
    private suspend fun push(): Triple<Int, Int, List<String>> {
        val allChanges = mutableListOf<Pair<String, SyncChangeDto>>() // table-qualified-id -> change

        for (adapter in SYNC_REGISTRY) {
            val unsynced = adapter.getUnsynced(db)
            for ((id, wireMap) in unsynced) {
                // updatedAt isn't part of the entity's own wire map (it's
                // metadata about the row, not a field of it), so each
                // adapter's wire map plus this clientUpdatedAt together
                // form the full SyncChangeDto the backend expects.
                val updatedAtMillis = getEntityUpdatedAt(adapter.tableName, id) ?: continue
                val change = SyncChangeDto(
                    table = adapter.tableName,
                    id = id,
                    op = "upsert", // soft-deletes flow through as an upsert with isDeleted=true field, not a separate delete op
                    data = wireMap,
                    clientUpdatedAt = IsoDates.toIsoString(updatedAtMillis),
                )
                allChanges += "${adapter.tableName}:$id" to change
            }
        }

        if (allChanges.isEmpty()) return Triple(0, 0, emptyList())

        var totalApplied = 0
        var totalConflicts = 0
        val errors = mutableListOf<String>()

        // Chunk to respect the backend's max-500-changes-per-push limit.
        allChanges.chunked(500).forEach { chunk ->
            try {
                val response = syncApi.push(SyncPushRequest(changes = chunk.map { it.second }))
                totalApplied += response.appliedCount
                totalConflicts += response.conflictCount
                response.results.forEach { result ->
                    when (result.status) {
                        "applied" -> markRowSynced(result.table, result.id)
                        "conflict" -> { /* leave unsynced intentionally — see doc comment above */ }
                        "error", "rejected" -> errors += "${result.table}/${result.id}: ${result.reason ?: "unknown error"}"
                    }
                }
            } catch (e: Exception) {
                errors += "Push batch failed: ${e.message ?: "network error"}"
            }
        }

        return Triple(totalApplied, totalConflicts, errors)
    }

    /**
     * PULL: fetches everything changed since the last successful pull
     * (using the SERVER's clock, never the device's — see
     * AuthPreferences.getLastSyncServerTime doc comment) and applies
     * it to local tables via each adapter's applyFromServer.
     */
    private suspend fun pull(): Int {
        val since = authPreferences.getLastSyncServerTime()
        val response = syncApi.pull(since)

        var pulledCount = 0
        for (adapter in SYNC_REGISTRY) {
            val rows = response.tables[adapter.tableName] ?: continue
            for (row in rows) {
                val id = row["id"] as? String ?: continue
                val isDeleted = (row["isDeleted"] as? Boolean) ?: false
                val updatedAtIso = row["updatedAt"] as? String
                val updatedAtMillis = updatedAtIso?.let(IsoDates::parseToMillis) ?: System.currentTimeMillis()
                adapter.applyFromServer(db, id, row, updatedAtMillis, isDeleted)
                pulledCount++
            }
        }

        // Persist the new cursor ONLY after every table's rows were
        // applied successfully — if pull() throws partway through, the
        // cursor stays at the old value and the next sync re-fetches
        // from the same point, rather than silently skipping rows that
        // failed to apply.
        authPreferences.setLastSyncServerTime(response.serverTime)
        return pulledCount
    }

    /** Looks up a single row's local updatedAt — used to build the clientUpdatedAt sent with each push. */
    private suspend fun getEntityUpdatedAt(tableName: String, id: String): Long? = when (tableName) {
        "AcademicYear" -> db.academicYearDao().getById(id)?.updatedAt
        "Student" -> db.studentDao().getById(id)?.updatedAt
        "Teacher" -> db.teacherDao().getById(id)?.updatedAt
        "Section" -> db.sectionDao().getById(id)?.updatedAt
        "SchoolClass" -> db.schoolClassDao().getById(id)?.updatedAt
        "StudentFee" -> db.studentFeeDao().getById(id)?.updatedAt
        "Payment" -> db.paymentDao().getById(id)?.updatedAt
        "StudentItemPurchase" -> db.studentItemPurchaseDao().getById(id)?.updatedAt
        "ItemCategory" -> db.itemCategoryDao().getById(id)?.updatedAt
        "ItemVariant" -> db.itemVariantDao().getById(id)?.updatedAt
        "InventoryTransaction" -> db.inventoryTransactionDao().getById(id)?.updatedAt
        "Attendance" -> db.attendanceDao().getById(id)?.updatedAt
        "TeacherAttendance" -> db.teacherAttendanceDao().getById(id)?.updatedAt
        "Staff" -> db.staffDao().getById(id)?.updatedAt
        "SalaryPayment" -> db.salaryPaymentDao().getById(id)?.updatedAt
        "RecurringExpenseTemplate" -> db.recurringExpenseTemplateDao().getById(id)?.updatedAt
        else -> null
    } ?: System.currentTimeMillis()

    private suspend fun markRowSynced(tableName: String, id: String) {
        val syncedAt = System.currentTimeMillis()
        SYNC_REGISTRY.find { it.tableName == tableName }?.markSynced(db, id, syncedAt)
    }
}
