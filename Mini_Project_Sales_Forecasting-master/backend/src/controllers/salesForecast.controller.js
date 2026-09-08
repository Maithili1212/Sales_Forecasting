const db = require('../config/db');
const { runMlScript } = require('../utils/mlRunner');

// POST /api/sales-forecast/run
async function runSalesForecast(req, res) {
  const scriptOutput = await runMlScript('train_forecast.py');

  const rows = await db.query(
    `SELECT model_used,
            COUNT(*) FILTER (WHERE actual_value IS NOT NULL) AS evaluation_rows,
            COUNT(*) FILTER (WHERE actual_value IS NULL) AS future_rows
     FROM sales_forecast
     GROUP BY model_used`
  );

  res.json({
    message: 'Sales forecast run complete.',
    models_used: rows.rows,
    script_output: scriptOutput,
  });
}

// GET /api/sales-forecast
// Returns every row (past evaluation + future forecast) ordered by date —
// the Android results screen plots this directly rather than the backend
// pre-computing MAE/RMSE itself, since that's a trivial derived value the
// client can compute from actual_value/predicted_value pairs it already
// has, same "don't duplicate a value that's derivable" principle as
// total_amount back in Phase 1.
async function listSalesForecast(req, res) {
  const result = await db.query('SELECT * FROM sales_forecast ORDER BY forecast_date');
  res.json(result.rows);
}

module.exports = { runSalesForecast, listSalesForecast };
