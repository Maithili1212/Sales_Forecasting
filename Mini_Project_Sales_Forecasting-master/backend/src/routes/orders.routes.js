const express = require('express');
const asyncHandler = require('../utils/asyncHandler');
const { requireRole } = require('../middleware/auth');
const ordersController = require('../controllers/orders.controller');
const batchesController = require('../controllers/batches.controller');
const paymentsController = require('../controllers/payments.controller');

const router = express.Router();

// Production Head "enters order/job/sales data" per the project brief;
// Plant Head is the admin role and can do anything. Quality Head has no
// business creating or editing orders.
const canWriteOrders = requireRole('production_head', 'plant_head');

router.post('/', canWriteOrders, asyncHandler(ordersController.createOrder));
router.get('/', asyncHandler(ordersController.listOrders));
router.get('/:orderId', asyncHandler(ordersController.getOrder));
router.patch('/:orderId', canWriteOrders, asyncHandler(ordersController.updateOrder));
router.patch('/:orderId/close', canWriteOrders, asyncHandler(ordersController.closeOrder));

// Batches are a sub-resource of an order: you always create/list them
// through the order they belong to (POST/GET /api/orders/:orderId/batches).
router.post('/:orderId/batches', canWriteOrders, asyncHandler(batchesController.createBatch));
router.get('/:orderId/batches', asyncHandler(batchesController.listBatchesForOrder));

// Payments are a sub-resource of an order too. Deliberately NOT gated by
// order status (unlike batches) — payment can still be recorded against
// a closed order.
router.post('/:orderId/payments', canWriteOrders, asyncHandler(paymentsController.createPayment));
router.get('/:orderId/payments', asyncHandler(paymentsController.listPaymentsForOrder));

module.exports = router;
