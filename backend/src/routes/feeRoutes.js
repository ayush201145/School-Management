const express = require("express");
const router = express.Router();

const {
  listFeeCategories,
  createFeeCategory,
  listFeeStructures,
  createFeeStructure,
  assignFeeStructure,
} = require("../controllers/feeController");
const { requireAuth, requireRole } = require("../middleware/auth");

// Fee categories (Tuition, Exam, Transport...)
router.get("/fee-categories", requireAuth, listFeeCategories);
router.post("/fee-categories", requireAuth, requireRole("ADMIN"), createFeeCategory);

// Fee structures (class-level fee definitions)
router.get("/fee-structures", requireAuth, listFeeStructures);
router.post("/fee-structures", requireAuth, requireRole("ADMIN", "ACCOUNTANT"), createFeeStructure);

// The bulk-assign button
router.post(
  "/fee-structures/:id/assign",
  requireAuth,
  requireRole("ADMIN", "ACCOUNTANT"),
  assignFeeStructure
);

module.exports = router;
