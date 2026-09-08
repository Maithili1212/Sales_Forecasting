package com.smsms.app.model

data class Batch(
    val batch_id: Int,
    val order_id: Int,
    val quantity: Int,
    val status: String, // "pending" | "in_progress" | "completed"
    val created_at: String
)

data class CreateBatchRequest(val quantity: Int)
data class UpdateBatchStatusRequest(val status: String)
