package com.schoolmgmt.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceStatus { PRESENT, ABSENT, LATE, LEAVE }

/** Mirrors `Attendance` — one student's attendance on one date. */
@Entity(
    tableName = "attendance",
    indices = [
        Index(value = ["studentId", "date"], unique = true),
        Index(value = ["sectionId", "date"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class AttendanceEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val sectionId: String,
    val date: Long,                  // normalized to midnight UTC for that calendar day
    val status: AttendanceStatus,
    val markedById: String? = null,  // teacher User id who marked it

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)

/** Mirrors `TeacherAttendance`. */
@Entity(
    tableName = "teacher_attendance",
    indices = [Index(value = ["teacherId", "date"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = TeacherEntity::class,
            parentColumns = ["id"],
            childColumns = ["teacherId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ]
)
data class TeacherAttendanceEntity(
    @PrimaryKey val id: String,
    val teacherId: String,
    val date: Long,
    val status: AttendanceStatus,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)
