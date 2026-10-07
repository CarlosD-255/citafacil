package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AppointmentStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: AppointmentStatus?): String {
        return status?.name ?: AppointmentStatus.PENDING.name
    }

    @TypeConverter
    fun toStatus(value: String?): AppointmentStatus {
        return try {
            if (value != null) AppointmentStatus.valueOf(value) else AppointmentStatus.PENDING
        } catch (e: Exception) {
            AppointmentStatus.PENDING
        }
    }
}
