const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");
const { recalculateStudentFeeStatus } = require("../services/feeStatusService");

/**
 * POST /api/student-fees/:studentFeeId/payments
 * Records ONE payment transaction against a StudentFee. Multiple
 * payments can be recorded against the same fee over time (this is
 * what makes "partial payment" work — each call just adds another
 * row; status is recalculated from the running total every time).
 */
async function createPayment(req, res) {
  const { studentFeeId } = req.params;
  const { amount, mode, referenceNo, notes, paidAt } = req.body;

  if (!amount || Number(amount) <= 0) throw new ApiError(400, "amount must be greater than 0");
  if (!mode) throw new ApiError(400, "mode is required (CASH, UPI, CHEQUE, BANK_TRANSFER, CARD, OTHER)");

  const studentFee = await prisma.studentFee.findFirst({
    where: { id: studentFeeId, isDeleted: false },
    include: { payments: { where: { isDeleted: false } } },
  });
  if (!studentFee) throw new ApiError(404, "StudentFee not found");

  const alreadyPaid = studentFee.payments.reduce((sum, p) => sum + Number(p.amount), 0);
  const payable = Number(studentFee.amount) - Number(studentFee.discount);
  const remaining = payable - alreadyPaid;

  // Guard against overpayment — a common real-world data-entry mistake
  // (e.g. accidentally typing the full fee amount on a fee that already
  // has a partial payment). Reject rather than silently allow negative balance.
  if (Number(amount) > remaining + 0.01) {
    throw new ApiError(
      400,
      `Payment of ${amount} exceeds remaining balance of ${remaining.toFixed(2)} for this fee`
    );
  }

  const payment = await prisma.payment.create({
    data: {
      studentFeeId,
      amount,
      mode,
      referenceNo,
      notes,
      paidAt: paidAt ? new Date(paidAt) : undefined,
      recordedById: req.user.id,
    },
  });

  const updatedFee = await recalculateStudentFeeStatus(studentFeeId);

  res.status(201).json({ payment, studentFee: updatedFee });
}

/**
 * GET /api/student-fees/:studentFeeId/payments
 * The transaction history for one specific fee.
 */
async function listPaymentsForFee(req, res) {
  const payments = await prisma.payment.findMany({
    where: { studentFeeId: req.params.studentFeeId, isDeleted: false },
    orderBy: { paidAt: "desc" },
    include: { recordedBy: { select: { id: true, username: true } } },
  });
  res.json(payments);
}

/**
 * GET /api/transactions
 * The FULL transaction ledger across the whole school, with filters.
 * Query params: ?from=&to=&studentId=&mode=
 */
async function listTransactions(req, res) {
  const { from, to, studentId, mode } = req.query;

  const where = { isDeleted: false };
  if (mode) where.mode = mode;
  if (from || to) {
    where.paidAt = {};
    if (from) where.paidAt.gte = new Date(from);
    if (to) where.paidAt.lte = new Date(to);
  }
  if (studentId) {
    where.studentFee = { studentId };
  }

  const payments = await prisma.payment.findMany({
    where,
    orderBy: { paidAt: "desc" },
    include: {
      recordedBy: { select: { id: true, username: true } },
      studentFee: {
        select: {
          id: true,
          description: true,
          student: { select: { id: true, firstName: true, lastName: true, admissionNo: true } },
        },
      },
    },
  });

  const totalAmount = payments.reduce((sum, p) => sum + Number(p.amount), 0);
  res.json({ count: payments.length, totalAmount, transactions: payments });
}

/**
 * GET /api/dues
 * Every StudentFee that is UNPAID or PARTIAL — the dues report (#5).
 * Query params: ?sectionId=&classId=&overdueOnly=true
 */
async function listDues(req, res) {
  const { sectionId, classId, overdueOnly } = req.query;

  const where = {
    isDeleted: false,
    status: { in: ["UNPAID", "PARTIAL"] },
  };
  if (overdueOnly === "true") {
    where.dueDate = { lt: new Date() };
  }
  if (sectionId) {
    where.student = { sectionId };
  }
  if (classId) {
    where.student = { ...where.student, section: { classId } };
  }

  const dues = await prisma.studentFee.findMany({
    where,
    orderBy: { dueDate: "asc" },
    include: {
      student: {
        select: {
          id: true,
          firstName: true,
          lastName: true,
          admissionNo: true,
          guardianPhone: true,
          section: { include: { schoolClass: true } },
        },
      },
      payments: { where: { isDeleted: false } },
    },
  });

  const withBalance = dues.map((fee) => {
    const paid = fee.payments.reduce((sum, p) => sum + Number(p.amount), 0);
    const payable = Number(fee.amount) - Number(fee.discount);
    return {
      id: fee.id,
      description: fee.description,
      dueDate: fee.dueDate,
      status: fee.status,
      amount: fee.amount,
      paid,
      balance: Math.max(payable - paid, 0),
      student: fee.student,
    };
  });

  const totalOutstanding = withBalance.reduce((sum, f) => sum + f.balance, 0);
  res.json({ count: withBalance.length, totalOutstanding, dues: withBalance });
}

module.exports = { createPayment, listPaymentsForFee, listTransactions, listDues };
