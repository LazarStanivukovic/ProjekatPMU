package com.example.projekat.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ScheduleApi {

    @POST("/api/schedule")
    suspend fun getSchedule(@Body request: ScheduleRequestDto): ScheduleResponseDto

    @GET("/api/health")
    suspend fun healthCheck(): Map<String, String>
}
