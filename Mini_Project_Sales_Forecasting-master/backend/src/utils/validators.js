// Small, boring validation helpers shared by the controllers. Nothing
// fancy — a mini-project doesn't need a validation library, just enough
// to stop obviously-bad data from ever reaching the database.

const isNonEmptyString = (v) => typeof v === 'string' && v.trim().length > 0;

const isPositiveInt = (v) => Number.isInteger(v) && v > 0;

const isPositiveNumber = (v) => typeof v === 'number' && Number.isFinite(v) && v > 0;

// Accepts 'YYYY-MM-DD' and rejects garbage like 'not-a-date' or '2026-13-40'
// by checking that JS actually parsed it back to the same calendar date.
const isValidDateString = (v) => {
  if (typeof v !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(v)) return false;
  const d = new Date(v + 'T00:00:00Z');
  return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === v;
};

module.exports = { isNonEmptyString, isPositiveInt, isPositiveNumber, isValidDateString };
