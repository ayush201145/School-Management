package com.schoolmgmt.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Mirrors the `Teacher` model in prisma/schema.prisma. */
@Entity(
    tableName = "teachers",
    indices = [Index(value = ["employeeNo"], unique = true)]
)
data class TeacherEntity(
    @PrimaryKey val id: String,
    val employeeNo: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val qualification: String? = null,
    val joiningDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,

    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "isDeleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "syncedAt") val syncedAt: Long? = null,
)
