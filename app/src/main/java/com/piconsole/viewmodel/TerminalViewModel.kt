package com.piconsole.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.piconsole.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlinx.coroutines.launch

class TerminalViewModel : ViewModel() {
    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun connect() {
        val baseUrl = RetrofitClient.baseUrl ?: return
        val wsUrl = baseUrl.replace("http://", "ws://") + "terminal"

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                // Strip basic ANSI cursor codes (optional) or just append
                // Since this is a raw stream, we'll just append it directly
                _output.value += text
                
                // Keep output length reasonable to prevent OOM
                if (_output.value.length > 20000) {
                    _output.value = _output.value.takeLast(10000)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _output.value += "\n\r[Connection Error: ${t.message}]\n\r"
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _output.value += "\n\r[Connection Closed]\n\r"
            }
        })
    }

    fun sendCommand(cmd: String) {
        webSocket?.send(cmd + "\n")
    }

    fun sendControlCharacter(charStr: String) {
        webSocket?.send(charStr)
    }

    fun clear() {
        _output.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.close(1000, "ViewModel Cleared")
    }
}
