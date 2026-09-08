const express = require('express');
const asyncHandler = require('../utils/asyncHandler');
const { requireRole } = require('../middleware/auth');
const controller = require('../controllers/salesForecast.controller');

const router = express.Router();

router.post('/run', requireRole('plant_head'), asyncHandler(controller.runSalesForecast));
router.get('/', asyncHandler(controller.listSalesForecast));

module.exports = router;
