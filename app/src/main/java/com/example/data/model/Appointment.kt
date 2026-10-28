package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppointmentStatus(val displayNameEs: String, val displayNameEn: String) {
    PENDING( "Pendiente", "Pending"),
    IN_PROGRESS("En atención", "In Progress"),
    COMPLETED("Completada", "Completed"),
    CANCELLED("Cancelada", "Cancelled")
}

data class ServiceOption(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    val basePrice: Double,
    val durationMinutes: Int,
    val descriptionEs: String,
    val descriptionEn: String
)

data class AddonOption(
    val id: String,
    val nameEs: String,
    val nameEn: String,
    val price: Double,
    val extraMinutes: Int
)

object BusinessCatalog {
    val defaultServices = listOf(
        ServiceOption(
            id = "corte_clasico",
            nameEs = "Corte Clásico",
            nameEn = "Classic Haircut",
            basePrice = 180.0,
            durationMinutes = 30,
            descriptionEs = "Corte tradicional a tijera o máquina con acabado navaja",
            descriptionEn = "Traditional scissor/clipper cut with razor finish"
        ),
        ServiceOption(
            id = "barba_perfilado",
            nameEs = "Barba & Perfilado",
            nameEn = "Beard & Contouring",
            basePrice = 140.0,
            durationMinutes = 25,
            descriptionEs = "Alineado con toalla caliente, vapor ozono y bálsamo",
            descriptionEn = "Hot towel alignment, ozone steam and conditioning balm"
        ),
        ServiceOption(
            id = "combo_corte_barba",
            nameEs = "Combo Corte + Barba",
            nameEn = "Haircut + Beard Combo",
            basePrice = 280.0,
            durationMinutes = 55,
            descriptionEs = "Experiencia completa de corte de cabello y arreglo de barba",
            descriptionEn = "Complete experience with haircut and full beard grooming"
        ),
        ServiceOption(
            id = "tinte_color",
            nameEs = "Tinte & Colorimetría",
            nameEn = "Hair Coloring & Dye",
            basePrice = 450.0,
            durationMinutes = 75,
            descriptionEs = "Matización de canas, decoloración o color fantasía",
            descriptionEn = "Grey coverage, bleaching or fantasy colors"
        ),
        ServiceOption(
            id = "limpieza_facial",
            nameEs = "Limpieza Facial Profunda",
            nameEn = "Deep Facial Cleansing",
            basePrice = 320.0,
            durationMinutes = 40,
            descriptionEs = "Exfoliación, extracción de puntos negros y mascarilla calmante",
            descriptionEn = "Exfoliation, blackhead extraction and soothing mask"
        ),
        ServiceOption(
            id = "diseno_tatuaje",
            nameEs = "Diseño / Tatuaje Express",
            nameEn = "Express Tattoo / Design",
            basePrice = 500.0,
            durationMinutes = 60,
            descriptionEs = "Mini tattoo o diseño personalizado de arte corporal",
            descriptionEn = "Mini tattoo or custom body art design"
        )
    )

    val defaultAddons = listOf(
        AddonOption(
            id = "lavado_masaje",
            nameEs = "Lavado con Masaje Capilar",
            nameEn = "Hair Wash & Scalp Massage",
            price = 50.0,
            extraMinutes = 10
        ),
        AddonOption(
            id = "mascarilla_carbon",
            nameEs = "Mascarilla Negra Anti-impurezas",
            nameEn = "Charcoal Peel-off Mask",
            price = 60.0,
            extraMinutes = 15
        ),
        AddonOption(
            id = "exfoliacion_express",
            nameEs = "Exfoliación Facial Express",
            price = 70.0,
            nameEn = "Express Facial Scrub",
            extraMinutes = 10
        ),
        AddonOption(
            id = "aceites_tonico",
            nameEs = "Tratamiento de Aceites & Tónico",
            nameEn = "Nourishing Oil & Tonic Treatment",
            price = 45.0,
            extraMinutes = 5
        ),
        AddonOption(
            id = "bebida_cortesia",
            nameEs = "Bebida Especial de Cortesía",
            nameEn = "Premium Complimentary Drink",
            price = 30.0,
            extraMinutes = 0
        )
    )
}

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val clientName: String,
    val clientPhone: String,
    val clientEmail: String = "",
    val serviceId: String,
    val serviceName: String,
    val servicePrice: Double,
    val durationMinutes: Int,
    val selectedAddonIds: String = "",
    val selectedAddonNames: String = "",
    val addonsPrice: Double = 0.0,
    val totalPrice: Double,
    val appointmentDateMillis: Long,
    val appointmentHour: Int,
    val appointmentMinute: Int,
    val status: AppointmentStatus = AppointmentStatus.PENDING,
    val notes: String = "",
    val isSynced: Boolean = false,
    val remoteId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(appointmentDateMillis))
    }

    fun getFormattedTime(): String {
        val isAm = appointmentHour < 12
        val hour12 = when {
            appointmentHour == 0 -> 12
            appointmentHour > 12 -> appointmentHour - 12
            else -> appointmentHour
        }
        val minStr = appointmentMinute.toString().padStart(2, '0')
        val amPm = if (isAm) "AM" else "PM"
        return "$hour12:$minStr $amPm"
    }

    fun isToday(): Boolean {
        val now = Calendar.getInstance()
        val appt = Calendar.getInstance().apply { timeInMillis = appointmentDateMillis }
        return now.get(Calendar.YEAR) == appt.get(Calendar.YEAR) &&
               now.get(Calendar.DAY_OF_YEAR) == appt.get(Calendar.DAY_OF_YEAR)
    }

    fun getAddonIdsList(): List<String> {
        if (selectedAddonIds.isBlank()) return emptyList()
        return selectedAddonIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
