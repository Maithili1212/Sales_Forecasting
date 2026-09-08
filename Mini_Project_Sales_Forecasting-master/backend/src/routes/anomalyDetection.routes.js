const express = require('express');
const asyncHandler = require('../utils/asyncHandler');
const { requireRole } = require('../middleware/auth');
const controller = require('../controllers/anomalyDetection.controller');

const router = express.Router();

// Running the ML script is an analysis/admin action; Plant Head is the
// role with "sees forecasts, reports, full visibility" per the brief.
router.post('/anomaly-detection/run', requireRole('plant_head'), asyncHandler(controller.runAnomalyDetection));
router.get('/anomaly-flags', asyncHandler(controller.listAnomalyFlags));

module.exports = router;
