"""
Phase 3 — Anomaly Detection.

Flags quality_logs whose defect rate is unusually HIGH compared to the
rest of the dataset, using a z-score. Deliberately simple (per the project
brief this is NOT the main ML deliverable — Sales Forecasting is):

    defect_rate = defect_count / qty_checked

    z = (this log's defect_rate - mean defect_rate) / standard deviation

A z-score says "how many standard deviations away from average is this
value?". z = 0 means exactly average; z = 2 means two standard deviations
above average, which — for roughly bell-shaped data — only happens by
chance about 2.5% of the time. That's why 2.0 is a common, easy-to-defend
default cutoff.

This check is intentionally ONE-SIDED: we only flag defect rates that are
higher than usual. An unusually LOW defect rate isn't a quality problem,
so there's nothing to flag there — unlike a generic "detect any outlier"
script, this one only looks in the direction that actually matters for
quality control.

Run it any time after new quality_logs exist:
    .venv/Scripts/python.exe anomaly_detection.py
"""

import os
import sys

import numpy as np
import psycopg2
from dotenv import load_dotenv

load_dotenv()

# Below this many data points, mean/standard-deviation are too unstable to
# trust — e.g. with 2 logs, one of them is *always* "above average" by
# definition. This isn't a statistically rigorous minimum, just a sane
# floor for a mini-project dataset.
MIN_SAMPLES = 5

# How many standard deviations above the mean counts as "unusual".
Z_THRESHOLD = 2.0

METRIC_TYPE = "defect_rate"


def get_connection():
    return psycopg2.connect(
        host=os.environ["PGHOST"],
        port=os.environ["PGPORT"],
        dbname=os.environ["PGDATABASE"],
        user=os.environ["PGUSER"],
        password=os.environ["PGPASSWORD"],
    )


def main():
    conn = get_connection()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT log_id, defect_count, qty_checked "
                "FROM quality_logs WHERE qty_checked > 0"
            )
            rows = cur.fetchall()

        if len(rows) < MIN_SAMPLES:
            print(
                f"Only {len(rows)} quality log(s) with qty_checked > 0 — "
                f"need at least {MIN_SAMPLES} to compute a meaningful "
                f"z-score. Skipping; no flags changed."
            )
            return

        log_ids = [r[0] for r in rows]
        defect_rates = np.array([r[1] / r[2] for r in rows])

        mean = float(np.mean(defect_rates))
        std = float(np.std(defect_rates))  # population std (ddof=0) — fine for this purpose

        # The rate value above which a log gets flagged, expressed in the
        # same units as defect_rate itself (not as an abstract z-score),
        # so it's meaningful to show on a dashboard later: "defect rate
        # 0.18 exceeded the threshold of 0.11".
        threshold_rate = mean + Z_THRESHOLD * std

        if std == 0:
            # Every log has the identical defect rate — there's no
            # variation, so by definition nothing is unusual.
            is_outlier = np.zeros(len(defect_rates), dtype=bool)
        else:
            z_scores = (defect_rates - mean) / std
            is_outlier = z_scores > Z_THRESHOLD

        with conn.cursor() as cur:
            # Full recompute every run: defect rates are judged relative
            # to the WHOLE current dataset, and that dataset grows over
            # time, so yesterday's flags can become stale. Deleting and
            # re-inserting is simpler and safer than trying to patch
            # individual rows, and this script only ever owns rows with
            # metric_type = 'defect_rate' — it never touches other
            # metric types that might be added later.
            cur.execute("DELETE FROM anomaly_flags WHERE metric_type = %s", (METRIC_TYPE,))

            cur.executemany(
                """
                INSERT INTO anomaly_flags
                    (quality_log_id, metric_type, value, threshold, is_outlier)
                VALUES (%s, %s, %s, %s, %s)
                """,
                [
                    # psycopg2 doesn't know how to adapt numpy's own
                    # float64/bool_ types, so cast back to native Python
                    # types before sending them over as query parameters.
                    (log_id, METRIC_TYPE, round(float(rate), 4), round(threshold_rate, 4), bool(outlier))
                    for log_id, rate, outlier in zip(log_ids, defect_rates, is_outlier)
                ],
            )
        conn.commit()

        flagged_count = int(is_outlier.sum())
        print(
            f"Processed {len(rows)} quality logs. "
            f"mean={mean:.4f} std={std:.4f} threshold_rate={threshold_rate:.4f}. "
            f"Flagged {flagged_count} as outliers."
        )

    finally:
        conn.close()


if __name__ == "__main__":
    try:
        main()
    except KeyError as e:
        print(f"Missing environment variable: {e}. Did you create ml/.env?", file=sys.stderr)
        sys.exit(1)
