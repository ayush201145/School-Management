const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");

function signToken(user) {
  return jwt.sign(
    {
      id: user.id,
      username: user.username,
      role: user.role,
      teacherId: user.teacherId,
    },
    process.env.JWT_SECRET,
    { expiresIn: process.env.JWT_EXPIRES_IN || "7d" }
  );
}

/**
 * POST /api/auth/login
 * Body: { username, password }
 */
async function login(req, res) {
  const { username, password, clientType } = req.body;
  if (!username || !password) {
    throw new ApiError(400, "username and password are required");
  }

  const user = await prisma.user.findFirst({
    where: { username, isDeleted: false, isActive: true },
  });
  if (!user) {
    throw new ApiError(401, "Invalid username or password");
  }

  const valid = await bcrypt.compare(password, user.passwordHash);
  if (!valid) {
    throw new ApiError(401, "Invalid username or password");
  }

  if (user.role === "MASTER" && clientType !== "web") {
    throw new ApiError(403, "Master accounts are restricted to the web portal.");
  }

  const token = signToken(user);
  res.json({
    token,
    user: {
      id: user.id,
      username: user.username,
      role: user.role,
      teacherId: user.teacherId,
    },
  });
}

/**
 * POST /api/auth/bootstrap-admin
 * One-time setup route to create the FIRST admin or master account when the
 * database is empty for that role.
 */
async function bootstrapAdmin(req, res) {
  const { username, password, role } = req.body;
  if (!username || !password) {
    throw new ApiError(400, "username and password are required");
  }

  const targetRole = role === "MASTER" ? "MASTER" : "ADMIN";

  const existingUser = await prisma.user.findFirst({
    where: { role: targetRole, isDeleted: false },
  });
  if (existingUser) {
    throw new ApiError(403, `A user with role ${targetRole} already exists.`);
  }

  const passwordHash = await bcrypt.hash(password, 10);
  const user = await prisma.user.create({
    data: { username, passwordHash, role: targetRole },
  });

  const token = signToken(user);
  res.status(201).json({
    token,
    user: { id: user.id, username: user.username, role: user.role },
  });
}

/**
 * GET /api/auth/me
 */
async function me(req, res) {
  const user = await prisma.user.findUnique({
    where: { id: req.user.id },
    select: { id: true, username: true, role: true, teacherId: true, isActive: true },
  });
  if (!user) throw new ApiError(404, "User not found");
  res.json(user);
}

module.exports = { login, bootstrapAdmin, me };
