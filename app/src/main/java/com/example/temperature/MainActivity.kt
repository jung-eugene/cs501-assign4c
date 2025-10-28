package com.example.temperature

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.temperature.ui.theme.TemperatureTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*

data class Reading(val timeMillis: Long, val tempF: Float)

data class UiState(
    val readings: List<Reading> = emptyList(),
    val paused: Boolean = false
)

class TemperatureViewModel : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var job: Job? = null

    fun togglePaused() {
        val newPaused = !_state.value.paused
        _state.value = _state.value.copy(paused = newPaused)
        if (newPaused) stop() else start()
    }

    private fun start() {
        job?.cancel()
        job = viewModelScope.launch {
            while (true) {
                delay(2_000L) // simulate sensor every 2s
                val next = Reading(
                    timeMillis = System.currentTimeMillis(),
                    tempF = Random.nextFloat() * (85f - 65f) + 65f
                )
                val nextList = (_state.value.readings + next).takeLast(20) // keep last 20
                _state.value = _state.value.copy(readings = nextList)
            }
        }
    }

    private fun stop() {
        job?.cancel()
        job = null
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemperatureTheme { AppRoot() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: TemperatureViewModel = viewModel()) {
    val ui by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Temperature Dashboard") },
                actions = {
                    Text(
                        if (ui.paused) "Paused" else "Live",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = vm::togglePaused,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text(if (ui.paused) "Resume" else "Pause") }
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { inner ->
        Dashboard(
            readings = ui.readings,
            paused = ui.paused,
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

@Composable
private fun Dashboard(readings: List<Reading>, paused: Boolean, modifier: Modifier = Modifier) {
    val current = readings.lastOrNull()?.tempF
    val avg = readings.takeIf { it.isNotEmpty() }?.map { it.tempF }?.average()
    val min = readings.minByOrNull { it.tempF }?.tempF
    val max = readings.maxByOrNull { it.tempF }?.tempF

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simple line chart for last 20 points
        Card {
            Column(Modifier.padding(12.dp)) {
                Text("Last ${readings.size} readings", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                TemperatureChart(
                    points = readings.map { it.tempF },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color(0xFFF6F8FA))
                )
            }
        }

        // Summary stats
        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Stat("Current", current)
                Stat("Avg", avg?.toFloat())
                Stat("Min", min)
                Stat("Max", max)
            }
        }

        // List of readings
        Card(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize()) {
                Text(
                    "Readings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(12.dp)
                )
                Divider()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(readings) { r ->
                        val time = remember(r.timeMillis) {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(Date(r.timeMillis))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(time)
                            Text("${"%.1f".format(r.tempF)} °F")
                        }
                    }
                }
            }
        }

        // Status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dot = if (paused) Color(0xFFBDBDBD) else Color(0xFF4CAF50)
            Box(
                Modifier
                    .size(10.dp)
                    .background(dot, shape = MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(8.dp))
            Text("Auto: ${if (paused) "OFF" else "ON"}")
        }
    }
}

@Composable
private fun Stat(label: String, value: Float?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Text(
            text = value?.let { "%.1f °F".format(it) } ?: "—",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Minimal line chart: scales values to the canvas and draws a polyline.
 * Expects small lists (we show last 20).
 */
@Composable
private fun TemperatureChart(points: List<Float>, modifier: Modifier = Modifier) {
    val p = if (points.isEmpty()) listOf<Float>() else points
    Canvas(modifier) {
        if (p.isEmpty()) return@Canvas

        val min = p.minOrNull() ?: 0f
        val max = p.maxOrNull() ?: 1f
        val range = (max - min).takeIf { it > 0f } ?: 1f

        val w = size.width
        val h = size.height
        val stepX = w / (p.size - 1).coerceAtLeast(1)

        val toY: (Float) -> Float = { value ->
            // Higher temp → higher y visually: invert because (0,0) is top-left
            val norm = (value - min) / range
            h - norm * h
        }

        val path = Path()
        p.forEachIndexed { i, v ->
            val x = stepX * i
            val y = toY(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // line
        drawPath(path = path, color = Color(0xFF1976D2), alpha = 0.9f)
        // dots
        p.forEachIndexed { i, v ->
            val x = stepX * i
            val y = toY(v)
            drawCircle(color = Color(0xFF1976D2), radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewTemperature() {
    TemperatureTheme {
        // Static sample to see the chart/summary in Preview
        val fake = List(12) { i -> Reading(System.currentTimeMillis() + i * 2000L, 70f + i) }
        Dashboard(readings = fake, paused = false, modifier = Modifier.fillMaxSize().padding(16.dp))
    }
}
