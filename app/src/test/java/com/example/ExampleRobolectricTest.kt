package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.BusinessCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CitaFácil", appName)
    }

    @Test
    fun `appointment date and time formatting`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 15)
        }

        val appt = Appointment(
            clientName = "Juan Carlos",
            clientPhone = "5512345678",
            serviceId = "corte_clasico",
            serviceName = "Corte Clásico",
            servicePrice = 180.0,
            durationMinutes = 30,
            totalPrice = 180.0,
            appointmentDateMillis = cal.timeInMillis,
            appointmentHour = 14,
            appointmentMinute = 30,
            status = AppointmentStatus.PENDING
        )

        assertEquals("15/10/2026", appt.getFormattedDate())
        assertEquals("2:30 PM", appt.getFormattedTime())
        assertEquals(AppointmentStatus.PENDING, appt.status)
    }

    @Test
    fun `business catalog services and addons exist`() {
        val services = BusinessCatalog.defaultServices
        val addons = BusinessCatalog.defaultAddons

        assertTrue("Services catalog should not be empty", services.isNotEmpty())
        assertTrue("Addons catalog should not be empty", addons.isNotEmpty())

        val classicHaircut = services.find { it.id == "corte_clasico" }
        assertNotNull(classicHaircut)
        assertEquals(180.0, classicHaircut!!.basePrice, 0.01)
    }
}
