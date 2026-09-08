package com.smsms.app.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smsms.app.model.SalesForecast
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    onBack: () -> Unit,
    viewModel: ForecastViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Forecast") },
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
                is ForecastUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is ForecastUiState.Error ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadForecast() }) { Text("Retry") }
                    }

                is ForecastUiState.Loaded -> ForecastContent(state, onRunForecast = { viewModel.runForecast() })
            }
        }
    }
}

@Composable
private fun ForecastContent(state: ForecastUiState.Loaded, onRunForecast: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (state.rows.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            ) {
                Text("No forecast yet. Tap \"Run forecast\" below to train the model on the shop's sales history.")
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Model: ${state.modelUsed ?: "—"}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (state.mae != null && state.rmse != null) {
                        "Evaluated on ${state.rows.count { it.actual_value != null }} known months — " +
                            "MAE ${formatCurrency(state.mae)}, RMSE ${formatCurrency(state.rmse)}"
                    } else {
                        "No evaluated months yet."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                ForecastChart(
                    rows = state.rows,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Spacer(Modifier.height(8.dp))
                Legend()
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.rows) { row -> ForecastRow(row) }
            }
        }

        state.runError?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Button(
            onClick = onRunForecast,
            enabled = !state.running,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (state.running) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Run forecast")
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row {
            LegendSwatch(MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text("Actual", style = MaterialTheme.typography.labelSmall)
        }
        Row {
            LegendSwatch(MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(4.dp))
            Text("Predicted", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LegendSwatch(color: Color) {
    Box(
        modifier = Modifier
            .height(10.dp)
            .padding(top = 2.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
            drawLine(color, Offset(0f, size.height / 2), Offset(20.dp.toPx(), size.height / 2), strokeWidth = 3.dp.toPx())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForecastRow(row: SalesForecast) {
    val isFuture = row.actual_value == null
    ListItem(
        headlineContent = { Text(row.forecast_date) },
        supportingContent = {
            Text(
                if (isFuture) {
                    "Predicted ${formatCurrency(row.predicted_value.toDouble())} (upcoming)"
                } else {
                    "Predicted ${formatCurrency(row.predicted_value.toDouble())} · Actual ${formatCurrency(row.actual_value!!.toDouble())}"
                }
            )
        }
    )
    HorizontalDivider()
}

// A hand-rolled line chart on Compose's Canvas rather than a charting
// library — one fewer third-party dependency to version-match against
// the rest of the Gradle setup, and simple enough (two polylines plus a
// handful of dots) that drawing it directly is genuinely less code than
// wiring up a library would be.
@Composable
private fun ForecastChart(rows: List<SalesForecast>, modifier: Modifier = Modifier) {
    if (rows.isEmpty()) return

    val actualColor = MaterialTheme.colorScheme.primary
    val predictedColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val predictedValues = rows.map { it.predicted_value.toDouble() }
    val actualPoints = rows.mapIndexedNotNull { i, r -> r.actual_value?.let { i to it.toDouble() } }
    val allValues = predictedValues + actualPoints.map { it.second }
    val minValue = allValues.min()
    val maxValue = allValues.max()
    val valueRange = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

    Canvas(modifier = modifier) {
        val n = rows.size

        fun xFor(index: Int): Float =
            if (n <= 1) size.width / 2 else size.width * index / (n - 1)

        fun yFor(value: Double): Float =
            size.height - ((value - minValue) / valueRange * size.height).toFloat()

        // Three horizontal gridlines (min / mid / max) — enough to read
        // relative scale without cluttering a small chart with labels.
        listOf(0f, 0.5f, 1f).forEach { frac ->
            val y = size.height * (1 - frac)
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        val predictedPath = Path().apply {
            rows.forEachIndexed { i, r ->
                val x = xFor(i)
                val y = yFor(r.predicted_value.toDouble())
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            predictedPath,
            color = predictedColor,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )
        )

        if (actualPoints.size >= 2) {
            val actualPath = Path().apply {
                actualPoints.forEachIndexed { idx, (i, v) ->
                    val x = xFor(i)
                    val y = yFor(v)
                    if (idx == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(actualPath, color = actualColor, style = Stroke(width = 3.dp.toPx()))
        }

        actualPoints.forEach { (i, v) ->
            drawCircle(actualColor, radius = 4.dp.toPx(), center = Offset(xFor(i), yFor(v)))
        }
        rows.forEachIndexed { i, r ->
            if (r.actual_value == null) {
                drawCircle(predictedColor, radius = 4.dp.toPx(), center = Offset(xFor(i), yFor(r.predicted_value.toDouble())))
            }
        }
    }
}

private fun formatCurrency(value: Double): String =
    "₹" + String.format(Locale.US, "%,.0f", value)
