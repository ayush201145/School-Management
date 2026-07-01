const express = require("express");
const router = express.Router();
const { getAttendance, saveAttendance } = require("../controllers/attendanceController");
const { requireAuth, requireRole } = require("../middleware/auth");

router.get("/attendance", requireAuth, requireRole("ADMIN", "TEACHER"), getAttendance);
router.post("/attendance", requireAuth, requireRole("ADMIN", "TEACHER"), saveAttendance);

module.exports = router;
