---
name: pr-writer
description: Writes pull request descriptions for this repo (Clientè) matching the structure and honesty conventions found in its actual closed PR history, rather than a generic template. Use whenever drafting a PR body for this repo.
---

# Clientè PR conventions

Grounded in this repo's real closed PRs (#1-#5): only #2 (initial scaffold), #4 (CI config), and
#5 (refactor) actually merged; #1 and #3 closed unmerged. Two body shapes recur:

## Shape A — Motivation/Description/Testing (PR #2, #3)
```
### Motivation
- Why, in product/security terms (e.g. "close a privilege-escalation path where...").
### Description
- One bullet per change, each naming the exact file in backticks
  (`DeleteAccountUseCase.kt`, `firebaserules/firestore.rules`) and the specific behavior change.
### Testing
- Name the exact command run (`./gradlew test`) and the honest result — several real PRs here
  admit the run failed or couldn't execute rather than claiming green tests. Never write
  "tests pass" without having actually run them.
```

## Shape B — Summary/findings (PR #5, the cleanest merged example)
```
## Summary
Numbered list of changes, each citing the file (`Repositories.kt`, `Permissions.kt`) and the
extracted concept (`isDuplicate()`, `isOwner()` extension).
## Findings not changed (noted for future work)
Real gaps noticed but out of scope here (e.g. "`AppointmentEntity` has no DAO").
```
Keep that last section — it's this repo's habit of surfacing latent bugs (the known
`DeleteAccountUseCase` inversion, unscoped `listClients()`) without silently hiding them.

## Titles
Inconsistent in history — `refactor: improve maintainability...` (colon-prefixed) vs. plain
imperative (`Add optimized CircleCI...`, `Scaffold Clientè Android MVP...`). Prefer the
colon-prefixed conventional-commit style (`fix:`, `refactor:`, `feat:`) — the more recent,
intentional choice (#5).

## Don't do
- Don't add a `[Codex Task]`-style footer link — an artifact of a different agent, not a convention.
- Don't claim a Firestore rules or RBAC change is safe without naming the specific rule block or
  `PermissionPolicy` function touched, as #3 did with `businessUsers/{uid}`.
