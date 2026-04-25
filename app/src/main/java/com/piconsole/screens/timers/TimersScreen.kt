package com.piconsole.screens.timers

import android.app.Activity
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piconsole.components.TimerCard
import com.piconsole.components.WheelTimePicker
import com.piconsole.components.ErrorBanner
import com.piconsole.viewmodel.ClockViewModel
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ClockHubScreen(viewModel: ClockViewModel) {
    var selectedTabIndex by remember { mutableIntStateOf(1) } // Default to Timers
    val error by viewModel.error.collectAsState()
    val tabs = listOf("Alarms", "Timers")

    Column(modifier = Modifier.fillMaxSize()) {
        error?.let {
            ErrorBanner(
                message = it,
                onDismiss = { viewModel.clearError() },
                onRetry = { viewModel.clearError() }
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
                        ringtone = alarm.ringtone,
                        repeatDays = alarm.repeatDays,
                        onDelete = { viewModel.deleteAlarm(alarm.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddAlarmDialog(
                viewModel = viewModel,
                onDismiss = { showAddDialog = false },
                onAdd = { label, hrs, mins, amPm, ringtone, repeatDays ->
                    viewModel.createAlarm(label, hrs, mins, amPm, ringtone, repeatDays)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AlarmCard(label: String, time: String, isActive: Boolean, ringtone: String, repeatDays: List<String>?, onDelete: () -> Unit) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = time, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                if (ringtone != "default") {
                    Text(
                        text = "🔔 $ringtone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!repeatDays.isNullOrEmpty()) {
                    Text(
                        text = repeatDays.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
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
                        remainingSeconds = timer.remainingSeconds,
                        onCancel = { viewModel.deleteTimer(timer.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddTimerWheelDialog(
                viewModel = viewModel,
                onDismiss = { showAddDialog = false },
                onAdd = { durationSecs, ringtone ->
                    viewModel.createTimer("Timer", durationSecs, ringtone)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun RingtoneSelector(viewModel: ClockViewModel, selectedRingtone: String, onSelect: (String) -> Unit) {
    val ringtones by viewModel.ringtones.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val cursor = context.contentResolver.query(it, null, null, null, null)
                var fileName = "ringtone.mp3"
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) fileName = c.getString(nameIndex)
                    }
                }
                val tempFile = File(context.cacheDir, fileName)
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.uploadRingtone(tempFile)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    Column {
        Text("Ringtone", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))

        val allOptions = listOf("default") + ringtones.map { it.name }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(allOptions) { name ->
                val isSelected = name == selectedRingtone
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(name) },
                    label = {
                        Text(
                            text = if (name == "default") "🔔 Default" else "🎵 ${name.substringBeforeLast(".")}",
                            fontSize = 12.sp
                        )
                    }
                )
            }
            item {
                AssistChip(
                    onClick = { filePicker.launch("audio/*") },
                    label = { Text("+ Upload", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
fun DaySelector(selectedDays: List<String>, onToggle: (List<String>) -> Unit) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column {
        Text("Repeat", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            days.forEach { day ->
                val isSelected = day in selectedDays
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            onToggle(
                                if (isSelected) selectedDays - day
                                else selectedDays + day
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.take(1),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AddAlarmDialog(viewModel: ClockViewModel, onDismiss: () -> Unit, onAdd: (String, Int, Int, String, String, List<String>?) -> Unit) {
    val hrsList = (1..12).map { it.toString() }
    val minsList = (0..59).map { String.format("%02d", it) }
    val apmList = listOf("AM", "PM")

    var selectedHr by remember { mutableIntStateOf(0) }
    var selectedMin by remember { mutableIntStateOf(0) }
    var selectedApm by remember { mutableIntStateOf(0) }
    var selectedRingtone by remember { mutableStateOf("default") }
    var selectedDays by remember { mutableStateOf<List<String>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Alarm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    WheelTimePicker(items = hrsList, onItemSelected = { selectedHr = it })
                    Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 8.dp))
                    WheelTimePicker(items = minsList, onItemSelected = { selectedMin = it })
                    WheelTimePicker(items = apmList, onItemSelected = { selectedApm = it })
                }

                RingtoneSelector(
                    viewModel = viewModel,
                    selectedRingtone = selectedRingtone,
                    onSelect = { selectedRingtone = it }
                )

                DaySelector(
                    selectedDays = selectedDays,
                    onToggle = { selectedDays = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd("Alarm", hrsList[selectedHr].toInt(), selectedMin, apmList[selectedApm], selectedRingtone, selectedDays.ifEmpty { null })
                }
            ) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddTimerWheelDialog(viewModel: ClockViewModel, onDismiss: () -> Unit, onAdd: (Int, String) -> Unit) {
    val hrsList = (0..23).map { String.format("%02d", it) }
    val minsList = (0..59).map { String.format("%02d", it) }
    val secsList = (0..59).map { String.format("%02d", it) }

    var selectedHr by remember { mutableIntStateOf(0) }
    var selectedMin by remember { mutableIntStateOf(0) }
    var selectedSec by remember { mutableIntStateOf(0) }
    var selectedRingtone by remember { mutableStateOf("default") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Timer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

                RingtoneSelector(
                    viewModel = viewModel,
                    selectedRingtone = selectedRingtone,
                    onSelect = { selectedRingtone = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val duration = (selectedHr * 3600) + (selectedMin * 60) + selectedSec
                    onAdd(duration, selectedRingtone)
                }
            ) { Text("Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
