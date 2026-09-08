"""
Phase 4 — Sales Forecasting (the main ML deliverable).

Trains on data/SMSMS_monthly_sales_history.csv — 13 real months (Jul 2025
through Jul 2026) of the shop's actual dispatched sales value, consolidated
from raw Excel exports in SMSMS_EDA_SalesHistory.ipynb. That notebook's own
EDA conclusion drives the modeling choice here: one year of data is enough
to see a trend, but NOT enough to reliably prove a repeating seasonal
pattern (that needs at least 2 full annual cycles). So none of the three
candidate models below assume seasonality — they only model level/trend.

Three candidates, cheapest-and-simplest first, per the project brief:

  1. Moving average baseline — predict next month as the average of the
     last K real months. No trend awareness at all; the honest "simplest
     thing that could possibly work" baseline every other model has to
     beat to justify its own existence.

  2. Linear regression — fit a straight line against time (sklearn).
     Captures a trend, assumes it's linear and continues indefinitely.

  3. Holt's linear trend method (statsmodels) — exponential smoothing
     with a trend component, no seasonal component (because we can't
     estimate one reliably from 13 points). More adaptive than a fixed
     straight line: recent months are weighted more heavily.

Evaluation uses WALK-FORWARD validation, not random train/test splitting.
Random splitting would let a model "see the future" (e.g. train on month
12 to predict month 5), which is meaningless for a forecasting problem —
in production you only ever have the past to predict the future. Walk-
forward instead does several honest one-step-ahead predictions in
chronological order: train on months 0-8, predict month 9; train on
0-9, predict month 10; and so on. With only 13 months total this gives
just 4 evaluation folds — small, and that limitation is reported alongside
the numbers rather than hidden.

Usage:
    .venv/Scripts/python.exe train_forecast.py
"""

import os
import sys
import warnings
from datetime import date

import numpy as np
import pandas as pd
import psycopg2
from dotenv import load_dotenv
from sklearn.linear_model import LinearRegression
from statsmodels.tsa.holtwinters import Holt

load_dotenv()

DATA_CSV = os.path.join(os.path.dirname(__file__), "..", "data", "SMSMS_monthly_sales_history.csv")

# First month we're willing to train on before evaluating — with 13 points
# total this leaves exactly 4 one-step-ahead folds (predicting months
# index 9, 10, 11, 12).
MIN_TRAIN_SIZE = 9

MOVING_AVERAGE_WINDOW = 3
FUTURE_MONTHS_TO_FORECAST = 3


def get_connection():
    return psycopg2.connect(
        host=os.environ["PGHOST"],
        port=os.environ["PGPORT"],
        dbname=os.environ["PGDATABASE"],
        user=os.environ["PGUSER"],
        password=os.environ["PGPASSWORD"],
    )


def moving_average_forecast(history: np.ndarray, steps: int) -> np.ndarray:
    """Flat-line the average of the last K real months forward.

    Deliberately does NOT update its own window with previous forecasts —
    that would let small errors compound silently step after step. A
    moving average has no concept of trend, so multi-step-ahead is just
    "repeat the last known average" — this is a real, known limitation of
    the baseline, not a bug, and it's exactly what the other two models
    are meant to improve on.
    """
    avg = np.mean(history[-MOVING_AVERAGE_WINDOW:])
    return np.full(steps, avg)


def linear_regression_forecast(history: np.ndarray, steps: int) -> np.ndarray:
    t = np.arange(len(history)).reshape(-1, 1)
    model = LinearRegression().fit(t, history)
    future_t = np.arange(len(history), len(history) + steps).reshape(-1, 1)
    return model.predict(future_t)


def holt_forecast(history: np.ndarray, steps: int) -> np.ndarray:
    with warnings.catch_warnings():
        # statsmodels warns about the tiny sample size on every fit —
        # true, and already the whole reason this script reports its
        # evaluation limitations explicitly rather than hiding them.
        warnings.simplefilter("ignore")
        model = Holt(history, initialization_method="estimated").fit()
    return np.asarray(model.forecast(steps))


