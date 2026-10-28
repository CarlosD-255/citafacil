package com.example.data.remote

import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppointmentDto(
    val id: String? = null,
    val clientName: String,
    val clientPhone: String,
    val clientEmail: String,
    val serviceId: String,
    val serviceName: String,
    val servicePrice: Double,
    val durationMinutes: Int,
    val selectedAddonIds: String,
    val selectedAddonNames: String,
    val addonsPrice: Double,
    val totalPrice: Double,
    val appointmentDateMillis: Long,
    val appointmentHour: Int,
    val appointmentMinute: Int,
    val status: String,
    val notes: String,
    val remoteId: String = ""
)

fun Appointment.toDto(): AppointmentDto {
    return AppointmentDto(
        id = if (remoteId.isBlank()) null else remoteId,
        clientName = clientName,
        clientPhone = clientPhone,
        clientEmail = clientEmail,
        serviceId = serviceId,
        serviceName = serviceName,
        servicePrice = servicePrice,
        durationMinutes = durationMinutes,
        selectedAddonIds = selectedAddonIds,
        selectedAddonNames = selectedAddonNames,
        addonsPrice = addonsPrice,
        totalPrice = totalPrice,
        appointmentDateMillis = appointmentDateMillis,
        appointmentHour = appointmentHour,
        appointmentMinute = appointmentMinute,
        status = status.name,
        notes = notes,
        remoteId = remoteId
    )
}

fun AppointmentDto.toEntity(localId: Long = 0L): Appointment {
    val parsedStatus = try {
        AppointmentStatus.valueOf(status)
    } catch (e: Exception) {
        AppointmentStatus.PENDING
    }
    return Appointment(
        id = localId,
        clientName = clientName ?: "",
        clientPhone = clientPhone ?: "",
        clientEmail = clientEmail ?: "",
        serviceId = serviceId ?: "",
        serviceName = serviceName ?: "",
        servicePrice = servicePrice ?: 0.0,
        durationMinutes = durationMinutes ?: 30,
        selectedAddonIds = selectedAddonIds ?: "",
        selectedAddonNames = selectedAddonNames ?: "",
        addonsPrice = addonsPrice ?: 0.0,
        totalPrice = totalPrice ?: 0.0,
        appointmentDateMillis = appointmentDateMillis ?: System.currentTimeMillis(),
        appointmentHour = appointmentHour ?: 10,
        appointmentMinute = appointmentMinute ?: 0,
        status = parsedStatus,
        notes = notes ?: "",
        isSynced = true,
        remoteId = id ?: remoteId
    )
}
