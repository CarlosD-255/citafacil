package com.example.data.repository

import com.example.data.local.AppointmentDao
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.BusinessCatalog
import com.example.data.remote.CloudSyncService
import com.example.data.remote.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class AppointmentRepository(
    private val appointmentDao: AppointmentDao,
    private val cloudSyncService: CloudSyncService,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val allAppointments: Flow<List<Appointment>> = appointmentDao.getAllAppointments()
    val syncState: StateFlow<SyncState> = cloudSyncService.syncState

    fun getAppointmentsByStatus(status: AppointmentStatus): Flow<List<Appointment>> {
        return appointmentDao.getAppointmentsByStatus(status)
    }

    fun getAppointmentById(id: Long): Flow<Appointment?> {
        return appointmentDao.getAppointmentById(id)
    }

    suspend fun getAppointmentByIdDirect(id: Long): Appointment? {
        return appointmentDao.getAppointmentByIdDirect(id)
    }

    suspend fun insert(appointment: Appointment): Long {
        val insertedId = appointmentDao.insert(appointment.copy(isSynced = false))
        triggerBackgroundSync()
        return insertedId
    }

    suspend fun update(appointment: Appointment) {
        appointmentDao.update(appointment.copy(isSynced = false, updatedAt = System.currentTimeMillis()))
        triggerBackgroundSync()
    }

    suspend fun updateStatus(id: Long, newStatus: AppointmentStatus) {
        appointmentDao.updateStatus(id, newStatus)
        triggerBackgroundSync()
    }

    suspend fun delete(appointment: Appointment) {
        appointmentDao.delete(appointment)
    }

    suspend fun deleteById(id: Long) {
        appointmentDao.deleteById(id)
    }

    suspend fun syncNow(): Result<Int> {
        return cloudSyncService.performCloudSync()
    }

    private fun triggerBackgroundSync() {
        externalScope.launch {
            if (cloudSyncService.isNetworkAvailable()) {
                cloudSyncService.performCloudSync()
            }
        }
    }

    suspend fun clearAllAppointments() {
        appointmentDao.deleteAll()
    }

    suspend fun seedSampleAppointmentsIfEmpty() {
        val count = appointmentDao.getCount()
        if (count == 0) {
            seedSampleAppointments()
        }
    }

    suspend fun seedSampleAppointments() {
        val calendar = Calendar.getInstance()
        val todayMillis = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val tomorrowMillis = todayMillis + (24 * 60 * 60 * 1000)
        val inTwoDaysMillis = todayMillis + (2 * 24 * 60 * 60 * 1000)

        val samples = listOf(
            Appointment(
                clientName = "Carlos Mendoza",
                clientPhone = "5541982341",
                clientEmail = "carlos.mendoza@gmail.com",
                serviceId = "combo_corte_barba",
                serviceName = "Combo Corte + Barba",
                servicePrice = 280.0,
                durationMinutes = 65,
                selectedAddonIds = "lavado_masaje",
                selectedAddonNames = "Lavado con Masaje Capilar",
                addonsPrice = 50.0,
                totalPrice = 330.0,
                appointmentDateMillis = todayMillis,
                appointmentHour = 10,
                appointmentMinute = 30,
                status = AppointmentStatus.PENDING,
                notes = "Fade medio con diseño de líneas laterales. Cliente habitual.",
                isSynced = true,
                remoteId = "CLOUD-9812A"
            ),
            Appointment(
                clientName = "Alan Rodrigo Silva",
                clientPhone = "5587654321",
                clientEmail = "alan.silva@outlook.com",
                serviceId = "barba_perfilado",
                serviceName = "Barba & Perfilado",
                servicePrice = 140.0,
                durationMinutes = 40,
                selectedAddonIds = "mascarilla_carbon",
                selectedAddonNames = "Mascarilla Negra Anti-impurezas",
                addonsPrice = 60.0,
                totalPrice = 200.0,
                appointmentDateMillis = todayMillis,
                appointmentHour = 12,
                appointmentMinute = 0,
                status = AppointmentStatus.IN_PROGRESS,
                notes = "Perfilado cuadrado de barba. Aplicar toalla fría al terminar.",
                isSynced = true,
                remoteId = "CLOUD-4451B"
            ),
            Appointment(
                clientName = "Valeria Ramos",
                clientPhone = "5512349988",
                clientEmail = "valeria.ramos@gmail.com",
                serviceId = "limpieza_facial",
                serviceName = "Limpieza Facial Profunda",
                servicePrice = 320.0,
                durationMinutes = 55,
                selectedAddonIds = "exfoliacion_express,bebida_cortesia",
                selectedAddonNames = "Exfoliación Facial Express, Bebida Especial de Cortesía",
                addonsPrice = 100.0,
                totalPrice = 420.0,
                appointmentDateMillis = todayMillis,
                appointmentHour = 16,
                appointmentMinute = 15,
                status = AppointmentStatus.PENDING,
                notes = "Piel sensible, usar producto hipoalergénico.",
                isSynced = false,
                remoteId = ""
            ),
            Appointment(
                clientName = "Miguel Ángel Torres",
                clientPhone = "5576123490",
                clientEmail = "miguel.torres@empresa.com",
                serviceId = "corte_clasico",
                serviceName = "Corte Clásico",
                servicePrice = 180.0,
                durationMinutes = 30,
                selectedAddonIds = "",
                selectedAddonNames = "",
                addonsPrice = 0.0,
                totalPrice = 180.0,
                appointmentDateMillis = todayMillis,
                appointmentHour = 9,
                appointmentMinute = 0,
                status = AppointmentStatus.COMPLETED,
                notes = "Corte ejecutivo clásico número 3 a los lados.",
                isSynced = true,
                remoteId = "CLOUD-2210C"
            ),
            Appointment(
                clientName = "Daniel García Nieto",
                clientPhone = "5567891234",
                clientEmail = "daniel.garcia@estudio.mx",
                serviceId = "diseno_tatuaje",
                serviceName = "Diseño / Tatuaje Express",
                servicePrice = 500.0,
                durationMinutes = 60,
                selectedAddonIds = "",
                selectedAddonNames = "",
                addonsPrice = 0.0,
                totalPrice = 500.0,
                appointmentDateMillis = tomorrowMillis,
                appointmentHour = 15,
                appointmentMinute = 0,
                status = AppointmentStatus.PENDING,
                notes = "Diseño minimalista de rosa en antebrazo (5x5 cm).",
                isSynced = false,
                remoteId = ""
            ),
            Appointment(
                clientName = "Esteban Morales",
                clientPhone = "5599887766",
                clientEmail = "esteban.morales@gmail.com",
                serviceId = "tinte_color",
                serviceName = "Tinte & Colorimetría",
                servicePrice = 450.0,
                durationMinutes = 85,
                selectedAddonIds = "lavado_masaje",
                selectedAddonNames = "Lavado con Masaje Capilar",
                addonsPrice = 50.0,
                totalPrice = 500.0,
                appointmentDateMillis = inTwoDaysMillis,
                appointmentHour = 11,
                appointmentMinute = 0,
                status = AppointmentStatus.PENDING,
                notes = "Matizado platinado en puntas.",
                isSynced = true,
                remoteId = "CLOUD-8877D"
            )
        )

        appointmentDao.insertAll(samples)
    }
}
