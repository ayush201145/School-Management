const { ApiError } = require("../middleware/errorHandler");

const INCREASING_TYPES = new Set(["IN", "ADJUSTMENT_IN"]);
const DECREASING_TYPES = new Set(["OUT", "ADJUSTMENT_OUT"]);

/**
 * Applies a stock movement to an ItemVariant and records it in the
 * InventoryTransaction ledger, atomically, inside the given transaction
 * client. This is the ONLY function that should ever change
 * stockQuantity — every restock, sale, or correction goes through here
 * so the running total can never drift from the ledger that explains it.
 *
 * Stock is NEVER blocked for sales (type "OUT") — schools may need to
 * sell against backorder (e.g. new admission needs a uniform that's
 * out of stock right now). Instead, if the sale would take stock
 * negative, this function flags it via `wentNegative`/`shortBy` on the
 * return value so the caller can attach a warning to the bill.
 *
 * Manual corrections (ADJUSTMENT_OUT) still block on insufficient stock
 * by default, since an admin manually removing units that don't exist
 * usually indicates a mistake, not a real-world backorder — pass
 * opts.allowNegativeStock = true to override.
 *
 * @param tx - a Prisma transaction client (from prisma.$transaction)
 * @param itemVariantId
 * @param type - "IN" | "OUT" | "ADJUSTMENT_IN" | "ADJUSTMENT_OUT"
 * @param quantity - always positive
 * @param opts - { note, recordedById, purchaseId, allowNegativeStock }
 */
async function applyStockMovement(tx, itemVariantId, type, quantity, opts = {}) {
  if (quantity <= 0) throw new ApiError(400, "quantity must be greater than 0");
  if (!INCREASING_TYPES.has(type) && !DECREASING_TYPES.has(type)) {
    throw new ApiError(400, "Invalid inventory movement type");
  }

  const variant = await tx.itemVariant.findFirst({
    where: { id: itemVariantId, isDeleted: false },
  });
  if (!variant) throw new ApiError(404, "Item variant not found");

  const delta = INCREASING_TYPES.has(type) ? quantity : -quantity;

  const updatedVariant = await tx.itemVariant.update({
    where: { id: itemVariantId },
    data: {
      stockQuantity: {
        increment: delta
      }
    }
  });

  const newStock = updatedVariant.stockQuantity;

  // Sales (OUT) never block — backorder is allowed by default.
  // Manual decreases (ADJUSTMENT_OUT) still block unless explicitly overridden.
  const blockOnNegative = type === "ADJUSTMENT_OUT" && !opts.allowNegativeStock;
  if (newStock < 0 && blockOnNegative) {
    throw new ApiError(
      400,
      `Insufficient stock for "${variant.label}": had ${variant.stockQuantity}, tried to remove ${quantity}`
    );
  }

  const transaction = await tx.inventoryTransaction.create({
    data: {
      itemVariantId,
      type,
      quantity,
      note: opts.note,
      recordedById: opts.recordedById,
      purchaseId: opts.purchaseId,
    },
  });

  return {
    variant: { ...variant, stockQuantity: newStock },
    transaction,
    wentNegative: newStock < 0,
    shortBy: newStock < 0 ? Math.abs(newStock) : 0,
  };
}

module.exports = { applyStockMovement };
