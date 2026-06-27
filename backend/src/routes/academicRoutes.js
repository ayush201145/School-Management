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
} = require("../controllers/academicController");
const { requireAuth, requireRole } = require("../middleware/auth");

// Academic years
router.get("/academic-years", requireAuth, listAcademicYears);
router.post("/academic-years", requireAuth, requireRole("ADMIN"), createAcademicYear);

// Classes
router.get("/classes", requireAuth, listClasses);
router.post("/classes", requireAuth, requireRole("ADMIN"), createClass);

// Sections
router.get("/sections", requireAuth, listSections);
router.post("/sections", requireAuth, requireRole("ADMIN"), createSection);
router.patch("/sections/:id", requireAuth, requireRole("ADMIN"), updateSection);

module.exports = router;
