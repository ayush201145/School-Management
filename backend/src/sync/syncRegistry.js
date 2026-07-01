/**
 * Registry of tables that participate in offline sync.
 *
 * To make a new model syncable: add an entry here with its Prisma
 * client accessor name and the fields a client is allowed to write.
 * Everything else (push/pull/conflict logic) is generic and driven
 * entirely by this registry — no per-table sync code needed.
 */
const SYNC_TABLES = {
  AcademicYear: {
    prismaModel: "academicYear",
    writableFields: ["label", "startDate", "endDate", "isCurrent"],
  },
  FeeCategory: {
    prismaModel: "feeCategory",
    writableFields: ["name", "description"],
  },
  FeeStructure: {
    prismaModel: "feeStructure",
    writableFields: ["feeCategoryId", "classId", "academicYearId", "amount", "dueDate", "description"],
  },
  Student: {
    prismaModel: "student",
    writableFields: [
      "admissionNo", "firstName", "lastName", "dateOfBirth", "gender",
      "guardianName", "guardianPhone", "guardianEmail", "address",
      "fatherPhone", "motherPhone", "whatsappPhone", "tuitionFee",
      "sectionId", "admissionDate", "isActive",
      "withdrawalReason", "withdrawnDate", "withdrawalNotes",
    ],
  },
  Teacher: {
    prismaModel: "teacher",
    writableFields: [
      "employeeNo", "firstName", "lastName", "phone", "email",
      "address", "qualification", "joiningDate", "isActive",
    ],
  },
  Section: {
    prismaModel: "section",
    writableFields: ["name", "classId", "classTeacherId"],
  },
  SchoolClass: {
    prismaModel: "schoolClass",
    writableFields: ["name", "academicYearId"],
  },
  StudentFee: {
    prismaModel: "studentFee",
    writableFields: [
      "studentId", "feeStructureId", "purchaseId", "description",
      "amount", "dueDate", "discount", "status",
    ],
  },
  Payment: {
    prismaModel: "payment",
    writableFields: [
      "studentFeeId", "amount", "mode", "referenceNo", "paidAt",
      "recordedById", "notes",
    ],
  },
  StudentItemPurchase: {
    prismaModel: "studentItemPurchase",
    writableFields: ["studentId", "itemVariantId", "quantity"],
  },
  ItemCategory: {
    prismaModel: "itemCategory",
    writableFields: ["name", "type", "description"],
  },
  ItemVariant: {
    prismaModel: "itemVariant",
    writableFields: ["itemCategoryId", "label", "classId", "size", "price", "stockQuantity", "isActive", "costPrice"],
  },
  InventoryTransaction: {
    prismaModel: "inventoryTransaction",
    writableFields: ["itemVariantId", "type", "quantity", "note", "recordedById", "purchaseId"],
  },
  Attendance: {
    prismaModel: "attendance",
    writableFields: ["studentId", "sectionId", "date", "status", "markedById"],
  },
  TeacherAttendance: {
    prismaModel: "teacherAttendance",
    writableFields: ["teacherId", "date", "status"],
  },
  Staff: {
    prismaModel: "staff",
    writableFields: ["name", "type", "teacherId", "phone", "monthlySalary", "joiningDate", "isActive"],
  },
  SalaryPayment: {
    prismaModel: "salaryPayment",
    writableFields: [
      "staffId", "amount", "forMonth", "forYear", "paidAt", "mode",
      "referenceNo", "notes", "recordedById",
    ],
  },
  ExpenseCategory: {
    prismaModel: "expenseCategory",
    writableFields: ["name", "description"],
  },
  RecurringExpenseTemplate: {
    prismaModel: "recurringExpenseTemplate",
    writableFields: ["expenseCategoryId", "label", "amount", "frequency", "dayOfMonth", "isActive"],
  },
  Expense: {
    prismaModel: "expense",
    writableFields: [
      "expenseCategoryId", "recurringTemplateId", "description", "amount",
      "spentAt", "mode", "referenceNo", "recordedById",
    ],
  },
  InvoiceSettings: {
    prismaModel: "invoiceSettings",
    writableFields: [
      "schoolName", "address", "phone", "email", "footerNote",
      "thermalWidth", "marginSize", "headerFontSize", "bodyFontSize"
    ],
  },
};

function isSyncable(tableName) {
  return Object.prototype.hasOwnProperty.call(SYNC_TABLES, tableName);
}

function getTableConfig(tableName) {
  return SYNC_TABLES[tableName];
}

module.exports = { SYNC_TABLES, isSyncable, getTableConfig };
