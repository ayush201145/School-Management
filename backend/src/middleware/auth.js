const jwt = require("jsonwebtoken");

/**
 * Verifies the Bearer token on the request and attaches the decoded
 * payload to req.user. Use on any route that requires login.
 */
function requireAuth(req, res, next) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : null;

  if (!token) {
    return res.status(401).json({ error: "Missing or invalid Authorization header" });
  }

  try {
    const payload = jwt.verify(token, process.env.JWT_SECRET);
    req.user = payload; // { id, username, role, teacherId }
    next();
  } catch (err) {
    return res.status(401).json({ error: "Invalid or expired token" });
  }
}

/**
 * Restricts a route to specific roles. Use AFTER requireAuth.
 * Example: router.post('/students', requireAuth, requireRole('ADMIN', 'ACCOUNTANT'), ...)
 */
function requireRole(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ error: "Not authenticated" });
    }
    // MASTER role inherits all system permissions
    if (req.user.role === "MASTER") {
      return next();
    }
    if (!allowedRoles.includes(req.user.role)) {
      return res.status(403).json({ error: "Insufficient permissions for this action" });
    }
    next();
  };
}

module.exports = { requireAuth, requireRole };
