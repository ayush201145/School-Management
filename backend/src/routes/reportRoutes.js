const express = require("express");
const router = express.Router();

const { getMonthlyReport } = require("../controllers/reportController");
const { requireAuth, requireRole } = require("../middleware/auth");

router.get("/reports/monthly", requireAuth, requireRole("ADMIN", "ACCOUNTANT"), getMonthlyReport);

module.exports = router;
