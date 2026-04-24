package com.piconsole.screens.timers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piconsole.components.TimerCard
import com.piconsole.components.WheelTimePicker
import com.piconsole.components.ErrorBanner
import com.piconsole.viewmodel.ClockViewModel
import kotlinx.coroutines.delay

@Composable
fun ClockHubScreen(viewModel: ClockViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(1) } // Default to Timers
    val error by viewModel.error.collectAsState()
    val tabs = listOf("Alarms", "Timers", "Stopwatch")

    Column(modifier = Modifier.fillMaxSize()) {
        error?.let {
            ErrorBanner(
                message = it,
                onDismiss = { viewModel.clearError() },
                onRetry = { viewModel.fetchStopwatchState() } // Or some generic refresh
            )
        }
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { 
                        Text(
                            text = title, 
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal 
                        ) 
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> AlarmsContent(viewModel)
                1 -> TimersContent(viewModel)
                2 -> StopwatchContent(viewModel)
            }
        }
    }
}

@Composable
fun AlarmsContent(viewModel: ClockViewModel) {
    val alarms by viewModel.alarms.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    ) { paddingValues ->
        if (alarms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No alarms set", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(alarms) { alarm ->
                    AlarmCard(
                        label = alarm.label,
                        time = alarm.timeFormatted,
                        isActive = alarm.isActive,
                        onDelete = { viewModel.deleteAlarm(alarm.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddAlarmDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { label, hrs, mins, amPm ->
                    viewModel.createAlarm(label, hrs, mins, amPm)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AlarmCard(label: String, time: String, isActive: Boolean, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = time, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Alarm", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TimersContent(viewModel: ClockViewModel) {
    val timers by viewModel.timers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(Icons.Default.Add, contentDescription = "Add Timer")
            }
        }
    ) { paddingValues ->
        if (timers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No active timers", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(timers) { timer ->
                    TimerCard(
                        label = timer.label,
                        remainingSeconds = timer.remainingSeconds, // Ideally this ticks down locally, but keeping static for UI preview
                        onCancel = { viewModel.deleteTimer(timer.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddTimerWheelDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { durationSecs ->
                    viewModel.createTimer("Timer", durationSecs)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun StopwatchContent(viewModel: ClockViewModel) {
    val state by viewModel.stopwatchState.collectAsState()
    var displayMillis by remember { mutableLongStateOf(0L) }

    // High performance UI tick sync with Pi backend state
    LaunchedEffect(state) {
        var localTickStart = System.currentTimeMillis()
        var baseMillis = state.elapsedMilliseconds
        displayMillis = baseMillis

        if (state.isRunning) {
            while (true) {
                val now = System.currentTimeMillis()
                displayMillis = baseMillis + (now - localTickStart)
                delay(30) // smooth 30FPS UI refresh
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val totalSecs = displayMillis / 1000
        val millis = (displayMillis % 1000) / 10
        val seconds = totalSecs % 60
        val minutes = (totalSecs / 60) % 60
        val hours = totalSecs / 3600

        val timeString = if (hours > 0) {
            String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, millis)
        } else {
            String.format("%02d:%02d.%02d", minutes, seconds, millis)
        }

        Text(
            text = timeString,
            fontSize = 64.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(bottom = 64.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { viewModel.sendStopwatchAction("reset") },
                modifier = Modifier.size(80.dp),
                shape = CircleShape
            ) {
                Text("Reset")
            }

            Button(
                onClick = { 
                    if (state.isRunning) viewModel.sendStopwatchAction("stop")
                    else viewModel.sendStopwatchAction("start")
                },
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRunning) MaterialTheme.colorScheme.error else Color(0xFF00C853)
                )
            ) {
                Text(if (state.isRunning) "Stop" else "Start", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun AddAlarmDialog(onDismiss: () -> Unit, onAdd: (String, Int, Int, String) -> Unit) {
    val hrsList = (1..12).map { it.toString() }
    val minsList = (0..59).map { String.format("%02d", it) }
    val apmList = listOf("AM", "PM")

    var selectedHr by remember { mutableIntStateOf(0) }
    var selectedMin by remember { mutableIntStateOf(0) }
    var selectedApm by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Alarm") },
        text = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                WheelTimePicker(items = hrsList, onItemSelected = { selectedHr = it })
                Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 8.dp))
                WheelTimePicker(items = minsList, onItemSelected = { selectedMin = it })
                WheelTimePicker(items = apmList, onItemSelected = { selectedApm = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd("Alarm", hrsList[selectedHr].toInt(), selectedMin, apmList[selectedApm])
                }
            ) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddTimerWheelDialog(onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    val hrsList = (0..23).map { String.format("%02d", it) }
    val minsList = (0..59).map { String.format("%02d", it) }
    val secsList = (0..59).map { String.format("%02d", it) }

    var selectedHr by remember { mutableIntStateOf(0) }
    var selectedMin by remember { mutableIntStateOf(0) }
    var selectedSec by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Timer") },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Hours", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("Mins", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("Secs", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelTimePicker(items = hrsList, onItemSelected = { selectedHr = it })
                    WheelTimePicker(items = minsList, onItemSelected = { selectedMin = it })
                    WheelTimePicker(items = secsList, onItemSelected = { selectedSec = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val duration = (selectedHr * 3600) + (selectedMin * 60) + selectedSec
                    onAdd(duration)
                }
            ) { Text("Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
