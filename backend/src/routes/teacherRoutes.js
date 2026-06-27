const express = require("express");
const { body } = require("express-validator");
const router = express.Router();

const {
  listTeachers,
  getTeacher,
  createTeacher,
  updateTeacher,
  deleteTeacher,
} = require("../controllers/teacherController");
const { requireAuth, requireRole } = require("../middleware/auth");
const { handleValidation } = require("../middleware/validate");

const createValidation = [
  body("employeeNo").notEmpty().withMessage("employeeNo is required"),
  body("firstName").notEmpty().withMessage("firstName is required"),
  body("lastName").notEmpty().withMessage("lastName is required"),
  body("email").optional({ values: "falsy" }).isEmail().withMessage("must be a valid email"),
];

// All routes require login. Only ADMIN can create/edit/delete teacher
// records; everyone logged in can view (teachers viewing colleagues, etc).
router.get("/", requireAuth, listTeachers);
router.get("/:id", requireAuth, getTeacher);
router.post("/", requireAuth, requireRole("ADMIN"), createValidation, handleValidation, createTeacher);
router.patch("/:id", requireAuth, requireRole("ADMIN"), updateTeacher);
router.delete("/:id", requireAuth, requireRole("ADMIN"), deleteTeacher);

module.exports = router;
