// Loads .env into process.env as soon as this module is required, so every
// other file can just read process.env.WHATEVER without repeating this.
require('dotenv').config();

const { Pool, types } = require('pg');

// By default node-postgres turns SQL DATE columns into JS Date objects at
// local midnight, and JSON.stringify then renders that in UTC — on a
// UTC+5:30 machine, 2026-09-01 becomes "2026-08-31T18:30:00.000Z", i.e.
// the date shifts back a day. We only ever store/display calendar dates
// (no time-of-day meaning), so we keep DATE columns as plain 'YYYY-MM-DD'
// strings instead and skip the Date-object conversion entirely.
// OID 1082 is Postgres's internal type id for DATE.
types.setTypeParser(1082, (val) => val);

// A Pool keeps a handful of open connections to Postgres ready to reuse,
// instead of opening/closing a new TCP connection for every query — that
// would be slow and would exhaust Postgres's connection limit under load.
const pool = new Pool({
  host: process.env.PGHOST,
  port: process.env.PGPORT,
  database: process.env.PGDATABASE,
  user: process.env.PGUSER,
  password: process.env.PGPASSWORD,
});

pool.on('error', (err) => {
  // Fires for errors on idle clients in the pool (e.g. DB restarted).
  // Logging and letting the process keep running is standard for `pg`.
  console.error('Unexpected error on idle Postgres client', err);
});

module.exports = {
  // Every query goes through this. We always use parameterized queries
  // (query(text, [values])) rather than string-concatenating values into
  // SQL — that's what prevents SQL injection.
  query: (text, params) => pool.query(text, params),
  pool,
};
