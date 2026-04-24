package com.piconsole.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piconsole.network.RetrofitClient
import com.piconsole.network.models.MediaRequest
import com.piconsole.network.models.MediaResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaViewModel : ViewModel() {
    private val _mediaState = MutableStateFlow<MediaResponse?>(null)
    val mediaState: StateFlow<MediaResponse?> = _mediaState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun sendMediaAction(action: String, volume: Float? = null) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.controlMedia(MediaRequest(action, volume))
                _mediaState.value = response
            } catch (e: Exception) {
                _error.value = "Failed to send media action: ${e.message}"
            }
        }
    }
}
