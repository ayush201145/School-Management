package com.schoolmgmt.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.schoolmgmt.app.data.local.dao.AcademicYearDao
import com.schoolmgmt.app.data.local.dao.AttendanceDao
import com.schoolmgmt.app.data.local.dao.ExpenseCategoryDao
import com.schoolmgmt.app.data.local.dao.ExpenseDao
import com.schoolmgmt.app.data.local.dao.FeeCategoryDao
import com.schoolmgmt.app.data.local.dao.FeeStructureDao
import com.schoolmgmt.app.data.local.dao.InventoryTransactionDao
import com.schoolmgmt.app.data.local.dao.ItemCategoryDao
import com.schoolmgmt.app.data.local.dao.ItemVariantDao
import com.schoolmgmt.app.data.local.dao.PaymentDao
import com.schoolmgmt.app.data.local.dao.RecurringExpenseTemplateDao
import com.schoolmgmt.app.data.local.dao.SalaryPaymentDao
import com.schoolmgmt.app.data.local.dao.SchoolClassDao
import com.schoolmgmt.app.data.local.dao.SectionDao
import com.schoolmgmt.app.data.local.dao.StaffDao
import com.schoolmgmt.app.data.local.dao.StudentDao
import com.schoolmgmt.app.data.local.dao.StudentFeeDao
import com.schoolmgmt.app.data.local.dao.StudentItemPurchaseDao
import com.schoolmgmt.app.data.local.dao.SyncConflictDao
import com.schoolmgmt.app.data.local.dao.TeacherAttendanceDao
import com.schoolmgmt.app.data.local.dao.TeacherDao
import com.schoolmgmt.app.data.local.dao.UserDao
import com.schoolmgmt.app.data.local.entity.AcademicYearEntity
import com.schoolmgmt.app.data.local.entity.AttendanceEntity
import com.schoolmgmt.app.data.local.entity.ExpenseCategoryEntity
import com.schoolmgmt.app.data.local.entity.ExpenseEntity
import com.schoolmgmt.app.data.local.entity.FeeCategoryEntity
import com.schoolmgmt.app.data.local.entity.FeeStructureEntity
import com.schoolmgmt.app.data.local.entity.InventoryTransactionEntity
import com.schoolmgmt.app.data.local.entity.ItemCategoryEntity
import com.schoolmgmt.app.data.local.entity.ItemVariantEntity
import com.schoolmgmt.app.data.local.entity.PaymentEntity
import com.schoolmgmt.app.data.local.entity.RecurringExpenseTemplateEntity
import com.schoolmgmt.app.data.local.entity.SalaryPaymentEntity
import com.schoolmgmt.app.data.local.entity.SchoolClassEntity
import com.schoolmgmt.app.data.local.entity.SectionEntity
import com.schoolmgmt.app.data.local.entity.StaffEntity
import com.schoolmgmt.app.data.local.entity.StudentEntity
import com.schoolmgmt.app.data.local.entity.StudentFeeEntity
import com.schoolmgmt.app.data.local.entity.StudentItemPurchaseEntity
import com.schoolmgmt.app.data.local.entity.SyncConflictEntity
import com.schoolmgmt.app.data.local.entity.TeacherAttendanceEntity
import com.schoolmgmt.app.data.local.entity.TeacherEntity
import com.schoolmgmt.app.data.local.entity.UserEntity
import com.schoolmgmt.app.data.local.dao.InvoiceSettingsDao
import com.schoolmgmt.app.data.local.entity.InvoiceSettingsEntity

/**
 * The single Room database for the app — mirrors prisma/schema.prisma
 * one-to-one. version = 1 for the initial release; every future schema
 * change MUST bump this and ship a Migration (see MIGRATION_1_2 etc.
 * below once they exist) rather than relying on destructive fallback
 * in production, or every user's local data gets wiped on update.
 */
