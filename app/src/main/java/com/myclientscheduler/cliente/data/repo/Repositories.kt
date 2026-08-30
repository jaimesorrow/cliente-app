package com.myclientscheduler.cliente.data.repo

import com.myclientscheduler.cliente.core.model.*
import com.myclientscheduler.cliente.core.time.ConflictDetector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

interface SessionRepository {
    fun currentUser(): UserContext
}

class FakeSessionRepository : SessionRepository {
    override fun currentUser(): UserContext = UserContext("owner-1", "biz-1", Role.OWNER)
}

class InMemoryRepository {
    private val lock = Mutex()
    val clients = mutableMapOf<String, Client>()
    val services = mutableMapOf<String, Service>()
    val appointments = mutableMapOf<String, Appointment>()
    val timeOff = mutableListOf<TimeOffBlock>()
    val auditLog = mutableMapOf<String, AuditEntry>()
    private val operationTokens = mutableSetOf<String>()

    suspend fun upsertClient(client: Client) = lock.withLock { clients[client.id] = client }
    suspend fun listClients(): List<Client> = lock.withLock { clients.values.toList() }

    suspend fun upsertService(service: Service) = lock.withLock {
        require(service.durationMin in 15..720)
        require(service.priceCents >= 0)
        services[service.id] = service
    }

    suspend fun archiveService(serviceId: String) = lock.withLock {
        val existing = services[serviceId] ?: return@withLock
        services[serviceId] = existing.copy(archived = true)
    }

    suspend fun listActiveServices(): List<Service> = lock.withLock { services.values.filter { !it.archived } }

    suspend fun createAppointment(
        actor: UserContext,
        clientId: String,
        serviceId: String,
        startsAt: LocalDateTime,
        operationToken: String,
    ): Result<Appointment> = lock.withLock {
        if (isDuplicate(operationToken)) return@withLock duplicateFailure()
        val service = services[serviceId] ?: return@withLock Result.failure(IllegalArgumentException("Service missing"))
        if (service.archived) return@withLock Result.failure(IllegalArgumentException("Service archived"))
        val endsAt = startsAt.plusMinutes(service.durationMin.toLong())
        if (ConflictDetector.hasConflict(startsAt, endsAt, appointments.values.toList(), timeOff)) {
            return@withLock Result.failure(IllegalStateException("Conflict detected"))
        }
        val appointment = Appointment(
            id = UUID.randomUUID().toString(), businessId = actor.businessId, clientId = clientId, serviceId = serviceId,
            startsAt = startsAt, endsAt = endsAt, status = AppointmentStatus.SCHEDULED, priceSnapshotCents = service.priceCents
        )
        appointments[appointment.id] = appointment
        Result.success(appointment)
    }

    suspend fun transitionStatus(
        actor: UserContext,
        appointmentId: String,
        toStatus: AppointmentStatus,
        reason: String?,
        operationToken: String,
    ): Result<Appointment> = lock.withLock {
        if (isDuplicate(operationToken)) return@withLock duplicateFailure()
        val current = appointments[appointmentId] ?: return@withLock Result.failure(IllegalArgumentException("Missing"))
        val allowed = when (current.status) {
            AppointmentStatus.SCHEDULED -> setOf(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELED)
            AppointmentStatus.CONFIRMED -> setOf(AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW, AppointmentStatus.CANCELED)
            else -> emptySet()
        }
        if (toStatus !in allowed) return@withLock Result.failure(IllegalStateException("Invalid transition"))
        val next = current.copy(status = toStatus)
        appointments[appointmentId] = next
        val action = if (toStatus == AppointmentStatus.CANCELED) "cancel" else "status_change"
        auditLog += buildAuditEntry(actor, appointmentId, action, current.status, toStatus, reason = reason)
        Result.success(next)
    }

    suspend fun reschedule(
        actor: UserContext,
        appointmentId: String,
        newStart: LocalDateTime,
        reason: String,
        operationToken: String,
    ): Result<Appointment> = lock.withLock {
        if (isDuplicate(operationToken)) return@withLock duplicateFailure()
        val current = appointments[appointmentId] ?: return@withLock Result.failure(IllegalArgumentException("Missing"))
        val service = services[current.serviceId] ?: return@withLock Result.failure(IllegalArgumentException("Service missing"))
        val newEnd = newStart.plusMinutes(service.durationMin.toLong())
        if (ConflictDetector.hasConflict(newStart, newEnd, appointments.values.toList(), timeOff, appointmentId)) {
            return@withLock Result.failure(IllegalStateException("Conflict detected"))
        }
        val next = current.copy(startsAt = newStart, endsAt = newEnd)
        appointments[appointmentId] = next
        auditLog += buildAuditEntry(actor, appointmentId, "reschedule", current.status, next.status, current.startsAt, newStart, reason)
        Result.success(next)
    }

    fun canMutateAuditLog(): Boolean = false

    private fun isDuplicate(operationToken: String) = !operationTokens.add(operationToken)

    private fun <T> duplicateFailure(): Result<T> = Result.failure(IllegalStateException("Duplicate action"))

    private fun buildAuditEntry(
        actor: UserContext,
        appointmentId: String,
        action: String,
        fromStatus: AppointmentStatus?,
        toStatus: AppointmentStatus?,
        oldDateTime: LocalDateTime? = null,
        newDateTime: LocalDateTime? = null,
        reason: String? = null,
    ): Pair<String, AuditEntry> {
        val entry = AuditEntry(
            id = UUID.randomUUID().toString(), appointmentId = appointmentId,
            actorId = actor.userId, actorRole = actor.role, timestamp = Instant.now(),
            action = action, fromStatus = fromStatus, toStatus = toStatus,
            oldDateTime = oldDateTime, newDateTime = newDateTime, reason = reason,
        )
        return entry.id to entry
    }
}
