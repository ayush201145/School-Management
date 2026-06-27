const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");

/**
 * GET /api/teachers
 * Query params: ?search=&isActive=true
 */
async function listTeachers(req, res) {
  const { search, isActive } = req.query;

  const where = { isDeleted: false };
  if (isActive !== undefined) where.isActive = isActive === "true";
  if (search) {
    where.OR = [
      { firstName: { contains: search, mode: "insensitive" } },
      { lastName: { contains: search, mode: "insensitive" } },
      { employeeNo: { contains: search, mode: "insensitive" } },
    ];
  }

  const teachers = await prisma.teacher.findMany({
    where,
    orderBy: { firstName: "asc" },
    include: { sections: { select: { id: true, name: true, classId: true } } },
  });
  res.json(teachers);
}

/**
 * GET /api/teachers/:id
 */
async function getTeacher(req, res) {
  const teacher = await prisma.teacher.findFirst({
    where: { id: req.params.id, isDeleted: false },
    include: { sections: true, user: { select: { id: true, username: true, role: true } } },
  });
  if (!teacher) throw new ApiError(404, "Teacher not found");
  res.json(teacher);
}

/**
 * POST /api/teachers
 */
async function createTeacher(req, res) {
  const { employeeNo, firstName, lastName, phone, email, address, qualification, joiningDate } = req.body;

  const teacher = await prisma.teacher.create({
    data: { employeeNo, firstName, lastName, phone, email, address, qualification, joiningDate },
  });
  res.status(201).json(teacher);
}

/**
 * PATCH /api/teachers/:id
 */
async function updateTeacher(req, res) {
  const existing = await prisma.teacher.findFirst({
    where: { id: req.params.id, isDeleted: false },
  });
  if (!existing) throw new ApiError(404, "Teacher not found");

  const allowed = ["firstName", "lastName", "phone", "email", "address", "qualification", "isActive"];
  const data = {};
  for (const key of allowed) {
    if (req.body[key] !== undefined) data[key] = req.body[key];
  }

  const teacher = await prisma.teacher.update({ where: { id: req.params.id }, data });
  res.json(teacher);
}

/**
 * DELETE /api/teachers/:id
 * Soft delete — keeps history intact (sections referencing this
 * teacher as classTeacher are NOT auto-reassigned; admin should
 * reassign sections before/after removing a teacher).
 */
async function deleteTeacher(req, res) {
  const existing = await prisma.teacher.findFirst({
    where: { id: req.params.id, isDeleted: false },
  });
  if (!existing) throw new ApiError(404, "Teacher not found");

  await prisma.teacher.update({
    where: { id: req.params.id },
    data: { isDeleted: true, isActive: false },
  });
  res.status(204).send();
}

module.exports = { listTeachers, getTeacher, createTeacher, updateTeacher, deleteTeacher };
