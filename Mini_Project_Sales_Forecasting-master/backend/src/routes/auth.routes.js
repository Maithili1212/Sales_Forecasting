const express = require('express');
const asyncHandler = require('../utils/asyncHandler');
const { authenticateJWT, requireRole } = require('../middleware/auth');
const controller = require('../controllers/auth.controller');

const router = express.Router();

// Public — this is how a client obtains a token in the first place.
router.post('/login', asyncHandler(controller.login));

// Only an already-logged-in Plant Head can create new accounts.
router.post('/register', authenticateJWT, requireRole('plant_head'), asyncHandler(controller.register));

module.exports = router;
