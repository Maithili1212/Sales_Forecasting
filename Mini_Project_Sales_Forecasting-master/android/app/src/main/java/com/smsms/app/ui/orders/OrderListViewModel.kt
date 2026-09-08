package com.smsms.app.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsms.app.model.Order
import com.smsms.app.repository.OrdersRepository
import com.smsms.app.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OrderListUiState {
    data object Loading : OrderListUiState()
    data class Loaded(val orders: List<Order>) : OrderListUiState()
    data class Error(val message: String) : OrderListUiState()
}

// A ViewModel survives Compose recompositions and configuration changes
// (e.g. screen rotation) — the network call in init{} only runs once per
// screen visit, not on every recomposition.
class OrderListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<OrderListUiState>(OrderListUiState.Loading)
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = OrderListUiState.Loading
            when (val result = OrdersRepository.listOrders()) {
                is ApiResult.Success -> _uiState.value = OrderListUiState.Loaded(result.data)
                is ApiResult.Failure -> _uiState.value = OrderListUiState.Error(result.message)
            }
        }
    }
}
