package com.schoolmgmt.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mirrors the `AcademicYear` model. */
@Entity(
    tableName = "academic_years",
    indices = [Index(value = ["label"], unique = true)]
)
data class AcademicYearEntity(
    @PrimaryKey val id: String,
    val label: String,           // "2026-27"
    val startDate: Long,
    val endDate: Long,
    val isCurrent: Boolean = false,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

/** Mirrors the `SchoolClass` model — Play, Nursery, LKG, UKG, Class 1-5, etc. */
@Entity(
    tableName = "school_classes",
    indices = [Index(value = ["academicYearId"])],
    foreignKeys = [
        ForeignKey(
            entity = AcademicYearEntity::class,
            parentColumns = ["id"],
            childColumns = ["academicYearId"],
            onDelete = ForeignKey.NO_ACTION,
        )
    ]
)
data class SchoolClassEntity(
    @PrimaryKey val id: String,
    val name: String,            // "Play", "Nursery", "LKG", "UKG", "Class 1"...
    val academicYearId: String,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)
