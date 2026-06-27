const express = require("express");
const router = express.Router();
const { login, bootstrapAdmin, me } = require("../controllers/authController");
const { requireAuth } = require("../middleware/auth");

router.post("/login", login);
router.post("/bootstrap-admin", bootstrapAdmin);
router.get("/me", requireAuth, me);

module.exports = router;
