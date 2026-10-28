package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AddonOption
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.BusinessCatalog
import com.example.data.model.ServiceOption
import com.example.data.remote.CloudSyncService
import com.example.data.remote.NotificationResult
import com.example.data.remote.RetrofitClient
import com.example.data.remote.SyncState
import com.example.data.remote.WebNotificationService
import com.example.data.repository.AppointmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class AppointmentStats(
    val todayCount: Int = 0,
    val pendingCount: Int = 0,
    val estimatedIncome: Double = 0.0,
    val completedCount: Int = 0
)

data class AppointmentFormState(
    val editingId: Long? = null,
    val clientName: String = "",
    val clientPhone: String = "",
    val clientEmail: String = "",
    val selectedService: ServiceOption = BusinessCatalog.defaultServices.first(),
    val selectedAddonIds: Set<String> = emptySet(),
    val dateMillis: Long = System.currentTimeMillis(),
    val hour: Int = 10,
    val minute: Int = 0,
    val notes: String = "",
    val nameError: String? = null,
    val phoneError: String? = null,
    val isSaving: Boolean = false
) {
    val totalAddonsPrice: Double
        get() = BusinessCatalog.defaultAddons
            .filter { selectedAddonIds.contains(it.id) }
            .sumOf { it.price }

    val totalDurationMinutes: Int
        get() = selectedService.durationMinutes + BusinessCatalog.defaultAddons
            .filter { selectedAddonIds.contains(it.id) }
            .sumOf { it.extraMinutes }

    val totalPrice: Double
        get() = selectedService.basePrice + totalAddonsPrice
}

