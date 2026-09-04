---
name: data-model
description: Reference for Clientè's real domain models (core/model/Models.kt), Room cache entities (data/local/RoomModels.kt), and the validation/query/mutation rules actually enforced in data/repo/Repositories.kt, core/rbac/Permissions.kt, and core/time/ConflictDetector.kt. Use this to look up exact field names/types before adding a column, query, or mutation.
---

# Clientè data model

## Primary entities (`core/model/Models.kt`)
- `UserContext(userId: String, businessId: String, role: Role)` — `Role` is `OWNER` or `STAFF`; exactly one `businessId` per user (single-tenant membership).
- `Client(id, businessId, name, phone: String?, email: String?, tags: List<String> = emptyList(), notes: List<NoteEntry> = emptyList())`; `NoteEntry(text, authorId, timestamp: Instant)`.
- `Service(id, businessId, name, durationMin: Int, priceCents: Long, bufferBeforeMin: Int = 0, bufferAfterMin: Int = 0, archived: Boolean = false)`.
- `Appointment(id, businessId, clientId, serviceId, startsAt: LocalDateTime, endsAt: LocalDateTime, status: AppointmentStatus, priceSnapshotCents: Long)`; `AppointmentStatus` = `SCHEDULED|CONFIRMED|COMPLETED|NO_SHOW|CANCELED`.
- `TimeOffBlock(start, end)`; `AuditEntry(id, appointmentId, actorId, actorRole, timestamp: Instant, action: String, fromStatus, toStatus, oldDateTime, newDateTime, reason: String?)`.

## Room cache (`data/local/RoomModels.kt`) — narrower than the domain models
- `ClientEntity(id, businessId, name, phone: String?, email: String?)` — no `tags`/`notes` columns, so those `Client` fields never persist locally.
- `AppointmentEntity(id, businessId, clientId, serviceId, startsAtIso: String, endsAtIso: String, status: String)` — times/status stored as strings, not typed.
- `ClienteDatabase` (`version = 1, exportSchema = false`) exposes only `ClientDao`; there is no `AppointmentDao` despite `AppointmentEntity` existing.

## Relationships
Every `Client`/`Service`/`Appointment`/`AuditEntry` (via its `appointmentId`) is scoped to one `businessId`. `Appointment.clientId`/`serviceId` are unenforced references into `clients`/`services` — no FK check exists (see the cache-scoping skill for the cross-tenant gap this creates).

## Validation rules enforced today
- `upsertService`: `durationMin in 15..720`, `priceCents >= 0`.
- Status transitions (`transitionStatus`): `SCHEDULED→{CONFIRMED,CANCELED}`, `CONFIRMED→{COMPLETED,NO_SHOW,CANCELED}`, both terminal.
- `ConflictDetector.hasConflict`: half-open overlap (`aStart < bEnd && bStart < aEnd`) against `timeOffBlocks` and non-`CANCELED` appointments.
- `createAppointment`/`transitionStatus`/`reschedule` all require a caller `operationToken`, rejecting repeats.
- `PermissionPolicy` (`core/rbac/Permissions.kt`): owner-only checks are `user.role == Role.OWNER`.

## Query patterns
`ClientDao.observeAll()` — `SELECT * FROM clients ORDER BY name` returning `Flow<List<ClientEntity>>`, **no `WHERE businessId`**. `InMemoryRepository.listClients()`/`listActiveServices()` are similarly unscoped in-memory reads.

## Mutations
`ClientDao.upsert(items: List<ClientEntity>)` (`OnConflictStrategy.REPLACE`). All `InMemoryRepository` writes go through a single `Mutex.withLock`; audit entries are insert-only (`canMutateAuditLog() = false`).
