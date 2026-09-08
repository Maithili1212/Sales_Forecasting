const db = require('../config/db');
const AppError = require('../utils/AppError');
const { isValidDateString } = require('../utils/validators');

const isNonNegativeInt = (v) => Number.isInteger(v) && v >= 0;
const isPositiveInt = (v) => Number.isInteger(v) && v > 0;

// POST /api/batches/:batchId/quality-logs
// Records an inspection result for a batch.
//
// pass_fail is NEVER accepted from the client — it's always computed here
// from defect_count (0 defects = pass, any defects = fail). Same
// principle as total_amount in orders.controller.js: a value that can be
// derived from other stored data shouldn't also be hand-entered, because
// the two copies can then disagree (e.g. someone logging 3 defects but
// clicking "pass").
//
// logged_by is read from req.user (set by authenticateJWT), NOT from the
// request body. Before Phase 5 auth existed, this had to be a client-
// supplied user_id — trusted, but forgeable by anything sent over the
// wire. Now identity comes from a verified token signature instead, so
// a client genuinely cannot claim to be someone else. The route-level
// requireRole('quality_head', 'plant_head') in batches.routes.js is what
// guarantees req.user is actually allowed to log a quality check at all.
async function createQualityLog(req, res) {
  const { batchId } = req.params;
  const { qty_checked, defect_count, check_date } = req.body;
  const logged_by = req.user.user_id;

  if (!isPositiveInt(qty_checked)) {
    throw new AppError(400, 'qty_checked is required and must be a positive integer.');
  }
  if (!isNonNegativeInt(defect_count)) {
    throw new AppError(400, 'defect_count is required and must be a non-negative integer.');
  }
  if (check_date !== undefined && check_date !== null && !isValidDateString(check_date)) {
    throw new AppError(400, 'check_date must be YYYY-MM-DD.');
  }
  if (defect_count > qty_checked) {
    throw new AppError(400, 'defect_count cannot exceed qty_checked.');
  }

  const batchResult = await db.query('SELECT * FROM batches WHERE batch_id = $1', [batchId]);
  if (batchResult.rows.length === 0) {
    throw new AppError(404, 'Batch not found.');
  }
  const batch = batchResult.rows[0];

  // Mirrors the Phase 1 rule that batches only move forward through
  // pending -> in_progress -> completed: you can't meaningfully inspect
  // a batch that hasn't started production yet.
  if (batch.status === 'pending') {
    throw new AppError(400, 'Cannot log a quality check for a batch that has not started production yet.');
  }
  if (qty_checked > batch.quantity) {
    throw new AppError(400, `qty_checked (${qty_checked}) cannot exceed the batch quantity (${batch.quantity}).`);
  }

  const pass_fail = defect_count === 0 ? 'pass' : 'fail';

  const result = await db.query(
    `INSERT INTO quality_logs (batch_id, logged_by, check_date, qty_checked, defect_count, pass_fail)
     VALUES ($1, $2, COALESCE($3, CURRENT_DATE), $4, $5, $6)
     RETURNING *`,
    [batchId, logged_by, check_date ?? null, qty_checked, defect_count, pass_fail]
  );

  res.status(201).json(result.rows[0]);
}

// GET /api/batches/:batchId/quality-logs
async function listLogsForBatch(req, res) {
  const { batchId } = req.params;
  const result = await db.query(
    'SELECT * FROM quality_logs WHERE batch_id = $1 ORDER BY check_date DESC, log_id DESC',
    [batchId]
  );
  res.json(result.rows);
}

// GET /api/quality-logs?pass_fail=fail&batch_id=3
// A general listing endpoint — useful for the Phase 3 anomaly-detection
// script and any future "recent failures" dashboard view.
async function listQualityLogs(req, res) {
  const { pass_fail, batch_id } = req.query;
  const conditions = [];
  const params = [];

  if (pass_fail) {
    params.push(pass_fail);
    conditions.push(`pass_fail = $${params.length}`);
  }
  if (batch_id) {
    params.push(batch_id);
    conditions.push(`batch_id = $${params.length}`);
  }

  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
  const result = await db.query(
    `SELECT * FROM quality_logs ${where} ORDER BY check_date DESC, log_id DESC`,
    params
  );
  res.json(result.rows);
}

// GET /api/quality-logs/:logId
async function getQualityLog(req, res) {
  const { logId } = req.params;
  const result = await db.query('SELECT * FROM quality_logs WHERE log_id = $1', [logId]);
  if (result.rows.length === 0) {
    throw new AppError(404, 'Quality log not found.');
  }
  res.json(result.rows[0]);
}

module.exports = { createQualityLog, listLogsForBatch, listQualityLogs, getQualityLog };
