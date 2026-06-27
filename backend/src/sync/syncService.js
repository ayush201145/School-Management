const prisma = require("../prismaClient");
const { isSyncable, getTableConfig } = require("./syncRegistry");

/**
 * PUSH: applies a batch of client-side changes to the server.
 *
 * Each change is one of:
 *   { table, id, op: "upsert", data, clientUpdatedAt }
 *   { table, id, op: "delete", clientUpdatedAt }
 *
 * Conflict rule (per design decision: flag for manual review):
 *   - If the row doesn't exist on the server yet → no conflict, just create it.
 *   - If the row exists and server.updatedAt <= clientUpdatedAt → no conflict,
 *     the client's version is at least as new as what the server has seen
 *     from this client before → apply it.
 *   - If the row exists and server.updatedAt > clientUpdatedAt → the server
 *     has a newer change the client never saw → CONFLICT. We do NOT
 *     overwrite. We record a SyncConflict row and skip applying this change.
 *
 * Returns a per-change result so the device knows what was applied,
 * what conflicted (and needs admin resolution), and what failed.
 */
async function pushChanges(changes, clientUserId) {
  const results = [];

  for (const change of changes) {
    const { table, id, op, data, clientUpdatedAt } = change;

    if (!isSyncable(table)) {
      results.push({ table, id, status: "rejected", reason: `Table "${table}" is not syncable` });
      continue;
    }
    const config = getTableConfig(table);
    const model = prisma[config.prismaModel];

    try {
      const existing = await model.findUnique({ where: { id } });

      if (op === "delete") {
        if (!existing) {
          results.push({ table, id, status: "applied", note: "Already absent on server" });
          continue;
        }
        if (new Date(existing.updatedAt) > new Date(clientUpdatedAt)) {
          await recordConflict(table, id, data || {}, existing, clientUserId);
          results.push({ table, id, status: "conflict" });
          continue;
        }
        await model.update({ where: { id }, data: { isDeleted: true } });
        results.push({ table, id, status: "applied", op: "delete" });
        continue;
      }

      // op === "upsert"
      const cleanData = pickWritableFields(data, config.writableFields);

      if (!existing) {
        await model.create({ data: { id, ...cleanData, syncedAt: new Date() } });
        results.push({ table, id, status: "applied", op: "create" });
        continue;
      }

      if (new Date(existing.updatedAt) > new Date(clientUpdatedAt)) {
        await recordConflict(table, id, cleanData, existing, clientUserId);
        results.push({ table, id, status: "conflict" });
        continue;
      }

      await model.update({ where: { id }, data: { ...cleanData, syncedAt: new Date() } });
      results.push({ table, id, status: "applied", op: "update" });
    } catch (err) {
      results.push({ table, id, status: "error", reason: err.message });
    }
  }

  return results;
}

/**
 * PULL: returns every row across all syncable tables that has changed
 * since `since` (an ISO timestamp), so the device can apply server-side
 * changes to its local Room database.
 *
 * Soft-deleted rows ARE included (with isDeleted: true) so the device
 * knows to remove them locally too — this is why we never hard-delete.
 */
async function pullChanges(since) {
  const sinceDate = since ? new Date(since) : new Date(0);
  const out = {};

  for (const [tableName, config] of Object.entries(require("./syncRegistry").SYNC_TABLES)) {
    const model = prisma[config.prismaModel];
    const rows = await model.findMany({
      where: { updatedAt: { gt: sinceDate } },
      orderBy: { updatedAt: "asc" },
    });
    out[tableName] = rows;
  }

  return { serverTime: new Date().toISOString(), tables: out };
}

function pickWritableFields(data, allowedFields) {
  const out = {};
  for (const key of allowedFields) {
    if (data && data[key] !== undefined) out[key] = data[key];
  }
  return out;
}

async function recordConflict(table, recordId, clientData, serverRow, clientUserId) {
  // serverRow may contain Prisma Decimal or Date objects that don't
  // serialize correctly into a Json column on their own — round-trip
  // through JSON.stringify/parse (using a replacer for Decimal) so what
  // lands in the database is plain, query-safe JSON.
  const safeServerData = JSON.parse(
    JSON.stringify(serverRow, (key, value) => {
      if (value && typeof value === "object" && typeof value.toFixed === "function") {
        return value.toString(); // Prisma Decimal
      }
      return value;
    })
  );
  const safeClientData = JSON.parse(JSON.stringify(clientData));

  await prisma.syncConflict.create({
    data: {
      tableName: table,
      recordId,
      clientData: safeClientData,
      serverData: safeServerData,
      clientUserId,
      status: "PENDING",
    },
  });
}

module.exports = { pushChanges, pullChanges };
