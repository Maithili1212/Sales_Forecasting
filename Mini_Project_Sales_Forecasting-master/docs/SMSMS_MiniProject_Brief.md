# SMSMS Mini-Project — Build Brief for Claude Code

## What this is
Android app + small backend for a college mini-project (BTech CSE AIML, Sem 5,
Mini Project course). Digitizes a small machine shop's order tracking, quality
checks, and basic accounts. Sales forecasting is the core ML deliverable and
should get the most build effort and polish.

Team: 3 students, one semester timeline, two evaluation phases (ISE1 done,
now working toward ISE2 — implementation + demo).

## Tech stack
- Android: Kotlin + Jetpack Compose
- Backend: Node.js + Express
- Database: PostgreSQL
- ML: Python + scikit-learn / pandas / statsmodels, exposed via a small API
- Auth: JWT, role-based access (bcrypt for password hashing)

## Roles (3 total — finalized, do not add more)
- **Plant Head (Admin)** — sees forecasts, reports, full visibility
- **Production Head** — enters order/job/sales data
- **Quality Head** — verifies production correctness, enters quality check data

No "Operator" role. No login-less employee tracking in this version.

## Modules and build priority
1. Order & Batch Management — CRUD: create order, split into batches. **Build first** — this is also the source data for forecasting.
2. Quality Control Logging — log defect counts, pass/fail per batch.
3. Anomaly Detection — z-score/IQR script on defect rates from quality_logs. Keep intentionally simple, this is NOT the main ML deliverable.
4. **Sales Forecasting — the highlight.** EDA (trend/seasonality) + a trained time-series model (start with moving-average baseline, then linear regression or a proper time-series model depending on data volume) + evaluation (MAE/RMSE) + a forecast endpoint + a results screen in the app.
5. Role dashboards (3 roles above) + JWT auth + role checks enforced server-side, not just in the app UI.
6. Accounts — amount due/paid/pending per order, simple CRUD.

## Explicitly OUT of scope for this version
- Machine allocation/scheduling — was in the original plan, has been dropped
  for this build. Mention as future scope in docs, do not implement.
- Real-time IoT/machine integration — future scope only.
- Multi-shop support, external accounting/GST integration.

## Database schema (Phase 1 — see accompanying SQL file)
Tables: users, orders, batches, quality_logs, anomaly_flags, payments,
sales_forecast. No machines/schedule_slots table in this version.

## Data note
Forecasting will train on real shop order history (Excel export, being
retrieved). Minimum needed: a date column + one numeric value (quantity or
amount) per order/period. If real history turns out too short/messy to
forecast well, ask before generating synthetic data to supplement it — and
clearly label any synthetic data as synthetic in code comments and reports.

## How to work with the student on this
- Student is not highly proficient in coding — explain what code does in
  plain terms as you write it, not just generate silently. They need to be
  able to explain every part in a viva.
- Flag security basics explicitly wherever relevant: password hashing,
  server-side auth checks, no hardcoded secrets/API keys.
- Build one module at a time, working code before moving to the next —
  don't scaffold everything at once.
- Point out good DB/API design practices as they come up (e.g. why
  role-checks must live server-side, why financial fields should be
  auto-calculated not hand-entered, why certain fields shouldn't allow
  UPDATE/DELETE from the app layer).
- Push back if scope creep starts happening — this needs to stay buildable
  by 3 students in one semester alongside their other coursework.

## Current status
Phase 1 (Foundations + schema) ready to start. Excel order history not yet
reviewed for exact columns/date range — confirm with student before
finalizing the sales_forecast approach.
