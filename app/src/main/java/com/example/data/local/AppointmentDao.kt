package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY appointmentDateMillis ASC, appointmentHour ASC, appointmentMinute ASC")
    fun getAllAppointments(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE status = :status ORDER BY appointmentDateMillis ASC, appointmentHour ASC, appointmentMinute ASC")
    fun getAppointmentsByStatus(status: AppointmentStatus): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    fun getAppointmentById(id: Long): Flow<Appointment?>

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    suspend fun getAppointmentByIdDirect(id: Long): Appointment?

    @Query("SELECT * FROM appointments WHERE isSynced = 0")
    suspend fun getUnsyncedAppointments(): List<Appointment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appointment: Appointment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appointments: List<Appointment>)

    @Update
    suspend fun update(appointment: Appointment)

    @Delete
    suspend fun delete(appointment: Appointment)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE appointments SET isSynced = 1, remoteId = :remoteId, updatedAt = :syncTime WHERE id = :id")
    suspend fun markAsSynced(id: Long, remoteId: String, syncTime: Long)

    @Query("UPDATE appointments SET status = :newStatus, isSynced = 0, updatedAt = :updateTime WHERE id = :id")
    suspend fun updateStatus(id: Long, newStatus: AppointmentStatus, updateTime: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM appointments")
    suspend fun getCount(): Int

    @Query("DELETE FROM appointments")
    suspend fun deleteAll()
}
