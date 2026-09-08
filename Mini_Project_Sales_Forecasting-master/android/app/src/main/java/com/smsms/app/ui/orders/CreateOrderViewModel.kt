package com.smsms.app.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsms.app.model.CreateOrderRequest
import com.smsms.app.repository.OrdersRepository
import com.smsms.app.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CreateOrderUiState {
    data object Idle : CreateOrderUiState()
    data object Submitting : CreateOrderUiState()
    data object Success : CreateOrderUiState()
    data class Error(val message: String) : CreateOrderUiState()
}

class CreateOrderViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<CreateOrderUiState>(CreateOrderUiState.Idle)
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    // Basic client-side checks exist purely for a fast, friendly UI
    // (no round trip needed for an obviously-empty field). The backend
    // re-validates everything anyway — that's the copy of these rules
    // that actually matters, since it's the one no client can bypass.
    fun createOrder(
        customerName: String,
        jobType: String?,
        orderDate: String,
        dueDate: String?,
        totalQty: Int?,
        rate: Double?
    ) {
        if (customerName.isBlank()) {
            _uiState.value = CreateOrderUiState.Error("Customer name is required.")
            return
        }
        if (orderDate.isBlank()) {
            _uiState.value = CreateOrderUiState.Error("Order date is required (YYYY-MM-DD).")
            return
        }
        if (totalQty == null || totalQty <= 0) {
            _uiState.value = CreateOrderUiState.Error("Total quantity must be a positive number.")
            return
        }

        viewModelScope.launch {
            _uiState.value = CreateOrderUiState.Submitting
            val request = CreateOrderRequest(
                customer_name = customerName,
                job_type = jobType,
                order_date = orderDate,
                due_date = dueDate,
                total_qty = totalQty,
                rate = rate
            )
            when (val result = OrdersRepository.createOrder(request)) {
                is ApiResult.Success -> _uiState.value = CreateOrderUiState.Success
                is ApiResult.Failure -> _uiState.value = CreateOrderUiState.Error(result.message)
            }
        }
    }
}
