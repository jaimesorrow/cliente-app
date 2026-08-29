---
name: cliente-android-review
description: Reviews diffs in this repo (Clientè, a provider-side Kotlin/Compose Android booking-concierge app, package com.myclientscheduler.cliente, backed by Firebase Auth/Firestore + a local Room cache) against its actual RBAC, multi-tenant business-scoping, appointment-conflict, audit-immutability, and scope-lockdown invariants, on top of ordinary correctness review. Use this instead of a generic code review for any change touching core/rbac/Permissions.kt, core/time/ConflictDetector.kt, data/repo/Repositories.kt, data/local/RoomModels.kt, domain/usecase/DeleteAccountUseCase.kt or LegalAcceptanceUseCase.kt, firebaserules/firestore.rules, app/MainActivity.kt's route table, or AppPolicyTests.kt.
---

# Clientè Android review

This app (`app/src/main/java/com/myclientscheduler/cliente/...`) is a single-tenant-per-user,
provider-side booking concierge — Owner/Staff roles, no client-facing UI at all. Check diffs
against the invariants below, which come from this repo's actual code and README, not generic
Android advice.

## 1. Locked product identity
- User-facing product name is **Clientè** (one grave accent, final character only). The string
  `MyClientScheduler` (the package/legal name) must appear **only** in legal/account metadata —
  today that's the `LegalScreen` composable's "Legal account entity: MyClientScheduler" line in
  `app/MainActivity.kt`. Flag any change that prints `MyClientScheduler` in a dashboard/hub/regular
  screen, or that renames `Clientè` elsewhere.

## 2. Scope lockdown (README "Scope constraints")
This is a provider-only app by design. Flag any change that adds:
- a public/self-service client booking UI or route (no `AppRoute` should let an unauthenticated or
  client-role user create their own appointment),
- payment capture/processing (the `priceCents`/`priceSnapshotCents` fields on `Service`/
  `Appointment` are record-keeping only, not a payment flow),
- in-app messaging/chat. Client contact must stay limited to device intents — see
  `openDialIntent`/`openSmsIntent`/`openEmailIntent` in `app/MainActivity.kt`
  (`ACTION_DIAL`/`ACTION_SENDTO`) — not a composed in-app message thread.

## 3. RBAC (`core/rbac/Permissions.kt`)
- `AppRoute.ownerOnly` must stay `true` for `Services`, `Availability`, `Analytics`, `Settings`.
- `Templates` is intentionally `ownerOnly = false` (staff can view/apply templates per README), but
  `PermissionPolicy.canEditTemplates()` must stay Owner-only. If a diff adds template-editing UI,
  verify it gates on `canEditTemplates`, not just on being able to reach the Templates route.
- Any new `AppRoute` needs the correct `ownerOnly` value, and any new owner-only capability should
  get its own `PermissionPolicy.canXxx` check rather than being folded into `canAccessRoute`.
- The debug QA matrix (`AppRoute.QA`, `DebugQaScreen`) is gated by `BuildConfig.DEBUG` both in the
  route list built in `HubScreen` and in the `composable(AppRoute.QA.path)` branch in
  `app/MainActivity.kt`. Both checks must remain — a change that drops either one ships a
  debug-only surface into release.

## 4. Multi-tenant business scoping
Every `Client`/`Service`/`Appointment`/`AuditEntry` carries a `businessId`
(`core/model/Models.kt`), and `UserContext` has exactly one `businessId` (single-tenant membership
per user, per README). Any new repository method or query must filter/scope by
`actor.businessId`; any new Firestore collection needs a matching rule under the
`sameBusiness(bizId)` match block in `firebaserules/firestore.rules`, respecting the existing
owner-only vs. both-roles collection split (`services`/`templates`/`availability`/`analytics`/
`settings` = Owner only; `clients`/`appointments` = both roles). `businesses/{bizId}` itself is
`allow create, delete: if false` — business creation/deletion is not a client-side operation; flag
any change that tries to do either from the app.

## 5. Appointment conflict detection (`core/time/ConflictDetector.kt`)
- Overlap test is half-open: `aStart < bEnd && bStart < aEnd`. Any new conflict/availability logic
  must reuse `ConflictDetector.hasConflict` (or match this exact semantics) rather than
  reimplementing interval math with different edge behavior.
- `CANCELED` appointments are excluded from conflict checks — don't let a "no-show blocks the slot"
  or similar feature accidentally start counting canceled appointments as conflicts (or vice versa,
  silently exclude `NO_SHOW`, which today still blocks the slot).
