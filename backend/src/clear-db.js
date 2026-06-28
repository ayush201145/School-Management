const path = require("path");
require("dotenv").config({ path: path.join(__dirname, "../.env") });

const { PrismaClient } = require("@prisma/client");
const prisma = new PrismaClient();

async function main() {
  console.log("Clearing all data from database...");

  // Delete records in dependency order (children first)
  const deleteOperations = [
    prisma.payment.deleteMany(),
    prisma.studentFee.deleteMany(),
    prisma.studentItemPurchase.deleteMany(),
    prisma.inventoryTransaction.deleteMany(),
    prisma.itemVariant.deleteMany(),
    prisma.itemCategory.deleteMany(),
    prisma.attendance.deleteMany(),
    prisma.teacherAttendance.deleteMany(),
    prisma.salaryPayment.deleteMany(),
    prisma.staff.deleteMany(),
    prisma.teacher.deleteMany(),
    prisma.student.deleteMany(),
    prisma.section.deleteMany(),
    prisma.feeStructure.deleteMany(),
    prisma.feeCategory.deleteMany(),
    prisma.schoolClass.deleteMany(),
    prisma.academicYear.deleteMany(),
    prisma.expense.deleteMany(),
    prisma.recurringExpenseTemplate.deleteMany(),
    prisma.expenseCategory.deleteMany(),
  ];

  try {
    for (const op of deleteOperations) {
      await op;
    }
    console.log("Database successfully cleared.");
  } catch (err) {
    console.error("Error during database clear:", err);
    process.exit(1);
  }
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
