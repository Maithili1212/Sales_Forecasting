// Express recognizes this as an error-handling middleware specifically
// because it takes 4 arguments (err, req, res, next) — that's how Express
// tells it apart from a normal middleware. It must be registered last,
// after all routes, so it catches errors from anything above it.
function errorHandler(err, req, res, next) {
  const statusCode = err.statusCode || 500;

  // Postgres foreign-key violation — e.g. creating a batch for an
  // order_id that doesn't exist. Turn the cryptic DB error into a
  // message someone using the API can actually understand.
  if (err.code === '23503') {
    return res.status(400).json({ error: 'Referenced record does not exist.' });
  }

  // Postgres check-constraint violation — e.g. status set to something
  // outside the allowed CHECK(...) values in the schema.
  if (err.code === '23514') {
    return res.status(400).json({ error: 'Value violates a database constraint.' });
  }

  if (statusCode === 500) {
    // Don't leak internal error details/stack traces to API clients —
    // log the full thing server-side, send back a generic message.
    console.error(err);
    return res.status(500).json({ error: 'Internal server error.' });
  }

  res.status(statusCode).json({ error: err.message });
}

module.exports = errorHandler;
