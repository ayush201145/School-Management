const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");
const { applyStockMovement } = require("../services/inventoryService");

// ---------- Item Categories ----------

/**
 * GET /api/item-categories
 * e.g. "Books", "Uniform - Summer", "Uniform - Winter"
 */
async function listItemCategories(req, res) {
  const categories = await prisma.itemCategory.findMany({
    where: { isDeleted: false },
    orderBy: { name: "asc" },
    include: { variants: { where: { isDeleted: false }, include: { schoolClass: true } } },
  });
  res.json(categories);
}

async function createItemCategory(req, res) {
  const { name, type, description } = req.body;
  if (!name || !type) throw new ApiError(400, "name and type are required");
  if (!["BOOK", "UNIFORM_SUMMER", "UNIFORM_WINTER", "OTHER"].includes(type)) {
    throw new ApiError(400, "type must be BOOK, UNIFORM_SUMMER, UNIFORM_WINTER, or OTHER");
  }

  const category = await prisma.itemCategory.create({ data: { name, type, description } });
  res.status(201).json(category);
}

// ---------- Item Variants ----------

/**
 * GET /api/item-variants
 * Query params: ?itemCategoryId=&classId=
 */
async function listItemVariants(req, res) {
  const { itemCategoryId, classId } = req.query;
  const where = { isDeleted: false };
  if (itemCategoryId) where.itemCategoryId = itemCategoryId;
  if (classId) where.classId = classId;

  const variants = await prisma.itemVariant.findMany({
    where,
    orderBy: [{ itemCategoryId: "asc" }, { label: "asc" }],
    include: { itemCategory: true, schoolClass: true },
  });
  res.json(variants);
}

/**
 * POST /api/item-variants
 * Examples:
 *   Books:   { itemCategoryId, label: "Class 8 Book Set", classId: "class8id", price: 1200 }
 *   Uniform: { itemCategoryId, label: "Size M", size: "M", price: 600 }
 */
async function createItemVariant(req, res) {
  const { itemCategoryId, label, classId, size, price } = req.body;
  if (!itemCategoryId || !label || price === undefined) {
    throw new ApiError(400, "itemCategoryId, label, and price are required");
  }
  if (Number(price) <= 0) throw new ApiError(400, "price must be greater than 0");

  const category = await prisma.itemCategory.findFirst({
    where: { id: itemCategoryId, isDeleted: false },
  });
  if (!category) throw new ApiError(400, "itemCategoryId does not refer to a valid item category");

  const variant = await prisma.itemVariant.create({
    data: { itemCategoryId, label, classId, size, price },
  });
  res.status(201).json(variant);
}

async function updateItemVariant(req, res) {
  const existing = await prisma.itemVariant.findFirst({ where: { id: req.params.id, isDeleted: false } });
  if (!existing) throw new ApiError(404, "Item variant not found");

  const allowed = ["label", "price", "isActive"];
  const data = {};
  for (const key of allowed) {
    if (req.body[key] !== undefined) data[key] = req.body[key];
  }

  const variant = await prisma.itemVariant.update({ where: { id: req.params.id }, data });
  res.json(variant);
}

// ---------- Purchases (the actual sale to a student) ----------

/**
 * POST /api/students/:studentId/purchases
 * Body: { itemVariantId, quantity (default 1), dueDate (optional, default today) }
 *
 * Records that a student bought a specific variant (e.g. "Class 8 Book
 * Set" or "Uniform Size M"), and creates a StudentFee for it so it
 * flows through the existing payment/partial/dues system automatically.
 *
 * Stock is NOT a hard gate here — a sale always goes through even if
 * stock is 0 or insufficient (schools need to sell on backorder). If
 * the sale takes stock negative, the response includes a `warning`
 * field so the bill/receipt can flag it to whoever is at the counter.
 */
async function createPurchase(req, res) {
  const { studentId } = req.params;
  const { itemVariantId, quantity, dueDate } = req.body;

  if (!itemVariantId) throw new ApiError(400, "itemVariantId is required");
  const qty = quantity ? Number(quantity) : 1;
  if (qty <= 0) throw new ApiError(400, "quantity must be greater than 0");

  const student = await prisma.student.findFirst({ where: { id: studentId, isDeleted: false } });
  if (!student) throw new ApiError(404, "Student not found");

  const variant = await prisma.itemVariant.findFirst({
    where: { id: itemVariantId, isDeleted: false, isActive: true },
    include: { itemCategory: true },
  });
  if (!variant) throw new ApiError(400, "itemVariantId does not refer to an active item variant");

  const totalAmount = Number(variant.price) * qty;
  const label = `${variant.itemCategory.name} - ${variant.label}${qty > 1 ? ` x${qty}` : ""}`;

  // Create the purchase, decrement stock, and create the StudentFee
  // all in ONE transaction. applyStockMovement no longer throws on
  // insufficient stock for sales — it returns wentNegative/shortBy
  // instead, which we turn into a `warning` string on the response.
  const result = await prisma.$transaction(async (tx) => {
    const purchase = await tx.studentItemPurchase.create({
      data: { studentId, itemVariantId, quantity: qty },
    });

    const stockResult = await applyStockMovement(tx, itemVariantId, "OUT", qty, {
      note: `Sold to student ${studentId}`,
      recordedById: req.user.id,
      purchaseId: purchase.id,
    });

    const studentFee = await tx.studentFee.create({
      data: {
        studentId,
        purchaseId: purchase.id,
        description: label,
        amount: totalAmount,
        dueDate: dueDate ? new Date(dueDate) : new Date(),
      },
    });

    return { purchase, studentFee, stockResult };
  });

  const warning = result.stockResult.wentNegative
    ? `Stock for "${variant.label}" is now short by ${result.stockResult.shortBy} unit(s) — this sale was completed on backorder.`
    : null;

  res.status(201).json({
    purchase: result.purchase,
    studentFee: result.studentFee,
    remainingStock: result.stockResult.variant.stockQuantity,
    warning,
  });
}

