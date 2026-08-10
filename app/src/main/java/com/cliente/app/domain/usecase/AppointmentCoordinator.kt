package com.cliente.app.domain.usecase

import com.cliente.app.domain.model.Appointment
import com.cliente.app.domain.model.AppointmentAuditLog
import com.cliente.app.domain.model.AppointmentStatus
import com.cliente.app.domain.model.AuditEventType
import com.cliente.app.domain.model.Role
import com.cliente.app.domain.model.TimeOffBlock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

class AppointmentCoordinator {
    private val lock = Mutex()
    private val appointments = mutableMapOf<String, Appointment>()
    private val logs = mutableListOf<AppointmentAuditLog>()

    // Bounded LRU-style set: evict oldest entries when over capacity to prevent unbounded growth
    // while still rejecting duplicate keys seen recently.
    private val idempotencyGuard = LinkedHashMap<String, Unit>(64, 0.75f, true)
    private val idempotencyGuardMaxSize = 1_000

    // True upsert: keyed by block.id so repeated calls with the same id replace rather than append.
    private val timeOffBlocks = mutableMapOf<String, TimeOffBlock>()

    suspend fun upsertTimeOff(block: TimeOffBlock) = lock.withLock {
        timeOffBlocks[block.id] = block
    }

    suspend fun create(appointment: Appointment): Result<Appointment> = lock.withLock {
        if (!appointment.startsAt.isBefore(appointment.endsAt)) {
            return Result.failure(IllegalArgumentException("startsAt must be before endsAt"))
        }
        if (hasConflict(appointment.startsAt, appointment.endsAt, null)) {
            return Result.failure(IllegalStateException("Conflict with existing booking or time-off"))
        }
        appointments[appointment.id] = appointment
        Result.success(appointment)
    }

    suspend fun reschedule(
        appointmentId: String,
        newStart: LocalDateTime,
        newEnd: LocalDateTime,
        actorId: String,
        actorRole: Role,
        reason: String,
        idempotencyKey: String,
    ): Result<Appointment> = lock.withLock {
        if (!newStart.isBefore(newEnd)) {
            return Result.failure(IllegalArgumentException("newStart must be before newEnd"))
        }
        if (!trackIdempotency(idempotencyKey)) {
            return Result.success(appointments[appointmentId] ?: return Result.failure(NoSuchElementException()))
        }
        val existing = appointments[appointmentId] ?: return Result.failure(NoSuchElementException())
        if (existing.status == AppointmentStatus.CANCELED) {
            return Result.failure(IllegalStateException("Canceled appointments cannot be rescheduled"))
        }
        if (hasConflict(newStart, newEnd, appointmentId)) {
            return Result.failure(IllegalStateException("Conflict detected"))
        }
        val updated = existing.copy(startsAt = newStart, endsAt = newEnd)
        appointments[appointmentId] = updated
        logs += AppointmentAuditLog(
            id = UUID.randomUUID().toString(),
            appointmentId = appointmentId,
            actorId = actorId,
            actorRole = actorRole,
            type = AuditEventType.RESCHEDULE,
            timestamp = Instant.now(),
            oldDateTime = existing.startsAt,
            newDateTime = newStart,
            reason = reason,
            idempotencyKey = idempotencyKey,
        )
        Result.success(updated)
    }

    suspend fun transitionStatus(
        appointmentId: String,
        to: AppointmentStatus,
        actorId: String,
        actorRole: Role,
        reason: String?,
        idempotencyKey: String,
    ): Result<Appointment> = lock.withLock {
        if (!trackIdempotency(idempotencyKey)) {
            return Result.success(appointments[appointmentId] ?: return Result.failure(NoSuchElementException()))
        }
        val existing = appointments[appointmentId] ?: return Result.failure(NoSuchElementException())
        val allowed = when (existing.status) {
            AppointmentStatus.SCHEDULED -> to in setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELED)
            AppointmentStatus.CONFIRMED -> to in setOf(AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW, AppointmentStatus.CANCELED)
            AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW, AppointmentStatus.CANCELED -> false
        }
        if (!allowed) return Result.failure(IllegalStateException("Invalid state transition"))

        val updated = existing.copy(status = to)
        appointments[appointmentId] = updated
        logs += AppointmentAuditLog(
            id = UUID.randomUUID().toString(),
            appointmentId = appointmentId,
            actorId = actorId,
            actorRole = actorRole,
            type = if (to == AppointmentStatus.CANCELED) AuditEventType.CANCEL else AuditEventType.STATUS_CHANGE,
            timestamp = Instant.now(),
            fromStatus = existing.status,
            toStatus = to,
            reason = reason,
            idempotencyKey = idempotencyKey,
        )
        Result.success(updated)
    }

    suspend fun listLogs(): List<AppointmentAuditLog> = lock.withLock { logs.toList() }

    /** Returns true if the key is new (should proceed), false if duplicate (should return cached). */
    private fun trackIdempotency(key: String): Boolean {
        if (idempotencyGuard.containsKey(key)) return false
        idempotencyGuard[key] = Unit
        if (idempotencyGuard.size > idempotencyGuardMaxSize) {
            idempotencyGuard.iterator().also { it.next(); it.remove() }
        }
        return true
    }

    private fun hasConflict(start: LocalDateTime, end: LocalDateTime, ignoreAppointmentId: String?): Boolean {
        val blockedByTimeOff = timeOffBlocks.values.any { overlaps(start, end, it.startsAt, it.endsAt) }
        val blockedByAppointment = appointments.values
            .filter { it.id != ignoreAppointmentId && it.status != AppointmentStatus.CANCELED }
            .any { overlaps(start, end, it.startsAt, it.endsAt) }
        return blockedByTimeOff || blockedByAppointment
    }

    private fun overlaps(aStart: LocalDateTime, aEnd: LocalDateTime, bStart: LocalDateTime, bEnd: LocalDateTime): Boolean =
        aStart < bEnd && bStart < aEnd
}
