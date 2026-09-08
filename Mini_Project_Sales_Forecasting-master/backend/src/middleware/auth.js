const jwt = require('jsonwebtoken');
const AppError = require('../utils/AppError');

// Reads "Authorization: Bearer <token>", verifies it was signed by this
// server (JWT_SECRET) and hasn't expired, then attaches the decoded
// payload as req.user for every handler downstream. This is the ONLY
// place identity gets established — nothing after this point should ever
// trust a user_id or role sent in a request body, because a request body
// is just text the client typed; a verified JWT signature is the one
// thing a client cannot forge without knowing JWT_SECRET.
function authenticateJWT(req, res, next) {
  const header = req.headers.authorization;
  if (!header || !header.startsWith('Bearer ')) {
    throw new AppError(401, 'Missing or malformed Authorization header.');
  }

  const token = header.slice('Bearer '.length);
  try {
    req.user = jwt.verify(token, process.env.JWT_SECRET);
  } catch (err) {
    throw new AppError(401, 'Invalid or expired token.');
  }

  next();
}

// A factory, not middleware itself — call it with the roles allowed for
// a route: requireRole('plant_head', 'production_head'). Must run AFTER
// authenticateJWT, since it reads req.user.role.
//
// Why this has to live on the server: the Android app can hide a button
// for a role it thinks shouldn't see it, but that's just UI polish —
// nothing stops someone from calling the API directly (curl, Postman,
// a modified client) with a valid token from a different role. Only a
// check here, on every request, actually enforces anything.
function requireRole(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user) {
      throw new AppError(401, 'Not authenticated.');
    }
    if (!allowedRoles.includes(req.user.role)) {
      throw new AppError(403, `This action requires role: ${allowedRoles.join(' or ')}.`);
    }
    next();
  };
}

module.exports = { authenticateJWT, requireRole };