MODELS = {
    "moving_average": moving_average_forecast,
    "linear_regression": linear_regression_forecast,
    "holt_linear_trend": holt_forecast,
}


def walk_forward_evaluate(values: np.ndarray, forecast_fn) -> dict:
    """One-step-ahead predictions in chronological order, from
    MIN_TRAIN_SIZE up to the second-to-last point, each time predicting
    exactly the next real month so it can be checked against ground truth.
    """
    actuals, predictions = [], []
    for train_end in range(MIN_TRAIN_SIZE, len(values)):
        history = values[:train_end]
        actual = values[train_end]
        predicted = forecast_fn(history, 1)[0]
        actuals.append(actual)
        predictions.append(predicted)

    actuals = np.array(actuals)
    predictions = np.array(predictions)
    mae = float(np.mean(np.abs(actuals - predictions)))
    rmse = float(np.sqrt(np.mean((actuals - predictions) ** 2)))
    return {"mae": mae, "rmse": rmse, "predictions": predictions, "actuals": actuals}


def main():
    df = pd.read_csv(DATA_CSV, parse_dates=["month"])
    df = df.sort_values("month").reset_index(drop=True)
    values = df["total_dispatch_amount"].to_numpy(dtype=float)
    months = df["month"]

    if len(values) < MIN_TRAIN_SIZE + 2:
        print(
            f"Only {len(values)} months of data — need at least "
            f"{MIN_TRAIN_SIZE + 2} for a meaningful walk-forward evaluation. "
            f"Aborting; sales_forecast left unchanged."
        )
        return

    print(f"Loaded {len(values)} months: {months.iloc[0].date()} to {months.iloc[-1].date()}\n")

    results = {}
    print("Walk-forward evaluation (lower is better):")
    for name, fn in MODELS.items():
        results[name] = walk_forward_evaluate(values, fn)
        r = results[name]
        print(f"  {name:20s}  MAE={r['mae']:>12,.0f}   RMSE={r['rmse']:>12,.0f}")

    winner = min(results, key=lambda name: results[name]["rmse"])
    print(f"\nBest model by RMSE: {winner}")

    # Refit the winning model on ALL 13 months (evaluation is over —
    # for the real forecast we want it trained on every real data point
    # we have) and project forward.
    future_predictions = MODELS[winner](values, FUTURE_MONTHS_TO_FORECAST)
    last_month = months.iloc[-1]
    future_months = [
        (last_month + pd.DateOffset(months=i)).date() for i in range(1, FUTURE_MONTHS_TO_FORECAST + 1)
    ]

    print(f"\nForecast for the next {FUTURE_MONTHS_TO_FORECAST} months ({winner}):")
    for d, v in zip(future_months, future_predictions):
        print(f"  {d}  ->  {v:,.0f}")

    # Persist: this table is entirely owned by this script (nothing else
    # writes to it), so a full clear-and-regenerate each run is simple
    # and safe — no partial/stale rows from a previous run or a
    # since-abandoned model can linger.
    eval_start_idx = MIN_TRAIN_SIZE
    eval_rows = [
        (months.iloc[i].date(), float(results[winner]["predictions"][i - eval_start_idx]), float(values[i]), winner)
        for i in range(eval_start_idx, len(values))
    ]
    future_rows = [(d, float(v), None, winner) for d, v in zip(future_months, future_predictions)]

    conn = get_connection()
    try:
        with conn.cursor() as cur:
            cur.execute("DELETE FROM sales_forecast")
            cur.executemany(
                """
                INSERT INTO sales_forecast (forecast_date, predicted_value, actual_value, model_used)
                VALUES (%s, %s, %s, %s)
                """,
                eval_rows + future_rows,
            )
        conn.commit()
    finally:
        conn.close()

    print(
        f"\nWrote {len(eval_rows)} evaluation rows (actual_value known) and "
        f"{len(future_rows)} future forecast rows (actual_value NULL, to be "
        f"filled in once those months pass) to sales_forecast."
    )


if __name__ == "__main__":
    try:
        main()
    except KeyError as e:
        print(f"Missing environment variable: {e}. Did you create ml/.env?", file=sys.stderr)
        sys.exit(1)
