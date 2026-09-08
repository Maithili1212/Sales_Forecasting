const db = require('../config/db');
const AppError = require('../utils/AppError');
const { isPositiveNumber, isValidDateString } = require('../utils/validators');

const VALID_STATUSES = ['pending', 'partial', 'paid'];

// Forward-only, same reasoning as batch status in batches.controller.js:
// once a payment is fully 'paid' there's nothing left to transition to,
// and there's no legitimate reason to move a payment backwards from
// 'paid' or 'partial' to an earlier state — a correction should be a new
// payment record, not rewriting what was already collected.
const ALLOWED_TRANSITIONS = {
  pending: ['partial', 'paid'],
  partial: ['paid'],
  paid: [],
};

// POST /api/orders/:orderId/payments
// Records a payment against an order. Unlike batches/quality-logs, this
// is intentionally NOT blocked by order status — production can finish
// and the order can close while payment is still outstanding, and a
// payment can still come in afterwards to settle it.
async function createPayment(req, res) {
  const { orderId } = req.params;
  const { amount, status, payment_date } = req.body;

  if (!isPositiveNumber(amount)) {
    throw new AppError(400, 'amount is required and must be a positive number.');
  }
  if (status !== undefined && !VALID_STATUSES.includes(status)) {
    throw new AppError(400, `status must be one of: ${VALID_STATUSES.join(', ')}.`);
  }
  if (payment_date !== undefined && payment_date !== null && !isValidDateString(payment_date)) {
    throw new AppError(400, 'payment_date must be YYYY-MM-DD.');
  }

  const orderResult = await db.query('SELECT 1 FROM orders WHERE order_id = $1', [orderId]);
  if (orderResult.rows.length === 0) {
    throw new AppError(404, 'Order not found.');
  }

  const result = await db.query(
    `INSERT INTO payments (order_id, amount, status, payment_date)
     VALUES ($1, $2, COALESCE($3, 'pending'), COALESCE($4, CURRENT_DATE))
     RETURNING *`,
    [orderId, amount, status ?? null, payment_date ?? null]
  );

  res.status(201).json(result.rows[0]);
}

// GET /api/orders/:orderId/payments
async function listPaymentsForOrder(req, res) {
  const { orderId } = req.params;
  const result = await db.query(
    'SELECT * FROM payments WHERE order_id = $1 ORDER BY payment_date DESC, payment_id DESC',
    [orderId]
  );
  res.json(result.rows);
}

// GET /api/payments/:paymentId
async function getPayment(req, res) {
  const { paymentId } = req.params;
  const result = await db.query('SELECT * FROM payments WHERE payment_id = $1', [paymentId]);
  if (result.rows.length === 0) {
    throw new AppError(404, 'Payment not found.');
  }
  res.json(result.rows[0]);
}

// PATCH /api/payments/:paymentId/status
async function updatePaymentStatus(req, res) {
  const { paymentId } = req.params;
  const { status } = req.body;

  if (!VALID_STATUSES.includes(status)) {
    throw new AppError(400, `status must be one of: ${VALID_STATUSES.join(', ')}.`);
  }

  const existing = await db.query('SELECT * FROM payments WHERE payment_id = $1', [paymentId]);
  if (existing.rows.length === 0) {
    throw new AppError(404, 'Payment not found.');
  }
  const payment = existing.rows[0];

  if (status !== payment.status && !ALLOWED_TRANSITIONS[payment.status].includes(status)) {
    throw new AppError(
      400,
      `Cannot move payment from '${payment.status}' to '${status}'. Allowed next step(s): ${
        ALLOWED_TRANSITIONS[payment.status].join(', ') || 'none'
      }.`
    );
  }

  const result = await db.query(
    `UPDATE payments SET status = $1 WHERE payment_id = $2 RETURNING *`,
    [status, paymentId]
  );
  res.json(result.rows[0]);
}

// Shared with orders.controller.js's getOrder, so an order-detail
// response can include an account summary without a second round trip.
// amount_due is total_amount minus only what's actually confirmed 'paid'
// — a 'partial' payment's recorded amount hasn't fully landed yet, so it
// doesn't reduce the balance owed.
async function computeAccountSummary(orderId, totalAmount) {
  const result = await db.query(
    `SELECT
       COALESCE(SUM(amount) FILTER (WHERE status = 'paid'), 0) AS total_paid,
       COALESCE(SUM(amount) FILTER (WHERE status IN ('pending', 'partial')), 0) AS total_outstanding
     FROM payments WHERE order_id = $1`,
    [orderId]
  );
  const totalPaid = Number(result.rows[0].total_paid);
  const amountDue = totalAmount != null ? Number((totalAmount - totalPaid).toFixed(2)) : null;

  return {
    total_amount: totalAmount,
    total_paid: totalPaid,
    total_outstanding: Number(result.rows[0].total_outstanding),
    amount_due: amountDue,
  };
}

module.exports = {
  createPayment,
  listPaymentsForOrder,
  getPayment,
  updatePaymentStatus,
  computeAccountSummary,
};
