package com.piconsole.network

import com.piconsole.network.models.*
import okhttp3.MultipartBody
import retrofit2.http.*

data class DeviceRegistrationRequest(
    val token: String,
    val deviceName: String
)

interface ApiService {
    @GET("status")
    suspend fun getStatus(): StatusResponse

    @POST("timer")
    suspend fun createTimer(@Body request: TimerRequest): TimerResponse

    @DELETE("timer/{id}")
    suspend fun deleteTimer(@Path("id") id: String)

    @POST("alarm")
    suspend fun createAlarm(@Body request: AlarmRequest): AlarmResponse

    @DELETE("alarm/{id}")
    suspend fun deleteAlarm(@Path("id") id: String)

    @POST("alarm/stop")
    suspend fun stopAlarm()

    @POST("snooze/{alarmId}")
    suspend fun snoozeAlarm(@Path("alarmId") alarmId: String, @Query("minutes") minutes: Int = 5)

    @GET("ringtones")
    suspend fun getRingtones(): RingtoneListResponse

    @Multipart
    @POST("ringtones/upload")
    suspend fun uploadRingtone(@Part file: MultipartBody.Part)

    @DELETE("ringtones/{filename}")
    suspend fun deleteRingtone(@Path("filename") filename: String)

    @POST("media")
    suspend fun controlMedia(@Body request: MediaRequest): MediaResponse

    @POST("find-phone")
    suspend fun findPhone(): FindPhoneResponse

    @POST("reboot")
    suspend fun reboot(): StatusResponse

    @POST("shutdown")
    suspend fun shutdown(): StatusResponse

    @POST("mute")
    suspend fun mute(): StatusResponse

    @GET("jarvis/status")
    suspend fun getJarvisStatus(): JarvisStatusResponse

    @POST("jarvis/toggle")
    suspend fun toggleJarvis(): JarvisToggleResponse

    @POST("jarvis/command")
    suspend fun sendJarvisCommand(@Body request: JarvisCommandRequest): JarvisCommandResponse

    @POST("register-device")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): StatusResponse
}
