package com.cliente.app.ui.navigation

import com.cliente.app.domain.model.Role

sealed class AppRoute(val route: String, val ownerOnly: Boolean = false) {
    data object Login : AppRoute("login")
    data object Legal : AppRoute("legal")
    data object Dashboard : AppRoute("dashboard")
    data object Clients : AppRoute("clients")
    data object Services : AppRoute("services", ownerOnly = true)
    data object Appointments : AppRoute("appointments")
    data object Calendar : AppRoute("calendar")
    data object Templates : AppRoute("templates")
    data object Availability : AppRoute("availability", ownerOnly = true)
    data object Analytics : AppRoute("analytics", ownerOnly = true)
    data object Settings : AppRoute("settings", ownerOnly = true)
    data object QaMatrix : AppRoute("qa-matrix")
}

object RouteGuard {
    fun canAccess(route: AppRoute, role: Role): Boolean = !route.ownerOnly || role == Role.OWNER
}
