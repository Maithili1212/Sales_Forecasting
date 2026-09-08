const express = require('express');
const asyncHandler = require('../utils/asyncHandler');
const { requireRole } = require('../middleware/auth');
const batchesController = require('../controllers/batches.controller');
const qualityLogsController = require('../controllers/qualityLogs.controller');

const router = express.Router();

router.get('/:batchId', asyncHandler(batchesController.getBatch));
router.patch(
  '/:batchId/status',
  requireRole('production_head', 'plant_head'),
  asyncHandler(batchesController.updateBatchStatus)
);

// Quality logs are a sub-resource of a batch, same pattern as batches
// being a sub-resource of an order in orders.routes.js. No PATCH/DELETE
// here on purpose — an inspection record shouldn't be editable after the
// fact; a correction should be a new log entry, not a rewrite of history.
router.post(
  '/:batchId/quality-logs',
  requireRole('quality_head', 'plant_head'),
  asyncHandler(qualityLogsController.createQualityLog)
);
router.get('/:batchId/quality-logs', asyncHandler(qualityLogsController.listLogsForBatch));

module.exports = router;
