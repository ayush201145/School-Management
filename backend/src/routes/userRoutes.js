const express = require("express");
const router = express.Router();
const { listUsers, createUser, updateUser, deleteUser } = require("../controllers/userController");
const { requireAuth, requireRole } = require("../middleware/auth");

// CRUD routes for user accounts — restricted strictly to MASTER role
router.get("/users", requireAuth, requireRole("MASTER"), listUsers);
router.post("/users", requireAuth, requireRole("MASTER"), createUser);
router.patch("/users/:id", requireAuth, requireRole("MASTER"), updateUser);
router.delete("/users/:id", requireAuth, requireRole("MASTER"), deleteUser);

module.exports = router;
