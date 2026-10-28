package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AppointmentApiService {
    @GET("appointments")
    suspend fun getAppointments(): List<AppointmentDto>

    @POST("appointments")
    suspend fun createAppointment(@Body appointment: AppointmentDto): AppointmentDto

    @PUT("appointments/{id}")
    suspend fun updateAppointment(@Path("id") id: String, @Body appointment: AppointmentDto): AppointmentDto

    @DELETE("appointments/{id}")
    suspend fun deleteAppointment(@Path("id") id: String)
}
