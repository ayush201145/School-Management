package com.schoolmgmt.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Why a student became inactive. Distinct from a generic soft-delete
 * (isDeleted, which means "this record was a data-entry mistake and
 * shouldn't exist") — a withdrawal is a real, legitimate life event for
 * a student who genuinely studied here, and the record (and all their
 * fee/payment history) must be kept permanently for school records.
 */
enum class WithdrawalReason { TRANSFERRED, GRADUATED, EXPELLED, OTHER }

/**
 * Mirrors the `Student` model in prisma/schema.prisma.
 *
 * IDs are client-generated UUID strings (java.util.UUID.randomUUID().toString())
 * — NOT auto-increment — so a student created offline already has a
 * permanent, globally-unique ID before it ever reaches the server.
 *
 * updatedAt / isDeleted / syncedAt are the three fields every syncable
 * entity carries (see SyncableEntity below for the shared contract).
 */
@Entity(
    tableName = "students",
    indices = [
        Index(value = ["sectionId"]),
        Index(value = ["admissionNo"], unique = true),
        Index(value = ["guardianPhone"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.NO_ACTION, // soft-delete only — see schema notes
        )
    ]
)
data class StudentEntity(
    @PrimaryKey val id: String,
    val admissionNo: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: Long? = null,    // epoch millis, null if unknown
    val gender: String? = null,
    val guardianName: String? = null,
    val guardianPhone: String? = null,
    val guardianEmail: String? = null,
    val address: String? = null,
    val fatherPhone: String? = null,
    val motherPhone: String? = null,
    val whatsappPhone: String? = null,
    val tuitionFee: Double? = null,
    val sectionId: String,
    val admissionDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,

    // --- withdrawal ("quit school") tracking — separate from isDeleted ---
    val withdrawalReason: WithdrawalReason? = null, // null while still enrolled
    val withdrawnDate: Long? = null,
    val withdrawalNotes: String? = null,

    // --- sync bookkeeping (present on every syncable entity) ---
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null, // null = never synced
)
