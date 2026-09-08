const express = require('express');
const asyncHandler = require('../utils/asyncHandler');
const { requireRole } = require('../middleware/auth');
const controller = require('../controllers/payments.controller');

const router = express.Router();

router.get('/:paymentId', asyncHandler(controller.getPayment));
router.patch(
  '/:paymentId/status',
  requireRole('production_head', 'plant_head'),
  asyncHandler(controller.updatePaymentStatus)
);

module.exports = router;
