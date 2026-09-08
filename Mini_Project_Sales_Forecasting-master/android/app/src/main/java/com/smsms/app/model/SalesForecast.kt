package com.smsms.app.model

data class SalesForecast(
    val forecast_id: Int,
    val forecast_date: String,       // "YYYY-MM-DD", one row per month
    val predicted_value: String,     // NUMERIC comes through as a JSON string — see Order.total_amount
    val actual_value: String?,       // null = a future month, not reached yet
    val model_used: String,
    val generated_at: String
)

data class SalesForecastRunResponse(
    val message: String,
    val script_output: String
)
