package com.schoolmgmt.app.data.local

import androidx.room.TypeConverter
import com.schoolmgmt.app.data.local.entity.AttendanceStatus
import com.schoolmgmt.app.data.local.entity.FeeStatus
import com.schoolmgmt.app.data.local.entity.InventoryMovementType
import com.schoolmgmt.app.data.local.entity.ItemType
import com.schoolmgmt.app.data.local.entity.PaymentMode
import com.schoolmgmt.app.data.local.entity.RecurrenceFrequency
import com.schoolmgmt.app.data.local.entity.StaffType
import com.schoolmgmt.app.data.local.entity.SyncConflictStatus
import com.schoolmgmt.app.data.local.entity.UserRole
import com.schoolmgmt.app.data.local.entity.WithdrawalReason

/**
 * Room cannot store Kotlin enums natively — each one needs an explicit
 * to/from converter. We store enums as their String name() rather than
 * an ordinal Int, so the raw SQLite data stays human-readable if you
 * ever inspect the .db file directly, and so reordering an enum's
 * declared values later can never silently corrupt stored data the
 * way ordinal-based storage would.
 */
class Converters {

    @TypeConverter
    fun feeStatusToString(value: FeeStatus): String = value.name
    @TypeConverter
    fun stringToFeeStatus(value: String): FeeStatus = FeeStatus.valueOf(value)

    @TypeConverter
    fun paymentModeToString(value: PaymentMode): String = value.name
    @TypeConverter
    fun stringToPaymentMode(value: String): PaymentMode = PaymentMode.valueOf(value)

    @TypeConverter
    fun itemTypeToString(value: ItemType): String = value.name
    @TypeConverter
    fun stringToItemType(value: String): ItemType = ItemType.valueOf(value)

    @TypeConverter
    fun inventoryMovementTypeToString(value: InventoryMovementType): String = value.name
    @TypeConverter
    fun stringToInventoryMovementType(value: String): InventoryMovementType =
        InventoryMovementType.valueOf(value)

    @TypeConverter
    fun attendanceStatusToString(value: AttendanceStatus): String = value.name
    @TypeConverter
    fun stringToAttendanceStatus(value: String): AttendanceStatus = AttendanceStatus.valueOf(value)

    @TypeConverter
    fun userRoleToString(value: UserRole): String = value.name
    @TypeConverter
    fun stringToUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun syncConflictStatusToString(value: SyncConflictStatus): String = value.name
    @TypeConverter
    fun stringToSyncConflictStatus(value: String): SyncConflictStatus = SyncConflictStatus.valueOf(value)

    // Nullable — withdrawalReason is null for any student still enrolled.
    @TypeConverter
    fun withdrawalReasonToString(value: WithdrawalReason?): String? = value?.name
    @TypeConverter
    fun stringToWithdrawalReason(value: String?): WithdrawalReason? = value?.let { WithdrawalReason.valueOf(it) }

    @TypeConverter
    fun staffTypeToString(value: StaffType): String = value.name
    @TypeConverter
    fun stringToStaffType(value: String): StaffType = StaffType.valueOf(value)

    @TypeConverter
    fun recurrenceFrequencyToString(value: RecurrenceFrequency): String = value.name
    @TypeConverter
    fun stringToRecurrenceFrequency(value: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(value)
}
