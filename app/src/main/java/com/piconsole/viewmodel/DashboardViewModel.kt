package com.piconsole.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piconsole.network.RetrofitClient
import com.piconsole.network.models.ProcessInfo
import com.piconsole.network.models.StatusResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val _status = MutableStateFlow<StatusResponse?>(null)
    val status: StateFlow<StatusResponse?> = _status.asStateFlow()

    private val _processes = MutableStateFlow<List<ProcessInfo>>(emptyList())
    val processes: StateFlow<List<ProcessInfo>> = _processes.asStateFlow()

    init {
        RetrofitClient.webSocketManager.connect()
        viewModelScope.launch {
            RetrofitClient.webSocketManager.statusUpdate.collect { update ->
                if (update != null) {
                    _status.value = update
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        RetrofitClient.webSocketManager.disconnect()
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.apiService.getStatus()
                _status.value = response
            } catch (e: Exception) {
                _error.value = "Failed to fetch status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchProcesses() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getProcesses()
                _processes.value = response
            } catch (e: Exception) {
                _error.value = "Failed to fetch processes: ${e.message}"
            }
        }
    }

    fun reboot() {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.reboot()
            } catch (e: Exception) {
                _error.value = "Failed to reboot: ${e.message}"
            }
        }
    }

    fun shutdown() {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.shutdown()
            } catch (e: Exception) {
                _error.value = "Failed to shutdown: ${e.message}"
            }
        }
    }
}