class AppointmentViewModel(
    application: Application,
    private val repository: AppointmentRepository,
    private val notificationService: WebNotificationService
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<AppointmentStatus?>(null)
    val statusFilter: StateFlow<AppointmentStatus?> = _statusFilter.asStateFlow()

    private val _selectedAppointmentId = MutableStateFlow<Long?>(null)
    val selectedAppointmentId: StateFlow<Long?> = _selectedAppointmentId.asStateFlow()

    private val _formState = MutableStateFlow(AppointmentFormState())
    val formState: StateFlow<AppointmentFormState> = _formState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val syncState: StateFlow<SyncState> = repository.syncState

    val allAppointments: StateFlow<List<Appointment>> = repository.allAppointments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredAppointments: StateFlow<List<Appointment>> = combine(
        allAppointments,
        _searchQuery,
        _statusFilter
    ) { list, query, filter ->
        list.filter { appt ->
            val matchesFilter = (filter == null || appt.status == filter)
            val matchesQuery = if (query.isBlank()) true else {
                appt.clientName.contains(query, ignoreCase = true) ||
                appt.clientPhone.contains(query, ignoreCase = true) ||
                appt.serviceName.contains(query, ignoreCase = true) ||
                appt.notes.contains(query, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<AppointmentStats> = allAppointments.combine(_searchQuery) { list, _ ->
        val today = list.count { it.isToday() }
        val pending = list.count { it.status == AppointmentStatus.PENDING }
        val income = list.filter { it.status != AppointmentStatus.CANCELLED }.sumOf { it.totalPrice }
        val completed = list.count { it.status == AppointmentStatus.COMPLETED }
        AppointmentStats(
            todayCount = today,
            pendingCount = pending,
            estimatedIncome = income,
            completedCount = completed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppointmentStats())

    init {
    }

    fun clearAllAppointments() {
        viewModelScope.launch {
            repository.clearAllAppointments()
            _toastMessage.value = "Todas las citas han sido eliminadas"
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterChange(filter: AppointmentStatus?) {
        _statusFilter.value = filter
    }

    fun selectAppointment(id: Long?) {
        _selectedAppointmentId.value = id
    }

    fun updateStatus(id: Long, newStatus: AppointmentStatus) {
        viewModelScope.launch {
            repository.updateStatus(id, newStatus)
            _toastMessage.value = "Estado actualizado a ${newStatus.displayNameEs}"
        }
    }

    fun deleteAppointment(appointment: Appointment) {
        viewModelScope.launch {
            repository.delete(appointment)
            if (_selectedAppointmentId.value == appointment.id) {
                _selectedAppointmentId.value = null
            }
            _toastMessage.value = "Cita eliminada"
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val result = repository.syncNow()
            result.onSuccess { count ->
                _toastMessage.value = "Sincronización exitosa: $count citas actualizadas"
            }.onFailure { err ->
                _toastMessage.value = "Error al sincronizar: ${err.message}"
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.seedSampleAppointments()
            _toastMessage.value = "Citas de prueba añadidas"
        }
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun initNewForm() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
        }
        _formState.value = AppointmentFormState(
            editingId = null,
            clientName = "",
            clientPhone = "",
            clientEmail = "",
            selectedService = BusinessCatalog.defaultServices.first(),
            selectedAddonIds = emptySet(),
            dateMillis = cal.timeInMillis,
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = 0,
            notes = ""
        )
    }

    fun initEditForm(appointment: Appointment) {
        val service = BusinessCatalog.defaultServices.find { it.id == appointment.serviceId }
            ?: BusinessCatalog.defaultServices.first()
        val addonIds = appointment.getAddonIdsList().toSet()

        _formState.value = AppointmentFormState(
            editingId = appointment.id,
            clientName = appointment.clientName,
            clientPhone = appointment.clientPhone,
            clientEmail = appointment.clientEmail,
            selectedService = service,
            selectedAddonIds = addonIds,
            dateMillis = appointment.appointmentDateMillis,
            hour = appointment.appointmentHour,
            minute = appointment.appointmentMinute,
            notes = appointment.notes
        )
    }

    fun updateFormClientName(name: String) {
        _formState.value = _formState.value.copy(clientName = name, nameError = null)
    }

    fun updateFormClientPhone(phone: String) {
        _formState.value = _formState.value.copy(clientPhone = phone, phoneError = null)
    }

    fun updateFormClientEmail(email: String) {
        _formState.value = _formState.value.copy(clientEmail = email)
    }

    fun updateFormService(service: ServiceOption) {
        _formState.value = _formState.value.copy(selectedService = service)
    }

    fun toggleFormAddon(addonId: String) {
        val current = _formState.value.selectedAddonIds.toMutableSet()
        if (current.contains(addonId)) {
            current.remove(addonId)
        } else {
            current.add(addonId)
        }
        _formState.value = _formState.value.copy(selectedAddonIds = current)
    }

    fun updateFormDate(dateMillis: Long) {
        _formState.value = _formState.value.copy(dateMillis = dateMillis)
    }

    fun updateFormTime(hour: Int, minute: Int) {
        _formState.value = _formState.value.copy(hour = hour, minute = minute)
    }

    fun updateFormNotes(notes: String) {
        _formState.value = _formState.value.copy(notes = notes)
    }

    fun saveAppointment(onSuccess: (Long) -> Unit) {
        val state = _formState.value
        var hasError = false
        var nameErr: String? = null
        var phoneErr: String? = null

        if (state.clientName.trim().isEmpty()) {
            nameErr = "El nombre es obligatorio"
            hasError = true
        }

        if (state.clientPhone.trim().isEmpty() || state.clientPhone.filter { it.isDigit() }.length < 7) {
            phoneErr = "Ingrese un número telefónico válido (mín. 7 dígitos)"
            hasError = true
        }

        if (hasError) {
            _formState.value = state.copy(nameError = nameErr, phoneError = phoneErr)
            return
        }

        val selectedAddons = BusinessCatalog.defaultAddons.filter { state.selectedAddonIds.contains(it.id) }
        val addonIdsString = selectedAddons.joinToString(",") { it.id }
        val addonNamesString = selectedAddons.joinToString(", ") { it.nameEs }

        viewModelScope.launch {
            _formState.value = state.copy(isSaving = true)

            val isEditing = state.editingId != null
            val appointment = Appointment(
                id = state.editingId ?: 0L,
                clientName = state.clientName.trim(),
                clientPhone = state.clientPhone.trim(),
                clientEmail = state.clientEmail.trim(),
                serviceId = state.selectedService.id,
                serviceName = state.selectedService.nameEs,
                servicePrice = state.selectedService.basePrice,
                durationMinutes = state.totalDurationMinutes,
                selectedAddonIds = addonIdsString,
                selectedAddonNames = addonNamesString,
                addonsPrice = state.totalAddonsPrice,
                totalPrice = state.totalPrice,
                appointmentDateMillis = state.dateMillis,
                appointmentHour = state.hour,
                appointmentMinute = state.minute,
                notes = state.notes.trim(),
                status = if (isEditing) {
                    val existing = repository.getAppointmentByIdDirect(state.editingId!!)
                    existing?.status ?: AppointmentStatus.PENDING
                } else AppointmentStatus.PENDING,
                isSynced = false
            )

            val finalId: Long = if (isEditing) {
                repository.update(appointment)
                state.editingId!!
            } else {
                repository.insert(appointment)
            }

            _formState.value = state.copy(isSaving = false)
            _toastMessage.value = if (isEditing) "Cita actualizada correctamente" else "Cita agendada con éxito"
            onSuccess(finalId)
        }
    }

    fun sendWhatsApp(appointment: Appointment, businessName: String = "CitaFácil") {
        notificationService.sendWhatsApp(appointment, businessName)
    }

    fun sendSms(appointment: Appointment, businessName: String = "CitaFácil") {
        notificationService.sendSms(appointment, businessName)
    }

    fun sendEmail(appointment: Appointment, businessName: String = "CitaFাইল") {
        notificationService.sendEmail(appointment, businessName)
    }

    fun testApiWebhook(appointment: Appointment, serviceType: String, onResult: (NotificationResult) -> Unit) {
        viewModelScope.launch {
            val result = notificationService.dispatchThirdPartyWebhook(appointment, serviceType)
            _toastMessage.value = "${result.channel}: ${result.message}"
            onResult(result)
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val syncService = CloudSyncService(application, db.appointmentDao(), RetrofitClient.apiService)
                    val repository = AppointmentRepository(db.appointmentDao(), syncService)
                    val notificationService = WebNotificationService(application)
                    return AppointmentViewModel(application, repository, notificationService) as T
                }
            }
    }
}
