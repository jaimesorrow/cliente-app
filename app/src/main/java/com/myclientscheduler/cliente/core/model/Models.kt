package com.myclientscheduler.cliente.core.model

import java.time.Instant
import java.time.LocalDateTime

enum class Role { OWNER, STAFF }

data class UserContext(val userId: String, val businessId: String, val role: Role)

data class Client(
    val id: String,
    val businessId: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val tags: List<String> = emptyList(),
    val notes: List<NoteEntry> = emptyList(),
)

data class NoteEntry(val text: String, val authorId: String, val timestamp: Instant)

data class Service(
    val id: String,
    val businessId: String,
    val name: String,
    val durationMin: Int,
    val priceCents: Long,
    val bufferBeforeMin: Int = 0,
    val bufferAfterMin: Int = 0,
    val archived: Boolean = false,
)

enum class AppointmentStatus { SCHEDULED, CONFIRMED, COMPLETED, NO_SHOW, CANCELED }

data class Appointment(
    val id: String,
    val businessId: String,
    val clientId: String,
    val serviceId: String,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,
    val status: AppointmentStatus,
    val priceSnapshotCents: Long,
)

data class TimeOffBlock(val start: LocalDateTime, val end: LocalDateTime)

data class AuditEntry(
    val id: String,
    val appointmentId: String,
    val actorId: String,
    val actorRole: Role,
    val timestamp: Instant,
    val action: String,
    val fromStatus: AppointmentStatus?,
    val toStatus: AppointmentStatus?,
    val oldDateTime: LocalDateTime?,
    val newDateTime: LocalDateTime?,
    val reason: String?,
)
