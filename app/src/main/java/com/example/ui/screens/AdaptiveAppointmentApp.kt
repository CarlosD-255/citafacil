package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AppointmentViewModel

enum class AppScreen {
    LIST,
    DETAIL,
    FORM,
    SETTINGS
}

@Composable
fun AdaptiveAppointmentApp(
    viewModel: AppointmentViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(AppScreen.LIST) }
    var activeDetailId by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val allAppointments by viewModel.allAppointments.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("adaptive_appointment_app")) {
        val isExpanded = maxWidth >= 720.dp

        if (isExpanded) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(380.dp)
                        .fillMaxHeight()
                ) {
                    AppointmentsListScreen(
                        viewModel = viewModel,
                        onAppointmentClick = { id ->
                            activeDetailId = id
                            currentScreen = AppScreen.DETAIL
                        },
                        onNewAppointmentClick = {
                            viewModel.initNewForm()
                            currentScreen = AppScreen.FORM
                        },
                        onSettingsClick = {
                            currentScreen = AppScreen.SETTINGS
                        }
                    )
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (currentScreen) {
                        AppScreen.FORM -> {
                            AppointmentFormScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = AppScreen.DETAIL },
                                onSuccess = { newId ->
                                    activeDetailId = newId
                                    currentScreen = AppScreen.DETAIL
                                }
                            )
                        }
                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { currentScreen = AppScreen.LIST }
                            )
                        }
                        AppScreen.DETAIL, AppScreen.LIST -> {
                            val targetId = activeDetailId ?: allAppointments.firstOrNull()?.id
                            if (targetId != null) {
                                AppointmentDetailScreen(
                                    appointmentId = targetId,
                                    viewModel = viewModel,
                                    onBack = { },
                                    onEdit = { editId ->
                                        val appt = allAppointments.find { it.id == editId }
                                        if (appt != null) {
                                            viewModel.initEditForm(appt)
                                            currentScreen = AppScreen.FORM
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Seleccione una cita para ver los detalles",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            when (currentScreen) {
                AppScreen.LIST -> {
                    AppointmentsListScreen(
                        viewModel = viewModel,
                        onAppointmentClick = { id ->
                            activeDetailId = id
                            currentScreen = AppScreen.DETAIL
                        },
                        onNewAppointmentClick = {
                            viewModel.initNewForm()
                            currentScreen = AppScreen.FORM
                        },
                        onSettingsClick = {
                            currentScreen = AppScreen.SETTINGS
                        }
                    )
                }
                AppScreen.DETAIL -> {
                    activeDetailId?.let { id ->
                        AppointmentDetailScreen(
                            appointmentId = id,
                            viewModel = viewModel,
                            onBack = { currentScreen = AppScreen.LIST },
                            onEdit = { editId ->
                                val appt = allAppointments.find { it.id == editId }
                                if (appt != null) {
                                    viewModel.initEditForm(appt)
                                    currentScreen = AppScreen.FORM
                                }
                            }
                        )
                    } ?: run {
                        currentScreen = AppScreen.LIST
                    }
                }
                AppScreen.FORM -> {
                    AppointmentFormScreen(
                        viewModel = viewModel,
                        onBack = {
                            currentScreen = if (activeDetailId != null) AppScreen.DETAIL else AppScreen.LIST
                        },
                        onSuccess = { savedId ->
                            activeDetailId = savedId
                            currentScreen = AppScreen.DETAIL
                        }
                    )
                }
                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = AppScreen.LIST }
                    )
                }
            }
        }
    }
}
