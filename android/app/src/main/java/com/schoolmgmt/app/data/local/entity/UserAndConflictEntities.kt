package com.schoolmgmt.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole { ADMIN, ACCOUNTANT, TEACHER }

/**
 * Mirrors `User`. Note: passwordHash is intentionally NOT stored here —
 * the device only ever holds a JWT (in DataStore, see AuthPreferences)
 * plus this cached profile info for display/permission checks. The
 * actual password never touches local storage after login.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val role: UserRole,
    val teacherId: String? = null,
    val isActive: Boolean = true,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

enum class SyncConflictStatus { PENDING, RESOLVED_SERVER, RESOLVED_CLIENT, RESOLVED_MANUAL }

/**
 * Mirrors `SyncConflict`. This is populated FROM the server during a
 * pull (the server is the source of truth for conflict review — see
 * GET /api/sync/conflicts on the backend) so an admin can review
 * pending conflicts right from the device instead of needing a
 * separate admin web panel.
 *
 * clientData/serverData are stored as raw JSON strings (Room has no
 * native JSON column type) — parse with Moshi when displaying.
 */
@Entity(tableName = "sync_conflicts")
data class SyncConflictEntity(
    @PrimaryKey val id: String,
    val tableName: String,
    val recordId: String,
    val clientDataJson: String,
    val serverDataJson: String,
    val clientUserId: String? = null,
    val status: SyncConflictStatus = SyncConflictStatus.PENDING,
    val resolvedById: String? = null,
    val resolvedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
