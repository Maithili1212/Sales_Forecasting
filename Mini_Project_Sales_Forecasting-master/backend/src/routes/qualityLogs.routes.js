const express = require('express');
const asyncHandler = require('../utils/asyncHandler');
const qualityLogsController = require('../controllers/qualityLogs.controller');

const router = express.Router();

router.get('/', asyncHandler(qualityLogsController.listQualityLogs));
router.get('/:logId', asyncHandler(qualityLogsController.getQualityLog));

module.exports = router;
