package com.piconsole.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piconsole.network.RetrofitClient
import com.piconsole.network.models.TimerRequest
import com.piconsole.network.models.TimerResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerViewModel : ViewModel() {
    private val _timers = MutableStateFlow<List<TimerResponse>>(emptyList())
    val timers: StateFlow<List<TimerResponse>> = _timers.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun createTimer(label: String, durationSeconds: Int) {
        viewModelScope.launch {
            try {
                val newTimer = RetrofitClient.apiService.createTimer(TimerRequest(label, durationSeconds))
                _timers.value = _timers.value + newTimer
            } catch (e: Exception) {
                _error.value = "Failed to create timer: ${e.message}"
            }
        }
    }

    fun deleteTimer(id: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteTimer(id)
                _timers.value = _timers.value.filter { it.id != id }
            } catch (e: Exception) {
                _error.value = "Failed to delete timer: ${e.message}"
            }
        }
    }
}
