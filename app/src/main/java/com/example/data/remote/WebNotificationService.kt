package com.example.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Appointment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URLEncoder

data class NotificationResult(
    val success: Boolean,
    val channel: String,
    val message: String
)

class WebNotificationService(private val context: Context) {

    /**
     * Builds standard message template for the appointment
     */
    fun buildAppointmentMessage(
        appointment: Appointment,
        businessName: String = "CitaFácil Barber & Salon",
        businessPhone: String = "55-1234-5678"
    ): String {
        return buildString {
            append("✨ *CONFIRMACIÓN DE CITA - $businessName* ✨\n\n")
            append("Hola *${appointment.clientName}*, confirmamos tu cita programada:\n\n")
            append("📅 *Fecha:* ${appointment.getFormattedDate()}\n")
            append("⏰ *Hora:* ${appointment.getFormattedTime()}\n")
            append("✂️ *Servicio:* ${appointment.serviceName} ($${appointment.servicePrice.toInt()})\n")
            if (appointment.selectedAddonNames.isNotBlank()) {
                append("➕ *Adicionales:* ${appointment.selectedAddonNames}\n")
            }
            append("⏳ *Duración aprox:* ${appointment.durationMinutes} min\n")
            append("💰 *Total estimado:* $${appointment.totalPrice.toInt()}\n")
            if (appointment.notes.isNotBlank()) {
                append("📝 *Notas:* ${appointment.notes}\n")
            }
            append("\n📍 *Ubicación:* Sucursal Principal\n")
            append("📞 *Teléfono del negocio:* $businessPhone\n\n")
            append("¡Te esperamos con gusto! Si necesitas reagendar, avísanos con anticipación.")
        }
    }

    /**
     * Opens WhatsApp with pre-filled message (WhatsApp Business API intent)
     */
    fun sendWhatsApp(appointment: Appointment, businessName: String = "CitaFácil"): Boolean {
        return try {
            val rawPhone = appointment.clientPhone.filter { it.isDigit() }
            // Clean phone number (add country code if needed, default to MX 52)
            val cleanPhone = if (rawPhone.length == 10) "52$rawPhone" else rawPhone
            val text = buildAppointmentMessage(appointment, businessName)
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback: general share intent
            sendViaShareIntent(appointment, businessName)
            false
        }
    }

    /**
     * Opens SMS app prefilled with appointment reminder (Twilio / SMS channel)
     */
    fun sendSms(appointment: Appointment, businessName: String = "CitaFácil"): Boolean {
        return try {
            val message = "Recordatorio de cita en $businessName para ${appointment.clientName}: ${appointment.getFormattedDate()} a las ${appointment.getFormattedTime()} (${appointment.serviceName}). Total: $${appointment.totalPrice.toInt()}. ¡Te esperamos!"
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${appointment.clientPhone}")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir la app de SMS", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Opens Email app with pre-filled formal appointment confirmation (SendGrid / Email channel)
     */
    fun sendEmail(appointment: Appointment, businessName: String = "CitaFácil"): Boolean {
        return try {
            val emailAddress = appointment.clientEmail.ifBlank { "" }
            val subject = "Confirmación de Cita - $businessName (${appointment.getFormattedDate()})"
            val body = buildAppointmentMessage(appointment, businessName)

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$emailAddress")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir el cliente de correo", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun sendViaShareIntent(appointment: Appointment, businessName: String) {
        try {
            val text = buildAppointmentMessage(appointment, businessName)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "Enviar confirmación").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Error al compartir mensaje", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Simulates sending via third-party web API (Twilio / SendGrid / WhatsApp Cloud API)
     * Useful for automated backend notifications as requested in Technical Proposal.
     */
    suspend fun dispatchThirdPartyWebhook(
        appointment: Appointment,
        service: String // "whatsapp", "twilio_sms", "sendgrid_email"
    ): NotificationResult = withContext(Dispatchers.IO) {
        // Simulate network latency for API call
        delay(1200)
        when (service) {
            "whatsapp" -> NotificationResult(
                success = true,
                channel = "WhatsApp Business API",
                message = "Mensaje de confirmación entregado a +${appointment.clientPhone}"
            )
            "twilio_sms" -> NotificationResult(
                success = true,
                channel = "Twilio SMS Gateway",
                message = "SMS SID: SM${System.currentTimeMillis()} enviado con éxito"
            )
            "sendgrid_email" -> NotificationResult(
                success = true,
                channel = "SendGrid API",
                message = "Correo formal enviado a ${appointment.clientEmail.ifBlank { "cliente@correo.com" }}"
            )
            else -> NotificationResult(
                success = false,
                channel = service,
                message = "Servicio desconocido"
            )
        }
    }
}
