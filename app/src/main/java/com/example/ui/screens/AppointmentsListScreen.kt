package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.ui.components.AppointmentCard
import com.example.ui.components.EmptyAppointmentsView
import com.example.ui.components.StatsSummarySection
import com.example.ui.components.ThirdPartyNotificationDialog
import com.example.ui.viewmodel.AppointmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsListScreen(
    viewModel: AppointmentViewModel,
    onAppointmentClick: (Long) -> Unit,
    onNewAppointmentClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appointments by viewModel.filteredAppointments.collectAsStateWithLifecycle()
    val allAppointments by viewModel.allAppointments.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    var appointmentForNotification by remember { mutableStateOf<Appointment?>(null) }

    // Cloud sync icon rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Scaffold(
        modifier = modifier.testTag("appointments_list_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Sync status button
                    IconButton(
                        onClick = { viewModel.syncNow() },
                        modifier = Modifier.testTag("topbar_sync_btn")
                    ) {
                        if (syncState.isSyncing) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sincronizando...",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.rotate(spinAngle)
                            )
                        } else {
                            Icon(
                                imageVector = if (syncState.unsyncedCount == 0) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = "Sincronizar",
                                tint = if (syncState.unsyncedCount == 0) Color(0xFF059669) else Color(0xFFD97706)
                            )
                        }
                    }

                    // Settings button
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("topbar_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewAppointmentClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.nav_new_appointment), fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_new_appointment")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats summary row
            StatsSummarySection(stats = stats)

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = {
                    Text(
                        stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchQueryChange("") },
                            modifier = Modifier.testTag("clear_search_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("search_input")
            )

            // Status filter chips (RF04)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { viewModel.onStatusFilterChange(null) },
                    label = { Text(stringResource(R.string.filter_all)) },
                    modifier = Modifier.testTag("filter_all_chip")
                )
                FilterChip(
                    selected = statusFilter == AppointmentStatus.PENDING,
                    onClick = { viewModel.onStatusFilterChange(AppointmentStatus.PENDING) },
                    label = { Text(stringResource(R.string.status_pending)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFEF3C7),
                        selectedLabelColor = Color(0xFFB45309)
                    ),
                    modifier = Modifier.testTag("filter_pending_chip")
                )
                FilterChip(
                    selected = statusFilter == AppointmentStatus.IN_PROGRESS,
                    onClick = { viewModel.onStatusFilterChange(AppointmentStatus.IN_PROGRESS) },
                    label = { Text(stringResource(R.string.status_in_progress)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE0E7FF),
                        selectedLabelColor = Color(0xFF4338CA)
                    ),
                    modifier = Modifier.testTag("filter_in_progress_chip")
                )
                FilterChip(
                    selected = statusFilter == AppointmentStatus.COMPLETED,
                    onClick = { viewModel.onStatusFilterChange(AppointmentStatus.COMPLETED) },
                    label = { Text(stringResource(R.string.status_completed)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFDCFCE7),
                        selectedLabelColor = Color(0xFF15803D)
                    ),
                    modifier = Modifier.testTag("filter_completed_chip")
                )
                FilterChip(
                    selected = statusFilter == AppointmentStatus.CANCELLED,
                    onClick = { viewModel.onStatusFilterChange(AppointmentStatus.CANCELLED) },
                    label = { Text(stringResource(R.string.status_cancelled)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFE4E6),
                        selectedLabelColor = Color(0xFFBE123C)
                    ),
                    modifier = Modifier.testTag("filter_cancelled_chip")
                )
            }

            // Appointments List (RF04 LazyColumn)
            if (appointments.isEmpty()) {
                if (allAppointments.isEmpty()) {
                    EmptyAppointmentsView(
                        onNewAppointmentClick = onNewAppointmentClick,
                        onLoadSampleDataClick = { viewModel.loadSampleData() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron citas con los filtros aplicados",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("appointments_lazy_column"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = appointments, key = { it.id }) { appointment ->
                        AppointmentCard(
                            appointment = appointment,
                            onClick = { onAppointmentClick(appointment.id) },
                            onWhatsAppClick = { appointmentForNotification = appointment }
                        )
                    }
                }
            }
        }
    }

    // Direct Notification Dialog
    appointmentForNotification?.let { appt ->
        ThirdPartyNotificationDialog(
            appointment = appt,
            onWhatsApp = {
                viewModel.sendWhatsApp(appt)
                appointmentForNotification = null
            },
            onSms = {
                viewModel.sendSms(appt)
                appointmentForNotification = null
            },
            onEmail = {
                viewModel.sendEmail(appt)
                appointmentForNotification = null
            },
            onTestWebhook = { service ->
                viewModel.testApiWebhook(appt, service) {
                    appointmentForNotification = null
                }
            },
            onDismiss = { appointmentForNotification = null }
        )
    }
}
