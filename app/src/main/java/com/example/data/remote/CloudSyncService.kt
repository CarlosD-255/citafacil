package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.AppointmentDao
import com.example.data.model.Appointment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

data class SyncState(
    val isSyncing: Boolean = false,
    val isOnline: Boolean = true,
    val unsyncedCount: Int = 0,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncMessage: String = ""
)

class CloudSyncService(
    private val context: Context,
    private val appointmentDao: AppointmentDao
) {
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        updateNetworkStatus()
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun updateNetworkStatus() {
        val online = isNetworkAvailable()
        _syncState.value = _syncState.value.copy(isOnline = online)
    }

    suspend fun performCloudSync(): Result<Int> = withContext(Dispatchers.IO) {
        val online = isNetworkAvailable()
        if (!online) {
            _syncState.value = _syncState.value.copy(
                isOnline = false,
                lastSyncMessage = "Sin conexión a internet (Modo Offline Room)"
            )
            return@withContext Result.failure(Exception("Sin conexión a internet"))
        }

        _syncState.value = _syncState.value.copy(
            isSyncing = true,
            isOnline = true,
            lastSyncMessage = "Sincronizando con Web API..."
        )

        try {
            // Retrieve appointments that need synchronization
            val unsynced = appointmentDao.getUnsyncedAppointments()

            // Simulate cloud HTTP latency (posting to Web API)
            delay(1000)

            var syncedCount = 0
            val syncTime = System.currentTimeMillis()

            for (appt in unsynced) {
                val cloudId = if (appt.remoteId.isNotBlank()) appt.remoteId else "CLOUD-${UUID.randomUUID().toString().take(8).uppercase()}"
                appointmentDao.markAsSynced(
                    id = appt.id,
                    remoteId = cloudId,
                    syncTime = syncTime
                )
                syncedCount++
            }

            _syncState.value = SyncState(
                isSyncing = false,
                isOnline = true,
                unsyncedCount = 0,
                lastSyncTimestamp = syncTime,
                lastSyncMessage = if (syncedCount > 0) "$syncedCount citas sincronizadas en la nube" else "Base de datos al día con el servidor"
            )

            Result.success(syncedCount)
        } catch (e: Exception) {
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastSyncMessage = "Error de sincronización: ${e.message}"
            )
            Result.failure(e)
        }
    }
}
