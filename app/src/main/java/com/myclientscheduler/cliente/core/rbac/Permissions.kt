package com.myclientscheduler.cliente.core.rbac

import com.myclientscheduler.cliente.core.model.Role
import com.myclientscheduler.cliente.core.model.UserContext

sealed class AppRoute(val path: String, val ownerOnly: Boolean) {
    data object Dashboard : AppRoute("dashboard", false)
    data object Clients : AppRoute("clients", false)
    data object Appointments : AppRoute("appointments", false)
    data object Calendar : AppRoute("calendar", false)
    data object Templates : AppRoute("templates", false)
    data object Services : AppRoute("services", true)
    data object Availability : AppRoute("availability", true)
    data object Analytics : AppRoute("analytics", true)
    data object Settings : AppRoute("settings", true)
    data object Legal : AppRoute("legal", false)
    data object QA : AppRoute("qa", false)
}

object PermissionPolicy {
    fun canAccessRoute(route: AppRoute, user: UserContext): Boolean =
        !route.ownerOnly || user.isOwner()

    fun canEditTemplates(user: UserContext): Boolean = user.isOwner()
    fun canEditServices(user: UserContext): Boolean = user.isOwner()
    fun canEditAvailability(user: UserContext): Boolean = user.isOwner()
    fun canViewAnalytics(user: UserContext): Boolean = user.isOwner()
    fun canEditSettings(user: UserContext): Boolean = user.isOwner()
    fun canManageClients(user: UserContext): Boolean = true
    fun canManageAppointments(user: UserContext): Boolean = true

    private fun UserContext.isOwner() = role == Role.OWNER
}
