package com.smsms.app.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smsms.app.model.SalesForecast
import com.smsms.app.repository.OrdersRepository
import com.smsms.app.util.ApiResult
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ForecastUiState {
    data object Loading : ForecastUiState()
    data class Loaded(
        val rows: List<SalesForecast>,
        val mae: Double?,
        val rmse: Double?,
        val modelUsed: String?,
        val running: Boolean = false,
        val runError: String? = null
    ) : ForecastUiState()
    data class Error(val message: String) : ForecastUiState()
}

class ForecastViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ForecastUiState>(ForecastUiState.Loading)
    val uiState: StateFlow<ForecastUiState> = _uiState.asStateFlow()

    init {
        loadForecast()
    }

    fun loadForecast() {
        viewModelScope.launch {
            _uiState.value = ForecastUiState.Loading
            when (val result = OrdersRepository.listSalesForecast()) {
                is ApiResult.Success -> _uiState.value = buildLoadedState(result.data)
                is ApiResult.Failure -> _uiState.value = ForecastUiState.Error(result.message)
            }
        }
    }

    // MAE/RMSE are computed here, not read from the backend, because
    // they're fully derivable from the rows we already have (predicted
    // vs actual, wherever actual_value isn't null) — same "don't
    // duplicate a value the client can derive" reasoning used throughout
    // this app.
    private fun buildLoadedState(rows: List<SalesForecast>): ForecastUiState.Loaded {
        val evaluated = rows.filter { it.actual_value != null }
        val errors = evaluated.map { row ->
            row.actual_value!!.toDouble() - row.predicted_value.toDouble()
        }
        val mae = if (errors.isNotEmpty()) errors.map { abs(it) }.average() else null
        val rmse = if (errors.isNotEmpty()) sqrt(errors.map { it * it }.average()) else null

        return ForecastUiState.Loaded(
            rows = rows,
            mae = mae,
            rmse = rmse,
            modelUsed = rows.firstOrNull()?.model_used
        )
    }

    fun runForecast() {
        val current = _uiState.value as? ForecastUiState.Loaded
        _uiState.value = (current ?: ForecastUiState.Loaded(emptyList(), null, null, null))
            .copy(running = true, runError = null)
        viewModelScope.launch {
            when (val result = OrdersRepository.runSalesForecast()) {
                is ApiResult.Success -> loadForecast()
                is ApiResult.Failure -> {
                    val loaded = _uiState.value as? ForecastUiState.Loaded ?: return@launch
                    _uiState.value = loaded.copy(running = false, runError = result.message)
                }
            }
        }
    }
}
