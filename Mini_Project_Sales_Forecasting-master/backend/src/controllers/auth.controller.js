const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../config/db');
const AppError = require('../utils/AppError');
const { isNonEmptyString } = require('../utils/validators');

const VALID_ROLES = ['plant_head', 'production_head', 'quality_head'];

// A bcrypt hash embeds its own random salt, so identical passwords never
// produce the same stored hash — that's what stops a database leak from
// letting an attacker spot "these two accounts share a password" or use
// precomputed rainbow tables. 10 rounds is bcrypt's common default: high
// enough to be slow to brute-force, low enough not to noticeably delay a
// real login on ordinary hardware.
const BCRYPT_ROUNDS = 10;

// POST /api/auth/login — public, no token required (this IS how you get one)
async function login(req, res) {
  const { username, password } = req.body;

  if (!isNonEmptyString(username) || !isNonEmptyString(password)) {
    throw new AppError(400, 'username and password are required.');
  }

  const result = await db.query('SELECT * FROM users WHERE username = $1', [username]);
  const user = result.rows[0];

  // Deliberately the same error message whether the username doesn't
  // exist OR the password is wrong. Returning a different message for
  // "no such user" vs. "wrong password" would let an attacker enumerate
  // which usernames are real accounts just by trying logins.
  const invalidCredentialsError = new AppError(401, 'Invalid username or password.');

  if (!user || !user.is_active) {
    throw invalidCredentialsError;
  }

  const passwordMatches = await bcrypt.compare(password, user.password_hash);
  if (!passwordMatches) {
    throw invalidCredentialsError;
  }

  const token = jwt.sign(
    { user_id: user.user_id, username: user.username, role: user.role },
    process.env.JWT_SECRET,
    { expiresIn: process.env.JWT_EXPIRES_IN || '8h' }
  );

  res.json({ token, user: { user_id: user.user_id, username: user.username, role: user.role } });
}

// POST /api/auth/register — requires a valid plant_head token (see the
// requireRole('plant_head') on this route in auth.routes.js). Roles are
// fixed at exactly 3 types for this project (per the brief), but nothing
// stops the Plant Head from onboarding a new Production Head or Quality
// Head employee through the app as people join or leave the shop.
async function register(req, res) {
  const { username, password, role } = req.body;

  if (!isNonEmptyString(username)) {
    throw new AppError(400, 'username is required.');
  }
  if (!isNonEmptyString(password) || password.length < 8) {
    throw new AppError(400, 'password is required and must be at least 8 characters.');
  }
  if (!VALID_ROLES.includes(role)) {
    throw new AppError(400, `role must be one of: ${VALID_ROLES.join(', ')}.`);
  }

  const existing = await db.query('SELECT 1 FROM users WHERE username = $1', [username]);
  if (existing.rows.length > 0) {
    throw new AppError(409, 'That username is already taken.');
  }

  const password_hash = await bcrypt.hash(password, BCRYPT_ROUNDS);

  const result = await db.query(
    `INSERT INTO users (username, password_hash, role)
     VALUES ($1, $2, $3)
     RETURNING user_id, username, role, is_active, created_at`,
    [username.trim(), password_hash, role]
  );

  res.status(201).json(result.rows[0]);
}

module.exports = { login, register };
