const express = require("express");
const router = express.Router();
const { getInvoiceSettings, updateInvoiceSettings } = require("../controllers/settingsController");
const { requireAuth, requireRole } = require("../middleware/auth");

router.get("/invoice-settings", requireAuth, getInvoiceSettings);
router.patch("/invoice-settings", requireAuth, requireRole("MASTER"), updateInvoiceSettings);

module.exports = router;
