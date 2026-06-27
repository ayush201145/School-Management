const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");

// ---------- Expense Categories ----------

async function listExpenseCategories(req, res) {
  const categories = await prisma.expenseCategory.findMany({
    where: { isDeleted: false },
    orderBy: { name: "asc" },
  });
  res.json(categories);
}

async function createExpenseCategory(req, res) {
  const { name, description } = req.body;
  if (!name) throw new ApiError(400, "name is required");

  const category = await prisma.expenseCategory.create({ data: { name, description } });
  res.status(201).json(category);
}

// ---------- One-off / recorded expenses ----------

/**
 * GET /api/expenses
 * Query params: ?from=&to=&expenseCategoryId=
 */
async function listExpenses(req, res) {
  const { from, to, expenseCategoryId } = req.query;
  const where = { isDeleted: false };
  if (expenseCategoryId) where.expenseCategoryId = expenseCategoryId;
  if (from || to) {
    where.spentAt = {};
    if (from) where.spentAt.gte = new Date(from);
    if (to) where.spentAt.lte = new Date(to);
  }

  const expenses = await prisma.expense.findMany({
    where,
    orderBy: { spentAt: "desc" },
    include: { expenseCategory: true },
  });
  const totalAmount = expenses.reduce((sum, e) => sum + Number(e.amount), 0);
  res.json({ count: expenses.length, totalAmount, expenses });
}

/**
 * POST /api/expenses
 * Body: { expenseCategoryId, description, amount, mode, referenceNo?, spentAt? }
 * A plain one-off expense — pages, ink, repairs, anything not tied to
 * a recurring template.
 */
async function createExpense(req, res) {
  const { expenseCategoryId, description, amount, mode, referenceNo, spentAt } = req.body;
  if (!expenseCategoryId || !description || !amount || !mode) {
    throw new ApiError(400, "expenseCategoryId, description, amount, and mode are required");
  }
  if (Number(amount) <= 0) throw new ApiError(400, "amount must be greater than 0");

  const category = await prisma.expenseCategory.findFirst({
    where: { id: expenseCategoryId, isDeleted: false },
  });
  if (!category) throw new ApiError(400, "expenseCategoryId does not refer to a valid category");

  const expense = await prisma.expense.create({
    data: {
      expenseCategoryId,
      description,
      amount,
      mode,
      referenceNo,
      spentAt: spentAt ? new Date(spentAt) : undefined,
      recordedById: req.user.id,
    },
  });
  res.status(201).json(expense);
}

// ---------- Recurring expense templates ----------

async function listRecurringTemplates(req, res) {
  const templates = await prisma.recurringExpenseTemplate.findMany({
    where: { isDeleted: false, isActive: true },
    orderBy: { label: "asc" },
    include: { expenseCategory: true },
  });
  res.json(templates);
}

/**
 * POST /api/recurring-expenses
 * Body: { expenseCategoryId, label, amount, dayOfMonth? }
 */
async function createRecurringTemplate(req, res) {
  const { expenseCategoryId, label, amount, dayOfMonth } = req.body;
  if (!expenseCategoryId || !label || !amount) {
    throw new ApiError(400, "expenseCategoryId, label, and amount are required");
  }
  if (Number(amount) <= 0) throw new ApiError(400, "amount must be greater than 0");

  const category = await prisma.expenseCategory.findFirst({
    where: { id: expenseCategoryId, isDeleted: false },
  });
  if (!category) throw new ApiError(400, "expenseCategoryId does not refer to a valid category");

  const template = await prisma.recurringExpenseTemplate.create({
    data: { expenseCategoryId, label, amount, dayOfMonth: dayOfMonth || 1 },
  });
  res.status(201).json(template);
}

/**
 * POST /api/recurring-expenses/:id/generate
 * Body: { month, year }
 *
 * Creates a real Expense row from this template for the given month,
 * IF one doesn't already exist (idempotency guarantee — mirrors the
 * exact pattern proven by a real test on the fee-structure bulk-assign
 * endpoint: re-clicking "generate" never double-creates the expense).
 */
async function generateRecurringExpense(req, res) {
  const { id } = req.params;
  const { month, year } = req.body;
  if (!month || !year) throw new ApiError(400, "month and year are required");

  const template = await prisma.recurringExpenseTemplate.findFirst({
    where: { id, isDeleted: false, isActive: true },
    include: { expenseCategory: true },
  });
  if (!template) throw new ApiError(404, "Recurring expense template not found or inactive");

  const periodStart = new Date(year, month - 1, 1);
  const periodEnd = new Date(year, month, 1); // first day of NEXT month, exclusive upper bound

  const existing = await prisma.expense.findFirst({
    where: {
      recurringTemplateId: template.id,
      isDeleted: false,
      spentAt: { gte: periodStart, lt: periodEnd },
    },
  });
  if (existing) {
    return res.json({ created: false, expense: existing, message: "Already generated for this month" });
  }

  const spentAt = new Date(year, month - 1, Math.min(template.dayOfMonth, 28));
  const expense = await prisma.expense.create({
    data: {
      expenseCategoryId: template.expenseCategoryId,
      recurringTemplateId: template.id,
      description: template.label,
      amount: template.amount,
      spentAt,
      mode: "BANK_TRANSFER",
      recordedById: req.user.id,
    },
  });
  res.status(201).json({ created: true, expense });
}

module.exports = {
  listExpenseCategories,
  createExpenseCategory,
  listExpenses,
  createExpense,
  listRecurringTemplates,
  createRecurringTemplate,
  generateRecurringExpense,
};
