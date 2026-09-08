// A small helper so controllers can throw a specific HTTP status + message
// (e.g. `throw new AppError(404, 'Order not found')`) instead of every
// handler having its own res.status(...).json(...) error logic scattered
// around. The error middleware knows how to read `.statusCode` off this.
class AppError extends Error {
  constructor(statusCode, message) {
    super(message);
    this.statusCode = statusCode;
  }
}

module.exports = AppError;
