---
name: android-compose
description: Reference for Clientè's actual Jetpack Compose file layout, composable naming/organization, state management, navigation, and testing conventions — as implemented in app/MainActivity.kt, app/MainViewModel.kt, and AppPolicyTests.kt. Use this before adding a new screen, route, or ViewModel state.
---

# Clientè Android/Compose conventions

## File structure
No `ui/screens/` package exists — every composable lives inline in one file,
`app/src/main/java/com/myclientscheduler/cliente/app/MainActivity.kt`, next to `MainActivity`
itself; `MainViewModel.kt` sits beside it in the same `app` package. Only theme is split out
(`ui/theme/Theme.kt`, `Typography.kt`). Follow this single-file pattern for a new screen unless the
file has grown enough to justify extracting a `ui/screens` package.

## Composable naming & organization
Composables are `private fun` in PascalCase (`HubScreen`, `BasicScreen`, `OwnerGuard`,
`LegalScreen`, `DebugQaScreen`), taking plain params rather than a whole ViewModel — e.g.
`HubScreen(title: String, routes: List<AppRoute>, nav: NavHostController, role: String)`. The one
exception is `DebugQaScreen(vm: MainViewModel)`, debug-only tooling — don't use it as the model for
a real screen's signature.

## State management
`MainViewModel` exposes `StateFlow` via private `MutableStateFlow` + public
`StateFlow`/`asStateFlow()` (`user`, `offline`, `qaFilter`, `qaCsv`); composables read it with
`collectAsStateWithLifecycle()`. Permission gating is computed inline at the call site
(`PermissionPolicy.canAccessRoute(AppRoute.Services, user)`), not hoisted into a derived property.

## Navigation
`SafeNavHost` in `MainActivity.kt` is the single `NavHost`, keyed on `AppRoute.path` constants from
`core/rbac/Permissions.kt`'s sealed `AppRoute` (`startDestination = AppRoute.Dashboard.path`). The
debug `QA` route is double-gated — once building `HubScreen`'s route list
(`if (BuildConfig.DEBUG) listOf(AppRoute.QA)`) and again in the `composable(AppRoute.QA.path)`
branch — keep both checks if you touch it.

## Testing patterns
No `androidTest`/Compose UI test exists. `AppPolicyTests.kt` (JVM, JUnit4) tests only the logic
layer (RBAC, `ConflictDetector`, `InMemoryRepository`, `DeleteAccountUseCase`,
`BackStackSanitizer`) via Truth's `assertThat`, backtick-quoted names, and `runBlocking` for suspend
calls. Pull a new screen's logic into a plain Kotlin class/use-case to keep it testable this way.
