const express = require("express");
const router = express.Router();

const {
  listExpenseCategories,
  createExpenseCategory,
  listExpenses,
  createExpense,
  listRecurringTemplates,
  createRecurringTemplate,
  generateRecurringExpense,
} = require("../controllers/expenseController");
const { requireAuth, requireRole } = require("../middleware/auth");

router.get("/expense-categories", requireAuth, listExpenseCategories);
router.post("/expense-categories", requireAuth, requireRole("ADMIN"), createExpenseCategory);

router.get("/expenses", requireAuth, listExpenses);
router.post("/expenses", requireAuth, requireRole("ADMIN", "ACCOUNTANT"), createExpense);

router.get("/recurring-expenses", requireAuth, listRecurringTemplates);
router.post("/recurring-expenses", requireAuth, requireRole("ADMIN"), createRecurringTemplate);
router.post(
  "/recurring-expenses/:id/generate",
  requireAuth,
  requireRole("ADMIN", "ACCOUNTANT"),
  generateRecurringExpense
);

module.exports = router;
