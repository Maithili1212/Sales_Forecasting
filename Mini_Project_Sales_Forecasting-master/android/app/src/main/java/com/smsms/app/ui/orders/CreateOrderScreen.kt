package com.smsms.app.ui.orders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    onOrderCreated: () -> Unit,
    viewModel: CreateOrderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var customerName by remember { mutableStateOf("") }
    var jobType by remember { mutableStateOf("") }
    var orderDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var totalQty by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }

    // Fires once, exactly when uiState becomes Success — navigates back to
    // the order list without re-triggering on every recomposition.
    LaunchedEffect(uiState) {
        if (uiState is CreateOrderUiState.Success) onOrderCreated()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("New Order") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Customer name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = jobType,
                onValueChange = { jobType = it },
                label = { Text("Job type (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = orderDate,
                onValueChange = { orderDate = it },
                label = { Text("Order date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = dueDate,
                onValueChange = { dueDate = it },
                label = { Text("Due date (YYYY-MM-DD, optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = totalQty,
                onValueChange = { totalQty = it },
                label = { Text("Total quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it },
                label = { Text("Rate per unit (optional — auto-calculates total amount)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            if (uiState is CreateOrderUiState.Error) {
                Text(
                    (uiState as CreateOrderUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    viewModel.createOrder(
                        customerName = customerName,
                        jobType = jobType.ifBlank { null },
                        orderDate = orderDate,
                        dueDate = dueDate.ifBlank { null },
                        totalQty = totalQty.toIntOrNull(),
                        rate = rate.toDoubleOrNull()
                    )
                },
                enabled = uiState !is CreateOrderUiState.Submitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is CreateOrderUiState.Submitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Create order")
                }
            }
        }
    }
}
