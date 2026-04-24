package com.piconsole.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piconsole.network.RetrofitClient
import com.piconsole.network.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClockViewModel : ViewModel() {
    private val _timers = MutableStateFlow<List<TimerResponse>>(emptyList())
    val timers: StateFlow<List<TimerResponse>> = _timers.asStateFlow()

    private val _alarms = MutableStateFlow<List<AlarmResponse>>(emptyList())
    val alarms: StateFlow<List<AlarmResponse>> = _alarms.asStateFlow()

    private val _stopwatchState = MutableStateFlow(StopwatchState(false, 0))
    val stopwatchState: StateFlow<StopwatchState> = _stopwatchState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    init {
        fetchStopwatchState()
    }

    // --- Timers ---
    fun createTimer(label: String, durationSeconds: Int) {
        viewModelScope.launch {
            try {
                val newTimer = RetrofitClient.apiService.createTimer(TimerRequest(label, durationSeconds))
                _timers.value = _timers.value + newTimer
            } catch (e: Exception) {
                _error.value = "Failed to create timer: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTimer(id: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteTimer(id)
                _timers.value = _timers.value.filter { it.id != id }
            } catch (e: Exception) {
                _error.value = "Failed to delete timer: ${e.localizedMessage}"
            }
        }
    }

    // --- Alarms ---
    fun createAlarm(label: String, hours: Int, minutes: Int, amPm: String) {
        val formattedTime = String.format("%02d:%02d %s", hours, minutes, amPm)
        viewModelScope.launch {
            try {
                val newAlarm = RetrofitClient.apiService.createAlarm(AlarmRequest(label, formattedTime))
                _alarms.value = _alarms.value + newAlarm
            } catch (e: Exception) {
                _error.value = "Failed to create alarm: ${e.localizedMessage}"
            }
        }
    }

    fun deleteAlarm(id: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteAlarm(id)
                _alarms.value = _alarms.value.filter { it.id != id }
            } catch (e: Exception) {
                _error.value = "Failed to delete alarm: ${e.localizedMessage}"
            }
        }
    }

    // --- Stopwatch ---
    fun fetchStopwatchState() {
        viewModelScope.launch {
            try {
                val state = RetrofitClient.apiService.getStopwatchState()
                _stopwatchState.value = state
            } catch (e: Exception) {
                _error.value = "Failed to fetch stopwatch: ${e.localizedMessage}"
            }
        }
    }

    fun sendStopwatchAction(action: String) {
        viewModelScope.launch {
            try {
                val state = RetrofitClient.apiService.controlStopwatch(StopwatchAction(action))
                _stopwatchState.value = state
            } catch (e: Exception) {
                _error.value = "Stopwatch action failed: ${e.localizedMessage}"
            }
        }
    }
}
