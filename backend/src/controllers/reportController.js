const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");

/**
 * GET /api/reports/monthly?month=&year=
 *
 * THE MONTHLY FINANCIAL REPORT. Computes:
 *   Cash Collected = sum of Payment rows (student fees + book/uniform
 *                     sales — Payment already covers both, since
 *                     StudentItemPurchase bills flow through StudentFee)
 *   Expenses       = sum of Expense rows (rent, supplies, etc.)
 *   Salaries       = sum of SalaryPayment rows
 *   Net for month  = Cash Collected - Expenses - Salaries
 *
 * Deliberately called "Net for the month", NOT "profit" — a school
 * isn't a profit-seeking business in the accounting/tax sense, and
 * this is a simple cash-basis subtraction (money in minus money out
 * this calendar month), not a real accrual-based profit figure. Don't
 * present this number as if it were audited/tax-significant.
 */
async function getMonthlyReport(req, res) {
  const month = Number(req.query.month);
  const year = Number(req.query.year);
  if (!month || month < 1 || month > 12) throw new ApiError(400, "month (1-12) is required");
  if (!year) throw new ApiError(400, "year is required");

  const periodStart = new Date(year, month - 1, 1);
  const periodEnd = new Date(year, month, 1); // exclusive upper bound — first day of next month

  const [payments, expenses, salaryPayments] = await Promise.all([
    prisma.payment.findMany({
      where: { isDeleted: false, paidAt: { gte: periodStart, lt: periodEnd } },
    }),
    prisma.expense.findMany({
      where: { isDeleted: false, spentAt: { gte: periodStart, lt: periodEnd } },
      include: { expenseCategory: true },
    }),
    prisma.salaryPayment.findMany({
      where: { isDeleted: false, paidAt: { gte: periodStart, lt: periodEnd } },
    }),
  ]);

  const cashCollected = payments.reduce((sum, p) => sum + Number(p.amount), 0);
  const totalExpenses = expenses.reduce((sum, e) => sum + Number(e.amount), 0);
  const totalSalaries = salaryPayments.reduce((sum, s) => sum + Number(s.amount), 0);
  const netForMonth = cashCollected - totalExpenses - totalSalaries;

  const collectedByMode = {};
  for (const p of payments) {
    collectedByMode[p.mode] = (collectedByMode[p.mode] || 0) + Number(p.amount);
  }

  const expensesByCategory = {};
  for (const e of expenses) {
    const name = e.expenseCategory.name;
    expensesByCategory[name] = (expensesByCategory[name] || 0) + Number(e.amount);
  }

  res.json({
    month,
    year,
    cashCollected,
    collectedByMode,
    totalExpenses,
    expensesByCategory,
    totalSalaries,
    netForMonth,
    transactionCount: payments.length,
    expenseCount: expenses.length,
    salaryPaymentCount: salaryPayments.length,
  });
}

module.exports = { getMonthlyReport };
