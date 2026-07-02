const express = require("express");
const router = express.Router();

const { getMonthlyReport } = require("../controllers/reportController");
const { getDashboardStats } = require("../controllers/dashboardController");
const { requireAuth, requireRole } = require("../middleware/auth");

router.get("/reports/monthly", requireAuth, requireRole("ADMIN", "ACCOUNTANT"), getMonthlyReport);
router.get("/dashboard/stats", requireAuth, getDashboardStats);

module.exports = router;
