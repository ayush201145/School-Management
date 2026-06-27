const express = require("express");
const router = express.Router();

const {
  listItemCategories,
  createItemCategory,
  listItemVariants,
  createItemVariant,
  updateItemVariant,
  createPurchase,
  listPurchasesForStudent,
  listPurchasesForVariant,
  restockItemVariant,
  adjustItemVariantStock,
  listInventory,
  listInventoryHistory,
} = require("../controllers/itemController");
const { requireAuth, requireRole } = require("../middleware/auth");

// Item categories (Books, Uniform - Summer A, Uniform - Summer B, Uniform - Winter)
router.get("/item-categories", requireAuth, listItemCategories);
router.post("/item-categories", requireAuth, requireRole("ADMIN"), createItemCategory);

// Item variants (priced per class for books, per size for uniforms)
router.get("/item-variants", requireAuth, listItemVariants);
router.post("/item-variants", requireAuth, requireRole("ADMIN", "ACCOUNTANT"), createItemVariant);
router.patch("/item-variants/:id", requireAuth, requireRole("ADMIN", "ACCOUNTANT"), updateItemVariant);

// Inventory — restock, manual adjustment, remaining-stock report, history
router.post(
  "/item-variants/:id/restock",
  requireAuth,
  requireRole("ADMIN", "ACCOUNTANT"),
  restockItemVariant
);
router.post(
  "/item-variants/:id/adjust",
  requireAuth,
  requireRole("ADMIN", "ACCOUNTANT"),
  adjustItemVariantStock
);
router.get("/inventory", requireAuth, listInventory);
router.get("/item-variants/:id/inventory-history", requireAuth, listInventoryHistory);

// Purchases (selling an item to a student — creates a StudentFee, decrements stock)
router.post(
  "/students/:studentId/purchases",
  requireAuth,
  requireRole("ADMIN", "ACCOUNTANT"),
  createPurchase
);
router.get("/students/:studentId/purchases", requireAuth, listPurchasesForStudent);
router.get("/item-variants/:id/purchases", requireAuth, listPurchasesForVariant);

module.exports = router;