/**
 * GET /api/students/:studentId/purchases
 */
async function listPurchasesForStudent(req, res) {
  const purchases = await prisma.studentItemPurchase.findMany({
    where: { studentId: req.params.studentId, isDeleted: false },
    orderBy: { createdAt: "desc" },
    include: {
      itemVariant: { include: { itemCategory: true } },
      studentFee: { include: { payments: { where: { isDeleted: false } } } },
    },
  });
  res.json(purchases);
}

// ---------- Inventory ----------

/**
 * POST /api/item-variants/:id/restock
 * Body: { quantity, note }
 * Increases stock — use when new stock arrives from a supplier.
 */
async function restockItemVariant(req, res) {
  const { id } = req.params;
  const { quantity, note } = req.body;
  if (!quantity || Number(quantity) <= 0) throw new ApiError(400, "quantity must be greater than 0");

  const result = await prisma.$transaction((tx) =>
    applyStockMovement(tx, id, "IN", Number(quantity), { note, recordedById: req.user.id })
  );
  res.status(201).json(result);
}

/**
 * POST /api/item-variants/:id/adjust
 * Body: { quantity, direction: "increase"|"decrease", note }
 * Manual correction, e.g. after a physical stock count finds damaged
 * or miscounted units. Requires a note so there's always a reason on record.
 */
async function adjustItemVariantStock(req, res) {
  const { id } = req.params;
  const { quantity, direction, note } = req.body;
  if (!quantity || Number(quantity) <= 0) throw new ApiError(400, "quantity must be greater than 0");
  if (!["increase", "decrease"].includes(direction)) {
    throw new ApiError(400, "direction must be 'increase' or 'decrease'");
  }
  if (!note) throw new ApiError(400, "note is required for manual stock adjustments");

  const type = direction === "increase" ? "ADJUSTMENT_IN" : "ADJUSTMENT_OUT";
  const result = await prisma.$transaction((tx) =>
    applyStockMovement(tx, id, type, Number(quantity), { note, recordedById: req.user.id })
  );
  res.status(201).json(result);
}

/**
 * GET /api/inventory
 * The remaining-inventory report (#6). Query params: ?itemCategoryId=&lowStockBelow=
 */
async function listInventory(req, res) {
  const { itemCategoryId, lowStockBelow } = req.query;
  const where = { isDeleted: false, isActive: true };
  if (itemCategoryId) where.itemCategoryId = itemCategoryId;
  if (lowStockBelow !== undefined) {
    where.stockQuantity = { lt: Number(lowStockBelow) };
  }

  const variants = await prisma.itemVariant.findMany({
    where,
    orderBy: [{ itemCategoryId: "asc" }, { label: "asc" }],
    include: { itemCategory: true, schoolClass: true },
  });

  res.json({
    count: variants.length,
    items: variants.map((v) => ({
      id: v.id,
      category: v.itemCategory.name,
      label: v.label,
      size: v.size,
      class: v.schoolClass ? v.schoolClass.name : null,
      price: v.price,
      stockQuantity: v.stockQuantity,
    })),
  });
}

/**
 * GET /api/item-variants/:id/inventory-history
 * Full movement ledger for one variant — for auditing "where did the stock go".
 */
async function listInventoryHistory(req, res) {
  const transactions = await prisma.inventoryTransaction.findMany({
    where: { itemVariantId: req.params.id, isDeleted: false },
    orderBy: { createdAt: "desc" },
  });
  res.json(transactions);
}

/**
 * GET /api/item-variants/:id/purchases
 * THE "WHO BOUGHT THIS ITEM" REPORT — the reverse lookup of
 * listPurchasesForStudent. Shows every student who bought this
 * specific variant (e.g. every student who bought "Uniform Size 24"),
 * with quantity and payment status.
 */
async function listPurchasesForVariant(req, res) {
  const variant = await prisma.itemVariant.findFirst({
    where: { id: req.params.id, isDeleted: false },
    include: { itemCategory: true },
  });
  if (!variant) throw new ApiError(404, "Item variant not found");

  const purchases = await prisma.studentItemPurchase.findMany({
    where: { itemVariantId: req.params.id, isDeleted: false },
    orderBy: { createdAt: "desc" },
    include: {
      student: { select: { id: true, firstName: true, lastName: true, admissionNo: true } },
      studentFee: { include: { payments: { where: { isDeleted: false } } } },
    },
  });

  const totalQuantitySold = purchases.reduce((sum, p) => sum + p.quantity, 0);

  res.json({
    variant: { id: variant.id, label: variant.label, category: variant.itemCategory.name },
    count: purchases.length,
    totalQuantitySold,
    purchases,
  });
}

module.exports = {
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
};
