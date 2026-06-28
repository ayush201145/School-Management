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

async function rolloverAcademicYear(req, res) {
  const { fromYearId, toYearId } = req.body;
  if (!fromYearId || !toYearId) {
    throw new ApiError(400, "fromYearId and toYearId are required");
  }

  const fromYear = await prisma.academicYear.findUnique({ where: { id: fromYearId } });
  const toYear = await prisma.academicYear.findUnique({ where: { id: toYearId } });
  if (!fromYear || !toYear) {
    throw new ApiError(404, "One or both academic years not found");
  }

  const bookCategory = await prisma.itemCategory.findFirst({ where: { type: "BOOK" } });
  if (!bookCategory) {
    throw new ApiError(404, "Books category not found. Please seed the database first.");
  }

  // Fetch classes for both years
  const oldClasses = await prisma.schoolClass.findMany({ where: { academicYearId: fromYearId, isDeleted: false } });
  const newClasses = await prisma.schoolClass.findMany({ where: { academicYearId: toYearId, isDeleted: false } });

  const oldClassIds = oldClasses.map(c => c.id);

  // Find all old book variants linked to old classes
  const oldVariants = await prisma.itemVariant.findMany({
    where: {
      itemCategoryId: bookCategory.id,
      classId: { in: oldClassIds },
      isDeleted: false,
    },
  });

  const results = [];

  await prisma.$transaction(async (tx) => {
    for (const oldVariant of oldVariants) {
      const oldClass = oldClasses.find(c => c.id === oldVariant.classId);
      if (!oldClass) continue;

      // Find matching class in the new academic year
      const newClass = newClasses.find(c => c.name === oldClass.name);
      if (!newClass) continue; // New class level doesn't exist in the target year

      // 1. Rename old variant to include the fromYear label
      const cleanedLabel = oldVariant.label.replace(/\s*\(\d{4}-\d{2}\)/g, ""); // strip existing years
      const oldYearLabel = `${cleanedLabel} (${fromYear.label})`;
      await tx.itemVariant.update({
        where: { id: oldVariant.id },
        data: { label: oldYearLabel },
      });

      // 2. Check if a variant already exists for the new class
      let newVariant = await tx.itemVariant.findFirst({
        where: { itemCategoryId: bookCategory.id, classId: newClass.id, isDeleted: false },
      });

      const newYearLabel = `${cleanedLabel} (${toYear.label})`;

      if (!newVariant) {
        // Create new year variant with price copied, and stock set to 0
        newVariant = await tx.itemVariant.create({
          data: {
            itemCategoryId: bookCategory.id,
            classId: newClass.id,
            label: newYearLabel,
            price: oldVariant.price,
            costPrice: oldVariant.costPrice,
            stockQuantity: 0,
          },
        });
        results.push({ className: oldClass.name, status: "created", newVariantId: newVariant.id });
      } else {
        // Just update its label and prices
        await tx.itemVariant.update({
          where: { id: newVariant.id },
          data: {
            label: newYearLabel,
            price: oldVariant.price,
            costPrice: oldVariant.costPrice,
          },
        });
        results.push({ className: oldClass.name, status: "updated", newVariantId: newVariant.id });
      }
    }
  });

  res.json({ success: true, processed: results });
}

module.exports = {
  listAcademicYears,
  createAcademicYear,
  listClasses,
  createClass,
  listSections,
  createSection,
  updateSection,
  rolloverAcademicYear,
};
