// One-time migration: Phase 2 needed SOME row in `users` for
// quality_logs.logged_by to reference before real auth existed, so we
// inserted 3 placeholder rows with an obviously-fake password_hash
// (see database/seed_users_placeholder.sql). Now that Phase 5 has real
// login, this script turns those same rows into real, working accounts.
//
// It UPDATEs rather than DELETE+INSERTs on purpose: existing quality_logs
// rows already reference these user_ids via a foreign key, and Postgres
// will refuse to delete a referenced row. Updating in place keeps every
// existing quality_logs.logged_by pointing at a valid, now-real account.
//
// Run once: node scripts/seedRealUsers.js

const bcrypt = require('bcryptjs');
const db = require('../src/config/db');

const BCRYPT_ROUNDS = 10;

const ACCOUNTS = [
  { oldUsername: 'placeholder_plant_head', username: 'plant_head', password: 'PlantHead@123', role: 'plant_head' },
  { oldUsername: 'placeholder_production_head', username: 'production_head', password: 'ProductionHead@123', role: 'production_head' },
  { oldUsername: 'placeholder_quality_head', username: 'quality_head', password: 'QualityHead@123', role: 'quality_head' },
];

async function main() {
  console.log('Converting placeholder users into real accounts...\n');

  for (const account of ACCOUNTS) {
    const password_hash = await bcrypt.hash(account.password, BCRYPT_ROUNDS);
    const result = await db.query(
      `UPDATE users SET username = $1, password_hash = $2
       WHERE username = $3
       RETURNING user_id, username, role`,
      [account.username, password_hash, account.oldUsername]
    );

    if (result.rows.length === 0) {
      console.log(`  (skipped) no row found for old username "${account.oldUsername}" — already migrated?`);
      continue;
    }

    const row = result.rows[0];
    console.log(`  user_id ${row.user_id}: ${row.username} (${row.role})  password: ${account.password}`);
  }

  console.log('\nDone. Save these credentials somewhere safe -- this script will not print them again.');
  console.log('Log in via POST /api/auth/login. The plant_head account can create more accounts via POST /api/auth/register.');

  await db.pool.end();
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
