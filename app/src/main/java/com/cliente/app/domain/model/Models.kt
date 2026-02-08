package com.cliente.app.domain.model

import java.time.Instant
import java.time.LocalDateTime

enum class Role { OWNER, STAFF }

data class UserSession(
    val userId: String,
    val businessId: String,
    val role: Role,
)

data class Client(
    val id: String,
    val businessId: String,
    val name: String,
    val tags: List<String> = emptyList(),
    val notes: List<NoteEntry> = emptyList(),
)

data class NoteEntry(
    val text: String,
    val authorId: String,
    val createdAt: Instant,
)

data class Service(
    val id: String,
    val businessId: String,
    val title: String,
    val durationMin: Int,
    val priceCents: Int,
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
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val priceSnapshotCents: Int,
)

data class TimeOffBlock(
    val id: String,
    val businessId: String,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,
)

enum class AuditEventType { STATUS_CHANGE, RESCHEDULE, CANCEL }

data class AppointmentAuditLog(
    val id: String,
    val appointmentId: String,
    val actorId: String,
    val actorRole: Role,
    val type: AuditEventType,
    val timestamp: Instant,
    val fromStatus: AppointmentStatus? = null,
    val toStatus: AppointmentStatus? = null,
    val oldDateTime: LocalDateTime? = null,
    val newDateTime: LocalDateTime? = null,
    val reason: String? = null,
    val idempotencyKey: String,
)
