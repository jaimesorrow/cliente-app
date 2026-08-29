---
name: cliente-cache-scoping-review
description: Reviews Clientè's local-read-path and mutation-path business scoping — whether InMemoryRepository (data/repo/Repositories.kt) and the Room cache (data/local/RoomModels.kt) actually filter/verify by businessId, versus cliente-android-review's item 4 which covers Firestore rules and forward-looking guidance only. Use this for any change to ClientDao, ClienteDatabase, or any list*/observe*/get* or mutating method on InMemoryRepository.
---

# Clientè local-cache / in-memory scoping review

`cliente-android-review` item 4 says any new repository method "must filter/scope by
`actor.businessId`" and covers the Firestore rules side of that. This skill exists because, read
against the actual shipped code, several methods that exist **today** already don't do that — this
is not just guidance for future diffs, it's an existing gap worth checking on every touch of these
files.

## Read-path: methods that return data with no `businessId` filter at all
- `InMemoryRepository.listClients()` — `clients.values.toList()`. Every `Client` ever inserted,
  from any business, is returned to any caller regardless of `actor.businessId`.
- `InMemoryRepository.listActiveServices()` — `services.values.filter { !it.archived }`. Filters on
  `archived` only, not `businessId`.
- `ClientDao.observeAll()` (Room, `data/local/RoomModels.kt`) — `SELECT * FROM clients ORDER BY
  name`. No `WHERE businessId = :businessId`. Every cached client row from every business the
  device has ever synced comes back.

## Write/mutate-path: methods that trust a caller-supplied ID without checking ownership
- `createAppointment(actor, clientId, serviceId, ...)` looks up `services[serviceId]` and accepts
  `clientId` as given — it never checks `service.businessId == actor.businessId` (or the client's).
  A caller that supplies a `serviceId`/`clientId` belonging to a different business gets an
  `Appointment` recorded with `businessId = actor.businessId` but referencing another business's
  service/client.
- `transitionStatus(actor, appointmentId, ...)` and `reschedule(actor, appointmentId, ...)` both do
  `appointments[appointmentId] ?: return failure(...)` and then mutate it — neither checks
  `current.businessId == actor.businessId`. Any actor who can obtain/guess an `appointmentId` from a
  different business can confirm, cancel, complete, or reschedule that business's appointment.

Today's practical exposure is bounded by `FakeSessionRepository` always returning
`UserContext("owner-1", "biz-1", Role.OWNER)` — there is exactly one business in the store, so
nothing currently collides. That is exactly why this is easy to miss: the bug is latent until a
second business's data enters the same `InMemoryRepository`/Room instance (real multi-business
Firestore sync, an account switch, or a shared-device scenario), at which point every method above
starts leaking or accepting cross-tenant data with no code change required to trigger it.

## What to check in a diff
1. Any new or modified `list*`/`observe*`/`get*` method (Room DAO query or in-memory collection
   read) must filter by the caller's `businessId` — a `WHERE businessId = :businessId` Room query
   param, or a `.filter { it.businessId == actor.businessId }` in-memory — not just record
   `businessId` on the way in.
2. Any mutating method that takes an existing entity's ID (`appointmentId`, `clientId`,
   `serviceId`) must verify the looked-up entity's `businessId` matches `actor.businessId` **before**
   mutating it, in addition to stamping `businessId = actor.businessId` on newly created rows.
3. Don't treat the current shape of `listClients()`/`listActiveServices()`/`createAppointment`/
   `transitionStatus`/`reschedule` as a correctness baseline to match — a new method copying the
   same unscoped pattern is repeating a real bug, not "matching existing style."
4. If a fix adds real per-business filtering to the Room side, check whatever eventually calls
   `ClienteDatabase.clients()` actually passes the current business ID through, and that already
   cached cross-tenant rows don't silently linger — `ClienteDatabase` today is `version = 1,
   exportSchema = false` with no migration, so a scoping fix that changes the query shape may need a
   cache-clear or version bump, not just an edited `@Query`.
