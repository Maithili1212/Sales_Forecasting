-- TEMPORARY placeholder users — Phase 2 (Quality Control Logging) needs
-- quality_logs.logged_by to reference a real users row, but login/auth
-- (bcrypt password hashing, JWT, registration) isn't built until Phase 5.
--
-- password_hash below is NOT a usable bcrypt hash — it's a deliberately
-- obvious placeholder string so nobody mistakes these for real, working
-- accounts. Nobody can log in as these users right now. Once Phase 5
-- adds real registration, these stub rows should be deleted and replaced
-- with properly-hashed accounts.
INSERT INTO users (username, password_hash, role) VALUES
    ('placeholder_plant_head',      'NOT_A_REAL_HASH__PENDING_PHASE5_AUTH', 'plant_head'),
    ('placeholder_production_head', 'NOT_A_REAL_HASH__PENDING_PHASE5_AUTH', 'production_head'),
    ('placeholder_quality_head',    'NOT_A_REAL_HASH__PENDING_PHASE5_AUTH', 'quality_head')
ON CONFLICT (username) DO NOTHING;
