const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");

// ---------- Academic Years ----------

async function listAcademicYears(req, res) {
  const years = await prisma.academicYear.findMany({
    where: { isDeleted: false },
    orderBy: { startDate: "desc" },
  });
  res.json(years);
}

async function createAcademicYear(req, res) {
  const { label, startDate, endDate, isCurrent } = req.body;

  // If this year is marked current, un-mark any previous current year
  // so there's only ever one "current" academic year at a time.
  if (isCurrent) {
    await prisma.academicYear.updateMany({
      where: { isCurrent: true },
      data: { isCurrent: false },
    });
  }

  const year = await prisma.academicYear.create({
    data: { label, startDate, endDate, isCurrent: !!isCurrent },
  });
  res.status(201).json(year);
}

// ---------- Classes ----------

async function listClasses(req, res) {
  const { academicYearId } = req.query;
  const where = { isDeleted: false };
  if (academicYearId) where.academicYearId = academicYearId;

  const classes = await prisma.schoolClass.findMany({
    where,
    orderBy: { name: "asc" },
    include: { sections: { where: { isDeleted: false }, include: { classTeacher: true } } },
  });
  res.json(classes);
}

async function createClass(req, res) {
  const { name, academicYearId } = req.body;
  if (!name || !academicYearId) throw new ApiError(400, "name and academicYearId are required");

  const result = await prisma.$transaction(async (tx) => {
    const schoolClass = await tx.schoolClass.create({ data: { name, academicYearId } });
    await tx.section.create({
      data: {
        name: "General",
        classId: schoolClass.id,
      }
    });
    return schoolClass;
  });

  res.status(201).json(result);
}

// ---------- Sections ----------

async function listSections(req, res) {
  const { classId } = req.query;
  const where = { isDeleted: false };
  if (classId) where.classId = classId;

  const sections = await prisma.section.findMany({
    where,
    orderBy: { name: "asc" },
    include: {
      classTeacher: { select: { id: true, firstName: true, lastName: true } },
      _count: { select: { students: true } },
    },
  });
  res.json(sections);
}

async function createSection(req, res) {
  const { name, classId, classTeacherId } = req.body;
  if (!name || !classId) throw new ApiError(400, "name and classId are required");

  if (classTeacherId) {
    const teacher = await prisma.teacher.findFirst({
      where: { id: classTeacherId, isDeleted: false, isActive: true },
    });
    if (!teacher) throw new ApiError(400, "classTeacherId does not refer to an active teacher");
  }

  const section = await prisma.section.create({ data: { name, classId, classTeacherId } });
  res.status(201).json(section);
}

/**
 * PATCH /api/sections/:id
 * Mainly used to assign/reassign the class teacher for a section.
 */
async function updateSection(req, res) {
  const existing = await prisma.section.findFirst({ where: { id: req.params.id, isDeleted: false } });
  if (!existing) throw new ApiError(404, "Section not found");

  const { name, classTeacherId } = req.body;
  const data = {};
  if (name !== undefined) data.name = name;

  if (classTeacherId !== undefined) {
    if (classTeacherId !== null) {
      const teacher = await prisma.teacher.findFirst({
        where: { id: classTeacherId, isDeleted: false, isActive: true },
      });
      if (!teacher) throw new ApiError(400, "classTeacherId does not refer to an active teacher");
    }
    data.classTeacherId = classTeacherId;
  }

  const section = await prisma.section.update({ where: { id: req.params.id }, data });
  res.json(section);
}

module.exports = {
  listAcademicYears,
  createAcademicYear,
  listClasses,
  createClass,
  listSections,
  createSection,
  updateSection,
};
