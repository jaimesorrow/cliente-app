---
name: cliente-ci-review
description: Reviews whether Clientè's CI configuration actually runs AppPolicyTests.kt — this repo currently has no .github/workflows directory or any other CI config at all, so nothing enforces the RBAC/conflict/audit/delete-flow invariants cliente-android-review documents. Use this instead of assuming "there are tests" means "tests run" for any change that adds or edits .github/workflows/**, app/build.gradle.kts test config, or AppPolicyTests.kt itself.
---

# Clientè CI-enforcement review

## Current state (verified, not inferred)
This repository has zero `.yml`/`.yaml` files and no `.github` directory anywhere in it — confirmed
directly (`find . -iname "*.yml" -o -iname "*.yaml"` and `find . -name .github` both return
nothing). There is no CI of any kind: no GitHub Actions workflow, no other provider's config.

`AppPolicyTests.kt` (`app/src/test/java/com/myclientscheduler/cliente/AppPolicyTests.kt`) is real,
and per `cliente-android-review` item 13 is the executable spec for RBAC route gating,
conflict-detector cancel-exclusion, audit append-only/idempotency, the delete-flow sole-owner path,
and back-stack sanitization. Today it only runs when a human types `./gradlew test` (or
`testDebugUnitTest`) locally. A regression in any of those five areas — including the already-known
inverted branch in `DeleteAccountUseCase` (item 9) — ships silently on every push/PR until someone
remembers to run it by hand.

## What to check in a diff
1. If a diff adds `.github/workflows/*.yml` (or any other CI config) for the first time, verify it
   actually invokes the JVM unit tests — `./gradlew test` or `./gradlew testDebugUnitTest` — and not
   only `./gradlew assembleDebug`/`build -x test`/`lint`. A workflow that compiles but never runs
   `AppPolicyTests.kt` produces the same green checkmark whether or not the five invariants above
   hold.
2. Check the workflow actually triggers on the branches/events this repo uses for review (at
   minimum `push` and `pull_request` against the real default branch) — a workflow scoped to a
   branch nobody pushes to is equivalent to no CI.
3. If a diff touches `app/build.gradle.kts`'s `testOptions` or test dependencies
   (`junit`, `truth`, `kotlinx-coroutines-test`) in a way that could make `AppPolicyTests.kt`
   silently no-op or stop compiling, flag it even when no workflow file changed in the same diff.
4. If a diff adds new tests to `AppPolicyTests.kt` without also adding/fixing CI, note that the new
   assertions carry the same "a human has to remember to run this" exposure — don't let "we added a
   test for that" read as "that's now enforced on every push."
5. Don't accept a PR description or commit message claiming "CI passes"/"tests pass" for this repo
   at face value — re-derive it from the actual workflow file's steps (or the absence of one).
