package com.smsms.app.ui.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smsms.app.auth.AuthManager
import com.smsms.app.model.Order
import com.smsms.app.repository.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    onOrderClick: (Int) -> Unit,
    onCreateOrderClick: () -> Unit,
    onForecastClick: () -> Unit,
    viewModel: OrderListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val session by AuthManager.session.collectAsState()
    val role = session?.user?.role
    val coroutineScope = rememberCoroutineScope()

    // These mirror the server-side role checks in the backend routers —
    // hiding a button a role can't use isn't the actual security (the
    // server enforces that regardless), it's just not offering an action
    // that would only come back as a 403.
    val canCreateOrders = role == "production_head" || role == "plant_head"
    val canSeeForecast = role == "plant_head"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders") },
                actions = {
                    session?.user?.let { user ->
                        Text(
                            "${user.username} (${user.role})",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    if (canSeeForecast) {
                        TextButton(onClick = onForecastClick) { Text("Forecast") }
                    }
                    TextButton(onClick = { coroutineScope.launch { AuthRepository.logout() } }) {
                        Text("Log out")
                    }
                }
            )
        },
        floatingActionButton = {
            if (canCreateOrders) {
                FloatingActionButton(onClick = onCreateOrderClick) {
                    Icon(Icons.Default.Add, contentDescription = "Create order")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is OrderListUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is OrderListUiState.Error -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                ) {
                    Text("Couldn't load orders")
                    Spacer(Modifier.height(8.dp))
                    Text(state.message, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadOrders() }) { Text("Retry") }
                }

                is OrderListUiState.Loaded -> {
                    if (state.orders.isEmpty()) {
                        Text(
                            "No orders yet. Tap + to create one.",
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.orders) { order ->
                                OrderRow(order = order, onClick = { onOrderClick(order.order_id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderRow(order: Order, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(order.customer_name) },
        supportingContent = { Text("${order.job_type ?: "—"} · Qty ${order.total_qty} · ${order.status}") },
        trailingContent = { Text(order.order_date) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}
