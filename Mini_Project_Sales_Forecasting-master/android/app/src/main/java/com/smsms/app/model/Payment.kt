package com.smsms.app.model

data class Payment(
    val payment_id: Int,
    val order_id: Int,
    val amount: String,     // NUMERIC as JSON string, same reasoning as Order.total_amount
    val payment_date: String,
    val status: String,     // "pending" | "partial" | "paid"
    val created_at: String
)

// account_summary.amount_due is server-computed (total_amount minus
// confirmed-'paid' payments) — the app never calculates this itself, same
// "derive, don't duplicate" rule used throughout this app.
data class AccountSummary(
    val total_amount: String?,
    val total_paid: Double,
    val total_outstanding: Double,
    val amount_due: Double?
)

data class CreatePaymentRequest(val amount: Double, val status: String? = null)
data class UpdatePaymentStatusRequest(val status: String)
