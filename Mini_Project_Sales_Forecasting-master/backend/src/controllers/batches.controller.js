const db = require('../config/db');
const AppError = require('../utils/AppError');
const { isPositiveInt } = require('../utils/validators');

const VALID_STATUSES = ['pending', 'in_progress', 'completed'];

// Only forward progress is allowed: pending -> in_progress -> completed.
// No skipping backwards (completed -> pending makes no sense once quality
// checks may already reference the batch) and no re-completing. Modeled
// as a map so adding a status later is a one-line change, not a rewrite.
const ALLOWED_TRANSITIONS = {
  pending: ['in_progress'],
  in_progress: ['completed'],
  completed: [],
};

// POST /api/orders/:orderId/batches
// Splits part of an order's total quantity into a trackable production
// batch. The important rule: the sum of all batches under an order can
// never exceed the order's total_qty — otherwise "batches" stops
// meaning anything as a breakdown of the order. We enforce that here
// with a query, not just trust whatever quantity the app sends.
async function createBatch(req, res) {
  const { orderId } = req.params;
  const { quantity } = req.body;

  if (!isPositiveInt(quantity)) {
    throw new AppError(400, 'quantity is required and must be a positive integer.');
  }

  const orderResult = await db.query('SELECT * FROM orders WHERE order_id = $1', [orderId]);
  if (orderResult.rows.length === 0) {
    throw new AppError(404, 'Order not found.');
  }
  const order = orderResult.rows[0];

  if (order.status === 'closed') {
    throw new AppError(400, 'Cannot add batches to a closed order.');
  }

  const sumResult = await db.query(
    'SELECT COALESCE(SUM(quantity), 0) AS total FROM batches WHERE order_id = $1',
    [orderId]
  );
  const alreadyBatched = Number(sumResult.rows[0].total);

  if (alreadyBatched + quantity > order.total_qty) {
    const remaining = order.total_qty - alreadyBatched;
    throw new AppError(
      400,
      `Batch quantity exceeds order total. Remaining un-batched quantity: ${remaining}.`
    );
  }

  const result = await db.query(
    `INSERT INTO batches (order_id, quantity) VALUES ($1, $2) RETURNING *`,
    [orderId, quantity]
  );

  res.status(201).json(result.rows[0]);
}

// GET /api/orders/:orderId/batches
async function listBatchesForOrder(req, res) {
  const { orderId } = req.params;
  const result = await db.query(
    'SELECT * FROM batches WHERE order_id = $1 ORDER BY batch_id',
    [orderId]
  );
  res.json(result.rows);
}

// GET /api/batches/:batchId
async function getBatch(req, res) {
  const { batchId } = req.params;
  const result = await db.query('SELECT * FROM batches WHERE batch_id = $1', [batchId]);
  if (result.rows.length === 0) {
    throw new AppError(404, 'Batch not found.');
  }
  res.json(result.rows[0]);
}

// PATCH /api/batches/:batchId/status
async function updateBatchStatus(req, res) {
  const { batchId } = req.params;
  const { status } = req.body;

  if (!VALID_STATUSES.includes(status)) {
    throw new AppError(400, `status must be one of: ${VALID_STATUSES.join(', ')}.`);
  }

  const existing = await db.query('SELECT * FROM batches WHERE batch_id = $1', [batchId]);
  if (existing.rows.length === 0) {
    throw new AppError(404, 'Batch not found.');
  }
  const batch = existing.rows[0];

  if (status !== batch.status && !ALLOWED_TRANSITIONS[batch.status].includes(status)) {
    throw new AppError(
      400,
      `Cannot move batch from '${batch.status}' to '${status}'. Allowed next step(s): ${
        ALLOWED_TRANSITIONS[batch.status].join(', ') || 'none'
      }.`
    );
  }

  const result = await db.query(
    `UPDATE batches SET status = $1 WHERE batch_id = $2 RETURNING *`,
    [status, batchId]
  );
  res.json(result.rows[0]);
}

module.exports = { createBatch, listBatchesForOrder, getBatch, updateBatchStatus };
