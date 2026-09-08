const express = require('express');
const cors = require('cors');

const authRouter = require('./routes/auth.routes');
const ordersRouter = require('./routes/orders.routes');
const batchesRouter = require('./routes/batches.routes');
const qualityLogsRouter = require('./routes/qualityLogs.routes');
const anomalyDetectionRouter = require('./routes/anomalyDetection.routes');
const salesForecastRouter = require('./routes/salesForecast.routes');
const paymentsRouter = require('./routes/payments.routes');
const { authenticateJWT } = require('./middleware/auth');
const errorHandler = require('./middleware/errorHandler');

const app = express();

// Lets the Android app (running on a different origin — an emulator or
// device) call this API from a WebView/browser context. For a native
// Android HTTP client (Retrofit/OkHttp) CORS doesn't actually apply, but
// it's harmless to have and useful if we ever add a web admin panel.
app.use(cors());

// Parses incoming JSON request bodies into req.body. Without this,
// req.body would be undefined for POST/PATCH requests.
app.use(express.json());

// Simple liveness check — useful for confirming the server is up before
// debugging anything else, and later for things like uptime monitoring.
app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

// /api/auth/login is public (see auth.routes.js); everything else below
// requires a valid token. authenticateJWT is applied per-mount here
// rather than once globally so it's obvious at a glance, from this one
// file, exactly which routers are protected.
app.use('/api/auth', authRouter);
app.use('/api/orders', authenticateJWT, ordersRouter);
app.use('/api/batches', authenticateJWT, batchesRouter);
app.use('/api/quality-logs', authenticateJWT, qualityLogsRouter);
// Handles POST /api/anomaly-detection/run and GET /api/anomaly-flags —
// two different sub-paths, so it's mounted at the bare /api prefix.
app.use('/api', authenticateJWT, anomalyDetectionRouter);
app.use('/api/sales-forecast', authenticateJWT, salesForecastRouter);
app.use('/api/payments', authenticateJWT, paymentsRouter);

// 404 handler for anything that didn't match a route above.
app.use((req, res) => {
  res.status(404).json({ error: 'Not found' });
});

// Must be registered last — see middleware/errorHandler.js for why.
app.use(errorHandler);

module.exports = app;
