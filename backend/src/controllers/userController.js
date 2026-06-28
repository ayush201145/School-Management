const bcrypt = require("bcryptjs");
const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");

/**
 * GET /api/users
 * Returns list of active users, joined with teacher profiles
 */
async function listUsers(req, res) {
  const users = await prisma.user.findMany({
    where: { isDeleted: false },
    select: {
      id: true,
      username: true,
      role: true,
      isActive: true,
      teacherId: true,
      teacher: { select: { firstName: true, lastName: true, employeeNo: true } },
      createdAt: true,
      updatedAt: true,
    },
    orderBy: { username: "asc" },
  });
  res.json(users);
}

/**
 * POST /api/users
 * Creates a new user record with a hashed password
 */
async function createUser(req, res) {
  const { username, password, role, teacherId } = req.body;
  if (!username || !password || !role) {
    throw new ApiError(400, "username, password, and role are required");
  }
  if (!["MASTER", "ADMIN", "ACCOUNTANT", "TEACHER"].includes(role)) {
    throw new ApiError(400, "role must be MASTER, ADMIN, ACCOUNTANT, or TEACHER");
  }

  const existing = await prisma.user.findFirst({
    where: { username, isDeleted: false },
  });
  if (existing) {
    throw new ApiError(400, "Username already exists");
  }

  // If teacherId is provided, make sure it is valid
  if (teacherId) {
    const teacher = await prisma.teacher.findFirst({ where: { id: teacherId, isDeleted: false } });
    if (!teacher) throw new ApiError(400, "teacherId does not refer to a valid teacher");
  }

  const passwordHash = await bcrypt.hash(password, 10);
  const user = await prisma.user.create({
    data: { username, passwordHash, role, teacherId: teacherId || null },
    select: {
      id: true,
      username: true,
      role: true,
      isActive: true,
      teacherId: true,
    },
  });

  res.status(201).json(user);
}

/**
 * PATCH /api/users/:id
 * Updates user role, password, active state, or teacher relationship
 */
async function updateUser(req, res) {
  const { id } = req.params;
  const { role, isActive, password, teacherId } = req.body;

  const existing = await prisma.user.findFirst({
    where: { id, isDeleted: false },
  });
  if (!existing) {
    throw new ApiError(404, "User not found");
  }

  const data = {};
  if (role !== undefined) {
    if (!["MASTER", "ADMIN", "ACCOUNTANT", "TEACHER"].includes(role)) {
      throw new ApiError(400, "role must be MASTER, ADMIN, ACCOUNTANT, or TEACHER");
    }
    data.role = role;
  }
  if (isActive !== undefined) {
    data.isActive = !!isActive;
  }
  if (password !== undefined && password.trim() !== "") {
    data.passwordHash = await bcrypt.hash(password, 10);
  }
  if (teacherId !== undefined) {
    if (teacherId) {
      const teacher = await prisma.teacher.findFirst({ where: { id: teacherId, isDeleted: false } });
      if (!teacher) throw new ApiError(400, "teacherId does not refer to a valid teacher");
    }
    data.teacherId = teacherId || null;
  }

  const updated = await prisma.user.update({
    where: { id },
    data,
    select: {
      id: true,
      username: true,
      role: true,
      isActive: true,
      teacherId: true,
    },
  });

  res.json(updated);
}

/**
 * DELETE /api/users/:id
 * Soft deletes user (isActive = false, isDeleted = true)
 */
async function deleteUser(req, res) {
  const { id } = req.params;

  const existing = await prisma.user.findFirst({
    where: { id, isDeleted: false },
  });
  if (!existing) {
    throw new ApiError(404, "User not found");
  }

  if (existing.id === req.user.id) {
    throw new ApiError(400, "You cannot delete your own account.");
  }

  await prisma.user.update({
    where: { id },
    data: { isDeleted: true, isActive: false },
  });

  res.status(204).send();
}

module.exports = {
  listUsers,
  createUser,
  updateUser,
  deleteUser,
};
