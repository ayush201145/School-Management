const express = require("express");
const router = express.Router();

const {
  createPayment,
  listPaymentsForFee,
  listTransactions,
  listDues,
  markFeeAsDefaulted,
} = require("../controllers/paymentController");
const { requireAuth, requireRole } = require("../middleware/auth");

// Payments against a specific fee
router.post(
  "/student-fees/:studentFeeId/payments",
  requireAuth,
  requireRole("ADMIN", "ACCOUNTANT"),
  createPayment
);
router.get("/student-fees/:studentFeeId/payments", requireAuth, listPaymentsForFee);

// Mark fee as defaulted
router.post(
  "/student-fees/:studentFeeId/default",
  requireAuth,
  requireRole("ADMIN", "ACCOUNTANT"),
  markFeeAsDefaulted
);

// Full transaction ledger (#7)
router.get("/transactions", requireAuth, listTransactions);

// Dues report (#5)
router.get("/dues", requireAuth, listDues);

module.exports = router;
