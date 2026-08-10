# Clientè (Android MVP)

Provider-side luxury booking concierge app built with Kotlin + Jetpack Compose + Material3.

## Locked identity
- Product name: **Clientè** (single grave accent on final character only).
- `MyClientScheduler` appears only in legal/account metadata.

## Scope constraints
- No public client portal
- No self-booking UI
- No payments
- No in-app messaging (device call/text/email intents only)

## Architecture
- MVVM + Repository + clean package boundaries
- Room for local offline index/cache
- Firebase Auth + Firestore sync model
- Single-tenant membership per user (`businessId` exactly one)

## RBAC
- Roles: Owner, Staff
- Owner-only routes: `/availability`, `/analytics`, `/settings` and descendants
- Staff allowed: CRUD Clients/Appointments, view/apply Templates
- Staff blocked from Services/Templates edits/Availability/Analytics/Settings/Export/Team

## Firestore artifacts
See `firebaserules/firestore.rules` and `firebaserules/firestore.indexes.json`.

## Run tests
```bash
./gradlew test
```
