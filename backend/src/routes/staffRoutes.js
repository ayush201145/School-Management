const express = require("express");
const router = express.Router();

const {
  listStaff,
  getStaff,
  createStaff,
  updateStaff,
  deleteStaff,
} = require("../controllers/staffController");
const {
  createSalaryPayment,
  listSalaryPaymentsForStaff,
  getSalaryStatus,
} = require("../controllers/salaryController");
const { requireAuth, requireRole } = require("../middleware/auth");

// Staff CRUD — same role split as Teacher (ADMIN manages, others view)
router.get("/staff", requireAuth, listStaff);
router.get("/staff/:id", requireAuth, getStaff);
router.post("/staff", requireAuth, requireRole("ADMIN"), createStaff);
router.patch("/staff/:id", requireAuth, requireRole("ADMIN"), updateStaff);
router.delete("/staff/:id", requireAuth, requireRole("ADMIN"), deleteStaff);

// Salary payments
router.post(
  "/staff/:staffId/salary-payments",
  requireAuth,
  requireRole("ADMIN", "ACCOUNTANT"),
  createSalaryPayment
);
router.get("/staff/:staffId/salary-payments", requireAuth, listSalaryPaymentsForStaff);

// "Who hasn't been paid this month" status report
router.get("/salary-status", requireAuth, getSalaryStatus);

module.exports = router;
