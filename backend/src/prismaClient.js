const { PrismaClient } = require("@prisma/client");

// Singleton pattern — prevents exhausting DB connections from
// creating a new PrismaClient on every import (common bug with
// hot-reloading dev servers).
const prisma = new PrismaClient({
  log: process.env.NODE_ENV === "development" ? ["warn", "error"] : ["error"],
});

module.exports = prisma;
