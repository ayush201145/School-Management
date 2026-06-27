const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");
const { pushChanges, pullChanges } = require("../sync/syncService");
const { isSyncable, getTableConfig } = require("../sync/syncRegistry");

/**
 * POST /api/sync/push
 * Body: { changes: [{ table, id, op, data, clientUpdatedAt }, ...] }
 */
async function push(req, res) {
  const { changes } = req.body;
  if (!Array.isArray(changes)) throw new ApiError(400, "changes must be an array");
  if (changes.length > 500) throw new ApiError(400, "Max 500 changes per push — split into multiple batches");

  const results = await pushChanges(changes, req.user.id);
  const conflictCount = results.filter((r) => r.status === "conflict").length;
  const errorCount = results.filter((r) => r.status === "error").length;

  res.json({
    appliedCount: results.filter((r) => r.status === "applied").length,
    conflictCount,
    errorCount,
    results,
  });
}

/**
 * GET /api/sync/pull?since=<ISO timestamp>
 * Returns every changed row (across all syncable tables) since the
 * given timestamp. Pass the `serverTime` from the PREVIOUS pull
 * response as `since` next time, not the device's own clock — this
 * avoids any drift between device clocks and the server's clock.
 */
async function pull(req, res) {
  const { since } = req.query;
  const result = await pullChanges(since);
  res.json(result);
}

/**
 * GET /api/sync/conflicts?status=PENDING
 * For the admin conflict-review screen.
 */
async function listConflicts(req, res) {
  const { status } = req.query;
  const where = status ? { status } : {};
  const conflicts = await prisma.syncConflict.findMany({
    where,
    orderBy: { createdAt: "desc" },
  });
  res.json(conflicts);
}

/**
 * POST /api/sync/conflicts/:id/resolve
 * Body: { resolution: "keep_server" | "keep_client" }
 * Admin decides which version wins. Applies the chosen data to the
 * real table, then marks the conflict resolved.
 */
async function resolveConflict(req, res) {
  const { id } = req.params;
  const { resolution } = req.body;
  if (!["keep_server", "keep_client"].includes(resolution)) {
    throw new ApiError(400, "resolution must be 'keep_server' or 'keep_client'");
  }

  const conflict = await prisma.syncConflict.findUnique({ where: { id } });
  if (!conflict) throw new ApiError(404, "Conflict not found");
  if (conflict.status !== "PENDING") throw new ApiError(400, "Conflict has already been resolved");

  if (resolution === "keep_client") {
    if (!isSyncable(conflict.tableName)) {
      throw new ApiError(400, `Table "${conflict.tableName}" is no longer recognized as syncable`);
    }
    const config = getTableConfig(conflict.tableName);
    const model = prisma[config.prismaModel];
    const cleanData = {};
    for (const key of config.writableFields) {
      if (conflict.clientData[key] !== undefined) cleanData[key] = conflict.clientData[key];
    }
    await model.update({
      where: { id: conflict.recordId },
      data: { ...cleanData, syncedAt: new Date() },
    });
  }
  // resolution === "keep_server": the server row is already correct,
  // nothing to apply — we just mark the conflict resolved below.

  const updated = await prisma.syncConflict.update({
    where: { id },
    data: {
      status: resolution === "keep_client" ? "RESOLVED_CLIENT" : "RESOLVED_SERVER",
      resolvedById: req.user.id,
      resolvedAt: new Date(),
    },
  });

  res.json(updated);
}

module.exports = { push, pull, listConflicts, resolveConflict };
