const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");

// ---------- Fee Categories ----------

async function listFeeCategories(req, res) {
  const categories = await prisma.feeCategory.findMany({
    where: { isDeleted: false },
    orderBy: { name: "asc" },
  });
  res.json(categories);
}

async function createFeeCategory(req, res) {
  const { name, description } = req.body;
  if (!name) throw new ApiError(400, "name is required");

  const category = await prisma.feeCategory.create({ data: { name, description } });
  res.status(201).json(category);
}

// ---------- Fee Structures ----------

/**
 * GET /api/fee-structures
 * Query params: ?classId=&academicYearId=
 */
async function listFeeStructures(req, res) {
  const { classId, academicYearId } = req.query;
  const where = { isDeleted: false };
  if (classId) where.classId = classId;
  if (academicYearId) where.academicYearId = academicYearId;

  const structures = await prisma.feeStructure.findMany({
    where,
    orderBy: { dueDate: "desc" },
    include: { feeCategory: true, schoolClass: true },
  });
  res.json(structures);
}

/**
 * POST /api/fee-structures
 * Defines a fee at the class level (e.g. "Class 8, Exam Fee, ₹500, due March 10").
 * This does NOT bill any student yet — use POST /api/fee-structures/:id/assign
 * to actually generate StudentFee rows for students.
 */
async function createFeeStructure(req, res) {
  const { feeCategoryId, classId, academicYearId, amount, dueDate, description } = req.body;
  if (!feeCategoryId || !classId || !academicYearId || !amount || !dueDate) {
    throw new ApiError(400, "feeCategoryId, classId, academicYearId, amount, and dueDate are required");
  }
  if (Number(amount) <= 0) throw new ApiError(400, "amount must be greater than 0");

  const structure = await prisma.feeStructure.create({
    data: {
      feeCategoryId,
      classId,
      academicYearId,
      amount,
      dueDate: new Date(dueDate),
      description,
    },
  });
  res.status(201).json(structure);
}

/**
 * POST /api/fee-structures/:id/assign
 * Body (optional): { sectionId }  — limit to one section instead of the whole class
 *
 * THE BULK-ASSIGN ENGINE.
 * Generates one StudentFee row for every active student in the
 * FeeStructure's class (or a specific section within it), skipping
 * any student who already has a StudentFee for this exact structure
 * (so the button is safe to click more than once — it won't double-bill).
 */
async function assignFeeStructure(req, res) {
  const structure = await prisma.feeStructure.findFirst({
    where: { id: req.params.id, isDeleted: false },
    include: { feeCategory: true, schoolClass: true },
  });
  if (!structure) throw new ApiError(404, "Fee structure not found");

  const { sectionId } = req.body || {};

  const sectionWhere = { classId: structure.classId, isDeleted: false };
  if (sectionId) sectionWhere.id = sectionId;

  const sections = await prisma.section.findMany({ where: sectionWhere, select: { id: true } });
  if (sections.length === 0) {
    throw new ApiError(400, "No matching sections found for this class");
  }
  const sectionIds = sections.map((s) => s.id);

  const students = await prisma.student.findMany({
    where: { sectionId: { in: sectionIds }, isActive: true, isDeleted: false },
    select: { id: true, tuitionFee: true },
  });

  if (students.length === 0) {
    return res.json({ created: 0, skipped: 0, message: "No active students found in target section(s)" });
  }

  // Find which students ALREADY have a StudentFee for this structure,
  // so re-clicking "assign" is idempotent and never double-bills.
  const existing = await prisma.studentFee.findMany({
    where: {
      feeStructureId: structure.id,
      isDeleted: false,
      studentId: { in: students.map((s) => s.id) },
    },
    select: { studentId: true },
  });
  const alreadyBilled = new Set(existing.map((e) => e.studentId));

  const isTuitionCategory = structure.feeCategory.name.toLowerCase().includes("tuition");

  const toCreate = students
    .filter((s) => !alreadyBilled.has(s.id))
    .map((s) => {
      const finalAmount = (isTuitionCategory && s.tuitionFee !== null && s.tuitionFee !== undefined)
        ? s.tuitionFee
        : structure.amount;

      return {
        studentId: s.id,
        feeStructureId: structure.id,
        description: structure.description || `${structure.feeCategory.name} - ${structure.schoolClass.name}`,
        amount: finalAmount,
        dueDate: structure.dueDate,
      };
    });

  if (toCreate.length > 0) {
    await prisma.studentFee.createMany({ data: toCreate });
  }

  res.json({
    created: toCreate.length,
    skipped: alreadyBilled.size,
    totalStudentsInScope: students.length,
  });
}

module.exports = {
  listFeeCategories,
  createFeeCategory,
  listFeeStructures,
  createFeeStructure,
  assignFeeStructure,
};
