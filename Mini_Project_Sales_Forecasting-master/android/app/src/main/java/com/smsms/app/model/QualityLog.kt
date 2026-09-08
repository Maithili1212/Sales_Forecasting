package com.smsms.app.model

data class QualityLog(
    val log_id: Int,
    val batch_id: Int,
    val logged_by: Int,
    val check_date: String,
    val qty_checked: Int,
    val defect_count: Int,
    val pass_fail: String, // always server-derived — see backend qualityLogs.controller.js
    val created_at: String
)

// No pass_fail (backend derives it from defect_count) and no logged_by
// (backend reads that off the caller's verified JWT, not the request
// body — see backend qualityLogs.controller.js).
data class CreateQualityLogRequest(
    val qty_checked: Int,
    val defect_count: Int
)
