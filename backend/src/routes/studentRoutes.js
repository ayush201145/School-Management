const express = require("express");
const { body } = require("express-validator");
const router = express.Router();

const {
  listStudents,
  getStudent,
  createStudent,
  updateStudent,
  deleteStudent,
  withdrawStudent,
  reinstateStudent,
} = require("../controllers/studentController");
const { requireAuth, requireRole } = require("../middleware/auth");
const { handleValidation } = require("../middleware/validate");

const createValidation = [
  body("admissionNo").notEmpty().withMessage("admissionNo is required"),
  body("firstName").notEmpty().withMessage("firstName is required"),
  body("lastName").notEmpty().withMessage("lastName is required"),
  body("sectionId").notEmpty().withMessage("sectionId is required"),
  body("guardianPhone")
    .optional({ values: "falsy" })
    .isLength({ min: 7 })
    .withMessage("guardianPhone looks too short"),
];

// ADMIN and ACCOUNTANT can manage students (accountant needs to add
// students too in many small schools); TEACHER can only view.
router.get("/", requireAuth, listStudents);
router.get("/:id", requireAuth, getStudent);
router.post(
  "/",
  requireAuth,
  requireRole("ADMIN", "ACCOUNTANT"),
  createValidation,
  handleValidation,
  createStudent
);
router.patch("/:id", requireAuth, requireRole("ADMIN", "ACCOUNTANT"), updateStudent);
router.delete("/:id", requireAuth, requireRole("ADMIN"), deleteStudent);

// "Quit school" — accountant can mark this in most small schools, same
// permission level as editing student records.
router.post("/:id/withdraw", requireAuth, requireRole("ADMIN", "ACCOUNTANT"), withdrawStudent);
router.post("/:id/reinstate", requireAuth, requireRole("ADMIN", "ACCOUNTANT"), reinstateStudent);

module.exports = router;
