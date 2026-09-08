package com.smsms.app.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smsms.app.model.Order
import com.smsms.app.repository.OrdersRepository
import com.smsms.app.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OrderDetailUiState {
    data object Loading : OrderDetailUiState()
    data class Loaded(
        val order: Order,
        val actionInProgress: Boolean = false,
        val actionError: String? = null
    ) : OrderDetailUiState()
    data class Error(val message: String) : OrderDetailUiState()
}

class OrderDetailViewModel(private val orderId: Int) : ViewModel() {
    private val _uiState = MutableStateFlow<OrderDetailUiState>(OrderDetailUiState.Loading)
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        loadOrder()
    }

    fun loadOrder() {
        viewModelScope.launch {
            _uiState.value = OrderDetailUiState.Loading
            when (val result = OrdersRepository.getOrder(orderId)) {
                is ApiResult.Success -> _uiState.value = OrderDetailUiState.Loaded(result.data)
                is ApiResult.Failure -> _uiState.value = OrderDetailUiState.Error(result.message)
            }
        }
    }

    // addBatch / advanceBatch / closeOrder all follow the same shape: mark
    // actionInProgress, call the API, then reload the whole order on
    // success so the UI reflects exactly what the server now has — rather
    // than us guessing how to patch the local state ourselves.
    fun addBatch(quantity: Int) {
        val current = _uiState.value as? OrderDetailUiState.Loaded ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(actionInProgress = true, actionError = null)
            when (val result = OrdersRepository.createBatch(orderId, quantity)) {
                is ApiResult.Success -> loadOrder()
                is ApiResult.Failure -> _uiState.value = current.copy(actionInProgress = false, actionError = result.message)
            }
        }
    }

    fun advanceBatch(batchId: Int, nextStatus: String) {
        val current = _uiState.value as? OrderDetailUiState.Loaded ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(actionInProgress = true, actionError = null)
            when (val result = OrdersRepository.updateBatchStatus(batchId, nextStatus)) {
                is ApiResult.Success -> loadOrder()
                is ApiResult.Failure -> _uiState.value = current.copy(actionInProgress = false, actionError = result.message)
            }
        }
    }

    fun closeOrder() {
        val current = _uiState.value as? OrderDetailUiState.Loaded ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(actionInProgress = true, actionError = null)
            when (val result = OrdersRepository.closeOrder(orderId)) {
                is ApiResult.Success -> loadOrder()
                is ApiResult.Failure -> _uiState.value = current.copy(actionInProgress = false, actionError = result.message)
            }
        }
    }

    // No user_id parameter here — the backend reads who's logging the
    // check off the caller's JWT, not anything the client sends.
    fun logQualityCheck(batchId: Int, qtyChecked: Int, defectCount: Int) {
        val current = _uiState.value as? OrderDetailUiState.Loaded ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(actionInProgress = true, actionError = null)
            when (val result = OrdersRepository.createQualityLog(batchId, qtyChecked, defectCount)) {
                is ApiResult.Success -> loadOrder()
                is ApiResult.Failure -> _uiState.value = current.copy(actionInProgress = false, actionError = result.message)
            }
        }
    }

    fun recordPayment(amount: Double, status: String?) {
        val current = _uiState.value as? OrderDetailUiState.Loaded ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(actionInProgress = true, actionError = null)
            when (val result = OrdersRepository.createPayment(orderId, amount, status)) {
                is ApiResult.Success -> loadOrder()
                is ApiResult.Failure -> _uiState.value = current.copy(actionInProgress = false, actionError = result.message)
            }
        }
    }

    fun advancePaymentStatus(paymentId: Int, nextStatus: String) {
        val current = _uiState.value as? OrderDetailUiState.Loaded ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(actionInProgress = true, actionError = null)
            when (val result = OrdersRepository.updatePaymentStatus(paymentId, nextStatus)) {
                is ApiResult.Success -> loadOrder()
                is ApiResult.Failure -> _uiState.value = current.copy(actionInProgress = false, actionError = result.message)
            }
        }
    }
}

// Compose's default viewModel() factory only knows how to build
// no-argument ViewModels. OrderDetailViewModel needs the orderId from
// navigation, so this factory tells it how to construct one.
class OrderDetailViewModelFactory(private val orderId: Int) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OrderDetailViewModel(orderId) as T
    }
}
