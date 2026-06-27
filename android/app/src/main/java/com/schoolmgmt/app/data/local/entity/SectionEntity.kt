package com.schoolmgmt.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mirrors the `Section` model — one class teacher per section
 * (the simplified model, per your earlier instruction: no
 * per-subject teacher matrix).
 */
@Entity(
    tableName = "sections",
    indices = [
        Index(value = ["classId"]),
        Index(value = ["classTeacherId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = SchoolClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = TeacherEntity::class,
            parentColumns = ["id"],
            childColumns = ["classTeacherId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class SectionEntity(
    @PrimaryKey val id: String,
    val name: String,            // "A", "B"...
    val classId: String,
    val classTeacherId: String? = null,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)
