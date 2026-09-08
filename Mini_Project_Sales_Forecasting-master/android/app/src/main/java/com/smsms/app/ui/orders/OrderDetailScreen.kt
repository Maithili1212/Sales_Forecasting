package com.smsms.app.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smsms.app.auth.AuthManager
import com.smsms.app.model.AccountSummary
import com.smsms.app.model.Batch
import com.smsms.app.model.Order
import com.smsms.app.model.Payment
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Int,
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = viewModel(factory = OrderDetailViewModelFactory(orderId))
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddBatchDialog by remember { mutableStateOf(false) }
    var qualityLogDialogBatch by remember { mutableStateOf<Batch?>(null) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }

    val session by AuthManager.session.collectAsState()
    val role = session?.user?.role
    val canManageProduction = role == "production_head" || role == "plant_head"
    val canLogQuality = role == "quality_head" || role == "plant_head"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order #$orderId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is OrderDetailUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is OrderDetailUiState.Error ->
                    Text(
                        state.message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )

                is OrderDetailUiState.Loaded -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            OrderSummary(state.order)
                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Batches", style = MaterialTheme.typography.titleMedium)
                                if (canManageProduction) {
                                    TextButton(
                                        onClick = { showAddBatchDialog = true },
                                        enabled = state.order.status == "active" && !state.actionInProgress
                                    ) { Text("+ Add batch") }
                                }
                            }
                        }

                        if (state.order.batches.isEmpty()) {
                            item { Text("No batches yet.", modifier = Modifier.padding(vertical = 8.dp)) }
                        } else {
                            items(state.order.batches) { batch ->
                                BatchRow(
                                    batch = batch,
                                    enabled = !state.actionInProgress,
                                    canAdvance = canManageProduction,
                                    canLogQuality = canLogQuality,
                                    onAdvance = { next -> viewModel.advanceBatch(batch.batch_id, next) },
                                    onLogQuality = { qualityLogDialogBatch = batch }
                                )
                            }
                        }

                        item {
                            Spacer(Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Accounts", style = MaterialTheme.typography.titleMedium)
                                if (canManageProduction) {
                                    TextButton(
                                        onClick = { showRecordPaymentDialog = true },
                                        enabled = !state.actionInProgress
                                    ) { Text("+ Record payment") }
                                }
                            }
                            state.order.account_summary?.let { AccountSummaryCard(it) }
                        }

                        if (state.order.payments.isEmpty()) {
                            item { Text("No payments recorded yet.", modifier = Modifier.padding(vertical = 8.dp)) }
                        } else {
                            items(state.order.payments) { payment ->
                                PaymentRow(
                                    payment = payment,
                                    enabled = !state.actionInProgress,
                                    canAdvance = canManageProduction,
                                    onAdvance = { next -> viewModel.advancePaymentStatus(payment.payment_id, next) }
                                )
                            }
                        }

                        item {
                            state.actionError?.let { message ->
                                Spacer(Modifier.height(8.dp))
                                Text(message, color = MaterialTheme.colorScheme.error)
                            }

                            if (canManageProduction) {
                                Spacer(Modifier.height(16.dp))
                                // Closing is deliberately allowed to fail
                                // server-side (e.g. "2 batch(es) not
                                // completed yet") rather than us trying to
                                // perfectly replicate that rule here — the
                                // server is the one source of truth for it.
                                Button(
                                    onClick = { viewModel.closeOrder() },
                                    enabled = state.order.status == "active" && !state.actionInProgress,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Close order") }
                            }
                        }
                    }

                    if (showAddBatchDialog) {
                        AddBatchDialog(
                            onDismiss = { showAddBatchDialog = false },
                            onConfirm = { qty ->
                                showAddBatchDialog = false
                                viewModel.addBatch(qty)
                            }
                        )
                    }

                    qualityLogDialogBatch?.let { batch ->
                        LogQualityCheckDialog(
                            batch = batch,
                            onDismiss = { qualityLogDialogBatch = null },
                            onConfirm = { qtyChecked, defectCount ->
                                qualityLogDialogBatch = null
                                viewModel.logQualityCheck(batch.batch_id, qtyChecked, defectCount)
                            }
                        )
                    }

                    if (showRecordPaymentDialog) {
                        RecordPaymentDialog(
                            onDismiss = { showRecordPaymentDialog = false },
                            onConfirm = { amount, status ->
                                showRecordPaymentDialog = false
                                viewModel.recordPayment(amount, status)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderSummary(order: Order) {
    Column {
        Text(order.customer_name, style = MaterialTheme.typography.headlineSmall)
        Text(order.job_type ?: "—", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text("Ordered: ${order.order_date}   Due: ${order.due_date ?: "—"}")
        Text("Quantity: ${order.total_qty}   Amount: ${order.total_amount ?: "—"}")
        Spacer(Modifier.height(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                order.status,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchRow(
    batch: Batch,
    enabled: Boolean,
    canAdvance: Boolean,
    canLogQuality: Boolean,
    onAdvance: (String) -> Unit,
    onLogQuality: () -> Unit
) {
    // Forward-only progression, mirroring ALLOWED_TRANSITIONS in the
    // backend's batches.controller.js — "completed" has no next step.
    val nextStatus = when (batch.status) {
        "pending" -> "in_progress"
        "in_progress" -> "completed"
        else -> null
    }
    // Mirrors the backend rule: a batch that hasn't started production
    // (still "pending") has nothing to inspect yet.
    val batchIsInspectable = batch.status != "pending"

    ListItem(
        headlineContent = { Text("Batch #${batch.batch_id} — qty ${batch.quantity}") },
        supportingContent = { Text("Status: ${batch.status}") },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canLogQuality && batchIsInspectable) {
                    TextButton(onClick = onLogQuality, enabled = enabled) { Text("Log QC") }
                }
                if (canAdvance && nextStatus != null) {
                    TextButton(onClick = { onAdvance(nextStatus) }, enabled = enabled) {
                        Text(if (nextStatus == "in_progress") "Start" else "Complete")
                    }
                }
            }
        }
    )
    HorizontalDivider()
}

@Composable
private fun AccountSummaryCard(summary: AccountSummary) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Total: ${formatCurrency(summary.total_amount?.toDoubleOrNull())}")
            Text("Paid: ${formatCurrency(summary.total_paid)}")
            Text(
                "Due: ${formatCurrency(summary.amount_due)}",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentRow(payment: Payment, enabled: Boolean, canAdvance: Boolean, onAdvance: (String) -> Unit) {
    // Forward-only, mirroring ALLOWED_TRANSITIONS in the backend's
    // payments.controller.js — 'paid' has no next step.
    val nextStatus = when (payment.status) {
        "pending" -> "partial"
        "partial" -> "paid"
        else -> null
    }
    ListItem(
        headlineContent = { Text(formatCurrency(payment.amount.toDoubleOrNull())) },
        supportingContent = { Text("${payment.payment_date} · ${payment.status}") },
        trailingContent = {
            if (canAdvance && nextStatus != null) {
                TextButton(onClick = { onAdvance(nextStatus) }, enabled = enabled) {
                    Text(if (nextStatus == "partial") "Mark partial" else "Mark paid")
                }
            }
        }
    )
    HorizontalDivider()
}

@Composable
private fun RecordPaymentDialog(onDismiss: () -> Unit, onConfirm: (amount: Double, status: String?) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var markAsPaid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = markAsPaid, onCheckedChange = { markAsPaid = it })
                    Text("Already received (mark as paid)")
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Leave unchecked to record it as pending, e.g. an expected but not-yet-received payment.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                amountText.toDoubleOrNull()?.let { amount ->
                    onConfirm(amount, if (markAsPaid) "paid" else null)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatCurrency(value: Double?): String =
    if (value == null) "—" else "₹" + String.format(Locale.US, "%,.2f", value)

@Composable
private fun AddBatchDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var qtyText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add batch") },
        text = {
            OutlinedTextField(
                value = qtyText,
                onValueChange = { qtyText = it },
                label = { Text("Quantity") }
            )
        },
        confirmButton = {
            TextButton(onClick = { qtyText.toIntOrNull()?.let(onConfirm) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// No "who is logging this" field — the backend identifies the caller
// from their login token, not from anything typed into this form.
@Composable
private fun LogQualityCheckDialog(
    batch: Batch,
    onDismiss: () -> Unit,
    onConfirm: (qtyChecked: Int, defectCount: Int) -> Unit
) {
    var qtyCheckedText by remember { mutableStateOf("") }
    var defectCountText by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log quality check — Batch #${batch.batch_id}") },
        text = {
            Column {
                OutlinedTextField(
                    value = qtyCheckedText,
                    onValueChange = { qtyCheckedText = it },
                    label = { Text("Quantity checked (max ${batch.quantity})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = defectCountText,
                    onValueChange = { defectCountText = it },
                    label = { Text("Defect count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pass/fail is decided automatically: 0 defects = pass, any defects = fail.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val qtyChecked = qtyCheckedText.toIntOrNull()
                val defectCount = defectCountText.toIntOrNull()
                if (qtyChecked != null && defectCount != null) {
                    onConfirm(qtyChecked, defectCount)
                }
            }) { Text("Submit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
