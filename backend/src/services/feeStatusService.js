const prisma = require("../prismaClient");

/**
 * Recalculates and persists a StudentFee's status based on the sum
 * of its non-deleted payments vs (amount - discount).
 *
 * This is called after every payment create/update/delete so status
 * is always a correct reflection of reality, not something that can
 * drift out of sync with the underlying payments.
 *
 * Returns the updated StudentFee.
 */
async function recalculateStudentFeeStatus(studentFeeId, tx = prisma) {
  const studentFee = await tx.studentFee.findUnique({
    where: { id: studentFeeId },
    include: { payments: { where: { isDeleted: false } } },
  });
  if (!studentFee) return null;

  const totalPaid = studentFee.payments.reduce((sum, p) => sum + Number(p.amount), 0);
  const payable = Number(studentFee.amount) - Number(studentFee.discount);

  let status;
  if (totalPaid <= 0) {
    status = "UNPAID";
  } else if (totalPaid >= payable) {
    status = "PAID";
  } else {
    status = "PARTIAL";
  }

  if (status !== studentFee.status) {
    await tx.studentFee.update({ where: { id: studentFeeId }, data: { status } });
  }

  return { ...studentFee, status, totalPaid, payable, balance: Math.max(payable - totalPaid, 0) };
}

module.exports = { recalculateStudentFeeStatus };
