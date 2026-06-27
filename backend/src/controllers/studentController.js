const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");

/**
 * GET /api/students
 * Query params: ?search=&sectionId=&isActive=true
 */
async function listStudents(req, res) {
  const { search, sectionId, isActive } = req.query;

  const where = { isDeleted: false };
  if (sectionId) where.sectionId = sectionId;
  if (isActive !== undefined) where.isActive = isActive === "true";
  if (search) {
    where.OR = [
      { firstName: { contains: search, mode: "insensitive" } },
      { lastName: { contains: search, mode: "insensitive" } },
      { admissionNo: { contains: search, mode: "insensitive" } },
      { guardianPhone: { contains: search } },
    ];
  }

  const students = await prisma.student.findMany({
    where,
    orderBy: [{ firstName: "asc" }],
    include: {
      section: { include: { schoolClass: true } },
    },
  });
  res.json(students);
}

/**
 * GET /api/students/:id
 */
async function getStudent(req, res) {
  const student = await prisma.student.findFirst({
    where: { id: req.params.id, isDeleted: false },
    include: {
      section: { include: { schoolClass: true } },
      studentFees: {
        where: { isDeleted: false },
        orderBy: { dueDate: "desc" },
        include: { payments: { where: { isDeleted: false } } },
      },
    },
  });
  if (!student) throw new ApiError(404, "Student not found");
  res.json(student);
}

/**
 * POST /api/students
 */
async function createStudent(req, res) {
  const {
    admissionNo,
    firstName,
    lastName,
    dateOfBirth,
    gender,
    guardianName,
    guardianPhone,
    guardianEmail,
    address,
    fatherPhone,
    motherPhone,
    whatsappPhone,
    tuitionFee,
    sectionId,
    admissionDate,
  } = req.body;

  const section = await prisma.section.findFirst({ where: { id: sectionId, isDeleted: false } });
  if (!section) throw new ApiError(400, "sectionId does not refer to a valid section");

  let finalAdmissionNo = admissionNo;
  if (!finalAdmissionNo || finalAdmissionNo.trim() === "") {
    const allStudents = await prisma.student.findMany({
      select: { admissionNo: true }
    });
    let maxNo = 0;
    for (const s of allStudents) {
      const num = parseInt(s.admissionNo, 10);
      if (!isNaN(num) && num > maxNo) {
        maxNo = num;
      }
    }
    finalAdmissionNo = (maxNo + 1).toString();
  }

  const student = await prisma.student.create({
    data: {
      admissionNo: finalAdmissionNo,
      firstName,
      lastName,
      dateOfBirth: dateOfBirth ? new Date(dateOfBirth) : undefined,
      gender,
      guardianName,
      guardianPhone,
      guardianEmail,
      address,
      fatherPhone,
      motherPhone,
      whatsappPhone,
      tuitionFee: tuitionFee ? parseFloat(tuitionFee) : undefined,
      sectionId,
      admissionDate: admissionDate ? new Date(admissionDate) : undefined,
    },
  });
  res.status(201).json(student);
}

/**
 * PATCH /api/students/:id
 */
async function updateStudent(req, res) {
  const existing = await prisma.student.findFirst({ where: { id: req.params.id, isDeleted: false } });
  if (!existing) throw new ApiError(404, "Student not found");

  const allowed = [
    "firstName",
    "lastName",
    "dateOfBirth",
    "gender",
    "guardianName",
    "guardianPhone",
    "guardianEmail",
    "address",
    "fatherPhone",
    "motherPhone",
    "whatsappPhone",
    "tuitionFee",
    "sectionId",
    "isActive",
  ];
  const data = {};
  for (const key of allowed) {
    if (req.body[key] !== undefined) data[key] = req.body[key];
  }
  if (data.dateOfBirth) data.dateOfBirth = new Date(data.dateOfBirth);
  if (data.tuitionFee !== undefined) {
    data.tuitionFee = data.tuitionFee ? parseFloat(data.tuitionFee) : null;
  }

  if (data.sectionId) {
    const section = await prisma.section.findFirst({ where: { id: data.sectionId, isDeleted: false } });
    if (!section) throw new ApiError(400, "sectionId does not refer to a valid section");
  }

  const student = await prisma.student.update({ where: { id: req.params.id }, data });
  res.json(student);
}

/**
 * DELETE /api/students/:id
 * Soft delete only — financial history (StudentFee/Payment rows)
 * must never disappear, so we never hard-delete a student.
 */
async function deleteStudent(req, res) {
  const existing = await prisma.student.findFirst({ where: { id: req.params.id, isDeleted: false } });
  if (!existing) throw new ApiError(404, "Student not found");

  await prisma.student.update({
    where: { id: req.params.id },
    data: { isDeleted: true, isActive: false },
  });
  res.status(204).send();
}

/**
 * POST /api/students/:id/withdraw
 * Body: { reason: "TRANSFERRED"|"GRADUATED"|"EXPELLED"|"OTHER", notes?, withdrawnDate? }
 *
 * Marks a student as having left the school. Distinct from DELETE:
 * the record and ALL fee/payment history are kept permanently
 * (isDeleted stays false) — the student just stops appearing in
 * active rosters and dues reports.
 */
async function withdrawStudent(req, res) {
  const { reason, notes, withdrawnDate } = req.body;
  const validReasons = ["TRANSFERRED", "GRADUATED", "EXPELLED", "OTHER"];
  if (!validReasons.includes(reason)) {
    throw new ApiError(400, `reason must be one of: ${validReasons.join(", ")}`);
  }

  const existing = await prisma.student.findFirst({ where: { id: req.params.id, isDeleted: false } });
  if (!existing) throw new ApiError(404, "Student not found");

  const student = await prisma.student.update({
    where: { id: req.params.id },
    data: {
      isActive: false,
      withdrawalReason: reason,
      withdrawnDate: withdrawnDate ? new Date(withdrawnDate) : new Date(),
      withdrawalNotes: notes,
    },
  });
  res.json(student);
}

/**
 * POST /api/students/:id/reinstate
 * Reverses a withdrawal — e.g. marked by mistake, or the student re-enrolls.
 */
async function reinstateStudent(req, res) {
  const existing = await prisma.student.findFirst({ where: { id: req.params.id, isDeleted: false } });
  if (!existing) throw new ApiError(404, "Student not found");

  const student = await prisma.student.update({
    where: { id: req.params.id },
    data: {
      isActive: true,
      withdrawalReason: null,
      withdrawnDate: null,
      withdrawalNotes: null,
    },
  });
  res.json(student);
}

module.exports = {
  listStudents,
  getStudent,
  createStudent,
  updateStudent,
  deleteStudent,
  withdrawStudent,
  reinstateStudent,
};