@Database(
    entities = [
        StudentEntity::class,
        TeacherEntity::class,
        AcademicYearEntity::class,
        SchoolClassEntity::class,
        SectionEntity::class,
        FeeCategoryEntity::class,
        FeeStructureEntity::class,
        StudentFeeEntity::class,
        PaymentEntity::class,
        ItemCategoryEntity::class,
        ItemVariantEntity::class,
        StudentItemPurchaseEntity::class,
        InventoryTransactionEntity::class,
        AttendanceEntity::class,
        TeacherAttendanceEntity::class,
        UserEntity::class,
        SyncConflictEntity::class,
        StaffEntity::class,
        SalaryPaymentEntity::class,
        ExpenseCategoryEntity::class,
        RecurringExpenseTemplateEntity::class,
        ExpenseEntity::class,
        InvoiceSettingsEntity::class,
    ],
    version = 5,
    exportSchema = true, // schemas/ dir gets checked into version control — needed for migration testing
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun teacherDao(): TeacherDao
    abstract fun academicYearDao(): AcademicYearDao
    abstract fun schoolClassDao(): SchoolClassDao
    abstract fun sectionDao(): SectionDao
    abstract fun feeCategoryDao(): FeeCategoryDao
    abstract fun feeStructureDao(): FeeStructureDao
    abstract fun studentFeeDao(): StudentFeeDao
    abstract fun paymentDao(): PaymentDao
    abstract fun itemCategoryDao(): ItemCategoryDao
    abstract fun itemVariantDao(): ItemVariantDao
    abstract fun studentItemPurchaseDao(): StudentItemPurchaseDao
    abstract fun inventoryTransactionDao(): InventoryTransactionDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun teacherAttendanceDao(): TeacherAttendanceDao
    abstract fun userDao(): UserDao
    abstract fun syncConflictDao(): SyncConflictDao
    abstract fun invoiceSettingsDao(): InvoiceSettingsDao
    abstract fun staffDao(): StaffDao
    abstract fun salaryPaymentDao(): SalaryPaymentDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun recurringExpenseTemplateDao(): RecurringExpenseTemplateDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        const val DATABASE_NAME = "school_management.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * version 1 -> 2: adds Staff, SalaryPayment, ExpenseCategory,
         * RecurringExpenseTemplate, Expense. Every column here must
         * match its corresponding @Entity field exactly (name, type,
         * nullability) — Room validates the actual schema against what
         * the entities declare at runtime and crashes with a clear
         * mismatch error if they diverge, so any future edit to one of
         * these entities needs a matching edit here too.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `staff` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `teacherId` TEXT,
                        `phone` TEXT,
                        `monthlySalary` REAL,
                        `joiningDate` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `syncedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_staff_teacherId` ON `staff` (`teacherId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `salary_payments` (
                        `id` TEXT NOT NULL,
                        `staffId` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `forMonth` INTEGER NOT NULL,
                        `forYear` INTEGER NOT NULL,
                        `paidAt` INTEGER NOT NULL,
                        `mode` TEXT NOT NULL,
                        `referenceNo` TEXT,
                        `notes` TEXT,
                        `recordedById` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `syncedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_salary_payments_staffId_forYear_forMonth` " +
                        "ON `salary_payments` (`staffId`, `forYear`, `forMonth`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expense_categories` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `syncedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expense_categories_name` ON `expense_categories` (`name`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_expense_templates` (
                        `id` TEXT NOT NULL,
                        `expenseCategoryId` TEXT NOT NULL,
                        `label` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `dayOfMonth` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `syncedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_expense_templates_expenseCategoryId` " +
                        "ON `recurring_expense_templates` (`expenseCategoryId`)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `expenses` (
                        `id` TEXT NOT NULL,
                        `expenseCategoryId` TEXT NOT NULL,
                        `recurringTemplateId` TEXT,
                        `description` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `spentAt` INTEGER NOT NULL,
                        `mode` TEXT NOT NULL,
                        `referenceNo` TEXT,
                        `recordedById` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `syncedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_expenseCategoryId` ON `expenses` (`expenseCategoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_spentAt` ON `expenses` (`spentAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_recurringTemplateId` ON `expenses` (`recurringTemplateId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `students` ADD COLUMN `fatherPhone` TEXT")
                db.execSQL("ALTER TABLE `students` ADD COLUMN `motherPhone` TEXT")
                db.execSQL("ALTER TABLE `students` ADD COLUMN `whatsappPhone` TEXT")
                db.execSQL("ALTER TABLE `students` ADD COLUMN `tuitionFee` REAL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `item_variants` ADD COLUMN `costPrice` REAL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `invoice_settings` (
                        `id` TEXT NOT NULL,
                        `schoolName` TEXT NOT NULL,
                        `address` TEXT,
                        `phone` TEXT,
                        `email` TEXT,
                        `footerNote` TEXT,
                        `thermalWidth` INTEGER NOT NULL DEFAULT 576,
                        `marginSize` INTEGER NOT NULL DEFAULT 20,
                        `headerFontSize` INTEGER NOT NULL DEFAULT 28,
                        `bodyFontSize` INTEGER NOT NULL DEFAULT 14,
                        `updatedAt` INTEGER NOT NULL,
                        `isDeleted` INTEGER NOT NULL DEFAULT 0,
                        `syncedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """
                )
            }
        }
    }
}

/*
 * NOTE on cross-DAO transactions: Room's documented way to wrap calls
 * to MULTIPLE DAOs in one atomic transaction is NOT a @Transaction
 * method declared on AppDatabase itself — it's the suspend extension
 * function androidx.room.withTransaction(db) { ... }, called from the
 * repository layer where multiple DAOs are already injected. e.g.:
 *
 *   suspend fun recordPayment(...) = db.withTransaction {
 *       paymentDao.upsert(payment)
 *       studentFeeDao.update(updatedFee)
 *   }
 *
 * See FeeRepository.kt and InventoryRepository.kt for the actual usage.
 */