- `createAppointment`/`reschedule` in `data/repo/Repositories.kt` both pass `timeOffBlocks` into
  the conflict check — a new booking path that skips `timeOff` is a regression.

## 6. Idempotency via operation tokens
`InMemoryRepository.createAppointment`/`transitionStatus`/`reschedule` all require a caller-supplied
`operationToken` and reject a repeat via `operationTokens.add(operationToken)` before doing any
work. Any new mutating repo method (especially ones reachable from a retryable UI action) should
follow the same dedup pattern — a retried tap/network retry must not double-book, double-cancel, or
double-transition an appointment.

## 7. Audit log is append-only
`InMemoryRepository.canMutateAuditLog()` is hardcoded `false`, and every mutation
(`transitionStatus`, `reschedule`) only ever *inserts* a new `AuditEntry` — this mirrors
`firebaserules/firestore.rules`'s `auditLogs` rule (`allow update, delete: if false`). Reject any
change that updates or deletes an existing audit entry/doc instead of appending a new one.

## 8. Appointment status state machine
Valid transitions (`InMemoryRepository.transitionStatus`):
`SCHEDULED -> {CONFIRMED, CANCELED}`, `CONFIRMED -> {COMPLETED, NO_SHOW, CANCELED}`, and no
transitions out of `COMPLETED`/`NO_SHOW`/`CANCELED` (terminal). Flag any new edge that resurrects a
terminal-state appointment or skips a state (e.g. `SCHEDULED -> COMPLETED` directly).

## 9. `DeleteAccountUseCase` — known-suspicious branch, verify intent before trusting it
Current logic (`domain/usecase/DeleteAccountUseCase.kt`):
- `Role.STAFF` -> always succeeds (identity-only deletion, 30-day purge).
- `Role.OWNER` with `ownerCount <= 1` -> succeeds, **closes the business**.
- `Role.OWNER` with `ownerCount > 1` (i.e., other owners remain) -> **fails** with
  `"At least one owner must remain"`.
That last branch reads backwards: when other owners *do* remain, deletion is the case that should be
safe to allow (the business still has an owner afterward), yet it's the one that's rejected, while
the sole-owner case is the one that succeeds and auto-closes the business. The only existing test
(`delete flow allows last owner close business` in `AppPolicyTests.kt`) covers just the
`ownerCount <= 1` path, so this asymmetry isn't caught by CI. Do not assume either branch is correct
by default — if a diff touches this use case, confirm the intended behavior explicitly (with the
requester or via a new test for the `ownerCount > 1` path) rather than preserving or "fixing" the
direction on assumption.

## 10. Legal acceptance is currently decorative — don't let it stay that way silently
`LegalAcceptanceUseCase.accept()` requires non-blank `termsVersion`/`privacyVersion` and stamps
`Instant.now()`, but `LegalScreen` in `app/MainActivity.kt` currently only renders static text
("Terms v1.0 accepted timestamp stored at account level.") — it does not call
`LegalAcceptanceUseCase` or persist anything. If a diff adds real acceptance-tracking UI, verify it
actually invokes and persists through `LegalAcceptanceUseCase`, not just updated copy; if a diff
touches `LegalScreen` without wiring it up, don't assume the existing static text means acceptance
is already tracked.

## 11. Room is a cache, not a source of truth
`data/local/RoomModels.kt` (`ClienteDatabase`, `ClientEntity`/`AppointmentEntity`) is described by
the README as a "local offline index/cache" over Firebase Auth/Firestore sync. A new Room entity or
DAO method should not become the only place data is written — check that writes still go through
(or plan to go through) Firestore, and that a `version` bump on `ClienteDatabase` comes with a
migration, not a silent destructive-recreate (`exportSchema = false`, `version = 1` today).

## 12. `BackStackSanitizer` exists but is currently unwired
`core/util/BackStackSanitizer.kt` is built to strip a disallowed route (e.g. an owner-only screen)
out of the back stack, but nothing in `app/MainActivity.kt`'s `SafeNavHost` currently calls it. If a
diff wires it in, verify the `allowed` set passed is derived live from
`PermissionPolicy`/the current user's role — not a hardcoded/stale route list — otherwise a
role-downgrade (owner demoted to staff, or a business switch) could still leak a back-stack entry
into an owner-only screen for staff.

## 13. Treat `AppPolicyTests.kt` as the executable spec
It currently has 5 tests: RBAC route gating, conflict-detector cancel-exclusion, audit
append-only/idempotency, delete-flow (sole-owner path only, see #9), and back-stack sanitization.
Any change to the areas above should keep these green and, ideally, add a matching test in this file
rather than only manual verification.
