const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");

/**
 * GET /api/attendance
 * Query: ?sectionId=xxx&date=YYYY-MM-DD
 */
async function getAttendance(req, res) {
  const { sectionId, date } = req.query;
  if (!sectionId || !date) {
    throw new ApiError(400, "sectionId and date are required");
  }

  const targetDate = new Date(date);
  targetDate.setUTCHours(0, 0, 0, 0);

  const records = await prisma.attendance.findMany({
    where: {
      sectionId,
      date: targetDate,
      isDeleted: false,
    },
  });

  res.json(records);
}

/**
 * POST /api/attendance
 * Body: { sectionId, date, records: [ { studentId, status } ] }
 */
async function saveAttendance(req, res) {
  const { sectionId, date, records } = req.body;
  if (!sectionId || !date || !records || !Array.isArray(records)) {
    throw new ApiError(400, "sectionId, date, and records array are required");
  }

  const targetDate = new Date(date);
  targetDate.setUTCHours(0, 0, 0, 0);

  const now = new Date();
  const processed = [];

  await prisma.$transaction(async (tx) => {
    for (const rec of records) {
      const { studentId, status } = rec;
      if (!studentId || !status) continue;

      const existing = await tx.attendance.findFirst({
        where: {
          studentId,
          date: targetDate,
          isDeleted: false,
        },
      });

      if (existing) {
        const updated = await tx.attendance.update({
          where: { id: existing.id },
          data: {
            status,
            updatedAt: now,
          },
        });
        processed.push(updated);
      } else {
        const created = await tx.attendance.create({
          data: {
            studentId,
            sectionId,
            date: targetDate,
            status,
            markedById: req.user?.id,
          },
        });
        processed.push(created);
      }
    }
  });

  res.json({ success: true, count: processed.length });
}

module.exports = {
  getAttendance,
  saveAttendance,
};
