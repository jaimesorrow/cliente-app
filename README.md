# Clientè (Android MVP)

Provider-side luxury concierge scheduler for a single business tenant (`businessId`) using Kotlin + Jetpack Compose + Material3.

## Locked brand identity
- App-facing name is **Clientè** (grave accent only on final è).
- **MyClientScheduler** appears only in legal/account metadata references.

## MVP scope implemented
- MVVM/Clean package layout with repository/use-case boundaries.
- Room local cache foundation (`ClienteDatabase`) for indexed offline reads.
- Firebase Auth + Firestore integration dependencies and security policy artifacts.
- Strict RBAC (Owner vs Staff):
  - Owner-only routes: `/availability`, `/analytics`, `/settings`.
  - Staff can access Clients/Appointments and view/apply Templates.
- Core modules stubs in Compose navigation:
  - Clients CRM
  - Services (archive-only behavior policy)
  - Appointments with conflict detection and append-only audit events
  - Calendar day/week shell
  - Templates
  - Availability
  - Analytics (estimated revenue concept)
  - Settings + legal hub + logout + delete/close business confirmation flow
- Contact actions use platform intents only (call/text/email), no in-app messaging or payments.
- Debug-only QA matrix screen with filter + Copy CSV (`BuildConfig.DEBUG` guard).

## Data integrity highlights
- Appointment status workflow: `SCHEDULED -> CONFIRMED -> COMPLETED|NO_SHOW|CANCELED`.
- Concurrency-safe scheduling and updates via `Mutex` atomic checks.
- Idempotency keys prevent duplicate log writes on double taps.
- Conflict detection respects time-off blocks and ignores canceled appointments.
- Immutable append-only audit records for status change/reschedule/cancel.

## Firestore artifacts
- Rules: `firestore/firestore.rules`
  - Tenant checks by `businessId`
  - Owner-only writes for protected collections
  - `appointmentAuditLogs` immutable (`update/delete` denied)
- Indexes + cost controls: `firestore/firestore.indexes.json`

## Tests
- RBAC route guard tests
- Conflict/idempotency/audit tests
- Delete account/close business policy tests

Run:

```bash
./gradlew test
```

> Secrets are intentionally omitted from source control.
