const db = require('../config/db');
const { runMlScript } = require('../utils/mlRunner');

// POST /api/anomaly-detection/run
async function runAnomalyDetection(req, res) {
  const scriptOutput = await runMlScript('anomaly_detection.py');

  const summary = await db.query(
    `SELECT COUNT(*) AS total, COUNT(*) FILTER (WHERE is_outlier) AS outliers
     FROM anomaly_flags WHERE metric_type = 'defect_rate'`
  );

  res.json({
    message: 'Anomaly detection run complete.',
    total_flags: Number(summary.rows[0].total),
    outliers: Number(summary.rows[0].outliers),
    script_output: scriptOutput,
  });
}

// GET /api/anomaly-flags?is_outlier=true
// Joined with quality_logs so a flag is meaningful on its own (which
// batch, what was checked) without a second round trip.
async function listAnomalyFlags(req, res) {
  const { is_outlier } = req.query;
  const conditions = [];
  const params = [];

  if (is_outlier !== undefined) {
    params.push(is_outlier === 'true');
    conditions.push(`af.is_outlier = $${params.length}`);
  }

  const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
  const result = await db.query(
    `SELECT af.*, ql.batch_id, ql.check_date, ql.qty_checked, ql.defect_count
     FROM anomaly_flags af
     JOIN quality_logs ql ON ql.log_id = af.quality_log_id
     ${where}
     ORDER BY af.value DESC`,
    params
  );
  res.json(result.rows);
}

module.exports = { runAnomalyDetection, listAnomalyFlags };
