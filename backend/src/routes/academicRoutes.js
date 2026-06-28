const express = require("express");
const router = express.Router();

const {
  listAcademicYears,
  createAcademicYear,
  listClasses,
  createClass,
  listSections,
  createSection,
  updateSection,
  rolloverAcademicYear,
} = require("../controllers/academicController");
const { requireAuth, requireRole } = require("../middleware/auth");

// Academic years
router.get("/academic-years", requireAuth, listAcademicYears);
router.post("/academic-years", requireAuth, requireRole("MASTER"), createAcademicYear);
router.post("/academic/rollover", requireAuth, requireRole("MASTER"), rolloverAcademicYear);

// Classes
router.get("/classes", requireAuth, listClasses);
router.post("/classes", requireAuth, requireRole("MASTER"), createClass);

// Sections
router.get("/sections", requireAuth, listSections);
router.post("/sections", requireAuth, requireRole("MASTER"), createSection);
router.patch("/sections/:id", requireAuth, requireRole("MASTER"), updateSection);

module.exports = router;
