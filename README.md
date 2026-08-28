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

## Build setup

The app applies the `com.google.gms.google-services` plugin, so **the build
fails without `app/google-services.json`**:

```
Execution failed for task ':app:processDebugGoogleServices'.
> File google-services.json is missing.
```

That file is per-Firebase-project config, so it is not in the repo. Download it
for the `com.myclientscheduler.cliente` Android app from the Firebase console
(Project settings → Your apps) and put it at `app/google-services.json` before
building.

You also need an Android SDK; point the build at it with a `local.properties`
containing `sdk.dir=/path/to/android-sdk`.

## Run tests
```bash
./gradlew test
```
