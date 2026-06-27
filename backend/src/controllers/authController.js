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
  const { username, password } = req.body;
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
 * One-time setup route to create the FIRST admin account when the
 * database is empty. Refuses to run if any admin already exists,
 * so it can't be (mis)used to create extra admins later.
 */
async function bootstrapAdmin(req, res) {
  const { username, password } = req.body;
  if (!username || !password) {
    throw new ApiError(400, "username and password are required");
  }

  const existingAdmin = await prisma.user.findFirst({
    where: { role: "ADMIN", isDeleted: false },
  });
  if (existingAdmin) {
    throw new ApiError(403, "An admin account already exists. Use the normal user creation route.");
  }

  const passwordHash = await bcrypt.hash(password, 10);
  const user = await prisma.user.create({
    data: { username, passwordHash, role: "ADMIN" },
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
