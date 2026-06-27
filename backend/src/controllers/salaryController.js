const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");

/**
 * POST /api/staff/:staffId/salary-payments
 * Body: { amount, forMonth, forYear, mode, referenceNo?, notes?, paidAt? }
 * Multiple payments per staff per month are allowed (advance + balance),
 * same ledger pattern as student fee Payments.
 */
async function createSalaryPayment(req, res) {
  const { staffId } = req.params;
  const { amount, forMonth, forYear, mode, referenceNo, notes, paidAt } = req.body;

  if (!amount || Number(amount) <= 0) throw new ApiError(400, "amount must be greater than 0");
  if (!forMonth || forMonth < 1 || forMonth > 12) throw new ApiError(400, "forMonth must be 1-12");
  if (!forYear) throw new ApiError(400, "forYear is required");
  if (!mode) throw new ApiError(400, "mode is required");

  const staff = await prisma.staff.findFirst({ where: { id: staffId, isDeleted: false } });
  if (!staff) throw new ApiError(404, "Staff record not found");

  const payment = await prisma.salaryPayment.create({
    data: {
      staffId,
      amount,
      forMonth,
      forYear,
      mode,
      referenceNo,
      notes,
      paidAt: paidAt ? new Date(paidAt) : undefined,
      recordedById: req.user.id,
    },
  });
  res.status(201).json(payment);
}

/**
 * GET /api/staff/:staffId/salary-payments
 */
async function listSalaryPaymentsForStaff(req, res) {
  const payments = await prisma.salaryPayment.findMany({
    where: { staffId: req.params.staffId, isDeleted: false },
    orderBy: [{ forYear: "desc" }, { forMonth: "desc" }],
  });
  res.json(payments);
}

/**
 * GET /api/salary-status?month=&year=
 * For every active staff member, shows expected salary vs paid so far
 * for the given month — "who hasn't been paid yet this month".
 */
async function getSalaryStatus(req, res) {
  const month = Number(req.query.month);
  const year = Number(req.query.year);
  if (!month || !year) throw new ApiError(400, "month and year query params are required");

  const staff = await prisma.staff.findMany({
    where: { isActive: true, isDeleted: false },
    orderBy: { name: "asc" },
  });

  const status = await Promise.all(
    staff.map(async (s) => {
      const payments = await prisma.salaryPayment.findMany({
        where: { staffId: s.id, forMonth: month, forYear: year, isDeleted: false },
      });
      const paid = payments.reduce((sum, p) => sum + Number(p.amount), 0);
      const expected = s.monthlySalary ? Number(s.monthlySalary) : null;
      return {
        staffId: s.id,
        name: s.name,
        type: s.type,
        expected,
        paid,
        balance: expected !== null ? Math.max(expected - paid, 0) : null,
        fullyPaid: expected !== null ? paid >= expected : paid > 0,
      };
    })
  );

  res.json({ month, year, staff: status });
}

module.exports = { createSalaryPayment, listSalaryPaymentsForStaff, getSalaryStatus };
