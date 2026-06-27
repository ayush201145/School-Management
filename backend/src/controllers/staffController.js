const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");

const VALID_STAFF_TYPES = ["TEACHER", "ADMIN", "ACCOUNTANT_STAFF", "PEON", "DRIVER", "OTHER"];

/**
 * GET /api/staff
 * Query params: ?type=&isActive=true
 */
async function listStaff(req, res) {
  const { type, isActive } = req.query;
  const where = { isDeleted: false };
  if (type) where.type = type;
  if (isActive !== undefined) where.isActive = isActive === "true";

  const staff = await prisma.staff.findMany({
    where,
    orderBy: { name: "asc" },
    include: { teacher: { select: { id: true, employeeNo: true } } },
  });
  res.json(staff);
}

async function getStaff(req, res) {
  const staff = await prisma.staff.findFirst({
    where: { id: req.params.id, isDeleted: false },
    include: {
      teacher: true,
      salaryPayments: { where: { isDeleted: false }, orderBy: { paidAt: "desc" } },
    },
  });
  if (!staff) throw new ApiError(404, "Staff record not found");
  res.json(staff);
}

/**
 * POST /api/staff
 * Body: { name, type, teacherId?, phone?, monthlySalary? }
 * teacherId is optional — set it when this staff record IS a teacher
 * (so salary tracking links back to their existing Teacher profile
 * instead of duplicating name/contact info).
 */
async function createStaff(req, res) {
  const { name, type, teacherId, phone, monthlySalary } = req.body;
  if (!name || !type) throw new ApiError(400, "name and type are required");
  if (!VALID_STAFF_TYPES.includes(type)) {
    throw new ApiError(400, `type must be one of: ${VALID_STAFF_TYPES.join(", ")}`);
  }

  if (teacherId) {
    const teacher = await prisma.teacher.findFirst({ where: { id: teacherId, isDeleted: false } });
    if (!teacher) throw new ApiError(400, "teacherId does not refer to a valid teacher");

    const existingStaffForTeacher = await prisma.staff.findFirst({
      where: { teacherId, isDeleted: false },
    });
    if (existingStaffForTeacher) {
      throw new ApiError(409, "This teacher already has a staff/salary record");
    }
  }

  const staff = await prisma.staff.create({
    data: { name, type, teacherId, phone, monthlySalary },
  });
  res.status(201).json(staff);
}

async function updateStaff(req, res) {
  const existing = await prisma.staff.findFirst({ where: { id: req.params.id, isDeleted: false } });
  if (!existing) throw new ApiError(404, "Staff record not found");

  const allowed = ["name", "phone", "monthlySalary", "isActive"];
  const data = {};
  for (const key of allowed) {
    if (req.body[key] !== undefined) data[key] = req.body[key];
  }

  const staff = await prisma.staff.update({ where: { id: req.params.id }, data });
  res.json(staff);
}

async function deleteStaff(req, res) {
  const existing = await prisma.staff.findFirst({ where: { id: req.params.id, isDeleted: false } });
  if (!existing) throw new ApiError(404, "Staff record not found");

  // Soft delete only — salary payment history must be preserved, same
  // reasoning as Student/Teacher soft-deletes elsewhere in this app.
  await prisma.staff.update({
    where: { id: req.params.id },
    data: { isDeleted: true, isActive: false },
  });
  res.status(204).send();
}

module.exports = { listStaff, getStaff, createStaff, updateStaff, deleteStaff };
