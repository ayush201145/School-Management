const { validationResult } = require("express-validator");
const { ApiError } = require("../middleware/errorHandler");

/**
 * Place after express-validator chains in a route to short-circuit
 * with a 400 if any validation rule failed.
 */
function handleValidation(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    const first = errors.array()[0];
    throw new ApiError(400, `${first.path}: ${first.msg}`);
  }
  next();
}

module.exports = { handleValidation };
