// Express doesn't automatically catch errors thrown inside `async` route
// handlers — an unhandled rejection would just hang the request. Wrapping
// a handler in this forwards any thrown/rejected error to next(err), which
// our error-handling middleware (middleware/errorHandler.js) deals with.
// This lets every controller just `throw` or `await` freely without a
// try/catch in every single function.
const asyncHandler = (fn) => (req, res, next) => {
  Promise.resolve(fn(req, res, next)).catch(next);
};

module.exports = asyncHandler;
