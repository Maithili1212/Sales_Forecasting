package com.smsms.app.model

// Mirrors the JSON the backend's /api/orders endpoints return. Field names
// match the database column names (order_id, customer_name, ...) exactly,
// so Gson can map JSON straight into these without any renaming.
data class Order(
    val order_id: Int,
    val customer_name: String,
    val job_type: String?,
    val order_date: String,     // "YYYY-MM-DD" — kept as plain text, no Date parsing needed
    val due_date: String?,
    val total_qty: Int,
    val total_amount: String?,  // Postgres NUMERIC arrives as a JSON string (not a float)
                                 // so money values never pick up floating-point rounding error
    val status: String,
    val created_at: String,
    // batches/payments/account_summary are only populated by
    // GET /api/orders/{id} (the list endpoint omits them to stay light).
    val batches: List<Batch> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val account_summary: AccountSummary? = null
)

// Request body for POST /api/orders. rate is optional — when present the
// backend computes total_amount = rate * total_qty itself; we never send
// total_amount directly (see backend orders.controller.js for why: a
// hand-entered financial total is one typo away from being wrong).
data class CreateOrderRequest(
    val customer_name: String,
    val job_type: String?,
    val order_date: String,
    val due_date: String?,
    val total_qty: Int,
    val rate: Double?
)

data class UpdateOrderRequest(
    val customer_name: String? = null,
    val job_type: String? = null,
    val due_date: String? = null,
    val rate: Double? = null
)

// Shape of the backend's `{ "error": "message" }` responses, used by the
// repository layer to surface a real message instead of just "HTTP 400".
data class ApiErrorResponse(val error: String)
