package com.piconsole.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piconsole.network.RetrofitClient
import com.piconsole.network.models.StatusResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val _status = MutableStateFlow<StatusResponse?>(null)
    val status: StateFlow<StatusResponse?> = _status.asStateFlow()

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

    fun findPhone() {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.findPhone()
            } catch (e: Exception) {
                _error.value = "Failed to ring phone: ${e.message}"
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

    fun mute() {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.mute()
            } catch (e: Exception) {
                _error.value = "Failed to toggle mute: ${e.message}"
            }
        }
    }
}
