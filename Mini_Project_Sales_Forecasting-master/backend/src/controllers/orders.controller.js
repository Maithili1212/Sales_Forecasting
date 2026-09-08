const db = require('../config/db');
const AppError = require('../utils/AppError');
const { isNonEmptyString, isPositiveInt, isValidDateString } = require('../utils/validators');
const { computeAccountSummary } = require('./payments.controller');

// POST /api/orders
// Creates a new customer order. `rate` (price per unit) is accepted as
// input but NOT stored — we only store the computed total_amount. This is
// the "financial fields should be auto-calculated, not hand-entered" rule
// from the project brief: if we let the app send total_amount directly,
// a typo or a modified request could silently create a wrong invoice
// amount. Computing it server-side from rate * qty means the number in
// the database is always internally consistent.
async function createOrder(req, res) {
  const { customer_name, job_type, order_date, due_date, total_qty, rate } = req.body;

  if (!isNonEmptyString(customer_name)) {
    throw new AppError(400, 'customer_name is required.');
  }
  if (!isValidDateString(order_date)) {
    throw new AppError(400, 'order_date is required and must be YYYY-MM-DD.');
  }
  if (due_date !== undefined && due_date !== null && !isValidDateString(due_date)) {
    throw new AppError(400, 'due_date must be YYYY-MM-DD.');
  }
  if (!isPositiveInt(total_qty)) {
    throw new AppError(400, 'total_qty is required and must be a positive integer.');
  }
  if (rate !== undefined && rate !== null && (typeof rate !== 'number' || rate < 0)) {
    throw new AppError(400, 'rate must be a non-negative number.');
  }

  const total_amount = rate != null ? Number((rate * total_qty).toFixed(2)) : null;

  const result = await db.query(
    `INSERT INTO orders (customer_name, job_type, order_date, due_date, total_qty, total_amount)
     VALUES ($1, $2, $3, $4, $5, $6)
     RETURNING *`,
    [customer_name.trim(), job_type ?? null, order_date, due_date ?? null, total_qty, total_amount]
  );

  res.status(201).json(result.rows[0]);
}

// GET /api/orders?status=active&customer_name=acme
// Lists orders, most recent order_date first. Optional filters keep this
// usable once there are hundreds of orders instead of 3.
async function listOrders(req, res) {
  const { status, customer_name } = req.query;
  const conditions = [];
  const params = [];

  if (status) {
    params.push(status);
    conditions.push(`status = $${params.length}`);
  }
  if (customer_name) {
    params.push(`%${customer_name}%`);
    conditions.push(`customer_name ILIKE $${params.length}`);
  }

  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
  const result = await db.query(
    `SELECT * FROM orders ${where} ORDER BY order_date DESC, order_id DESC`,
    params
  );
  res.json(result.rows);
}

// GET /api/orders/:orderId
// Returns the order together with its batches, payments, and a computed
// account summary in one response, so the Android order-detail screen
// doesn't need three more round trips.
async function getOrder(req, res) {
  const { orderId } = req.params;

  const orderResult = await db.query('SELECT * FROM orders WHERE order_id = $1', [orderId]);
  if (orderResult.rows.length === 0) {
    throw new AppError(404, 'Order not found.');
  }
  const order = orderResult.rows[0];

  const batchesResult = await db.query(
    'SELECT * FROM batches WHERE order_id = $1 ORDER BY batch_id',
    [orderId]
  );
  const paymentsResult = await db.query(
    'SELECT * FROM payments WHERE order_id = $1 ORDER BY payment_date DESC, payment_id DESC',
    [orderId]
  );
  const accountSummary = await computeAccountSummary(orderId, order.total_amount);

  res.json({
    ...order,
    batches: batchesResult.rows,
    payments: paymentsResult.rows,
    account_summary: accountSummary,
  });
}

// PATCH /api/orders/:orderId
// Deliberately only allows editing customer_name, job_type, due_date and
// rate (which recomputes total_amount). order_date, total_qty and status
// are NOT editable here:
//  - total_qty drives how batches get split, so changing it after batches
//    already exist could make quantities stop adding up.
//  - status has its own endpoint (closeOrder) because closing an order is
//    a business decision with rules attached, not a free-form field edit.
async function updateOrder(req, res) {
  const { orderId } = req.params;
  const { customer_name, job_type, due_date, rate } = req.body;

  const existing = await db.query('SELECT * FROM orders WHERE order_id = $1', [orderId]);
  if (existing.rows.length === 0) {
    throw new AppError(404, 'Order not found.');
  }
  const order = existing.rows[0];

  if (customer_name !== undefined && !isNonEmptyString(customer_name)) {
    throw new AppError(400, 'customer_name cannot be empty.');
  }
  if (due_date !== undefined && due_date !== null && !isValidDateString(due_date)) {
    throw new AppError(400, 'due_date must be YYYY-MM-DD.');
  }
  if (rate !== undefined && rate !== null && (typeof rate !== 'number' || rate < 0)) {
    throw new AppError(400, 'rate must be a non-negative number.');
  }

  const newCustomerName = customer_name !== undefined ? customer_name.trim() : order.customer_name;
  const newJobType = job_type !== undefined ? job_type : order.job_type;
  const newDueDate = due_date !== undefined ? due_date : order.due_date;
  const newTotalAmount =
    rate !== undefined
      ? (rate != null ? Number((rate * order.total_qty).toFixed(2)) : null)
      : order.total_amount;

  const result = await db.query(
    `UPDATE orders
     SET customer_name = $1, job_type = $2, due_date = $3, total_amount = $4
     WHERE order_id = $5
     RETURNING *`,
    [newCustomerName, newJobType, newDueDate, newTotalAmount, orderId]
  );

  res.json(result.rows[0]);
}

// PATCH /api/orders/:orderId/close
// Business rule enforced here, server-side, on purpose: an order can only
// be closed once it actually has batches and every one of them is
// 'completed'. If this check only existed in the Android app's UI, anyone
// calling the API directly (or a future second client) could close an
// order with half-finished production. Server-side is the only place a
// rule like this can be trusted.
async function closeOrder(req, res) {
  const { orderId } = req.params;

  const orderResult = await db.query('SELECT * FROM orders WHERE order_id = $1', [orderId]);
  if (orderResult.rows.length === 0) {
    throw new AppError(404, 'Order not found.');
  }
  const order = orderResult.rows[0];

  if (order.status === 'closed') {
    throw new AppError(400, 'Order is already closed.');
  }

  const batchesResult = await db.query('SELECT status FROM batches WHERE order_id = $1', [orderId]);
  if (batchesResult.rows.length === 0) {
    throw new AppError(400, 'Cannot close an order with no batches.');
  }
  const incomplete = batchesResult.rows.filter((b) => b.status !== 'completed');
  if (incomplete.length > 0) {
    throw new AppError(400, `Cannot close order: ${incomplete.length} batch(es) not completed yet.`);
  }

  const result = await db.query(
    `UPDATE orders SET status = 'closed' WHERE order_id = $1 RETURNING *`,
    [orderId]
  );
  res.json(result.rows[0]);
}

module.exports = { createOrder, listOrders, getOrder, updateOrder, closeOrder };
