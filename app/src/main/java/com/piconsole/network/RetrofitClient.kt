package com.piconsole.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Using Android emulator default loopback address to connect to host dev machine
    private const val BASE_URL = "http://192.168.1.26:8000"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val webSocketManager: WebSocketManager by lazy {
        WebSocketManager(BASE_URL)
    }
}
