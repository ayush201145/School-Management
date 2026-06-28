const express = require("express");
const router = express.Router();

const { push, pull, listConflicts, resolveConflict } = require("../controllers/syncController");
const { requireAuth, requireRole } = require("../middleware/auth");

router.post("/sync/push", requireAuth, requireRole("ADMIN", "ACCOUNTANT", "TEACHER"), push);
router.get("/sync/pull", requireAuth, requireRole("ADMIN", "ACCOUNTANT", "TEACHER"), pull);

// Conflict review — admin only, since resolving wrong could lose data
router.get("/sync/conflicts", requireAuth, requireRole("ADMIN"), listConflicts);
router.post("/sync/conflicts/:id/resolve", requireAuth, requireRole("ADMIN"), resolveConflict);

module.exports = router;
